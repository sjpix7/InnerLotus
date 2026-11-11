package com.sjpixels.innerlotus.tts

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sjpixels.innerlotus.App
import com.sjpixels.innerlotus.core.I18n
import com.sjpixels.innerlotus.core.Lang
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Wrapper around the device's built-in Text-to-Speech engine (works offline with the
 * installed voices). [speak] suspends until the text has been spoken, so guided flows can be
 * written as simple sequential coroutines. Cancelling the coroutine stops speech.
 */
object Speaker {
    private var tts: TextToSpeech? = null
    private var ready = CompletableDeferred<Boolean>()
    private val waiters = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    @Volatile private var generation = 0

    /** Tag of the UI element currently speaking (e.g. "learn:eft"), for play/stop icons. */
    var speakingTag by mutableStateOf<String?>(null)
        private set

    fun init(context: Context) {
        if (tts != null) return
        ready = CompletableDeferred()
        tts = TextToSpeech(context.applicationContext) { status ->
            val ok = status == TextToSpeech.SUCCESS
            if (ok) {
                attachListener()
                setLanguageNow()
            }
            ready.complete(ok)
        }
    }

    private fun attachListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) = finish(utteranceId)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finish(utteranceId)
            override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = finish(utteranceId)
        })
    }

    private fun finish(id: String?) {
        if (id != null) waiters.remove(id)?.complete(Unit)
    }

    private fun setLanguageNow(): Boolean {
        val t = tts ?: return false
        val r = try { t.setLanguage(I18n.lang.locale) } catch (e: Exception) { TextToSpeech.LANG_NOT_SUPPORTED }
        return r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
    }

    /** Switch the voice to the current app language. [onResult] gets false when no voice is installed. */
    fun applyLanguage(onResult: ((Boolean) -> Unit)? = null) {
        scope.launch {
            val ok = withTimeoutOrNull(6000) { ready.await() } == true
            val available = ok && setLanguageNow()
            onResult?.invoke(available)
        }
    }

    fun isAvailable(lang: Lang): Boolean {
        val t = tts ?: return false
        if (!ready.isCompleted) return true
        return try { t.isLanguageAvailable(lang.locale) >= TextToSpeech.LANG_AVAILABLE } catch (e: Exception) { false }
    }

    fun stop() {
        generation++
        try { tts?.stop() } catch (_: Exception) {}
        val pending = waiters.values.toList()
        waiters.clear()
        pending.forEach { it.complete(Unit) }
        speakingTag = null
    }

    /**
     * Speaks [text] in the current language and suspends until finished.
     * Returns false if speech was interrupted or TTS is unavailable.
     */
    suspend fun speak(text: String, tag: String? = null): Boolean {
        val t = tts ?: return false
        if (withTimeoutOrNull(6000) { ready.await() } != true) return false
        stop()
        val gen = generation
        val clean = clean(text)
        if (clean.isBlank()) return true
        t.setSpeechRate(App.settings.ttsRate.coerceIn(0.4f, 2f))
        t.setPitch(App.settings.ttsPitch.coerceIn(0.5f, 2f))
        speakingTag = tag
        var spokenId: String? = null
        try {
            val max = (TextToSpeech.getMaxSpeechInputLength() - 100).coerceIn(300, 3800)
            for (chunk in chunks(clean, max)) {
                if (gen != generation) return false
                val id = UUID.randomUUID().toString()
                spokenId = id
                val done = CompletableDeferred<Unit>()
                waiters[id] = done
                if (t.speak(chunk, TextToSpeech.QUEUE_ADD, Bundle(), id) == TextToSpeech.ERROR) {
                    waiters.remove(id)
                    return false
                }
                done.await()
            }
            return gen == generation
        } finally {
            if (gen == generation) {
                if (spokenId != null && waiters.containsKey(spokenId)) {
                    // coroutine was cancelled mid-sentence
                    try { t.stop() } catch (_: Exception) {}
                    waiters.remove(spokenId)
                }
                speakingTag = null
            }
        }
    }

    /** Fire-and-forget speech (e.g. breath cues). */
    fun say(text: String) {
        scope.launch { speak(text) }
    }

    private fun clean(s: String): String = s
        .replace("•", "")
        .replace("»", "")
        .replace(Regex("[\\p{So}\\uFE0F\\u200D]+"), " ")
        .replace(Regex("[ \\t]+"), " ")
        .trim()

    private fun chunks(text: String, max: Int): List<String> {
        if (text.length <= max) return listOf(text)
        val out = ArrayList<String>()
        val sb = StringBuilder()
        val parts = text.split(Regex("(?<=[.!?。！？।\\n])"))
        for (p in parts) {
            if (sb.length + p.length > max && sb.isNotEmpty()) { out.add(sb.toString()); sb.setLength(0) }
            if (p.length > max) p.chunked(max).forEach { out.add(it) } else sb.append(p)
        }
        if (sb.isNotBlank()) out.add(sb.toString())
        return out
    }

    fun openInstallVoices(context: Context) {
        val intents = listOf(
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
            Intent("com.android.settings.TTS_SETTINGS"),
            Intent(android.provider.Settings.ACTION_SETTINGS),
        )
        for (i in intents) {
            try {
                context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
    }

    fun openTtsSettings(context: Context) {
        try {
            context.startActivity(Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            openInstallVoices(context)
        }
    }
}
