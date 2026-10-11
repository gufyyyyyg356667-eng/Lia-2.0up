package com.Lia.assistant.voice

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AudioEffect
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import kotlin.math.sqrt

/** Microphone capture (16 kHz) and speaker playback (24 kHz PCM16). Audio path has no logging. */
class LiveAudioIO(
    private val onMicChunk: (ByteArray) -> Unit,
    private val onMicAmplitude: (Float) -> Unit = {},
) {
    private var recorder: AudioRecord? = null
    private var recordThread: Thread? = null
    private val effects = mutableListOf<AudioEffect>()
    @Volatile private var running = false
    private var track: AudioTrack? = null

    fun isMicRunning(): Boolean = recorder != null

    @SuppressLint("MissingPermission")
    @Synchronized
    fun startMic() {
        if (recorder != null) return
        val minBuf = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, 4096)
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            throw IllegalStateException("Microphone unavailable")
        }
        attachEffects(rec.audioSessionId)
        recorder = rec
        running = true
        rec.startRecording()
        recordThread = Thread({
            val buf = ByteArray(2048)
            while (running) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) {
                    val chunk = buf.copyOf(n)
                    onMicAmplitude(PcmMath.rms(chunk))
                    onMicChunk(chunk)
                }
            }
        }, "lia-mic").also { it.start() }
    }

    @Synchronized
    fun stopMic() {
        val rec = recorder ?: return
        running = false
        try { rec.stop() } catch (_: IllegalStateException) { }
        recordThread?.join(300)
        recordThread = null
        rec.release()
        recorder = null
        effects.forEach { it.release() }
        effects.clear()
    }

    @Synchronized
    fun playChunk(pcm: ByteArray): Float {
        val t = track ?: createTrack().also { track = it }
        t.write(pcm, 0, pcm.size)
        return PcmMath.rms(pcm)
    }

    @Synchronized
    fun flushPlayback() {
        track?.let {
            it.pause()
            it.flush()
            it.play()
        }
    }

    fun setVolume(volume: Float) {
        track?.setVolume(volume.coerceIn(0f, 1f))
    }

    @Synchronized
    fun release() {
        stopMic()
        track?.let {
            try { it.stop() } catch (_: IllegalStateException) { }
            it.release()
        }
        track = null
    }

    private fun createTrack(): AudioTrack {
        val minBuf = AudioTrack.getMinBufferSize(24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(24000)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .also { it.play() }
    }

    private fun attachEffects(sessionId: Int) {
        if (AcousticEchoCanceler.isAvailable()) {
            runCatching { AcousticEchoCanceler.create(sessionId)?.also { it.enabled = true; effects += it } }
        }
        if (NoiseSuppressor.isAvailable()) {
            runCatching { NoiseSuppressor.create(sessionId)?.also { it.enabled = true; effects += it } }
        }
        if (AutomaticGainControl.isAvailable()) {
            runCatching { AutomaticGainControl.create(sessionId)?.also { it.enabled = true; effects += it } }
        }
    }
}

object PcmMath {
    /** RMS loudness of little-endian PCM16, scaled to 0..1. */
    fun rms(pcm: ByteArray): Float {
        var sum = 0.0
        var count = 0
        var i = 0
        while (i + 1 < pcm.size) {
            val s = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xFF)).toShort().toInt()
            sum += s.toDouble() * s
            count++
            i += 2
        }
        if (count == 0) return 0f
        return (sqrt(sum / count) / 32768.0).coerceIn(0.0, 1.0).toFloat()
    }
}
