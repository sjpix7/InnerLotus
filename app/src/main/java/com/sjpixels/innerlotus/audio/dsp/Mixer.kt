package com.sjpixels.innerlotus.audio.dsp

import kotlin.math.exp

/** Creates a [Voice] for a layer id such as "freq:f528" or "lib:rain". */
object VoiceFactory {

    data class FreqSpec(val id: String, val hz: Double, val beat: Double = 0.0) {
        val binaural: Boolean get() = beat > 0.0
    }

    val frequencies: List<FreqSpec> = listOf(
        FreqSpec("f174", 174.0), FreqSpec("f285", 285.0), FreqSpec("f396", 396.0), FreqSpec("f417", 417.0),
        FreqSpec("f432", 432.0), FreqSpec("f528", 528.0), FreqSpec("f639", 639.0), FreqSpec("f741", 741.0),
        FreqSpec("f852", 852.0), FreqSpec("f963", 963.0), FreqSpec("om", 136.1),
        FreqSpec("schumann", 136.1, 7.83), FreqSpec("delta", 200.0, 2.0), FreqSpec("theta", 200.0, 6.0),
        FreqSpec("alpha", 200.0, 10.0), FreqSpec("beta", 220.0, 18.0), FreqSpec("gamma", 240.0, 40.0),
    )

    /** Library sounds grouped by category (category -> ids). */
    val library: Map<String, List<String>> = linkedMapOf(
        "nature" to listOf("rain", "thunder", "ocean", "stream", "wind", "fire", "birds", "crickets"),
        "instruments" to listOf("flute", "bamboo", "chimes", "bell", "bowl", "crystal", "tanpura", "kalimba", "om", "drum", "heartbeat"),
        "noise" to listOf("white", "pink", "brown"),
    )

    private val trims = mapOf(
        "birds" to 2.2f, "crickets" to 1.8f, "bowl" to 2.0f, "kalimba" to 1.7f, "chimes" to 1.6f,
        "bamboo" to 1.7f, "stream" to 1.3f, "wind" to 1.5f, "heartbeat" to 0.6f, "bell" to 1.3f,
        "tanpura" to 1.4f, "pink" to 0.8f,
    )

    fun create(layerId: String, seed: Long = System.nanoTime()): Voice? =
        createRaw(layerId, seed)?.apply { trim = trims[layerId.substringAfter(':')] ?: 1f }

    private fun createRaw(layerId: String, seed: Long): Voice? {
        val (kind, id) = layerId.split(":", limit = 2).let { (it.getOrNull(0) ?: "") to (it.getOrNull(1) ?: "") }
        return when (kind) {
            "freq" -> frequencies.firstOrNull { it.id == id }?.let { if (it.binaural) BinauralVoice(it.hz, it.beat) else ToneVoice(it.hz) }
            "hz" -> id.toDoubleOrNull()?.let { ToneVoice(it) }
            "lib" -> when (id) {
                "rain" -> RainVoice(seed)
                "thunder" -> ThunderVoice(seed)
                "ocean" -> OceanVoice(seed)
                "stream" -> StreamVoice(seed)
                "wind" -> WindVoice(seed)
                "fire" -> FireVoice(seed)
                "birds" -> BirdsVoice(seed)
                "crickets" -> CricketsVoice(seed)
                "flute" -> FluteVoice(seed)
                "bamboo" -> BambooChimesVoice(seed)
                "chimes" -> WindChimesVoice(seed)
                "bell" -> TempleBellVoice(seed)
                "bowl" -> SingingBowlVoice(seed)
                "crystal" -> CrystalBowlVoice(seed)
                "tanpura" -> TanpuraVoice(seed)
                "kalimba" -> KalimbaVoice(seed)
                "om" -> OmVoice(seed)
                "drum" -> FrameDrumVoice(seed)
                "heartbeat" -> HeartbeatVoice(seed)
                "white", "pink", "brown" -> NoiseVoice(id, seed)
                else -> null
            }
            else -> null
        }
    }
}

/**
 * Mixes any number of voices with smooth per-layer gains, a shared reverb and a master fader.
 * The audio thread calls [render]; the UI thread calls [set]/[setVolume]/[remove]/[fadeTo].
 * Layer changes are published as an immutable array (copy-on-write) so rendering never locks.
 */
class Mixer(private val maxFrames: Int = 4096) {

    private class Layer(val id: String, val voice: Voice, @Volatile var target: Float) {
        var gain = 0f
        @Volatile var removing = false
    }

