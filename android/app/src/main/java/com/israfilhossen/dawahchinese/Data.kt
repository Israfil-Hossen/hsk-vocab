package com.israfilhossen.dawahchinese

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** One Chinese line with pinyin, meanings and tone-coloured pinyin pieces ([text, tone]; tone -1 = not pinyin). */
@Serializable
data class Item(
    val zh: String,
    val py: String,
    val en: String,
    val bn: String,
    val pc: List<JsonArray> = emptyList(),
    val n: String? = null,
    val s: String? = null,
)

@Serializable
data class Point(val t: String, val en: String, val bn: String, val ex: List<Item> = emptyList())

@Serializable
data class Lesson(
    val id: String,
    val type: String,
    val title: String,
    val bn: String,
    val items: List<Item> = emptyList(),
    val points: List<Point> = emptyList(),
    val page: String? = null,
    val sec: String? = null,
) {
    val size: Int get() = if (type == "grammar") points.size else if (type == "read") 1 else items.size
}

@Serializable
data class Section(val title: String, val bn: String, val units: List<Lesson>)

@Serializable
data class Level(
    val id: Int,
    val title: String,
    val bn: String,
    val hsk: String,
    val goal: String,
    val goalBn: String,
    val sections: List<Section>,
) {
    val lessons: List<Lesson> get() = sections.flatMap { it.units }
}

@Serializable
data class Content(val levels: List<Level>)

/** A block of book text. */
sealed class Block {
    data class Heading(val level: Int, val text: String) : Block()
    data class Para(val spans: List<Pair<String, Int>>) : Block()
    data class Bengali(val text: String) : Block()
    data class Bullets(val items: List<List<Block>>, val ordered: Boolean) : Block()
    data class Quote(val blocks: List<Block>) : Block()
    data class Table(val head: List<String>, val rows: List<List<String>>) : Block()
}

data class BookSection(val id: String, val title: String, val blocks: List<Block>)
data class BookPart(val id: String, val title: String, val bn: String, val sections: List<BookSection>)

object Data {
    lateinit var content: Content
    private lateinit var app: Context
    private val books = mutableMapOf<String, List<BookPart>>()
    private val json = Json { ignoreUnknownKeys = true }

    fun load(c: Context) {
        if (::content.isInitialized) return
        app = c.applicationContext
        content = json.decodeFromString(Content.serializer(), c.assets.open("data/content.json").bufferedReader().readText())
    }

    /** The book in the reader's chosen language: "en", "bn" (Bangla) or "ur" (Urdu). Loaded on first use. */
    fun bookFor(lang: String): List<BookPart> = books.getOrPut(lang) {
        val file = if (lang == "en") "data/book.json" else "data/book_$lang.json"
        runCatching { parseBook(app.assets.open(file).bufferedReader().readText()) }.getOrElse { if (lang == "en") throw it else bookFor("en") }
    }
    val book: List<BookPart> get() = bookFor(Store.bookLang)

    private fun parseBook(text: String): List<BookPart> {
        val root = json.parseToJsonElement(text).jsonObject
        return root["parts"]!!.jsonArray.map { p ->
            val o = p.jsonObject
            BookPart(
                o.str("id"), o.str("t"), o.str("bn"),
                o["s"]!!.jsonArray.map { s ->
                    val so = s.jsonObject
                    BookSection(so.str("id"), so.str("t"), blocks(so["b"]!!.jsonArray))
                })
        }
    }

    private fun JsonObject.str(k: String) = this[k]?.jsonPrimitive?.content ?: ""

    private fun blocks(a: JsonArray): List<Block> = a.mapNotNull { block(it) }

    private fun block(e: JsonElement): Block? {
        val o = e.jsonObject
        return when (o.str("t")) {
            "h" -> Block.Heading(o["l"]!!.jsonPrimitive.int, o.str("x"))
            "p" -> Block.Para(o["s"]!!.jsonArray.map { sp ->
                val x = sp.jsonArray; x[0].jsonPrimitive.content to x[1].jsonPrimitive.int
            })
            "bn" -> Block.Bengali(o.str("x"))
            "ul", "ol" -> Block.Bullets(o["i"]!!.jsonArray.map { blocks(it.jsonArray) }, o.str("t") == "ol")
            "q" -> Block.Quote(blocks(o["b"]!!.jsonArray))
            "tb" -> Block.Table(
                o["h"]!!.jsonArray.map { it.jsonPrimitive.content },
                o["r"]!!.jsonArray.map { r -> r.jsonArray.map { it.jsonPrimitive.content } })
            else -> null
        }
    }

    val allSections: List<Pair<BookPart, BookSection>> get() = book.flatMap { p -> p.sections.map { p to it } }

    fun lessonById(id: String): Pair<Level, Lesson>? {
        for (l in content.levels) for (u in l.lessons) if (u.id == id) return l to u
        return null
    }

    /** The book section a "read" lesson points to, e.g. part-0 / "0.2". */
    fun sectionFor(lesson: Lesson): Pair<BookPart, BookSection>? {
        val part = book.firstOrNull { it.id == lesson.page } ?: return null
        val sec = part.sections.firstOrNull { it.title.startsWith((lesson.sec ?: "") + " ") } ?: part.sections.firstOrNull() ?: return null
        return part to sec
    }

    /** Every Chinese line in the path, for the sentence of the day. */
    val sentences: List<Item> by lazy {
        content.levels.flatMap { l -> l.lessons.filter { it.type == "cards" || it.type == "listen" }.flatMap { it.items } }
            .filter { it.zh.length >= 5 }
    }
}
