package com.example.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.sin

object SoundSynth {
    private const val SAMPLE_RATE = 22050

    fun playTone(frequency: Double, durationMs: Int, waveType: String = "sine") {
        Thread {
            try {
                val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val angle = 2.0 * Math.PI * frequency * t
                    val sample = when (waveType) {
                        "square" -> {
                            if (sin(angle) >= 0) 20000.0 else -20000.0
                        }
                        "triangle" -> {
                            val saw = (angle % (2.0 * Math.PI)) / (2.0 * Math.PI)
                            val tri = 2.0 * kotlin.math.abs(2.0 * saw - 1.0) - 1.0
                            tri * 25000.0
                        }
                        else -> {
                            sin(angle) * 28000.0
                        }
                    }
                    
                    // Simple envelope fade factors to remove "clicking" click pop at start/end
                    val fadeFactor = if (i > numSamples - 1000) {
                        (numSamples - i).toDouble() / 1000.0
                    } else if (i < 400) {
                        i.toDouble() / 400.0
                    } else {
                        1.0
                    }
                    buffer[i] = (sample * fadeFactor).toInt().toShort()
                }

                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffer.size * 2,
                    AudioTrack.MODE_STATIC
                )

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                Thread.sleep(durationMs.toLong() + 30)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun playVictory() {
        Thread {
            try {
                playTone(440.0, 90, "sine")
                Thread.sleep(100)
                playTone(554.37, 90, "sine")
                Thread.sleep(100)
                playTone(659.25, 90, "sine")
                Thread.sleep(100)
                playTone(880.0, 250, "triangle")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun playDefeat() {
        Thread {
            try {
                playTone(261.63, 150, "sine")
                Thread.sleep(160)
                playTone(246.94, 150, "sine")
                Thread.sleep(160)
                playTone(196.00, 300, "square")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun playClickX() {
        playTone(659.25, 60, "triangle")
    }

    fun playClickO() {
        playTone(523.25, 60, "triangle")
    }

    fun playEmoji() {
        playTone(880.0, 45, "sine")
    }
}
