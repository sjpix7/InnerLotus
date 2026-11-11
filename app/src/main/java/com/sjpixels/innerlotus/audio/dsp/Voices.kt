package com.sjpixels.innerlotus.audio.dsp

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val TWO_PI = 2.0 * PI
private const val BLOCK = 32

private fun smoothstep(x: Float): Float { val t = x.coerceIn(0f, 1f); return t * t * (3f - 2f * t) }

// ------------------------------------------------------------------------------------------------
// Pure tones, binaural beats and noise colours
// ------------------------------------------------------------------------------------------------

/** A healing frequency. Low tones get soft harmonics so they are audible on phone speakers. */
class ToneVoice(private val freq: Double) : Voice() {
    override val reverbSend = 0.08f
    private var ph = 0.0
    private var lfo = 0.0
    private val inc = freq / SR
    private val amp = 0.22f * (if (freq > 500) sqrt(500.0 / freq).toFloat() else 1f)
    private val h2 = if (freq < 200) 0.28f else 0f
    private val h3 = if (freq < 200) 0.12f else 0f

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        for (k in 0 until frames) {
            ph += inc; if (ph >= 1.0) ph -= 1.0
            lfo += 0.07 / SR; if (lfo >= 1.0) lfo -= 1.0
            val breathe = 0.86f + 0.14f * sin(TWO_PI * lfo).toFloat()
            val th = TWO_PI * ph
            var v = sin(th).toFloat()
            if (h2 > 0f) v += h2 * sin(2 * th).toFloat() + h3 * sin(3 * th).toFloat()
            v *= amp * breathe
            l[k] += v; r[k] += v
        }
    }
}

/** Binaural beat: carrier in the left ear, carrier + beat in the right ear (headphones). */
class BinauralVoice(private val carrier: Double, private val beat: Double) : Voice() {
    override val reverbSend = 0.0f
    private var pl = 0.0
    private var pr = 0.0
    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        val il = carrier / SR
        val ir = (carrier + beat) / SR
        for (k in 0 until frames) {
            pl += il; if (pl >= 1.0) pl -= 1.0
            pr += ir; if (pr >= 1.0) pr -= 1.0
            l[k] += 0.2f * sin(TWO_PI * pl).toFloat()
            r[k] += 0.2f * sin(TWO_PI * pr).toFloat()
        }
    }
}

class NoiseVoice(private val kind: String, seed: Long) : Voice() {
    override val reverbSend = 0.0f
    private val rng = Rng(seed)
    private val pinkL = PinkNoise(rng); private val pinkR = PinkNoise(rng)
    private val brownL = BrownNoise(rng); private val brownR = BrownNoise(rng)
    private val lpL = OnePoleLP(9000f); private val lpR = OnePoleLP(9000f)
    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        for (k in 0 until frames) {
            when (kind) {
                "white" -> { l[k] += lpL.process(rng.bi()) * 0.12f; r[k] += lpR.process(rng.bi()) * 0.12f }
                "pink" -> { l[k] += pinkL.next() * 0.38f; r[k] += pinkR.next() * 0.38f }
                else -> { l[k] += brownL.next() * 0.4f; r[k] += brownR.next() * 0.4f }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Nature
// ------------------------------------------------------------------------------------------------

class RainVoice(seed: Long, private val intensity: Float = 1f) : Voice() {
    override val reverbSend = 0.12f
    private val rng = Rng(seed)
    private val pinkL = PinkNoise(rng); private val pinkR = PinkNoise(rng)
    private val hpL = Biquad().highpass(450f); private val hpR = Biquad().highpass(450f)
    private val lpL = Biquad().lowpass(7500f); private val lpR = Biquad().lowpass(7500f)
    private val drops = ModalBank(64)
    private val swell = Wander(rng, 0.75f, 1.1f, 2f, 7f)

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        var k = 0
        while (k < frames) {
            val len = min(BLOCK, frames - k)
            val sw = swell.step(len)
            val dropsPerBlock = 55f * intensity * sw * len / SR
            var expected = dropsPerBlock
            while (expected > 0f) {
                if (rng.next() < expected) {
                    if (rng.chance(0.85f)) drops.add(rng.range(2600f, 7200f), rng.range(0.015f, 0.06f) * intensity, rng.range(0.006f, 0.025f), rng.bi())
                    else drops.add(rng.range(650f, 1600f), rng.range(0.03f, 0.08f) * intensity, rng.range(0.025f, 0.07f), rng.bi())
                }
                expected -= 1f
            }
            val a = 0.42f * intensity * sw
            for (i in k until k + len) {
                l[i] += lpL.process(hpL.process(pinkL.next())) * a
                r[i] += lpR.process(hpR.process(pinkR.next())) * a
            }
            drops.render(l, r, k, len)
            k += len
        }
    }
}

class ThunderVoice(seed: Long) : Voice() {
    override val reverbSend = 0.25f
    private val rng = Rng(seed)
    private val rain = RainVoice(seed + 17, 1.15f)
    private val brown = BrownNoise(rng)
    private val lp = Biquad().lowpass(300f)
    private val crackHp = Biquad().highpass(1800f)
    private var nextStrike = (rng.range(3f, 9f) * SR).toInt()
    private var stage = 0
    private var env = 0f
    private var peak = 0f
    private var attackK = 0f
    private var decay = 0f
    private var crackLeft = 0
    private var crackLen = 1

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        rain.render(l, r, frames)
        var k = 0
        while (k < frames) {
            val len = min(BLOCK, frames - k)
            nextStrike -= len
            if (nextStrike <= 0) {
                stage = 1
                peak = rng.range(0.5f, 1f)
                attackK = 1f - exp(-1f / (rng.range(0.15f, 0.9f) * SR))
                decay = t60ToDecay(rng.range(3.5f, 8f))
                if (rng.chance(0.55f)) { crackLen = (rng.range(0.08f, 0.25f) * SR).toInt(); crackLeft = crackLen }
                nextStrike = (rng.range(12f, 34f) * SR).toInt()
            }
            lp.lowpass(110f + 420f * env)
            for (i in k until k + len) {
                when (stage) {
                    1 -> { env += (peak - env) * attackK; if (env > peak * 0.96f) stage = 2 }
                    2 -> { env *= decay; if (env < 0.0008f) { env = 0f; stage = 0 } }
                }
                var v = lp.process(brown.next()) * env * 1.1f
                if (crackLeft > 0) {
                    v += crackHp.process(rng.bi()) * 0.25f * crackLeft / crackLen
                    crackLeft--
                }
                l[i] += v; r[i] += v * 0.92f
            }
            k += len
        }
    }
}

class OceanVoice(seed: Long) : Voice() {
    override val reverbSend = 0.1f
    private val rng = Rng(seed)

