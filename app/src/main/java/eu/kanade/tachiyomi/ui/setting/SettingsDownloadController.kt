package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import androidx.core.net.toUri
import androidx.core.text.buildSpannedString
import androidx.preference.PreferenceScreen
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hippo.unifile.UniFile
import eu.kanade.domain.category.interactor.GetCategoriesAnime
import eu.kanade.domain.category.model.Category
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.preference.bindTo
import eu.kanade.tachiyomi.util.preference.defaultValue
import eu.kanade.tachiyomi.util.preference.entriesRes
import eu.kanade.tachiyomi.util.preference.intListPreference
import eu.kanade.tachiyomi.util.preference.listPreference
import eu.kanade.tachiyomi.util.preference.multiSelectListPreference
import eu.kanade.tachiyomi.util.preference.onClick
import eu.kanade.tachiyomi.util.preference.preference
import eu.kanade.tachiyomi.util.preference.preferenceCategory
import eu.kanade.tachiyomi.util.preference.switchPreference
import eu.kanade.tachiyomi.util.preference.titleRes
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.widget.materialdialogs.QuadStateTextView
import eu.kanade.tachiyomi.widget.materialdialogs.setQuadStateMultiChoiceItems
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.File
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsDownloadController : SettingsController() {

    private val getCategoriesAnime: GetCategoriesAnime by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.pref_category_downloads

        val dbAnimeCategories = runBlocking { getCategoriesAnime.await() }
        val animeCategories = listOf(Category.default(context)) + dbAnimeCategories

        preference {
            bindTo(preferences.downloadsDirectory())
            titleRes = R.string.pref_download_directory
            onClick {
                val ctrl = DownloadDirectoriesDialog()
                ctrl.targetController = this@SettingsDownloadController
                ctrl.showDialog(router)
            }

            preferences.downloadsDirectory().asFlow()
                .onEach { path ->
                    val dir = UniFile.fromUri(context, path.toUri())
                    summary = dir.filePath ?: path
                }
                .launchIn(viewScope)
        }
        switchPreference {
            key = Keys.downloadOnlyOverWifi
            titleRes = R.string.connected_to_wifi
            defaultValue = true
        }

        // AM -->
        switchPreference {
            key = Keys.showDownloadedEpisodeSize
            titleRes = R.string.show_downloaded_episode_size
            defaultValue = true
        }
        // AM  <--

        preferenceCategory {
            titleRes = R.string.pref_category_delete_episodes

            switchPreference {
                key = Keys.removeAfterMarkedAsSeen
                titleRes = R.string.pref_remove_after_marked_as_seen
                defaultValue = false
            }
            switchPreference {
                key = Keys.removeBookmarkedChapters
                titleRes = R.string.pref_remove_bookmarked_episodes
                defaultValue = false
            }
            intListPreference {
                key = Keys.removeAfterSeenSlots
                titleRes = R.string.pref_remove_after_seen
                entriesRes = arrayOf(
                    R.string.disabled,
                    R.string.last_seen_episode,
                    R.string.second_to_last,
                    R.string.third_to_last,
                    R.string.fourth_to_last,
                    R.string.fifth_to_last,
                )
                entryValues = arrayOf("-1", "0", "1", "2", "3", "4")
                defaultValue = "-1"
                summary = "%s"
            }
            multiSelectListPreference {
                bindTo(preferences.removeExcludeAnimeCategories())
                titleRes = R.string.pref_remove_exclude_categories
                entries = animeCategories.map { it.name }.toTypedArray()
                entryValues = animeCategories.map { it.id.toString() }.toTypedArray()

                preferences.removeExcludeAnimeCategories().asFlow()
                    .onEach { mutable ->
                        val selected = mutable
                            .mapNotNull { id -> animeCategories.find { it.id == id.toLong() } }
                            .sortedBy { it.order }

                        summary = if (selected.isEmpty()) {
                            resources?.getString(R.string.none)
                        } else {
                            selected.joinToString { it.name }
                        }
                    }.launchIn(viewScope)
            }
        }

        preferenceCategory {
            titleRes = R.string.pref_category_auto_download

            switchPreference {
                bindTo(preferences.downloadNewEpisode())
                titleRes = R.string.pref_download_new
            }
            preference {
                bindTo(preferences.downloadNewEpisodeCategories())
                titleRes = R.string.categories
                onClick {
                    DownloadAnimeCategoriesDialog().showDialog(router)
                }

                visibleIf(preferences.downloadNewEpisode()) { it }

                fun updateSummary() {
                    val selectedCategories = preferences.downloadNewEpisodeCategories().get()
                        .mapNotNull { id -> animeCategories.find { it.id == id.toLong() } }
                        .sortedBy { it.order }
                    val includedItemsText = if (selectedCategories.isEmpty()) {
                        context.getString(R.string.all)
                    } else {
                        selectedCategories.joinToString { it.name }
                    }

                    val excludedCategories = preferences.downloadNewEpisodeCategoriesExclude().get()
                        .mapNotNull { id -> animeCategories.find { it.id == id.toLong() } }
                        .sortedBy { it.order }
                    val excludedItemsText = if (excludedCategories.isEmpty()) {
                        context.getString(R.string.none)
                    } else {
                        excludedCategories.joinToString { it.name }
                    }

                    summary = buildSpannedString {
                        append(context.getString(R.string.include, includedItemsText))
                        appendLine()
                        append(context.getString(R.string.exclude, excludedItemsText))
                    }
                }

                preferences.downloadNewEpisodeCategories().asFlow()
                    .onEach { updateSummary() }
                    .launchIn(viewScope)
                preferences.downloadNewEpisodeCategoriesExclude().asFlow()
                    .onEach { updateSummary() }
                    .launchIn(viewScope)
            }
            // AM -->
            intListPreference {
                key = Keys.downloadAfterSeenSlots
                titleRes = R.string.pref_download_after_seen
                entriesRes = arrayOf(
                    R.string.disabled,
                    R.string.one_episode_ahead,
                    R.string.two_episodes_ahead,
                    R.string.three_episodes_ahead,
                    R.string.four_episodes_ahead,
                    R.string.five_episodes_ahead,
                )
                entryValues = arrayOf("0", "1", "2", "3", "4", "5")
                defaultValue = "0"
                summary = "%s"
            }
            intListPreference {
                bindTo(preferences.downloadOnAddingToLibrary())
                titleRes = R.string.pref_download_on_add_library
                entriesRes = arrayOf(
                    R.string.never_add_library_download,
                    R.string.prompt_add_library_download,
                    R.string.always_add_library_download,
                )
                entryValues = arrayOf("0", "1", "2")
                defaultValue = "1"
                summary = context.resources.getString(R.string.pref_download_on_add_library_summary)
                visibleIf(preferences.downloadAfterSeenSlots()) { it != 0 }
            }
            // AM <--
        }
        preferenceCategory {
            titleRes = R.string.pref_category_external_downloader
            switchPreference {
                key = Keys.useExternalDownloader
                titleRes = R.string.pref_use_external_downloader
                defaultValue = false
            }

            listPreference {
                key = Keys.externalDownloaderSelection
                titleRes = R.string.pref_external_downloader_selection

                val pm = context.packageManager
                val installedPackages = pm.getInstalledPackages(0)
                val supportedDownloaders = installedPackages.filter {
                    when (it.packageName) {
                        "idm.internet.download.manager" -> true
                        "idm.internet.download.manager.plus" -> true
                        "idm.internet.download.manager.lite" -> true
                        else -> false
                    }
                }
                val packageNames = supportedDownloaders.map { it.packageName }
                val packageNamesReadable = supportedDownloaders
                    .map { pm.getApplicationLabel(it.applicationInfo).toString() }

                entries = arrayOf("None") + packageNamesReadable.toTypedArray()
                entryValues = arrayOf("") + packageNames.toTypedArray()
                defaultValue = ""

                summary = "%s"
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            DOWNLOAD_DIR -> if (data != null && resultCode == Activity.RESULT_OK) {
                val context = applicationContext ?: return
                val uri = data.data
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                if (uri != null) {
                    @Suppress("NewApi")
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                }

                val file = UniFile.fromUri(context, uri)
                preferences.downloadsDirectory().set(file.uri.toString())
            }
        }
    }

    fun predefinedDirectorySelected(selectedDir: String) {
        val path = File(selectedDir).toUri()
        preferences.downloadsDirectory().set(path.toString())
    }

    fun customDirectorySelected() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        try {
            startActivityForResult(intent, DOWNLOAD_DIR)
        } catch (e: ActivityNotFoundException) {
            activity?.toast(R.string.file_picker_error)
        }
    }

    class DownloadDirectoriesDialog : DialogController() {

        private val preferences: PreferencesHelper = Injekt.get()

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val activity = activity!!
            val currentDir = preferences.downloadsDirectory().get()
            val externalDirs = listOf(getDefaultDownloadDir(), File(activity.getString(R.string.custom_dir))).map(File::toString)
            var selectedIndex = externalDirs.indexOfFirst { it in currentDir }

            return MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.pref_download_directory)
                .setSingleChoiceItems(externalDirs.toTypedArray(), selectedIndex) { _, which ->
                    selectedIndex = which
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val target = targetController as? SettingsDownloadController
                    if (selectedIndex == externalDirs.lastIndex) {
                        target?.customDirectorySelected()
                    } else {
                        target?.predefinedDirectorySelected(externalDirs[selectedIndex])
                    }
                }
                .create()
        }

        private fun getDefaultDownloadDir(): File {
            val defaultDir = Environment.getExternalStorageDirectory().absolutePath +
                File.separator + resources?.getString(R.string.app_name) +
                File.separator + "downloads"

            return File(defaultDir)
        }
    }

    class DownloadAnimeCategoriesDialog : DialogController() {

        private val preferences: PreferencesHelper = Injekt.get()
        private val getCategories: GetCategoriesAnime = Injekt.get()

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val dbCategories = runBlocking { getCategories.await() }
            val categories = listOf(Category.default(activity!!)) + dbCategories

            val items = categories.map { it.name }
            var selected = categories
                .map {
                    when (it.id.toString()) {
                        in preferences.downloadNewEpisodeCategories().get() -> QuadStateTextView.State.CHECKED.ordinal
                        in preferences.downloadNewEpisodeCategoriesExclude().get() -> QuadStateTextView.State.INVERSED.ordinal
                        else -> QuadStateTextView.State.UNCHECKED.ordinal
                    }
                }
                .toIntArray()

            return MaterialAlertDialogBuilder(activity!!)
                .setTitle(R.string.categories)
                .setQuadStateMultiChoiceItems(
                    message = R.string.pref_download_new_anime_categories_details,
                    items = items,
                    initialSelected = selected,
                ) { selections ->
                    selected = selections
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val included = selected
                        .mapIndexed { index, value -> if (value == QuadStateTextView.State.CHECKED.ordinal) index else null }
                        .filterNotNull()
                        .map { categories[it].id.toString() }
                        .toSet()
                    val excluded = selected
                        .mapIndexed { index, value -> if (value == QuadStateTextView.State.INVERSED.ordinal) index else null }
                        .filterNotNull()
                        .map { categories[it].id.toString() }
                        .toSet()

                    preferences.downloadNewEpisodeCategories().set(included)
                    preferences.downloadNewEpisodeCategoriesExclude().set(excluded)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        }
    }
}

private const val DOWNLOAD_DIR = 104
