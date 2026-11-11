package com.sjpixels.innerlotus.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.sjpixels.innerlotus.audio.dsp.Mixer
import com.sjpixels.innerlotus.audio.dsp.SR
import com.sjpixels.innerlotus.audio.dsp.VoiceFactory
import com.sjpixels.innerlotus.data.Mix

/**
 * Plays any combination of healing frequencies, nature sounds and instruments, synthesised live
 * and streamed gap-free through an AudioTrack. A foreground service keeps it playing with the
 * screen off. Layer ids: "freq:<id>", "lib:<id>", "hz:<number>".
 */
object SoundEngine {
    private const val FRAMES = 1024
    private const val MAX_LAYERS = 8

    private val mixer = Mixer()
    private val main = Handler(Looper.getMainLooper())
    private var thread: Thread? = null
    @Volatile private var running = false
    private var appContext: Context? = null

    /** Layer id -> volume (0..1). This is the UI's view of what is playing. */
    val layers = mutableStateMapOf<String, Float>()
    var isPlaying by mutableStateOf(false)
        private set

    /** Epoch millis when the sleep timer ends, or 0. */
    var sleepEndsAt by mutableStateOf(0L)
        private set

    /** Name of the mix currently loaded (for the now-playing bar), may be empty. */
    var currentMixName by mutableStateOf("")

    /** Journey mode: cycle through these mixes every [journeyMinutes]. */
    var journey by mutableStateOf<List<Mix>>(emptyList())
        private set
    var journeyMinutes by mutableStateOf(10)
    private var journeyIndex = 0

