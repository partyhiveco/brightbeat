package com.example.vosclone.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Thin wrapper around ExoPlayer used as the single source of truth for
 * "what time is it in the song right now". The note-fall animation and
 * hit-judgement both read [currentPositionMs] each frame so that visuals
 * stay locked to actual audio playback rather than to wall-clock time
 * (which drifts due to audio buffering / decode latency).
 */
class AudioEngine(context: Context) {

    private val player: ExoPlayer = ExoPlayer.Builder(context).build()

    fun loadAssetTrack(assetPath: String) {
        val uri = Uri.parse("asset:///$assetPath")
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    fun loadUriTrack(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    fun play() = player.play()
    fun pause() = player.pause()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    /**
     * Changes the song tempo while keeping the note chart on the same audio
     * clock. Beginner mode uses this instead of retiming individual notes, so
     * vocals, falling notes, and judgement remain locked together.
     */
    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed.coerceIn(0.5f, 2f))
    }

    /** Current playback position in the song, in milliseconds. */
    fun currentPositionMs(): Long = player.currentPosition

    /**
     * Duration reported by ExoPlayer after preparation, or 0 while it is still
     * resolving the media. Used to detect charts that stop before the audio.
     */
    fun durationMs(): Long = player.duration.takeUnless { it == C.TIME_UNSET || it <= 0L } ?: 0L

    fun isPlaying(): Boolean = player.isPlaying

    /** True only when ExoPlayer has reached the end of the loaded track. */
    fun isEnded(): Boolean = player.playbackState == Player.STATE_ENDED

    fun release() = player.release()
}
