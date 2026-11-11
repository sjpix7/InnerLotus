package com.sjpixels.innerlotus.audio.dsp

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Building blocks for the live sound synthesiser. Pure Kotlin (no Android imports) so it can be
 * rendered and tested on a desktop JVM. All voices ADD into the provided buffers.
 */

const val SR = 44100
const val TWO_PI_F = (2.0 * PI).toFloat()

/** Fast xorshift random generator (one per voice; not thread-safe). */
class Rng(seed: Long = System.nanoTime()) {
    private var x: Long = if (seed == 0L) 0x2545F4914F6CDD1DL else seed

    fun nextLong(): Long {
        x = x xor (x shl 13)
        x = x xor (x ushr 7)
        x = x xor (x shl 17)
        return x
    }

    /** Uniform in [0, 1). */
    fun next(): Float = (nextLong() ushr 40).toFloat() / 16777216f

    /** Uniform in [-1, 1). */
    fun bi(): Float = next() * 2f - 1f

    fun range(a: Float, b: Float): Float = a + (b - a) * next()

    fun int(n: Int): Int = ((nextLong() ushr 33) % n).toInt()

    fun chance(p: Float): Boolean = next() < p

    fun <T> pick(list: List<T>): T = list[int(list.size)]
}

fun t60ToDecay(t60: Float): Float = exp(ln(0.001) / (max(t60, 0.001f) * SR)).toFloat()

fun panGains(pan: Float): Pair<Float, Float> {
    val p = (pan.coerceIn(-1f, 1f) + 1f) * (PI.toFloat() / 4f)
    return cos(p) to sin(p)
}

/** One-pole low-pass. */
class OnePoleLP(cutoff: Float) {
    private var a = 0f
    private var y = 0f
    init { set(cutoff) }
    fun set(fc: Float) { a = 1f - exp(-TWO_PI_F * fc / SR) }
    fun process(x: Float): Float { y += a * (x - y); return y }
}

/** One-pole high-pass. */
class OnePoleHP(cutoff: Float) {
    private val lp = OnePoleLP(cutoff)
    fun set(fc: Float) = lp.set(fc)
    fun process(x: Float): Float = x - lp.process(x)
}

/** RBJ biquad, transposed direct form II. */
class Biquad {
    private var b0 = 1f; private var b1 = 0f; private var b2 = 0f
    private var a1 = 0f; private var a2 = 0f
    private var z1 = 0f; private var z2 = 0f

    private fun coeffs(fc: Float, q: Float): Triple<Float, Float, Float> {
        val w0 = TWO_PI_F * fc.coerceIn(10f, SR * 0.45f) / SR
        return Triple(cos(w0), sin(w0) / (2f * max(q, 0.05f)), w0)
    }

    fun lowpass(fc: Float, q: Float = 0.707f): Biquad {
        val (c, alpha, _) = coeffs(fc, q)
        val a0 = 1f + alpha
        b0 = (1f - c) / 2f / a0; b1 = (1f - c) / a0; b2 = b0
        a1 = -2f * c / a0; a2 = (1f - alpha) / a0
        return this
    }

    fun highpass(fc: Float, q: Float = 0.707f): Biquad {
        val (c, alpha, _) = coeffs(fc, q)
        val a0 = 1f + alpha
        b0 = (1f + c) / 2f / a0; b1 = -(1f + c) / a0; b2 = b0
        a1 = -2f * c / a0; a2 = (1f - alpha) / a0
        return this
    }

    /** Band-pass with 0 dB peak gain. */
    fun bandpass(fc: Float, q: Float): Biquad {
        val (c, alpha, _) = coeffs(fc, q)
        val a0 = 1f + alpha
        b0 = alpha / a0; b1 = 0f; b2 = -alpha / a0
        a1 = -2f * c / a0; a2 = (1f - alpha) / a0
        return this
    }

    fun process(x: Float): Float {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y
    }
}

/** Pink noise (Paul Kellet's economy filter), roughly in [-1, 1]. */
class PinkNoise(private val rng: Rng) {
    private var b0 = 0f; private var b1 = 0f; private var b2 = 0f
    fun next(): Float {
        val w = rng.bi()
        b0 = 0.99765f * b0 + w * 0.0990460f
        b1 = 0.96300f * b1 + w * 0.2965164f
        b2 = 0.57000f * b2 + w * 1.0526913f
        return (b0 + b1 + b2 + w * 0.1848f) * 0.22f
    }
}

/** Brown (red) noise, roughly in [-1, 1]. */
class BrownNoise(private val rng: Rng) {
    private var y = 0f
    fun next(): Float {
        y = (y + 0.02f * rng.bi()) / 1.02f
        return y * 3.5f
    }
}

/** Exponential smoother toward a target value. */
class Smooth(var value: Float, seconds: Float) {
    var target = value
    private var k = 1f - exp(-1f / (seconds * SR))
    fun time(seconds: Float) { k = 1f - exp(-1f / (seconds * SR)) }
    fun next(): Float { value += (target - value) * k; return value }
}

