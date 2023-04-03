package eu.kanade.tachiyomi.ui.recent.animeupdates

import android.view.View
import androidx.core.view.isVisible
import coil.dispose
import coil.load
import eu.kanade.tachiyomi.animesource.LocalAnimeSource
import eu.kanade.tachiyomi.databinding.AnimeUpdatesItemBinding
import eu.kanade.tachiyomi.ui.anime.episode.base.BaseEpisodeHolder

/**
 * Holder that contains episode item
 * UI related actions should be called from here.
 *
 * @param view the inflated view for this holder.
 * @param adapter the adapter handling this holder.
 * @constructor creates a new recent episode holder.
 */
class AnimeUpdatesHolder(private val view: View, private val adapter: AnimeUpdatesAdapter) :
    BaseEpisodeHolder(view, adapter) {

    private val binding = AnimeUpdatesItemBinding.bind(view)

    init {
        binding.mangaCover.setOnClickListener {
            adapter.coverClickListener.onCoverClick(bindingAdapterPosition)
        }

        binding.download.listener = downloadActionListener
    }

    fun bind(item: AnimeUpdatesItem) {
        // Set episode title
        binding.chapterTitle.text = item.episode.name

        // Set anime title
        binding.mangaTitle.text = item.anime.title

        // Check if episode is seen and/or bookmarked and/or fillermarked and set correct color
        if (item.episode.seen) {
            binding.chapterTitle.setTextColor(adapter.seenColor)
            binding.mangaTitle.setTextColor(adapter.seenColor)
        } else {
            binding.mangaTitle.setTextColor(adapter.unseenColor)
            binding.chapterTitle.setTextColor(
                if (item.episode.bookmark) {
                    adapter.bookmarkedColor
                } else {
                    if (item.episode.fillermark) {
                        adapter.fillermarkedColor
                    } else {
                        adapter.unseenColorSecondary
                    }
                },
            )
        }

        // Set bookmark status
        binding.bookmarkIcon.isVisible = item.episode.bookmark

        // Set fillermark status
        binding.fillermarkIcon.isVisible = item.episode.fillermark

        // Set episode status
        binding.download.isVisible = item.anime.source != LocalAnimeSource.ID
        binding.download.setState(item.status, item.progress) // TODO: downloadedEpisodeFileSizeMb
        // FIXME : State doesn't apply for the first time, until scrolled/redrawn

        // Set cover
        binding.mangaCover.dispose()
        binding.mangaCover.load(item.anime)
    }
}