    private inner class Wave(phaseOffset: Float) {
        val pink = PinkNoise(rng)
        val brown = BrownNoise(rng)
        val lp = Biquad().lowpass(400f)
        var t = phaseOffset
        var period = rng.range(7f, 12f)
        var env = 0f
        fun control(len: Int) {
            t += len / (period * SR)
            if (t >= 1f) { t -= 1f; period = rng.range(7f, 12.5f) }
            env = if (t < 0.38f) smoothstep(t / 0.38f) else (1f - smoothstep((t - 0.38f) / 0.62f)).pow(1.4f)
            lp.lowpass(220f + 3200f * env * env)
        }
        fun sample(): Float = lp.process(pink.next() * 0.8f + brown.next() * 0.45f) * (0.1f + 0.62f * env)
    }

    private val wl = Wave(0f)
    private val wr = Wave(0.18f)
    private val surf = BrownNoise(rng)
    private val surfLp = OnePoleLP(180f)

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        var k = 0
        while (k < frames) {
            val len = min(BLOCK, frames - k)
            wl.control(len); wr.control(len)
            for (i in k until k + len) {
                val base = surfLp.process(surf.next()) * 0.18f
                val a = wl.sample(); val b = wr.sample()
                l[i] += a * 0.8f + b * 0.2f + base
                r[i] += b * 0.8f + a * 0.2f + base
            }
            k += len
        }
    }
}

class StreamVoice(seed: Long) : Voice() {
    override val reverbSend = 0.12f
    private val rng = Rng(seed)
    private val bp1L = Biquad().bandpass(1100f, 0.6f); private val bp1R = Biquad().bandpass(1300f, 0.6f)
    private val bp2L = Biquad().bandpass(3400f, 1f); private val bp2R = Biquad().bandpass(3000f, 1f)
    private val flow = Wander(rng, 0.7f, 1.1f, 1f, 4f)

    private val max = 24
    private val bf = FloatArray(max); private val bph = FloatArray(max); private val ba = FloatArray(max)
    private val bd = FloatArray(max); private val brise = FloatArray(max); private val bl = FloatArray(max); private val br = FloatArray(max)
    private var count = 0

    private fun spawn() {
        if (count >= max) return
        val i = count++
        bf[i] = rng.range(420f, 2100f)
        bph[i] = 0f
        ba[i] = rng.range(0.03f, 0.11f)
        val life = rng.range(0.03f, 0.09f)
        bd[i] = t60ToDecay(life)
        brise[i] = exp(kotlin.math.ln(rng.range(1.4f, 2.3f)) / (life * SR))
        val (gl, gr) = panGains(rng.bi() * 0.8f)
        bl[i] = gl; br[i] = gr
    }

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        var k = 0
        while (k < frames) {
            val len = min(BLOCK, frames - k)
            val f = flow.step(len)
            var expected = 34f * f * len / SR
            while (expected > 0f) { if (rng.next() < expected) spawn(); expected -= 1f }
            for (i in k until k + len) {
                val nl = rng.bi(); val nr = rng.bi()
                var vl = (bp1L.process(nl) * 0.1f + bp2L.process(nl) * 0.045f) * f
                var vr = (bp1R.process(nr) * 0.1f + bp2R.process(nr) * 0.045f) * f
                var j = 0
                while (j < count) {
                    bph[j] += bf[j] / SR
                    if (bph[j] >= 1f) bph[j] -= 1f
                    val s = sin(TWO_PI_F * bph[j]) * ba[j]
                    vl += s * bl[j]; vr += s * br[j]
                    ba[j] *= bd[j]; bf[j] *= brise[j]
                    if (ba[j] < 0.0004f || bf[j] > 8000f) {
                        count--
                        bf[j] = bf[count]; bph[j] = bph[count]; ba[j] = ba[count]; bd[j] = bd[count]
                        brise[j] = brise[count]; bl[j] = bl[count]; br[j] = br[count]
                    } else j++
                }
                l[i] += vl; r[i] += vr
            }
            k += len
        }
    }
}