    @Volatile private var layers: Array<Layer> = emptyArray()
    private val lock = Any()

    private val tmpL = FloatArray(maxFrames); private val tmpR = FloatArray(maxFrames)
    private val dryL = FloatArray(maxFrames); private val dryR = FloatArray(maxFrames)
    private val sendL = FloatArray(maxFrames); private val sendR = FloatArray(maxFrames)
    private val wetL = FloatArray(maxFrames); private val wetR = FloatArray(maxFrames)
    private val reverb = Reverb()

    private val layerK = 1f - exp(-1f / (0.6f * SR))
    private var masterK = 1f - exp(-1f / (1.2f * SR))
    @Volatile private var masterTarget = 1f
    private var master = 0f

    val masterLevel: Float get() = master
    val layerIds: List<String> get() = layers.filter { !it.removing }.map { it.id }

    /** Adds a layer (or updates its volume if it already exists). */
    fun set(id: String, volume: Float, factory: () -> Voice?) {
        synchronized(lock) {
            val existing = layers.firstOrNull { it.id == id }
            if (existing != null) {
                existing.removing = false
                existing.target = volume.coerceIn(0f, 1f)
                return
            }
            val v = factory() ?: return
            layers = layers + Layer(id, v, volume.coerceIn(0f, 1f))
        }
    }

    fun setVolume(id: String, volume: Float) {
        layers.firstOrNull { it.id == id }?.target = volume.coerceIn(0f, 1f)
    }

    fun remove(id: String) {
        layers.firstOrNull { it.id == id }?.let { it.removing = true; it.target = 0f }
    }

    fun clear() {
        for (l in layers) { l.removing = true; l.target = 0f }
    }

    /** Master fader target (0..1) and fade time in seconds. */
    fun fadeTo(level: Float, seconds: Float) {
        masterK = 1f - exp(-1f / (seconds.coerceAtLeast(0.05f) * SR))
        masterTarget = level.coerceIn(0f, 1f)
    }

    fun hasAudibleLayers(): Boolean = layers.any { !it.removing || it.gain > 0.0005f }

    /** Renders [frames] stereo frames into interleaved 16-bit PCM. */
    fun render(out: ShortArray, frames: Int) {
        val n = frames.coerceAtMost(maxFrames)
        renderFloat(n)
        for (i in 0 until n) {
            out[2 * i] = (softClip(dryL[i]) * 32767f).toInt().toShort()
            out[2 * i + 1] = (softClip(dryR[i]) * 32767f).toInt().toShort()
        }
    }

    /** Renders into the internal float buffers; result readable via [readFloat] (tests). */
    fun renderFloat(n: Int) {
        java.util.Arrays.fill(dryL, 0, n, 0f); java.util.Arrays.fill(dryR, 0, n, 0f)
        java.util.Arrays.fill(sendL, 0, n, 0f); java.util.Arrays.fill(sendR, 0, n, 0f)
        val snapshot = layers
        var anyRemoved = false
        for (layer in snapshot) {
            java.util.Arrays.fill(tmpL, 0, n, 0f); java.util.Arrays.fill(tmpR, 0, n, 0f)
            layer.voice.render(tmpL, tmpR, n)
            val send = layer.voice.reverbSend
            val trim = layer.voice.trim
            var g = layer.gain
            val t = layer.target
            for (i in 0 until n) {
                g += (t - g) * layerK
                val a = tmpL[i] * g * trim; val b = tmpR[i] * g * trim
                dryL[i] += a; dryR[i] += b
                sendL[i] += a * send; sendR[i] += b * send
            }
            layer.gain = g
            if (layer.removing && g < 0.0005f) anyRemoved = true
        }
        if (anyRemoved) synchronized(lock) { layers = layers.filterNot { it.removing && it.gain < 0.0005f }.toTypedArray() }
        reverb.process(sendL, sendR, wetL, wetR, n)
        var m = master
        val mt = masterTarget
        for (i in 0 until n) {
            m += (mt - m) * masterK
            dryL[i] = (dryL[i] + wetL[i]) * m
            dryR[i] = (dryR[i] + wetR[i]) * m
        }
        master = m
    }

    fun readFloat(l: FloatArray, r: FloatArray, n: Int) {
        System.arraycopy(dryL, 0, l, 0, n); System.arraycopy(dryR, 0, r, 0, n)
    }
}
