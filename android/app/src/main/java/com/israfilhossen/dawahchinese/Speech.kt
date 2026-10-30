package com.israfilhossen.dawahchinese

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.mutableStateOf
import java.util.Locale

/**
 * Speaks Chinese. Plays a recorded MP3 from assets/audio/<key>.mp3 when one is bundled,
 * otherwise uses the phone's text-to-speech (Google's Chinese voice sounds natural on most phones).
 */
object Speech {
    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null
    private var recorded: Set<String> = emptySet()
    private lateinit var app: Context
    val ready = mutableStateOf(false)
    val missingVoice = mutableStateOf(false)
    val speaking = mutableStateOf<String?>(null)

    fun init(c: Context) {
        if (tts != null) return
        app = c.applicationContext
        recorded = runCatching { app.assets.list("audio")?.map { it.removeSuffix(".mp3") }?.toSet() }.getOrNull() ?: emptySet()
        tts = TextToSpeech(app) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE) ?: TextToSpeech.LANG_NOT_SUPPORTED
                missingVoice.value = r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED
                ready.value = !missingVoice.value
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) { speaking.value = null }
                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) { speaking.value = null }
                })
            } else missingVoice.value = true
        }
    }

    /** The same key the website uses: FNV-1a of the text without punctuation. */
    fun key(text: String): String {
        val t = text.filter { it in '一'..'鿿' || it.isLetterOrDigit() && it.code < 128 }
        var h = 0x811c9dc5.toInt()
        t.codePoints().forEach { c -> h = (h xor c) * 0x01000193 }
        return String.format("%08x", h)
    }

    fun speak(text: String, rate: Float = Store.rate) {
        stop()
        val clean = text.replace(Regex("[＿_]+"), "，")
        speaking.value = text
        val k = key(clean)
        if (k in recorded) {
            runCatching {
                val fd = app.assets.openFd("audio/$k.mp3")
                player = MediaPlayer().apply {
                    setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    setOnCompletionListener { speaking.value = null }
                    prepare()
                    playbackParams = playbackParams.setSpeed((rate / 0.9f).coerceIn(0.5f, 1.5f))
                    start()
                }
                return
            }
        }
        tts?.setSpeechRate((rate / 0.9f).coerceIn(0.4f, 1.6f))
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "u" + System.nanoTime())
    }

    fun stop() {
        player?.runCatching { stop(); release() }
        player = null
        tts?.stop()
        speaking.value = null
    }

    fun openSettings(c: Context) {
        runCatching { c.startActivity(Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { c.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