class WindVoice(seed: Long) : Voice() {
    override val reverbSend = 0.15f
    private val rng = Rng(seed)
    private val bpL = Biquad(); private val bpR = Biquad()
    private val fL = Wander(rng, 260f, 1100f, 2f, 6f); private val fR = Wander(rng, 260f, 1100f, 2f, 6f)
    private val qW = Wander(rng, 0.9f, 2.6f, 3f, 8f)
    private val gust = Wander(rng, 0.25f, 1f, 1.5f, 5f)
    private val brown = BrownNoise(rng)
    private val low = OnePoleLP(220f)

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        var k = 0
        while (k < frames) {
            val len = min(BLOCK, frames - k)
            val q = qW.step(len)
            bpL.bandpass(fL.step(len), q); bpR.bandpass(fR.step(len), q)
            val g = gust.step(len)
            for (i in k until k + len) {
                val rumble = low.process(brown.next()) * 0.18f * g
                l[i] += bpL.process(rng.bi()) * 0.55f * g + rumble
                r[i] += bpR.process(rng.bi()) * 0.55f * g + rumble
            }
            k += len
        }
    }
}

class FireVoice(seed: Long) : Voice() {
    override val reverbSend = 0.08f
    private val rng = Rng(seed)
    private val brown = BrownNoise(rng)
    private val lp = Biquad().lowpass(420f)
    private val hissHp = Biquad().highpass(3500f)
    private val flicker = Wander(rng, 0.65f, 1.1f, 0.2f, 0.9f)
    private val hpL = Biquad().highpass(1400f); private val hpR = Biquad().highpass(1400f)
    private val pops = ModalBank(16)

    private val maxB = 16
    private val ba = FloatArray(maxB); private val bd = FloatArray(maxB)
    private val bl = FloatArray(maxB); private val br = FloatArray(maxB)
    private var nb = 0

    private fun crackle(amp: Float) {
        if (nb >= maxB) return
        val i = nb++
        ba[i] = amp; bd[i] = t60ToDecay(rng.range(0.003f, 0.02f))
        val (gl, gr) = panGains(rng.bi() * 0.7f)
        bl[i] = gl; br[i] = gr
    }

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        var k = 0
        while (k < frames) {
            val len = min(BLOCK, frames - k)
            val fl = flicker.step(len)
            if (rng.next() < 7f * len / SR) {
                crackle(rng.range(0.15f, 0.6f))
                if (rng.chance(0.35f)) repeat(rng.int(4) + 1) { crackle(rng.range(0.05f, 0.35f)) }
            }
            if (rng.next() < 0.6f * len / SR) pops.add(rng.range(140f, 320f), rng.range(0.05f, 0.12f), rng.range(0.04f, 0.09f), rng.bi() * 0.5f)
            for (i in k until k + len) {
                val base = lp.process(brown.next()) * 0.32f * fl + hissHp.process(rng.bi()) * 0.012f
                var cl = 0f; var cr = 0f
                var j = 0
                while (j < nb) {
                    val n = rng.bi() * ba[j]
                    cl += n * bl[j]; cr += n * br[j]
                    ba[j] *= bd[j]
                    if (ba[j] < 0.001f) {
                        nb--; ba[j] = ba[nb]; bd[j] = bd[nb]; bl[j] = bl[nb]; br[j] = br[nb]
                    } else j++
                }
                l[i] += base + hpL.process(cl) * 0.45f
                r[i] += base + hpR.process(cr) * 0.45f
            }
            pops.render(l, r, k, len)
            k += len
        }
    }
}

class BirdsVoice(seed: Long) : Voice() {
    override val reverbSend = 0.35f
    private val rng = Rng(seed)

    private val max = 24
    private val delay = IntArray(max); private val dur = IntArray(max); private val t = IntArray(max)
    private val f1 = FloatArray(max); private val f2 = FloatArray(max)
    private val fmRate = FloatArray(max); private val fmDepth = FloatArray(max)
    private val amp = FloatArray(max); private val ph = FloatArray(max)
    private val gl = FloatArray(max); private val gr = FloatArray(max)
    private var n = 0
    private var nextCall = (0.4f * SR).toInt()
    private val leaves = PinkNoise(rng)
    private val leavesHp = Biquad().highpass(2500f)
    private val rustle = Wander(rng, 0.2f, 1f, 1f, 4f)

    private fun syl(d: Int, len: Float, a: Float, b: Float, fmr: Float, fmd: Float, am: Float, pan: Float) {
        if (n >= max) return
        val i = n++
        delay[i] = d; dur[i] = (len * SR).toInt().coerceAtLeast(64); t[i] = 0
        f1[i] = a; f2[i] = b; fmRate[i] = fmr; fmDepth[i] = fmd; amp[i] = am; ph[i] = 0f
        val (pl, pr) = panGains(pan); gl[i] = pl; gr[i] = pr
    }

