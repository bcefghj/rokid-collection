package com.rokid.wificonnect.service

import android.content.res.AssetManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.k2fsa.sherpa.onnx.*

class VoiceRecognizer(private val assetManager: AssetManager) {

    companion object {
        private const val TAG = "VoiceRecognizer"
        private const val SAMPLE_RATE = 16000
    }

    interface Listener {
        fun onPartial(text: String)
        fun onResult(text: String)
        fun onError(message: String)
    }

    private var recognizer: OnlineRecognizer? = null
    private var audioRecord: AudioRecord? = null
    private var recognizeThread: Thread? = null
    @Volatile
    private var isRunning = false

    fun init(): Boolean {
        return try {
            val config = OnlineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
                modelConfig = OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = "sherpa/encoder-epoch-99-avg-1.int8.onnx",
                        decoder = "sherpa/decoder-epoch-99-avg-1.int8.onnx",
                        joiner = "sherpa/joiner-epoch-99-avg-1.int8.onnx",
                    ),
                    tokens = "sherpa/tokens.txt",
                    numThreads = 2,
                    debug = false,
                    modelType = "zipformer",
                ),
                endpointConfig = EndpointConfig(
                    rule1 = EndpointRule(false, 2.4f, 0.0f),
                    rule2 = EndpointRule(true, 1.4f, 0.0f),
                    rule3 = EndpointRule(false, 0.0f, 20.0f),
                ),
                enableEndpoint = true,
                decodingMethod = "greedy_search",
            )
            recognizer = OnlineRecognizer(assetManager = assetManager, config = config)
            Log.d(TAG, "Sherpa-onnx initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Init failed", e)
            false
        }
    }

    fun start(listener: Listener) {
        if (isRunning) return
        val rec = recognizer ?: run {
            listener.onError("识别器未初始化")
            return
        }
        isRunning = true

        recognizeThread = Thread {
            var ar: AudioRecord? = null
            var stream: OnlineStream? = null
            try {
                val bufSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
                )
                ar = AudioRecord(
                    MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    bufSize * 2
                )
                audioRecord = ar

                if (ar.state != AudioRecord.STATE_INITIALIZED) {
                    listener.onError("麦克风初始化失败")
                    isRunning = false
                    return@Thread
                }

                ar.startRecording()
                Log.d(TAG, "Recording started")

                stream = rec.createStream()
                val interval = 0.1
                val readSize = (interval * SAMPLE_RATE).toInt()
                val buf = ShortArray(readSize)
                var lastText = ""

                while (isRunning) {
                    val ret = ar.read(buf, 0, buf.size)
                    if (ret > 0) {
                        val samples = FloatArray(ret) { buf[it] / 32768.0f }
                        stream.acceptWaveform(samples, sampleRate = SAMPLE_RATE)

                        while (rec.isReady(stream)) {
                            rec.decode(stream)
                        }

                        val isEndpoint = rec.isEndpoint(stream)
                        val text = rec.getResult(stream).text.trim()

                        if (text.isNotEmpty() && text != lastText) {
                            lastText = text
                            listener.onPartial(text)
                        }

                        if (isEndpoint && text.isNotEmpty()) {
                            Log.d(TAG, "Result: $text")
                            listener.onResult(text)
                            rec.reset(stream)
                            break
                        }

                        if (isEndpoint) {
                            rec.reset(stream)
                        }
                    } else if (ret < 0) {
                        Log.e(TAG, "AudioRecord read error: $ret")
                        listener.onError("麦克风读取失败")
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Recognition error", e)
                listener.onError("语音识别出错: ${e.message}")
            } finally {
                stream?.release()
                try { ar?.stop() } catch (_: Exception) {}
                try { ar?.release() } catch (_: Exception) {}
                audioRecord = null
                isRunning = false
            }
        }
        recognizeThread?.start()
    }

    fun stop() {
        isRunning = false
        try { audioRecord?.stop() } catch (_: Exception) {}
        recognizeThread?.join(2000)
        recognizeThread = null
    }

    fun release() {
        stop()
        recognizer?.release()
        recognizer = null
    }
}
