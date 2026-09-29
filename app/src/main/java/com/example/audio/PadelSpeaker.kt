package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.io.File
import java.util.Locale

class PadelSpeaker(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private var isInitialized = false
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    // Голосовой сервис Алисы (Yandex SpeechKit с оффлайн-кэшированием)
    val aliceTts = YandexAliceTtsService(appContext)

    // Текущий плеер воспроизведения оффлайн-аудио Алисы
    private var activeMediaPlayer: MediaPlayer? = null

    var isVoiceEnabled: Boolean = true
    var speechRate: Float = 1.05f // Чуть бодрее для спортивного корта
    var announceSpeed: Boolean = true
    var useAliceVoice: Boolean = true // Флаг предпочтения фирменного голоса SpeechKit
    var speechKitVoice: SpeechKitVoice
        get() = aliceTts.currentVoice
        set(value) {
            aliceTts.currentVoice = value
        }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                // Пытаемся выставить русский язык
                val russian = Locale("ru", "RU")
                val langResult = engine.setLanguage(russian)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w("PadelSpeaker", "Russian TTS not supported, falling back to default locale")
                    engine.language = Locale.getDefault()
                }

                // Настраиваем вывод в медиапоток (STREAM_MUSIC), чтобы звук гарантированно шёл на Bluetooth колонку!
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                engine.setAudioAttributes(audioAttributes)
                engine.setSpeechRate(speechRate)

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d("PadelSpeaker", "Started speaking: $utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d("PadelSpeaker", "Done speaking: $utteranceId")
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e("PadelSpeaker", "TTS error on: $utteranceId")
                    }
                })

                isInitialized = true
                Log.d("PadelSpeaker", "TextToSpeech initialized successfully for Padel Court audio")
            }
        } else {
            Log.e("PadelSpeaker", "TextToSpeech init failed with status: $status")
        }
    }

    /**
     * Озвучивает текст громко и четко.
     * Приоритет:
     * 1. Если включен режим Алисы -> воспроизводит из локального оффлайн-кэша или синтезирует через SpeechKit.
     * 2. Если сеть недоступна или произошла ошибка -> мгновенный фоллбэк на встроенный локальный Android TTS.
     */
    fun speak(text: String, flushQueue: Boolean = true) {
        if (!isVoiceEnabled || text.isBlank()) return

        requestAudioFocus()

        if (flushQueue) {
            stopCurrentAudio()
        }

        if (useAliceVoice) {
            aliceTts.synthesizeAliceVoice(
                textToSpeak = text,
                onSuccess = { audioFile ->
                    mainHandler.post {
                        playCachedAudio(audioFile)
                    }
                },
                onFailure = { error ->
                    Log.w("PadelSpeaker", "SpeechKit недоступен (${error.message}), переключаемся на офлайн TTS Android")
                    mainHandler.post {
                        speakViaNativeTts(text, flushQueue)
                    }
                }
            )
        } else {
            speakViaNativeTts(text, flushQueue)
        }
    }

    private fun playCachedAudio(audioFile: File) {
        try {
            stopCurrentAudio()
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(audioFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    it.release()
                    if (activeMediaPlayer == it) {
                        activeMediaPlayer = null
                    }
                }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    if (activeMediaPlayer == mp) {
                        activeMediaPlayer = null
                    }
                    false
                }
            }
            activeMediaPlayer = player
        } catch (e: Exception) {
            Log.e("PadelSpeaker", "Ошибка воспроизведения аудиофайла Алисы", e)
        }
    }

    private fun speakViaNativeTts(text: String, flushQueue: Boolean) {
        if (!isInitialized) return
        val queueMode = if (flushQueue) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = "padel_announcement_${System.currentTimeMillis()}"
        tts?.speak(text, queueMode, null, utteranceId)
    }

    private fun stopCurrentAudio() {
        try {
            activeMediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {}
        activeMediaPlayer = null
        tts?.stop()
    }

    /**
     * Специальное объявление счета
     */
    fun speakScore(scoreAnnouncement: String) {
        speak(scoreAnnouncement, flushQueue = true)
    }

    /**
     * Объявление скорости сильного удара (смэш, виннер)
     */
    fun speakWinnerSpeed(speedKmh: Float, shotName: String = "Удар") {
        if (!announceSpeed || speedKmh < 80f) return
        val speedInt = speedKmh.toInt()
        val text = "$shotName, $speedInt километров в час!"
        speak(text, flushQueue = false)
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d("PadelSpeaker", "Audio focus changed: $focusChange")
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .setAcceptsDelayedFocusGain(false)
                    .build()
                audioManager?.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            Log.e("PadelSpeaker", "Error requesting audio focus", e)
        }
    }

    fun shutdown() {
        stopCurrentAudio()
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