    private fun call() {
        val pan = rng.bi() * 0.85f
        val a = rng.range(0.04f, 0.11f)
        val ps = rng.range(0.85f, 1.2f)
        var at = 0
        when (rng.int(4)) {
            0 -> repeat(rng.int(3) + 2) {   // whistle
                val len = rng.range(0.12f, 0.24f)
                syl(at, len, 2200f * ps, rng.range(2800f, 3400f) * ps, 0f, 0f, a, pan)
                at += ((len + rng.range(0.06f, 0.12f)) * SR).toInt()
            }
            1 -> repeat(rng.int(7) + 6) {   // trill
                val len = rng.range(0.03f, 0.05f)
                syl(at, len, 4100f * ps, 3600f * ps, 0f, 0f, a * 0.8f, pan)
                at += ((len + rng.range(0.015f, 0.025f)) * SR).toInt()
            }
            2 -> repeat(rng.int(4) + 2) {   // falling chirps
                val len = rng.range(0.06f, 0.1f)
                syl(at, len, rng.range(5000f, 5800f) * ps, 2800f * ps, 0f, 0f, a, pan)
                at += ((len + rng.range(0.08f, 0.15f)) * SR).toInt()
            }
            else -> repeat(rng.int(2) + 1) { // warble
                val len = rng.range(0.3f, 0.6f)
                val c = rng.range(2700f, 3300f) * ps
                syl(at, len, c, c * rng.range(0.95f, 1.1f), rng.range(18f, 30f), rng.range(350f, 750f), a * 0.9f, pan)
                at += ((len + rng.range(0.1f, 0.2f)) * SR).toInt()
            }
        }
    }

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        var k = 0
        while (k < frames) {
            val len = min(BLOCK, frames - k)
            nextCall -= len
            if (nextCall <= 0) { call(); nextCall = (rng.range(0.5f, 3.2f) * SR).toInt() }
            val rs = rustle.step(len)
            for (i in k until k + len) {
                val leaf = leavesHp.process(leaves.next()) * 0.025f * rs
                var vl = leaf; var vr = leaf
                var j = 0
                while (j < n) {
                    if (delay[j] > 0) { delay[j]--; j++; continue }
                    val u = t[j].toFloat() / dur[j]
                    val f = f1[j] + (f2[j] - f1[j]) * u + fmDepth[j] * sin(TWO_PI_F * fmRate[j] * t[j] / SR)
                    ph[j] += f / SR; if (ph[j] >= 1f) ph[j] -= 1f
                    val e = sin(PI.toFloat() * u)
                    val s = sin(TWO_PI_F * ph[j]) * e * e * amp[j]
                    vl += s * gl[j]; vr += s * gr[j]
                    if (++t[j] >= dur[j]) {
                        n--
                        delay[j] = delay[n]; dur[j] = dur[n]; t[j] = t[n]; f1[j] = f1[n]; f2[j] = f2[n]
                        fmRate[j] = fmRate[n]; fmDepth[j] = fmDepth[n]; amp[j] = amp[n]; ph[j] = ph[n]; gl[j] = gl[n]; gr[j] = gr[n]
                    } else j++
                }
                l[i] += vl; r[i] += vr
            }
            k += len
        }
    }
}

class CricketsVoice(seed: Long) : Voice() {
    override val reverbSend = 0.25f
    private val rng = Rng(seed)

    private inner class Cricket {
        val freq = rng.range(4200f, 4900f)
        private val pan = panGains(rng.bi() * 0.9f)
        val gl = pan.first
        val gr = pan.second
        val amp = rng.range(0.02f, 0.04f)
        var ph = 0f
        var t = -(rng.range(0f, 1f) * SR).toInt()
        val period = (rng.range(0.7f, 1.25f) * SR).toInt()
        val pulses = rng.int(2) + 3
        val pulseLen = (0.018f * SR).toInt()
        val gap = (0.012f * SR).toInt()
        fun sample(): Float {
            t++
            if (t >= period) t = 0
            if (t < 0) return 0f
            val slot = pulseLen + gap
            val p = t / slot
            if (p >= pulses) return 0f
            val within = t - p * slot
            if (within >= pulseLen) return 0f
            ph += freq / SR; if (ph >= 1f) ph -= 1f
            val e = sin(PI.toFloat() * within / pulseLen)
            return sin(TWO_PI_F * ph) * e * amp
        }
    }

