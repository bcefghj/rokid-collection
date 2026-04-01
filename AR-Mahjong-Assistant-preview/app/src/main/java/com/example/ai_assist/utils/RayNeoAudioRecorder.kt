package com.example.ai_assist.utils

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class RayNeoAudioRecorder(
    context: Context,
    private val onChunkReady: (File) -> Unit
) {
    private val context = context.applicationContext
    private var audioRecord: AudioRecord? = null
    private var audioManager: AudioManager? = null
    private val isRecording = AtomicBoolean(false)
    private var recordingThread: Thread? = null

    // Config
    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    // 10 seconds * 16000 samples/sec * 2 bytes/sample = 320,000 bytes
    private val CHUNK_SIZE_BYTES = 10 * SAMPLE_RATE * 2
    
    // Buffer for 10 seconds of audio
    private val audioBuffer = ByteBuffer.allocate(CHUNK_SIZE_BYTES)

    fun start() {
        if (isRecording.get()) return

        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // Reference: ExampleRecordTranslation.java
        audioManager?.setParameters("audio_source_record=record_origin3")

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        // Ensure buffer is large enough for efficient reading
        val bufferSize = maxOf(minBufferSize, 4096)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            // Set preferred device (ID 23 or name matching RayNeo)
            val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_INPUTS)
            var targetDevice: AudioDeviceInfo? = null
            
            // Try to find RayNeo specific device first (ID 23 is common for RayNeo X2, but name check is safer)
            targetDevice = devices?.find { device ->
                 (device.type == AudioDeviceInfo.TYPE_BUILTIN_MIC || device.type == AudioDeviceInfo.TYPE_USB_HEADSET) && 
                 (device.id == 23 || device.productName.contains("RayNeo", ignoreCase = true))
            }
            
            if (targetDevice != null) {
                Log.i("RayNeoAudioRecorder", "Using preferred device: ${targetDevice.productName} (ID: ${targetDevice.id})")
                audioRecord?.preferredDevice = targetDevice
            } else {
                Log.w("RayNeoAudioRecorder", "RayNeo microphone not found. Using default device.")
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("RayNeoAudioRecorder", "AudioRecord init failed")
                return
            }

            audioRecord?.startRecording()
            isRecording.set(true)
            audioBuffer.clear()

            recordingThread = Thread {
                readAudioLoop(bufferSize)
            }
            recordingThread?.start()

        } catch (e: Exception) {
            Log.e("RayNeoAudioRecorder", "Start failed", e)
            stop()
        }
    }

    private fun readAudioLoop(readBufferSize: Int) {
        val readBuffer = ByteArray(readBufferSize)
        
        while (isRecording.get()) {
            val ret = audioRecord?.read(readBuffer, 0, readBufferSize) ?: -1
            
            if (ret < 0) {
                Log.e("RayNeoAudioRecorder", "Audio read error: $ret")
                if (ret == AudioRecord.ERROR_DEAD_OBJECT || ret == AudioRecord.ERROR_INVALID_OPERATION) {
                    break
                }
                continue
            }

            if (ret > 0) {
                val remaining = audioBuffer.remaining()
                if (ret <= remaining) {
                    audioBuffer.put(readBuffer, 0, ret)
                    // If buffer is exactly full, flush it
                    if (!audioBuffer.hasRemaining()) {
                        flushBufferToFile()
                    }
                } else {
                    // Buffer will overflow. Split the data.
                    // 1. Fill the rest of current buffer
                    audioBuffer.put(readBuffer, 0, remaining)
                    flushBufferToFile()
                    
                    // 2. Put the overflow into the new (cleared) buffer
                    val overflow = ret - remaining
                    // Check if overflow itself is larger than buffer (unlikely given buffer sizes, but good for safety)
                    if (overflow > audioBuffer.capacity()) {
                         Log.e("RayNeoAudioRecorder", "Read chunk larger than entire buffer! Dropping data.")
                         audioBuffer.put(readBuffer, remaining, audioBuffer.capacity())
                    } else {
                        audioBuffer.put(readBuffer, remaining, overflow)
                    }
                }
            }
        }
    }

    private fun flushBufferToFile() {
        if (audioBuffer.position() == 0) return

        val data = ByteArray(audioBuffer.position())
        audioBuffer.flip()
        audioBuffer.get(data)
        audioBuffer.clear()

        try {
            val fileName = "audio_${SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())}.wav"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            FileOutputStream(file).use { fos ->
                writeWavHeader(fos, data.size)
                fos.write(data)
            }
            
            onChunkReady(file)
        } catch (e: Exception) {
            Log.e("RayNeoAudioRecorder", "Write file failed", e)
        }
    }

    fun stop() {
        isRecording.set(false)
        try {
            // Stop recording first to force read() to return/throw, preventing thread block
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }

            recordingThread?.join(1000)
            
            audioRecord?.release()
            audioRecord = null
            
            audioManager?.setParameters("audio_source_record=off")
        } catch (e: Exception) {
            Log.e("RayNeoAudioRecorder", "Stop failed", e)
        }
    }
    
    private fun writeWavHeader(out: FileOutputStream, totalAudioLen: Int) {
        val totalDataLen = totalAudioLen + 36
        val longSampleRate = SAMPLE_RATE.toLong()
        val channels = 1
        val byteRate = SAMPLE_RATE * 2 * channels
        
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = ((longSampleRate shr 8) and 0xff).toByte()
        header[26] = ((longSampleRate shr 16) and 0xff).toByte()
        header[27] = ((longSampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (2 * 1).toByte() // block align
        header[33] = 0
        header[34] = 16
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()
        
        out.write(header, 0, 44)
    }
}