/** Slowly wandering random value in [lo, hi] (smoothed random targets). */
class Wander(private val rng: Rng, private val lo: Float, private val hi: Float, private val minSec: Float, private val maxSec: Float) {
    private val s = Smooth(rng.range(lo, hi), (minSec + maxSec) / 2f)
    private var left = 0
    fun next(): Float {
        if (--left <= 0) {
            s.target = rng.range(lo, hi)
            left = (rng.range(minSec, maxSec) * SR).toInt()
        }
        return s.next()
    }
    /** Advance n samples at once (for control-rate use). */
    fun step(n: Int): Float {
        var v = 0f
        repeat(n) { v = next() }
        return v
    }
}

/**
 * A bank of exponentially decaying sinusoids (modal synthesis): bells, bowls, chimes, drops.
 * Each mode is a rotating phasor, so a strike costs ~6 multiplies per sample per partial.
 */
class ModalBank(private val capacity: Int = 96) {
    private val re = FloatArray(capacity)
    private val im = FloatArray(capacity)
    private val c = FloatArray(capacity)
    private val s = FloatArray(capacity)
    private val d = FloatArray(capacity)
    private val gl = FloatArray(capacity)
    private val gr = FloatArray(capacity)
    private var n = 0

    val active: Int get() = n

    fun add(freq: Float, amp: Float, t60: Float, pan: Float = 0f) {
        if (freq <= 0f || freq >= SR * 0.45f || amp <= 0f) return
        var slot = n
        if (n >= capacity) {
            // replace the quietest mode
            var q = 0; var qa = Float.MAX_VALUE
            for (i in 0 until n) { val a = abs(re[i]) + abs(im[i]); if (a < qa) { qa = a; q = i } }
            slot = q
        } else n++
        val w = TWO_PI_F * freq / SR
        c[slot] = cos(w); s[slot] = sin(w); d[slot] = t60ToDecay(t60)
        re[slot] = amp; im[slot] = 0f
        val (l, r) = panGains(pan)
        gl[slot] = l; gr[slot] = r
    }

    fun render(l: FloatArray, r: FloatArray, off: Int, len: Int) {
        var i = 0
        val end = off + len
        while (i < n) {
            var x = re[i]; var y = im[i]
            val cc = c[i]; val ss = s[i]; val dd = d[i]; val lg = gl[i]; val rg = gr[i]
            for (k in off until end) {
                val nx = (x * cc - y * ss) * dd
                y = (x * ss + y * cc) * dd
                x = nx
                l[k] += y * lg
                r[k] += y * rg
            }
            if (abs(x) + abs(y) < 1e-5f) {
                // remove: move last into this slot
                n--
                re[i] = re[n]; im[i] = im[n]; c[i] = c[n]; s[i] = s[n]; d[i] = d[n]; gl[i] = gl[n]; gr[i] = gr[n]
            } else {
                re[i] = x; im[i] = y
                i++
            }
        }
    }
}

/** Lightweight stereo reverb (Freeverb-style: 4 combs + 2 all-passes per channel). */
class Reverb(room: Float = 0.86f, damp: Float = 0.35f) {
    private class Comb(size: Int, val feedback: Float, val damp: Float) {
        private val buf = FloatArray(size); private var idx = 0; private var store = 0f
        fun process(x: Float): Float {
            val out = buf[idx]
            store = out * (1f - damp) + store * damp
            buf[idx] = x + store * feedback
            if (++idx >= buf.size) idx = 0
            return out
        }
    }

    private class AllPass(size: Int) {
        private val buf = FloatArray(size); private var idx = 0
        fun process(x: Float): Float {
            val b = buf[idx]
            val out = -x + b
            buf[idx] = x + b * 0.5f
            if (++idx >= buf.size) idx = 0
            return out
        }
    }

    private val scale = SR / 44100f
    private val combL = intArrayOf(1116, 1188, 1277, 1356).map { Comb((it * scale).toInt(), room, damp) }
    private val combR = intArrayOf(1139, 1211, 1300, 1379).map { Comb((it * scale).toInt(), room, damp) }
    private val apL = intArrayOf(556, 441).map { AllPass((it * scale).toInt()) }
    private val apR = intArrayOf(579, 464).map { AllPass((it * scale).toInt()) }

    fun process(inL: FloatArray, inR: FloatArray, outL: FloatArray, outR: FloatArray, frames: Int) {
        for (k in 0 until frames) {
            val x = (inL[k] + inR[k]) * 0.05f
            var l = 0f; var r = 0f
            for (cb in combL) l += cb.process(x)
            for (cb in combR) r += cb.process(x)
            for (a in apL) l = a.process(l)
            for (a in apR) r = a.process(r)
            outL[k] = l; outR[k] = r
        }
    }
}

/** Gentle soft-clipper: linear below 0.7, smooth knee above, never exceeds 1. */
fun softClip(x: Float): Float {
    val a = abs(x)
    if (a <= 0.7f) return x
    val over = (a - 0.7f) / 0.3f
    val y = 0.7f + 0.3f * (over / sqrt(1f + over * over))
    return if (x < 0) -y else y
}

/** Base class for every sound. Voices add into [l] and [r] (frames samples each). */
abstract class Voice {
    /** How much of this voice is sent to the shared reverb (0..1). */
    open val reverbSend: Float = 0.15f

    /** Loudness trim applied by the mixer so sparse sounds sit level with continuous ones. */
    var trim: Float = 1f
    abstract fun render(l: FloatArray, r: FloatArray, frames: Int)
}