    private val crickets = List(3) { Cricket() }
    private var trillPh = 0f
    private var trillAm = 0f
    private val night = BrownNoise(rng)
    private val nightLp = OnePoleLP(300f)

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        for (k in 0 until frames) {
            var vl = 0f; var vr = 0f
            for (c in crickets) { val s = c.sample(); vl += s * c.gl; vr += s * c.gr }
            trillPh += 3800f / SR; if (trillPh >= 1f) trillPh -= 1f
            trillAm += 24f / SR; if (trillAm >= 1f) trillAm -= 1f
            val tr = sin(TWO_PI_F * trillPh) * (0.5f + 0.5f * sin(TWO_PI_F * trillAm)) * 0.008f
            val bg = nightLp.process(night.next()) * 0.06f
            l[k] += vl + tr * 0.7f + bg; r[k] += vr + tr + bg
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Instruments
// ------------------------------------------------------------------------------------------------

/** Bansuri-style bamboo flute improvising in raga Bhupali (Sa Re Ga Pa Dha). */
class FluteVoice(seed: Long) : Voice() {
    override val reverbSend = 0.45f
    private val rng = Rng(seed)
    private val tonic = 293.66f
    private val degrees = intArrayOf(-5, -3, 0, 2, 4, 7, 9, 12, 14)
    private val tonicIdx = 2
    private var idx = tonicIdx
    private val freq = Smooth(tonic, 0.06f)
    private val env = Smooth(0f, 0.12f)
    private val vib = Smooth(0f, 0.25f)
    private val chiff = Smooth(0f, 0.04f)
    private var ph = 0f
    private var vibPh = 0f
    private var noteLeft = (0.5f * SR).toInt()
    private var noteAge = 0
    private var phraseLeft = 0
    private var resting = true
    private val breath = Biquad()
    private val breathLp = OnePoleLP(5000f)

    private fun hz(i: Int) = tonic * 2f.pow(degrees[i] / 12f)

    private fun nextNote() {
        if (resting) {
            resting = false
            phraseLeft = rng.int(5) + 3
            idx = if (rng.chance(0.5f)) tonicIdx else rng.int(degrees.size)
            env.time(0.14f); env.target = 1f
        } else {
            var step = rng.int(5) - 2
            if (step == 0) step = if (rng.chance(0.5f)) 1 else -1
            if (idx + step !in degrees.indices) step = -step
            idx = (idx + step).coerceIn(0, degrees.size - 1)
        }
        phraseLeft--
        val target = hz(idx)
        if (rng.chance(0.25f) && idx > 0) { freq.value = hz(idx - 1); freq.time(0.12f) } else freq.time(0.05f)
        freq.target = target
        chiff.value = 1f; chiff.target = 0f
        vib.value = 0f; vib.target = 0f
        noteAge = 0
        val last = phraseLeft <= 0
        if (last && rng.chance(0.6f)) { idx = if (rng.chance(0.6f)) tonicIdx else 5; freq.target = hz(idx) }
        val d = if (last) rng.range(2.4f, 4f) else rng.pick(listOf(0.45f, 0.7f, 0.9f, 1.3f, 1.8f, 2.4f))
        noteLeft = (d * SR).toInt()
    }

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        var k = 0
        while (k < frames) {
            val len = min(BLOCK, frames - k)
            noteLeft -= len; noteAge += len
            if (noteLeft <= 0) {
                if (!resting && phraseLeft <= 0) {
                    resting = true
                    env.time(0.45f); env.target = 0f
                    noteLeft = (rng.range(1.5f, 4f) * SR).toInt()
                } else nextNote()
            }
            if (noteAge > 0.35f * SR) vib.target = 0.006f
            breath.bandpass(freq.value * 2.2f, 1.4f)
            for (i in k until k + len) {
                vibPh += 5.2f / SR; if (vibPh >= 1f) vibPh -= 1f
                val f = freq.next() * (1f + vib.next() * sin(TWO_PI_F * vibPh))
                ph += f / SR; if (ph >= 1f) ph -= 1f
                val th = TWO_PI_F * ph
                val s1 = sin(th); val c1 = kotlin.math.cos(th)
                val s2 = 2f * s1 * c1; val c2 = 1f - 2f * s1 * s1
                val s3 = s2 * c1 + c2 * s1
                val e = env.next()
                val ch = chiff.next()
                val noise = breathLp.process(breath.process(rng.bi())) * (0.35f + 1.2f * ch)
                val v = ((s1 + 0.3f * s2 + 0.09f * s3) * 0.2f + noise * 0.12f) * e
                l[i] += v * 0.95f; r[i] += v
            }
            k += len
        }
    }
}

/** Wind-driven chimes (shared by bamboo and metal chimes). */
abstract class ChimeVoice(seed: Long) : Voice() {
    protected val rng = Rng(seed)
    protected val bank = ModalBank(88)
    private var nextGust = (rng.range(0.5f, 2f) * SR).toInt()
    private var gustLeft = 0
    private var last = -1
    abstract val pitches: FloatArray
    abstract val strikeRate: Float
    abstract fun strike(f: Float, pan: Float, amp: Float)
    private val windBp = Biquad().bandpass(700f, 0.8f)
    private val windEnv = Smooth(0f, 0.6f)

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        var k = 0
        while (k < frames) {
            val len = min(BLOCK, frames - k)
            if (gustLeft > 0) {
                gustLeft -= len
                if (rng.next() < strikeRate * len / SR) {
                    var p = rng.int(pitches.size)
                    if (p == last) p = (p + 1) % pitches.size
                    last = p
                    val pan = -0.8f + 1.6f * p / (pitches.size - 1).coerceAtLeast(1)
                    strike(pitches[p], pan, rng.range(0.5f, 1f))
                }
                if (gustLeft <= 0) { windEnv.target = 0f; nextGust = (rng.range(2.5f, 7f) * SR).toInt() }
            } else {
                nextGust -= len
                if (nextGust <= 0) { gustLeft = (rng.range(0.8f, 2.6f) * SR).toInt(); windEnv.target = 1f }
            }
            for (i in k until k + len) {
                val w = windBp.process(rng.bi()) * 0.03f * windEnv.next()
                l[i] += w; r[i] += w
            }
            bank.render(l, r, k, len)
            k += len
        }
    }
}

class BambooChimesVoice(seed: Long) : ChimeVoice(seed) {
    override val reverbSend = 0.3f
    override val pitches = floatArrayOf(1f, 9f / 8f, 5f / 4f, 3f / 2f, 5f / 3f, 2f).map { it * 330f }.toFloatArray()
    override val strikeRate = 6f
    override fun strike(f: Float, pan: Float, amp: Float) {
        bank.add(f, 0.22f * amp, rng.range(0.3f, 0.55f), pan)
        bank.add(f * 2.76f, 0.07f * amp, 0.14f, pan)
        bank.add(rng.range(1700f, 2600f), 0.06f * amp, 0.012f, pan)
    }
}

