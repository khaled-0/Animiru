package tachiyomi.domain.download.service

import tachiyomi.core.preference.PreferenceStore

class DownloadPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun downloadOnlyOverWifi() = preferenceStore.getBoolean(
        "pref_download_only_over_wifi_key",
        true,
    )

    fun useExternalDownloader() = preferenceStore.getBoolean("use_external_downloader", false)

    fun externalDownloaderSelection() = preferenceStore.getString(
        "external_downloader_selection",
        "",
    )

    fun autoDownloadWhileWatching() = preferenceStore.getInt("auto_download_while_watching", 0)

    fun removeAfterSeenSlots() = preferenceStore.getInt("remove_after_read_slots", -1)

    fun removeAfterMarkedAsSeen() = preferenceStore.getBoolean(
        "pref_remove_after_marked_as_read_key",
        false,
    )

    fun removeBookmarkedEpisodes() = preferenceStore.getBoolean("pref_remove_bookmarked", false)

    // AM (FILLER) -->
    fun notDownloadFillermarkedItems() = preferenceStore.getBoolean("pref_no_download_fillermarked", false)
    // <-- AM (FILLER)

    // AM (FILE-SIZE) -->
    fun showEpisodeFileSize() = preferenceStore.getBoolean("pref_show_downloaded_episode_size", true)
    // <-- AM (FILE-SIZE)

    fun removeExcludeAnimeCategories() = preferenceStore.getStringSet(
        "remove_exclude_anime_categories",
        emptySet(),
    )

    fun downloadNewEpisodes() = preferenceStore.getBoolean("download_new_episode", false)

    fun downloadNewEpisodeCategories() = preferenceStore.getStringSet(
        "download_new_anime_categories",
        emptySet(),
    )

    fun downloadNewEpisodeCategoriesExclude() = preferenceStore.getStringSet(
        "download_new_anime_categories_exclude",
        emptySet(),
    )

    fun numberOfDownloads() = preferenceStore.getInt("download_slots", 1)
}