    private val attributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    // ---- Audio focus: pause for calls / other music, duck for short notifications ----
    private var focusRequest: AudioFocusRequest? = null
    private var resumeOnFocusGain = false
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        main.post {
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS -> { resumeOnFocusGain = false; pause() }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> { resumeOnFocusGain = isPlaying; pause() }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> if (isPlaying) mixer.fadeTo(0.25f, 0.4f)
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (isPlaying) mixer.fadeTo(1f, 0.8f)
                    else if (resumeOnFocusGain) appContext?.let { resume(it) }
                    resumeOnFocusGain = false
                }
            }
        }
    }

    private fun audioManager(): AudioManager? =
        appContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private fun requestFocus(): Boolean {
        val am = audioManager() ?: return true
        val req = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(focusListener, main)
            .setWillPauseWhenDucked(false)
            .build().also { focusRequest = it }
        return am.requestAudioFocus(req) != AudioManager.AUDIOFOCUS_REQUEST_FAILED
    }

    private fun abandonFocus() {
        val req = focusRequest ?: return
        audioManager()?.abandonAudioFocusRequest(req)
    }

    // ---- Headphones unplugged: pause instead of blasting the loudspeaker ----
    private var noisyRegistered = false
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) pause()
        }
    }

    private fun registerNoisy() {
        val ctx = appContext ?: return
        if (noisyRegistered) return
        ContextCompat.registerReceiver(
            ctx, noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        noisyRegistered = true
    }

    private fun unregisterNoisy() {
        if (!noisyRegistered) return
        try { appContext?.unregisterReceiver(noisyReceiver) } catch (_: Exception) {}
        noisyRegistered = false
    }

    private val sleepRunnable = Runnable { fadeOutAndStop() }
    private val journeyRunnable = object : Runnable {
        override fun run() {
            val list = journey
            if (list.isEmpty() || !isPlaying) return
            journeyIndex = (journeyIndex + 1) % list.size
            loadMix(list[journeyIndex], keepJourney = true)
            main.postDelayed(this, journeyMinutes * 60_000L)
        }
    }

    fun isOn(id: String) = layers.containsKey(id)

    fun toggle(context: Context, id: String, volume: Float = 0.7f) {
        if (isOn(id)) remove(id) else add(context, id, volume)
    }

    fun add(context: Context, id: String, volume: Float = 0.7f) {
        if (!layers.containsKey(id) && layers.size >= MAX_LAYERS) {
            // drop the oldest layer to keep the phone cool
            layers.keys.firstOrNull()?.let { remove(it) }
        }
        layers[id] = volume
        mixer.set(id, volume) { VoiceFactory.create(id) }
        start(context)
    }

    fun setVolume(id: String, volume: Float) {
        if (!layers.containsKey(id)) return
        layers[id] = volume
        mixer.setVolume(id, volume)
    }

    fun remove(id: String) {
        layers.remove(id)
        mixer.remove(id)
        if (layers.isEmpty()) fadeOutAndStop()
    }

    /** Replace everything with [mix] (smooth crossfade). */
    fun loadMix(mix: Mix, context: Context? = appContext, keepJourney: Boolean = false) {
        if (!keepJourney) stopJourney()
        val ctx = context ?: return
        val keep = mix.layers.keys
        layers.keys.filter { it !in keep }.forEach { id -> layers.remove(id); mixer.remove(id) }
        for ((id, v) in mix.layers) {
            layers[id] = v
            mixer.set(id, v) { VoiceFactory.create(id) }
        }
        currentMixName = mix.name
        start(ctx)
    }

    fun startJourney(context: Context, mixes: List<Mix>, minutes: Int) {
        if (mixes.isEmpty()) return
        journey = mixes
        journeyMinutes = minutes.coerceIn(1, 120)
        journeyIndex = 0
        loadMix(mixes[0], context, keepJourney = true)
        main.removeCallbacks(journeyRunnable)
        main.postDelayed(journeyRunnable, journeyMinutes * 60_000L)
    }

    fun stopJourney() {
        journey = emptyList()
        main.removeCallbacks(journeyRunnable)
    }

    /** Sleep timer in minutes; 0 cancels. */
    fun setSleepTimer(minutes: Int) {
        main.removeCallbacks(sleepRunnable)
        if (minutes <= 0) { sleepEndsAt = 0L; return }
        sleepEndsAt = System.currentTimeMillis() + minutes * 60_000L
        main.postDelayed(sleepRunnable, minutes * 60_000L)
    }

    /** Pause keeps the layer selection so [resume] can restart it. */
    fun pause() {
        if (!isPlaying) return
        running = false
        mixer.fadeTo(0f, 0.8f)
        isPlaying = false
        onStopped()
    }

    fun resume(context: Context) {
        if (layers.isEmpty()) return
        for ((id, v) in layers) mixer.set(id, v) { VoiceFactory.create(id) }
        start(context)
    }

    fun stopAll() {
        stopJourney()
        layers.clear()
        currentMixName = ""
        fadeOutAndStop()
    }

    private fun fadeOutAndStop() {
        main.removeCallbacks(sleepRunnable)
        sleepEndsAt = 0L
        running = false
        mixer.fadeTo(0f, 2.5f)
        if (layers.isNotEmpty() && isPlaying) {
            // sleep timer: keep selection for a later resume
        }
        mixer.clear()
        isPlaying = false
        onStopped()
    }

    private fun onStopped() {
        unregisterNoisy()
        abandonFocus()
        stopService()
    }

    @Synchronized
    private fun start(context: Context) {
        appContext = context.applicationContext
        if (!isPlaying) {
            if (!requestFocus()) {
                // e.g. during a phone call: don't start over it
                isPlaying = false
                return
            }
            registerNoisy()
        }
        mixer.fadeTo(1f, 1.2f)
        running = true
        isPlaying = true
        val alive = thread?.isAlive == true
        if (!alive) {
            thread = Thread({ audioLoop() }, "InnerLotusAudio").apply { start() }
        }
        startService(context)
    }

    private fun audioLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val minBuf = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SR)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, FRAMES * 4 * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            main.post { isPlaying = false; onStopped() }
            return
        }
        val buf = ShortArray(FRAMES * 2)
        try {
            track.play()
            while (running || mixer.masterLevel > 0.0008f) {
                mixer.render(buf, FRAMES)
                val written = track.write(buf, 0, buf.size)
                if (written < 0) break
            }
        } catch (_: Exception) {
        } finally {
            try { track.pause(); track.flush(); track.stop() } catch (_: Exception) {}
            track.release()
        }
        if (running) {
            // restarted while we were fading out: spin up a fresh loop
            main.post { appContext?.let { start(it) } }
        }
    }

    /**
     * Starts the foreground service once; while it is already up we only refresh its notification.
     * Re-sending startForegroundService on every layer change risks the "did not call
     * startForeground()" crash when a stop races the start.
     */
    private fun startService(context: Context) {
        when (SoundService.state) {
            SoundService.State.RUNNING -> { SoundService.refreshNotification(context); return }
            SoundService.State.STARTING -> return
            SoundService.State.STOPPED -> {}
        }
        try {
            SoundService.state = SoundService.State.STARTING
            ContextCompat.startForegroundService(context, Intent(context, SoundService::class.java))
        } catch (_: Exception) {
            // Starting a foreground service can be refused in the background; playback still works
            // while the app is visible.
            SoundService.state = SoundService.State.STOPPED
        }
    }

    private fun stopService() {
        val ctx = appContext ?: return
        // While STARTING, the service stops itself right after calling startForeground (it checks
        // isPlaying), so we never stop it before it has gone foreground.
        if (SoundService.state != SoundService.State.RUNNING) return
        // Mark stopped now so a quick restart issues a fresh startForegroundService
        // (the system delivers the stop before that new start).
        SoundService.state = SoundService.State.STOPPED
        try { ctx.stopService(Intent(ctx, SoundService::class.java)) } catch (_: Exception) {}
    }
}