class WindChimesVoice(seed: Long) : ChimeVoice(seed) {
    override val reverbSend = 0.3f
    override val pitches = floatArrayOf(1f, 9f / 8f, 5f / 4f, 3f / 2f, 5f / 3f, 2f, 9f / 4f).map { it * 880f }.toFloatArray()
    override val strikeRate = 4f
    override fun strike(f: Float, pan: Float, amp: Float) {
        val a = 0.07f * amp
        bank.add(f, a, 4.5f, pan)
        bank.add(f * 1.0015f, a * 0.4f, 4f, pan)
        bank.add(f * 2.76f, a * 0.5f, 2.2f, pan)
        bank.add(f * 5.4f, a * 0.25f, 1.1f, pan)
        bank.add(f * 8.93f, a * 0.12f, 0.6f, pan)
    }
}

class TempleBellVoice(seed: Long) : Voice() {
    override val reverbSend = 0.3f
    private val rng = Rng(seed)
    private val bank = ModalBank(40)
    private var next = (1.4f * SR).toInt()
    private val partials = arrayOf(
        floatArrayOf(0.5f, 0.35f, 14f), floatArrayOf(1f, 0.4f, 10f), floatArrayOf(1.183f, 0.3f, 8f),
        floatArrayOf(1.506f, 0.18f, 6f), floatArrayOf(2f, 0.35f, 7f), floatArrayOf(2.514f, 0.12f, 3.5f),
        floatArrayOf(2.662f, 0.1f, 3f), floatArrayOf(3.011f, 0.08f, 2.5f), floatArrayOf(4.166f, 0.06f, 1.6f),
        floatArrayOf(5.433f, 0.04f, 1f),
    )

    private fun strike() {
        val nominal = rng.pick(listOf(311f, 330f, 349f))
        val a = rng.range(0.75f, 1f) * 0.32f
        for ((i, p) in partials.withIndex()) {
            bank.add(nominal * p[0], a * p[1], p[2], 0f)
            if (i < 4) bank.add(nominal * p[0] * 1.0025f, a * p[1] * 0.6f, p[2] * 0.9f, 0f)
        }
        bank.add(rng.range(1800f, 2600f), 0.05f, 0.05f, 0f)
    }

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        var k = 0
        while (k < frames) {
            val len = min(BLOCK, frames - k)
            next -= len
            if (next <= 0) { strike(); next = (rng.range(8f, 15f) * SR).toInt() }
            bank.render(l, r, k, len)
            k += len
        }
    }
}

class SingingBowlVoice(seed: Long) : Voice() {
    override val reverbSend = 0.25f
    private val rng = Rng(seed)
    private val bank = ModalBank(48)
    private val f0 = rng.pick(listOf(196f, 220f, 233f))
    private var next = (1.2f * SR).toInt()
    private val modes = arrayOf(
        floatArrayOf(1f, 1f, 22f, 0.8f), floatArrayOf(2.71f, 0.45f, 12f, 1.6f),
        floatArrayOf(5.15f, 0.2f, 6f, 2.5f), floatArrayOf(8.17f, 0.08f, 3f, 3.5f),
    )
    private var p1 = 0f; private var p2 = 0f; private var swellPh = 0f

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        var k = 0
        while (k < frames) {
            val len = min(BLOCK, frames - k)
            next -= len
            if (next <= 0) {
                val a = rng.range(0.7f, 1f) * 0.17f
                for (m in modes) {
                    bank.add(f0 * m[0], a * m[1], m[2], -0.25f)
                    bank.add(f0 * m[0] + m[3], a * m[1] * 0.8f, m[2], 0.25f)
                }
                next = (rng.range(12f, 20f) * SR).toInt()
            }
            for (i in k until k + len) {
                swellPh += 0.05f / SR; if (swellPh >= 1f) swellPh -= 1f
                val sw = 0.5f - 0.5f * kotlin.math.cos(TWO_PI_F * swellPh)
                p1 += f0 / SR; if (p1 >= 1f) p1 -= 1f
                p2 += f0 * 2.71f / SR; if (p2 >= 1f) p2 -= 1f
                val v = (sin(TWO_PI_F * p1) * 0.035f + sin(TWO_PI_F * p2) * 0.012f) * sw
                l[i] += v; r[i] += v
            }
            bank.render(l, r, k, len)
            k += len
        }
    }
}

class CrystalBowlVoice(seed: Long) : Voice() {
    override val reverbSend = 0.35f
    private val rng = Rng(seed)
    private val bank = ModalBank(12)
    private val f = 256f
    private var a = 0f; private var b = 0f; private var c = 0f; private var swell = 0f
    private var next = (1.2f * SR).toInt()
    private val fadeIn = Smooth(0f, 2.5f).also { it.target = 1f }

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        var k = 0
        while (k < frames) {
            val len = min(BLOCK, frames - k)
            next -= len
            if (next <= 0) {
                bank.add(f, 0.1f, 15f, -0.1f); bank.add(f * 2.76f, 0.025f, 4f, 0.1f)
                next = (rng.range(20f, 30f) * SR).toInt()
            }
            for (i in k until k + len) {
                a += f / SR; if (a >= 1f) a -= 1f
                b += (f + 0.35f) / SR; if (b >= 1f) b -= 1f
                c += f * 2.76f / SR; if (c >= 1f) c -= 1f
                swell += 0.06f / SR; if (swell >= 1f) swell -= 1f
                val sw = (0.6f + 0.4f * sin(TWO_PI_F * swell)) * fadeIn.next()
                val v = (sin(TWO_PI_F * a) * 0.1f + sin(TWO_PI_F * b) * 0.1f + sin(TWO_PI_F * c) * 0.012f) * sw
                l[i] += v * 0.95f; r[i] += v
            }
            bank.render(l, r, k, len)
            k += len
        }
    }
}

