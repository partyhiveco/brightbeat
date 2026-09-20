package com.example.vosclone.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AudioMetadata(
    val title: String?,
    val artist: String?,
    val durationMs: Long?,
    val embeddedCover: ByteArray?
)

object AudioMetadataLoader {
    suspend fun loadAll(context: Context, audioFiles: List<String>): Map<String, AudioMetadata> =
        withContext(Dispatchers.IO) {
            audioFiles.associateWith { audioFile -> loadOne(context, audioFile) }
        }

    private fun loadOne(context: Context, audioFile: String): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            context.assets.openFd("audio/$audioFile").use { descriptor ->
                retriever.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
            }
            AudioMetadata(
                title = cleanTitle(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)),
                artist = cleanValue(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                embeddedCover = retriever.embeddedPicture
            )
        } catch (_: Exception) {
            AudioMetadata(title = null, artist = null, durationMs = null, embeddedCover = null)
        } finally {
            retriever.release()
        }
    }

    private fun cleanTitle(raw: String?): String? = cleanValue(raw)
        ?.replace(Regex("^\\s*\\d+\\s*[.\\-_)]\\s*"), "")
        ?.takeUnless { it.isBlank() }

    private fun cleanValue(raw: String?): String? = raw?.trim()?.takeUnless { it.isBlank() }
}
