package com.israfilhossen.dawahchinese

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf
import java.time.LocalDate

/** Progress and settings, kept on the device only. Reading [tick] makes a composable refresh after a change. */
object Store {
    private lateinit var p: SharedPreferences
    val tick = mutableIntStateOf(0)

    fun init(c: Context) {
        if (!::p.isInitialized) p = c.getSharedPreferences("dawah", Context.MODE_PRIVATE)
    }

    private fun changed() { tick.intValue++ }
    private fun set(k: String): Set<String> = p.getStringSet(k, emptySet()) ?: emptySet()
    private fun add(k: String, v: String) { p.edit().putStringSet(k, set(k) + v).apply(); changed() }

    // settings
    var onboarded: Boolean
        get() = p.getBoolean("onboarded", false)
        set(v) { p.edit().putBoolean("onboarded", v).apply(); changed() }
    var bengali: Boolean
        get() = p.getBoolean("bengali", true)
        set(v) { p.edit().putBoolean("bengali", v).apply(); changed() }
    var goal: Int
        get() = p.getInt("goal", 10)
        set(v) { p.edit().putInt("goal", v).apply(); changed() }
    var rate: Float
        get() = p.getFloat("rate", 0.9f)
        set(v) { p.edit().putFloat("rate", v).apply(); changed() }
    var startLevel: Int
        get() = p.getInt("startLevel", 0)
        set(v) { p.edit().putInt("startLevel", v).apply(); changed() }

    fun meaning(i: Item) = if (bengali) i.bn else i.en

    // lessons and book
    fun lessonDone(id: String) = id in set("done")
    fun markLesson(id: String) { if (!lessonDone(id)) { add("done", id); logActivity(3) } }
    fun sectionRead(partId: String, secId: String) = "$partId#$secId" in set("read")
    fun markSection(partId: String, secId: String) { if (!sectionRead(partId, secId)) { add("read", "$partId#$secId"); logActivity(3) } }
    val lessonsDone: Int get() = set("done").size
    val sectionsRead: Int get() = set("read").size

    // spaced review (Leitner boxes): box 1..5, due date
    private val gaps = intArrayOf(0, 1, 3, 7, 16, 35)
    fun known(zh: String) = p.getInt("box:$zh", 0) > 0
    fun review(zh: String, gotIt: Boolean) {
        val box = if (gotIt) (p.getInt("box:$zh", 0) + 1).coerceIn(2, 5) else 1
        p.edit().putInt("box:$zh", box).putLong("due:$zh", LocalDate.now().plusDays(gaps[box].toLong()).toEpochDay())
            .putStringSet("seen", set("seen") + zh).apply()
        logActivity(1)
    }
    val wordsKnown: Int get() = set("seen").count { p.getInt("box:$it", 0) >= 2 }
    fun dueItems(all: List<Item>): List<Item> {
        val today = LocalDate.now().toEpochDay(); val seen = set("seen")
        return all.filter { it.zh in seen && p.getLong("due:${it.zh}", Long.MAX_VALUE) <= today }.distinctBy { it.zh }
    }

    // daily activity, streak and goal
    private fun dayKey(d: LocalDate = LocalDate.now()) = "act:$d"
    fun points(d: LocalDate = LocalDate.now()) = p.getInt(dayKey(d), 0)
    private fun logActivity(n: Int) {
        val k = dayKey(); val before = p.getInt(k, 0)
        p.edit().putInt(k, before + n).apply()
        if (before < goal && before + n >= goal) p.edit().putString("celebrate", LocalDate.now().toString()).apply()
        changed()
    }
    fun takeCelebration(): Boolean {
        val c = p.getString("celebrate", null) ?: return false
        p.edit().remove("celebrate").apply(); return c == LocalDate.now().toString()
    }
    val streak: Int get() {
        var d = LocalDate.now(); if (points(d) == 0) d = d.minusDays(1)
        var n = 0; while (points(d) > 0) { n++; d = d.minusDays(1) }
        return n
    }

    // reminder
    var reminderOn: Boolean
        get() = p.getBoolean("rem", false)
        set(v) { p.edit().putBoolean("rem", v).apply(); changed() }
    var reminderHour: Int
        get() = p.getInt("remH", 20)
        set(v) { p.edit().putInt("remH", v).apply(); changed() }
    var reminderMinute: Int
        get() = p.getInt("remM", 0)
        set(v) { p.edit().putInt("remM", v).apply(); changed() }

    fun reset() { val keepOnboard = onboarded; p.edit().clear().putBoolean("onboarded", keepOnboard).apply(); changed() }

    /** The next lesson on the path: the first one not done, starting from the chosen start level. */
    fun nextLesson(): Pair<Level, Lesson>? {
        val levels = Data.content.levels
        for (l in levels.filter { it.id >= startLevel } + levels.filter { it.id < startLevel })
            for (u in l.lessons) if (!lessonDone(u.id)) return l to u
        return null
    }

    fun nextSection(): Pair<BookPart, BookSection>? = Data.allSections.firstOrNull { !sectionRead(it.first.id, it.second.id) }
}