/** Karplus-Strong string, used by the tanpura. */
class KsString(freq: Float, private val loss: Float, private val rng: Rng) {
    private val n = (SR / freq).roundToInt().coerceAtLeast(8)
    private val buf = FloatArray(n)
    private var idx = 0
    fun pluck(amp: Float) {
        val lp = OnePoleLP(2600f)
        for (i in 0 until n) buf[i] += lp.process(rng.bi()) * amp * 1.6f
    }
    fun next(): Float {
        val cur = buf[idx]
        val nx = buf[if (idx + 1 >= n) 0 else idx + 1]
        buf[idx] = (cur + nx) * 0.5f * loss
        idx = if (idx + 1 >= n) 0 else idx + 1
        return cur
    }
}

/** Tanpura drone (Pa - Sa - Sa - low Sa) with a sweeping "jawari" shimmer. */
class TanpuraVoice(seed: Long) : Voice() {
    override val reverbSend = 0.3f
    private val rng = Rng(seed)
    private val sa = 136.1f
    private val strings = listOf(
        KsString(sa * 0.75f, 0.9985f, rng), KsString(sa, 0.998f, rng),
        KsString(sa * 1.002f, 0.998f, rng), KsString(sa * 0.5f, 0.999f, rng),
    )
    private val pans = floatArrayOf(-0.5f, 0.15f, 0.35f, -0.15f)
    private val gaps = floatArrayOf(0.95f, 0.95f, 0.95f, 1.7f)
    private var which = 0
    private var next = (0.1f * SR).toInt()
    private val delay = FloatArray(512)
    private var dIdx = 0
    private var sweep = 0f
    private val gains = panGainsList()
    private val toneL = Biquad().lowpass(4200f)
    private val toneR = Biquad().lowpass(4200f)

    private fun panGainsList(): List<Pair<Float, Float>> = pans.map { panGains(it) }

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        var k = 0
        while (k < frames) {
            val len = min(BLOCK, frames - k)
            next -= len
            if (next <= 0) {
                strings[which].pluck(if (which == 3) 0.55f else 0.45f)
                sweep = 0f
                next = (gaps[which] * SR).toInt()
                which = (which + 1) % strings.size
            }
            for (i in k until k + len) {
                var vl = 0f; var vr = 0f
                for (s in strings.indices) {
                    val v = strings[s].next()
                    vl += v * gains[s].first; vr += v * gains[s].second
                }
                sweep += 1f / (1.6f * SR); if (sweep > 1f) sweep = 1f
                val dSamples = (0.0006f + 0.0026f * sweep) * SR
                val mono = (vl + vr) * 0.5f
                delay[dIdx] = mono
                val rd = (dIdx - dSamples.toInt() + delay.size) % delay.size
                val jaw = delay[rd]
                dIdx = (dIdx + 1) % delay.size
                l[i] += toneL.process(vl + jaw * 0.45f) * 0.55f
                r[i] += toneR.process(vr + jaw * 0.45f) * 0.55f
            }
            k += len
        }
    }
}

class KalimbaVoice(seed: Long) : Voice() {
    override val reverbSend = 0.4f
    private val rng = Rng(seed)
    private val bank = ModalBank(48)
    private val tonic = 523.25f
    private val deg = intArrayOf(0, 2, 4, 7, 9, 12, 14, 16)
    private var idx = 0
    private var left = 0
    private var next = (0.3f * SR).toInt()

    private fun hz(i: Int) = tonic * 2f.pow(deg[i] / 12f)

    private fun note(f: Float, a: Float, pan: Float) {
        bank.add(f, 0.16f * a, 1.6f, pan)
        bank.add(f * 5.95f, 0.035f * a, 0.18f, pan)
        bank.add(rng.range(2600f, 3400f), 0.02f * a, 0.01f, pan)
    }

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        var k = 0
        while (k < frames) {
            val len = min(BLOCK, frames - k)
            next -= len
            if (next <= 0) {
                if (left <= 0) {
                    left = rng.int(7) + 4
                    idx = rng.int(4)
                }
                var step = rng.int(5) - 2
                if (idx + step !in deg.indices) step = -step
                idx = (idx + step).coerceIn(0, deg.size - 1)
                val pan = -0.6f + 1.2f * idx / (deg.size - 1)
                val a = rng.range(0.6f, 1f)
                note(hz(idx), a, pan)
                if (rng.chance(0.15f) && idx + 3 < deg.size) note(hz(idx + 3), a * 0.6f, pan)
                left--
                next = if (left <= 0) (rng.range(1.5f, 3.2f) * SR).toInt() else (rng.range(0.25f, 0.6f) * SR).toInt()
            }
            bank.render(l, r, k, len)
            k += len
        }
    }
}

/** A breathing OM chant drone: "O" formants that close into an "M" hum. */
class OmVoice(seed: Long) : Voice() {
    override val reverbSend = 0.4f
    private val rng = Rng(seed)
    private val f0 = 136.1f
    private var t1 = 0f; private var t2 = 0.3f; private var sub = 0f
    private val f1L = Biquad().bandpass(450f, 4f); private val f2L = Biquad().bandpass(800f, 5f)
    private val mLp = Biquad().lowpass(280f, 0.8f)
    private var cyc = 0f
    private val period = 9f
    private val air = Biquad().bandpass(1500f, 0.7f)

