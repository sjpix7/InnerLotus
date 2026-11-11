// JVM tests for the pure-Kotlin parts of Inner Lotus (no Android needed).
// Run: tools/jvmtest/run.sh
import com.sjpixels.innerlotus.audio.dsp.*
import com.sjpixels.innerlotus.content.*
import com.sjpixels.innerlotus.core.*
import com.sjpixels.innerlotus.data.*
import java.io.File
import java.time.LocalDate

fun check(cond: Boolean, msg: String) { if (!cond) { println("FAIL: $msg"); failures++ } else println("ok   $msg") }
var failures = 0

fun main(args: Array<String>) {
    val out = File(args.getOrElse(0) { "build/jvmtest" }).apply { mkdirs() }

    // ---- Composer: 1008 unique cards per area
    for (c in Composer.CHAKRAS) {
        val d = Composer.chakraDeck(c, 24, 6)
        check(d.size == 144 && d.map { it.line to it.seed }.toSet().size == 144, "chakra deck $c has 144 unique (line,seed)")
        check((1 until d.size).none { d[it].line == d[it - 1].line }, "no repeated area line back-to-back ($c)")
    }
    val all = Composer.deck(null, 24, 6, 5L)
    check(all.size == 1008 && all.map { it.key }.toSet().size == 1008, "random deck has 1008 unique cards")
    check(Composer.deck("heart", 24, 6, 3L).toSet() == Composer.chakraDeck("heart", 24, 6).toSet(), "shuffled deck keeps the same cards")

    // ---- Real content: every language, every area, both address forms -> 1008 unique cards
    val i18nDir = File(System.getProperty("i18n.dir") ?: "app/src/main/assets/i18n")
    if (i18nDir.isDirectory) {
        val en = StringTable.parse(File(i18nDir, "en.json").readText())
        for (lang in Lang.entries) {
            val f = File(i18nDir, "${lang.code}.json")
            check(f.isFile, "i18n file exists for ${lang.code}")
            if (!f.isFile) continue
            val t = StringTable(StringTable.parse(f.readText()), en)
            var allUnique = true
            var stray = 0
            for (fem in listOf(true, false)) {
                for (area in Catalog.areas) {
                    val lines = t.list("area.${area.id}.lines", fem)
                    val cards = HashSet<String>()
                    for (ch in Composer.CHAKRAS) {
                        val seeds = t.list("chakra.$ch.seeds", fem)
                        for (r in Composer.chakraDeck(ch, 24, 6)) cards += seeds[r.seed] + "|" + lines[r.line]
                    }
                    if (lines.size != 24 || cards.size != 1008) allUnique = false
                    stray += lines.count { it.contains('{') || it.contains('|') }
                }
            }
            check(allUnique, "${lang.code}: 33 areas x 1008 unique cards (both forms)")
            check(stray == 0, "${lang.code}: no unresolved markup in affirmations")
        }
    }

    // ---- Strings + gender
    val table =StringTable(StringTable.parse("""{"a":"Je suis {prêt|prête}","l":["x {m|f}","y"],"p":"Hi {name}"}"""))
    check(table.str("a", true) == "Je suis prête" && table.str("a", false) == "Je suis prêt", "gender markup resolves")
    check(table.list("l", true) == listOf("x f", "y"), "gender markup in lists")
    check(table.str("p", false).fill("name" to "Asha") == "Hi Asha", "placeholder fill")
    check(table.str("missing", false) == "missing", "missing key falls back to key")

    // ---- Singular variants ("<key>__one" when n == 1, only from the current language)
    val enP = StringTable.parse("""{"d":"{n} days","d__one":"{n} day"}""")
    val jaP = StringTable(StringTable.parse("""{"d":"{n}日"}"""), enP)
    val en1 = StringTable(enP)
    check(en1.str(en1.pluralKey("d", 1), false).fill("n" to 1) == "1 day", "singular variant used for n=1")
    check(en1.str(en1.pluralKey("d", 2), false).fill("n" to 2) == "2 days", "plural used for n=2")
    check(en1.pluralKey("d", 0) == "d" && en1.pluralKey("d", null) == "d", "plural used for 0 / no count")
    check(jaP.pluralKey("d", 1) == "d", "singular never borrowed from English fallback")

    // ---- Prayer
    val pr = Prayer.build("O", "{name}, I bring you", "F", listOf("1", "2", "3", "4"), "C", "Ravi", 2)
    check(pr.startsWith("O") && pr.contains("Ravi, I bring you") && pr.endsWith("C"), "prayer builds with name")

    // ---- Stores round trip
    val dir = File(out, "store").apply { deleteRecursively(); mkdirs() }
    val aff = AffirmationStore(File(dir, "aff.json"))
    check(aff.addCustom("wealth", listOf("I am rich", "I am rich", " ", "Money loves me")) == 2, "custom affirmations dedupe")
    aff.toggleFavorite("b|wealth|root|3|2"); aff.setPosition("wealth|root", 17)
    val affExport = aff.exportJson()
    JsonFiles.flush()
    val aff2 = AffirmationStore(File(dir, "aff.json"))
    check(aff2.state.value.custom["wealth"] == listOf("I am rich", "Money loves me") && aff2.state.value.positions["wealth|root"] == 17, "affirmations persist")
    val aff3 = AffirmationStore(File(dir, "aff3.json"))
    check(aff3.importJson(affExport) == 2 && aff3.isFavorite("b|wealth|root|3|2"), "affirmations import")

    val j = JournalStore(File(dir, "j.json"))
    val now = System.currentTimeMillis()
    j.upsert(JournalEntry(id = "1", created = now - 86400000L, text = "Yesterday", mood = "calm"))
    j.upsert(JournalEntry(id = "2", created = now, mode = "gratitude", text = "a\nb\nc"))
    val jx = j.exportJson(); JsonFiles.flush()
    check(JournalStore(File(dir, "j.json")).entries.value.map { it.id } == listOf("2", "1"), "journal persists newest first")
    val j2 = JournalStore(File(dir, "j2.json"))
    check(j2.importJson(jx) == 2 && j2.importJson(jx) == 0, "journal import merges without duplicates")
    check(j.deleteWhere { it.mood == "calm" } == 1, "journal delete by filter")
    check(j.exportText(j2.entries.value, { "D$it".take(4) }, { it }).contains("» ").not(), "journal text export")

    val h = HabitStore(File(dir, "h.json"))
    val today = LocalDate.now().toEpochDay()
    val habit = Habit(name = "Meditate", created = today - 20)
    h.addHabit(habit)
    for (d in (today - 5)..(today - 1)) h.setCount(habit.id, d, 1)
    val hs = h.state.value
    val hb = hs.habits.first()
    check(HabitAnalytics.currentStreak(hs, hb, today) == 5, "streak counts back from yesterday when today not done")
    h.increment(hb.id, today, 1)
    check(HabitAnalytics.currentStreak(h.state.value, hb, today) == 6, "streak includes today when done")
    check(HabitAnalytics.bestStreak(h.state.value, hb, today) == 6, "best streak")
    val rate = HabitAnalytics.completionRate(h.state.value, hb, today, 7)
    check(rate > 0.85f && rate <= 1f, "7-day completion rate = $rate")
    val weekly = Habit(name = "Yoga", days = setOf(1, 3, 5), created = today - 30)
    check((0..6).count { HabitAnalytics.isScheduled(weekly, today - it) } == 3, "weekday schedule")
    val hx = h.exportJson(); JsonFiles.flush()
    val h2 = HabitStore(File(dir, "h2.json"))
    check(h2.importJson(hx) == 1 && h2.state.value.logs[hb.id]?.size == 6, "habits import with logs")

    val m = MixStore(File(dir, "m.json"))
    m.save("Rainy temple", mapOf("lib:rain" to 0.7f, "lib:bell" to 0.5f)); JsonFiles.flush()
    check(MixStore(File(dir, "m.json")).mixes.value.first().layers.size == 2, "mixes persist")

    // ---- CSV / import
    val rows = Csv.parse("area,affirmation\nwealth,\"I am, truly, rich\"\nlove,\"She said \"\"yes\"\"\"\n")
    check(rows.size == 3 && rows[1][1] == "I am, truly, rich" && rows[2][1] == "She said \"yes\"", "CSV quotes")
    val imp = AffirmationImport.parse("area,affirmation\nwealth,A\nLove & Romance,B\nunknown,C", "health", setOf("wealth", "love", "health"), mapOf("love & romance" to "love"))
    check(imp["wealth"] == listOf("A") && imp["love"] == listOf("B") && imp["health"] == listOf("C"), "CSV import maps areas")
    check(AffirmationImport.parse("- one\n• two\n\nthree", "joy", emptySet(), emptyMap())["joy"]?.size == 3, "text import")

    // ---- DSP: render every sound, check level, NaN and speed
    val ids = VoiceFactory.frequencies.map { "freq:${it.id}" } + VoiceFactory.library.values.flatten().map { "lib:$it" }
    val seconds = args.getOrElse(1) { "12" }.toInt()
    val wavDir = File(out, "wav").apply { mkdirs() }
    for (id in ids) {
        val mixer = Mixer()
        mixer.set(id, 0.8f) { VoiceFactory.create(id, 42L) }
        val frames = 1024
        val total = seconds * SR
        val pcm = ShortArray(total * 2)
        val buf = ShortArray(frames * 2)
        val l = FloatArray(frames); val r = FloatArray(frames)
        var pos = 0; var peak = 0f; var sumSq = 0.0; var nan = false
        val t0 = System.nanoTime()
        while (pos < total) {
            val n = minOf(frames, total - pos)
            mixer.renderFloat(n); mixer.readFloat(l, r, n)
            for (i in 0 until n) {
                if (l[i].isNaN() || r[i].isNaN()) nan = true
                peak = maxOf(peak, kotlin.math.abs(l[i]), kotlin.math.abs(r[i]))
                sumSq += (l[i] * l[i] + r[i] * r[i]) / 2.0
                pcm[2 * (pos + i)] = (softClip(l[i]) * 32767).toInt().toShort()
                pcm[2 * (pos + i) + 1] = (softClip(r[i]) * 32767).toInt().toShort()
            }
            pos += n
        }
        val ms = (System.nanoTime() - t0) / 1e6
        val rms = kotlin.math.sqrt(sumSq / total)
        val rtf = seconds * 1000.0 / ms
        writeWav(File(wavDir, id.replace(':', '_') + ".wav"), pcm)
        check(!nan && peak in 0.02f..1.2f && rms > 0.005, "sound %-14s peak=%.2f rms=%.3f  %.0fx realtime".format(id, peak, rms, rtf))
    }
    // mix of 5 layers for CPU budget
    val mix = Mixer()
    listOf("lib:rain", "lib:bell", "lib:flute", "lib:stream", "freq:f528").forEach { id -> mix.set(id, 0.7f) { VoiceFactory.create(id, 7L) } }
    val t0 = System.nanoTime(); val sb = ShortArray(2048)
    repeat(SR * 10 / 1024) { mix.render(sb, 1024) }
    val rtf = 10_000.0 / ((System.nanoTime() - t0) / 1e6)
    check(rtf > 8, "5-layer mix renders %.0fx faster than realtime".format(rtf))

    println(if (failures == 0) "\nALL TESTS PASSED" else "\n$failures FAILURE(S)")
    if (failures > 0) kotlin.system.exitProcess(1)
}

fun writeWav(f: File, pcm: ShortArray) {
    val data = java.nio.ByteBuffer.allocate(44 + pcm.size * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    data.put("RIFF".toByteArray()); data.putInt(36 + pcm.size * 2); data.put("WAVE".toByteArray())
    data.put("fmt ".toByteArray()); data.putInt(16); data.putShort(1); data.putShort(2); data.putInt(SR); data.putInt(SR * 4); data.putShort(4); data.putShort(16)
    data.put("data".toByteArray()); data.putInt(pcm.size * 2)
    for (s in pcm) data.putShort(s)
    f.writeBytes(data.array())
}
