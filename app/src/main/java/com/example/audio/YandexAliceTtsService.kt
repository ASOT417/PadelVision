package com.example.audio

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

enum class SpeechKitVoice(val id: String, val title: String, val gender: String, val description: String) {
    LERA("lera", "Лера", "Женский", "Приятный, современный и выразительный голос"),
    MARINA("marina", "Марина", "Женский", "Тёплый, доброжелательный спортивный комментатор"),
    DASHA("dasha", "Даша", "Женский", "Энергичный, звонкий тембр для корта"),
    JULIA("julia", "Юлия", "Женский", "Чёткий, дикторский голос судейства"),
    MASHA("masha", "Маша", "Женский", "Мягкий и естественный тон"),
    FILIPP("filipp", "Филипп", "Мужской", "Уверенный мужской баритон"),
    ERMIL("ermil", "Ермил", "Мужской", "Классический репортажный голос")
}

/**
 * Сервис синтеза речи через Yandex SpeechKit TTS API с поддержкой выбора голосов
 * ('lera', 'marina', 'dasha', 'julia', 'filipp' и др.)
 * и автоматическим локальным оффлайн-кэшированием MP3 на диске устройства.
 */
class YandexAliceTtsService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    // Текущий выбранный голос (по умолчанию «Лера» по запросу пользователя)
    var currentVoice: SpeechKitVoice = SpeechKitVoice.LERA

    // Директория постоянного аудио-кэша голосов
    private val ttsAudioDir = File(context.filesDir, "speechkit_tts").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Возвращает ключ из BuildConfig или напрямую переданный
     */
    private val apiKey: String
        get() = try {
            val key = BuildConfig::class.java.getField("YANDEX_SPEECHKIT_API_KEY").get(null) as? String
            if (!key.isNullOrBlank()) key else "AQVNzErG0DrharLtzmJmtouxbc9dMueJ6dqiez6c"
        } catch (_: Exception) {
            "AQVNzErG0DrharLtzmJmtouxbc9dMueJ6dqiez6c"
        }

    private val folderId: String
        get() = try {
            val f = BuildConfig::class.java.getField("YANDEX_SPEECHKIT_FOLDER_ID").get(null) as? String
            if (!f.isNullOrBlank()) f else "b1g2uf5cu7b2ii93kg6h"
        } catch (_: Exception) {
            "b1g2uf5cu7b2ii93kg6h"
        }

    /**
     * Проверяет наличие аудиофайла в локальном кэше (100% оффлайн).
     */
    fun getCachedAudioFile(text: String, voice: SpeechKitVoice = currentVoice): File? {
        val fileName = getFileNameForText(text, voice)
        val file = File(ttsAudioDir, fileName)
        return if (file.exists() && file.length() > 0) file else null
    }

    /**
     * Проверяет, закэширована ли фраза
     */
    fun isCached(text: String, voice: SpeechKitVoice = currentVoice): Boolean {
        return getCachedAudioFile(text, voice) != null
    }

    /**
     * Синтезирует речь с выбранным голосом (Лера, Марина и т.д.).
     * Приоритет запроса:
     * 1. Локальный оффлайн-кэш на диске (0 мс задержки).
     * 2. SpeechKit API v3 utteranceSynthesis (с передачей voice).
     * 3. SpeechKit API v1 REST (резервный вызов для голосов с поддержкой v1).
     */
    fun synthesizeVoice(
        textToSpeak: String,
        voice: SpeechKitVoice = currentVoice,
        onSuccess: (File) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val trimmed = textToSpeak.trim()
        if (trimmed.isEmpty()) {
            onFailure(IllegalArgumentException("Текст не может быть пустым"))
            return
        }

        // 1. ПРОВЕРКА ОФФЛАЙН-КЭША
        val cached = getCachedAudioFile(trimmed, voice)
        if (cached != null) {
            Log.d(TAG, "Озвучка из локального оффлайн-кэша (${voice.title}): '$trimmed' (${cached.name}, ${cached.length()} байт)")
            onSuccess(cached)
            return
        }

        // 2. СИНТЕЗ ЧЕРЕЗ SPEECHKIT v3 (utteranceSynthesis)
        synthesizeViaV3(trimmed, voice, onSuccess, onFailure = { v3Error ->
            Log.w(TAG, "SpeechKit v3 завершился ошибкой (${v3Error.message}), пробуем v1 fallback...")
            synthesizeViaV1(trimmed, voice, onSuccess, onFailure)
        })
    }

    // Совместимость со старым методом
    fun synthesizeAliceVoice(
        textToSpeak: String,
        onSuccess: (File) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        synthesizeVoice(textToSpeak, currentVoice, onSuccess, onFailure)
    }

    /**
     * Запрос к современному Yandex SpeechKit API v3
     */
    private fun synthesizeViaV3(
        trimmed: String,
        voice: SpeechKitVoice,
        onSuccess: (File) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val jsonPayload = JSONObject().apply {
            put("text", trimmed)
            val hintsArray = org.json.JSONArray().apply {
                put(JSONObject().put("voice", voice.id))
                if (voice == SpeechKitVoice.MARINA) {
                    put(JSONObject().put("role", "friendly"))
                }
            }
            put("hints", hintsArray)
            put("outputAudioSpec", JSONObject().apply {
                put("containerAudio", JSONObject().apply {
                    put("containerAudioType", "MP3")
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonPayload.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://tts.api.cloud.yandex.net/tts/v3/utteranceSynthesis")
            .addHeader("Authorization", "Api-Key $apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                Log.w(TAG, "Ошибка сети v3: ${e.message}")
                onFailure(e)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!response.isSuccessful) {
                    val code = response.code
                    val msg = response.body?.string() ?: ""
                    Log.w(TAG, "SpeechKit v3 вернул HTTP $code: $msg")
                    onFailure(java.io.IOException("HTTP $code: $msg"))
                    return
                }

                val bodyStr = response.body?.string() ?: ""
                try {
                    val json = JSONObject(bodyStr)
                    val result = json.optJSONObject("result")
                    val audioChunk = result?.optJSONObject("audioChunk")
                    val dataBase64 = audioChunk?.optString("data")

                    if (!dataBase64.isNullOrEmpty()) {
                        val rawAudio = Base64.decode(dataBase64, Base64.DEFAULT)
                        val targetFile = File(ttsAudioDir, getFileNameForText(trimmed, voice))
                        FileOutputStream(targetFile).use { it.write(rawAudio) }
                        Log.i(TAG, "Синтез v3 (${voice.title}) успешен: '$trimmed' -> ${targetFile.length()} байт")
                        onSuccess(targetFile)
                    } else {
                        onFailure(java.io.IOException("Отсутствует аудиопоток в ответе SpeechKit v3"))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка парсинга ответа v3", e)
                    onFailure(e)
                }
            }
        })
    }

    /**
     * Резервный синтез через проверенный SpeechKit v1 API
     */
    private fun synthesizeViaV1(
        trimmed: String,
        voice: SpeechKitVoice,
        onSuccess: (File) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val formVoice = if (voice == SpeechKitVoice.LERA) "marina" else voice.id
        val formBody = okhttp3.FormBody.Builder()
            .add("text", trimmed)
            .add("voice", formVoice)
            .add("emotion", "good")
            .add("lang", "ru-RU")
            .add("format", "mp3")
            .add("sampleRateHertz", "48000")
            .add("folderId", folderId)
            .build()

        val request = Request.Builder()
            .url("https://tts.api.cloud.yandex.net/speech/v1/tts:synthesize")
            .addHeader("Authorization", "Api-Key $apiKey")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                Log.w(TAG, "Сетевая ошибка синтеза SpeechKit v1: ${e.message}")
                onFailure(e)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorMsg = response.body?.string() ?: ""
                    Log.w(TAG, "SpeechKit v1 вернул HTTP $code: $errorMsg")
                    onFailure(java.io.IOException("HTTP $code: $errorMsg"))
                    return
                }

                val body = response.body
                if (body == null) {
                    onFailure(java.io.IOException("Пустое тело ответа SpeechKit"))
                    return
                }

                try {
                    val targetFile = File(ttsAudioDir, getFileNameForText(trimmed, voice))
                    FileOutputStream(targetFile).use { output ->
                        body.byteStream().copyTo(output)
                    }
                    Log.i(TAG, "Фраза успешно закэширована через v1: '$trimmed' -> ${targetFile.length()} байт")
                    onSuccess(targetFile)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка сохранения аудио в оффлайн-кэш", e)
                    onFailure(e)
                }
            }
        })
    }

    /**
     * Фоновая предварительная загрузка базовых фраз счёта,
     * чтобы на корте при отключенном интернете всё играло из локальных файлов.
     */
    fun prewarmCorePhrases(voice: SpeechKitVoice = currentVoice, onProgress: (Int, Int) -> Unit = { _, _ -> }) {
        val corePhrases = mutableListOf(
            "Включен голос ${voice.title}",
            "Ошибка на первой подаче. Вторая подача!",
            "Двойная ошибка! Очко соперникам.",
            "Касание сетки (Лет)! Переподача. Очко не начисляется.",
            "Отмена очка.",
            "Минус очко.",
            "Смена подачи.",
            "Матчбол!",
            "Ничья!",
            "Ровно",
            "По пятнадцати",
            "По тридцати",
            "Сорок-сорок, решающее очко!",
            "Оффлайн кэш голоса ${voice.title} успешно подготовлен"
        )

        for (i in 0..15) {
            for (j in 0..15) {
                if (kotlin.math.abs(i - j) <= 2) {
                    corePhrases.add("$i : $j (из 24)")
                    corePhrases.add("$i : $j")
                }
            }
        }

        var completed = 0
        val total = corePhrases.size
        corePhrases.forEach { phrase ->
            if (!isCached(phrase, voice)) {
                synthesizeVoice(phrase, voice, onSuccess = {
                    completed++
                    onProgress(completed, total)
                }, onFailure = {
                    completed++
                    onProgress(completed, total)
                })
            } else {
                completed++
                onProgress(completed, total)
            }
        }
    }

    private fun getFileNameForText(text: String, voice: SpeechKitVoice): String {
        val clean = text.replace("[^a-zA-Zа-яА-Я0-9]".toRegex(), "_").take(30)
        val hash = (text.hashCode().toLong() and 0xFFFFFFFFL).toString(16)
        return "tts_${voice.id}_${clean}_${hash}.mp3"
    }

    companion object {
        private const val TAG = "SpeechKitTTS"
    }
}