    private fun blep(t: Float, dt: Float): Float = when {
        t < dt -> { val x = t / dt; x + x - x * x - 1f }
        t > 1f - dt -> { val x = (t - 1f) / dt; x * x + x + x + 1f }
        else -> 0f
    }

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        val dt1 = f0 / SR
        val dt2 = f0 * 1.004f / SR
        for (k in 0 until frames) {
            cyc += 1f / (period * SR); if (cyc >= 1f) cyc -= 1f
            val u = cyc
            val amp = when {
                u < 0.08f -> 0.15f + 0.85f * smoothstep(u / 0.08f)
                u < 0.9f -> 1f
                else -> 1f - 0.85f * smoothstep((u - 0.9f) / 0.1f)
            }
            val oMix = 1f - smoothstep((u - 0.5f) / 0.2f)
            val mMix = 1f - oMix
            t1 += dt1; if (t1 >= 1f) t1 -= 1f
            t2 += dt2; if (t2 >= 1f) t2 -= 1f
            sub += f0 * 0.5f / SR; if (sub >= 1f) sub -= 1f
            val saw = (2f * t1 - 1f - blep(t1, dt1)) + (2f * t2 - 1f - blep(t2, dt2))
            val o = f1L.process(saw) * 1.0f + f2L.process(saw) * 0.55f
            val m = mLp.process(saw) * 0.5f
            val breath = air.process(rng.bi()) * 0.02f * (if (u > 0.9f) 1f else 0.2f)
            val v = ((o * oMix + m * mMix) * 0.22f + sin(TWO_PI_F * sub) * 0.05f) * amp + breath
            l[k] += v; r[k] += v
        }
    }
}

/** Low pitched "thump" with a falling pitch (used by drum and heartbeat). */
private class Thumps(private val maxN: Int = 6) {
    val ph = FloatArray(maxN); val f = FloatArray(maxN); val fEnd = FloatArray(maxN); val fK = FloatArray(maxN)
    val a = FloatArray(maxN); val d = FloatArray(maxN); val h2 = FloatArray(maxN)
    var n = 0
    fun add(start: Float, end: Float, amp: Float, t60: Float, harmonic: Float) {
        if (n >= maxN) return
        val i = n++
        ph[i] = 0f; f[i] = start; fEnd[i] = end; fK[i] = 1f - exp(-1f / (0.045f * SR)); a[i] = amp; d[i] = t60ToDecay(t60); h2[i] = harmonic
    }
    fun next(): Float {
        var v = 0f
        var i = 0
        while (i < n) {
            f[i] += (fEnd[i] - f[i]) * fK[i]
            ph[i] += f[i] / SR; if (ph[i] >= 1f) ph[i] -= 1f
            val th = TWO_PI_F * ph[i]
            v += (sin(th) + h2[i] * sin(2f * th)) * a[i]
            a[i] *= d[i]
            if (a[i] < 0.0005f) {
                n--; ph[i] = ph[n]; f[i] = f[n]; fEnd[i] = fEnd[n]; fK[i] = fK[n]; a[i] = a[n]; d[i] = d[n]; h2[i] = h2[n]
            } else i++
        }
        return v
    }
}

class FrameDrumVoice(seed: Long) : Voice() {
    override val reverbSend = 0.25f
    private val rng = Rng(seed)
    private val thumps = Thumps()
    private val bank = ModalBank(24)
    private val beat = (60f / 66f * SR).toInt()
    private var next = (0.2f * SR).toInt()
    private var count = 0

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        var k = 0
        while (k < frames) {
            val len = min(BLOCK, frames - k)
            next -= len
            if (next <= 0) {
                val strong = count % 4 == 0
                val a = (if (strong) 0.55f else 0.3f) * rng.range(0.9f, 1.05f)
                val f0 = if (strong) 92f else 118f
                thumps.add(f0 * 1.8f, f0, a, 0.5f, 0.35f)
                bank.add(f0 * 1.59f, 0.12f * a, 0.25f, 0f)
                bank.add(f0 * 2.14f, 0.08f * a, 0.18f, 0f)
                bank.add(f0 * 2.3f, 0.06f * a, 0.15f, 0f)
                bank.add(rng.range(800f, 1000f), 0.06f * a, 0.03f, 0f)
                count++
                next = beat
            }
            for (i in k until k + len) { val v = thumps.next(); l[i] += v; r[i] += v }
            bank.render(l, r, k, len)
            k += len
        }
    }
}

class HeartbeatVoice(seed: Long) : Voice() {
    override val reverbSend = 0.05f
    private val rng = Rng(seed)
    private val thumps = Thumps()
    private val lp = OnePoleLP(260f)
    private var next = (0.2f * SR).toInt()
    private var dub = -1

    override fun render(l: FloatArray, r: FloatArray, frames: Int) {
        var k = 0
        while (k < frames) {
            val len = min(BLOCK, frames - k)
            next -= len
            if (next <= 0) {
                thumps.add(80f, 55f, 0.75f * rng.range(0.95f, 1.05f), 0.16f, 0.6f)
                dub = (0.28f * SR).toInt()
                next = (rng.range(0.98f, 1.03f) * SR).toInt()
            }
            if (dub > 0) {
                dub -= len
                if (dub <= 0) { thumps.add(95f, 68f, 0.48f, 0.13f, 0.6f); dub = -1 }
            }
            for (i in k until k + len) { val v = lp.process(thumps.next()) * 1.3f; l[i] += v; r[i] += v }
            k += len
        }
    }
}

/** Utility for tests: peak absolute value. */
fun peakOf(a: FloatArray): Float { var p = 0f; for (x in a) p = max(p, abs(x)); return p }
