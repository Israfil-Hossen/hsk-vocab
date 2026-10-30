package com.israfilhossen.dawahchinese

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------- Lessons ----------------
@Composable
fun LessonScreen(nav: Nav, id: String) {
    val (level, lesson) = Data.lessonById(id) ?: return
    when (lesson.type) {
        "grammar" -> GrammarLesson(nav, lesson)
        "dialogue" -> DialogueLesson(nav, lesson)
        else -> CardsPlayer(nav, lesson.title, level.title, lesson.items, listen = lesson.type == "listen", lessonId = lesson.id)
    }
}

/** Big text for a card: shorter lines get larger characters. */
private fun cardSize(zh: String) = when {
    zh.length <= 2 -> 64.sp; zh.length <= 4 -> 52.sp; zh.length <= 8 -> 38.sp; zh.length <= 14 -> 30.sp; else -> 25.sp
}

/** One card at a time: hear it, think, show the meaning, say whether you knew it. */
@Composable
fun CardsPlayer(nav: Nav, title: String, sub: String, items: List<Item>, listen: Boolean, lessonId: String?) {
    var index by remember(items) { mutableIntStateOf(0) }
    var revealed by remember(items) { mutableStateOf(false) }
    var knew by remember(items) { mutableIntStateOf(0) }
    val finished = index >= items.size
    LaunchedEffect(index) { if (!finished) Speech.speak(items[index].zh) }
    LaunchedEffect(finished) { if (finished && lessonId != null) Store.markLesson(lessonId) }

    Screenful {
        Column(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                IconButton(onClick = { Speech.stop(); nav.back() }, modifier = Modifier.padding(start = 0.dp)) {
                    Icon(Icons.Filled.Close, tr("Close", "বন্ধ"), tint = C.muted)
                }
                Bar(index.toFloat() / items.size.coerceAtLeast(1), Modifier.weight(1f), height = 8.dp)
                RowSpacer()
                Text("${index.coerceAtMost(items.size)}/${items.size}", color = C.muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            if (finished) {
                Finished(nav, knew, items.size, lessonId)
                return@Column
            }
            val item = items[index]
            Text(title, color = C.muted, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp, top = 6.dp))
            Spacer(Modifier.weight(1f))
            AnimatedContent(targetState = index, transitionSpec = {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith fadeOut()
            }, label = "card") { i ->
                val card = items[i]
                Surface(shape = RoundedCornerShape(30.dp), color = C.card, border = BorderStroke(1.dp, C.line),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 22.dp, vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (listen && !revealed) {
                            Box(Modifier.size(110.dp).clip(CircleShape).background(C.mint).clickable { Speech.speak(card.zh) },
                                contentAlignment = Alignment.Center) { Text("🎧", fontSize = 48.sp) }
                            Gap(16.dp)
                            Text(tr("Listen. What does it mean?", "শোনো। এর অর্থ কী?"), color = C.muted, fontSize = 16.sp)
                        } else {
                            Text(card.zh, fontSize = cardSize(card.zh), style = chineseStyle, textAlign = TextAlign.Center,
                                lineHeight = cardSize(card.zh) * 1.25f, modifier = Modifier.clickable { Speech.speak(card.zh) })
                            Gap(10.dp)
                            Pinyin(it, size = 20.sp, align = TextAlign.Center)
                            Gap(18.dp)
                            SpeakButton(card.zh)
                        }
                        if (revealed) {
                            Gap(20.dp)
                            Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
                            Gap(18.dp)
                            Text(Store.meaning(it), fontSize = 21.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, color = C.green)
                            val other = if (Store.bengali) card.en else card.bn
                            if (other.isNotBlank() && other != Store.meaning(it)) {
                                Gap(6.dp)
                                Text(other, fontSize = 15.sp, color = C.muted, textAlign = TextAlign.Center)
                            }
                            if (!card.n.isNullOrBlank()) {
                                Gap(12.dp)
                                Text(card.n ?: "", fontSize = 14.sp, color = Color(0xFF7A6020), textAlign = TextAlign.Center,
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(C.goldSoft).padding(10.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1.3f))
            if (!revealed) {
                BigButton(tr("Show meaning", "অর্থ দেখাও")) { revealed = true }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { Store.review(item.zh, false); revealed = false; index++ },
                        modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.5.dp, C.bad.copy(alpha = 0.5f))) {
                        Text(tr("Again", "আবার"), color = C.bad, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(onClick = { Store.review(item.zh, true); knew++; revealed = false; index++ },
                        modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = C.ok)) {
                        Text(tr("I know it", "পারি"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Gap(18.dp)
        }
    }
}

@Composable
private fun Finished(nav: Nav, knew: Int, total: Int, lessonId: String?) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Ring(if (total == 0) 1f else knew.toFloat() / total, 150.dp, stroke = 14.dp, color = C.ok) {
            Text("$knew/$total", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = C.green)
        }
        Gap(24.dp)
        Text(tr("Masha'Allah, done!", "মাশাআল্লাহ, শেষ!"), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Gap(8.dp)
        Text(tr("Cards you missed will come back in Review tomorrow.", "যেগুলো পারোনি, কাল রিভিউতে আবার আসবে।"),
            color = C.muted, fontSize = 16.sp, textAlign = TextAlign.Center)
        Gap(32.dp)
        val next = if (lessonId != null) Store.nextLesson() else null
        if (next != null) BigButton(tr("Next: ", "পরের পাঠ: ") + next.second.title) { nav.back(); openLesson(nav, next.second) }
        if (lessonId != null && total >= 4) {
            Gap(10.dp)
            OutlinedWide(tr("Quick quiz", "ছোট্ট কুইজ")) { nav.replace(Screen.Quiz(lessonId)) }
        }
        Gap(10.dp)
        OutlinedWide(tr("Back", "ফিরে যাও")) { nav.back() }
    }
}

@Composable
fun BigButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = C.green)) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun SpeakButton(zh: String, size: Int = 52) {
    val on = Speech.speaking.value == zh
    Box(Modifier.size(size.dp).clip(CircleShape).background(if (on) C.green else C.mint).clickable { Speech.speak(zh) },
        contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.PlayArrow, tr("Listen", "শোনো"), tint = if (on) Color.White else C.green, modifier = Modifier.size((size * 0.55).dp))
    }
}

// ---------------- Dialogue ----------------
@Composable
fun DialogueLesson(nav: Nav, lesson: Lesson) {
    var meanings by remember { mutableStateOf(true) }
    var playing by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val done = Store.lessonDone(lesson.id)
    Screenful {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            TopBack(nav, lesson.title, tr("Dialogue · tap a line to hear it", "সংলাপ · শুনতে লাইনে চাপো"))
            Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    val running = playing
                    if (running != null) { running.cancel(); playing = null; Speech.stop() }
                    else playing = scope.launch {
                        for (line in lesson.items) {
                            Speech.speak(line.zh); delay(400)
                            var waited = 0
                            while (Speech.speaking.value != null && waited < 20000) { delay(100); waited += 100 }
                            delay(350)
                        }
                        playing = null
                    }
                }, shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = C.green)) {
                    Text(if (playing != null) tr("■ Stop", "■ থামাও") else tr("▶ Play all", "▶ সব শোনো"))
                }
                Spacer(Modifier.weight(1f))
                Text(tr("Meanings", "অর্থ"), color = C.muted, fontSize = 14.sp)
                RowSpacer(8.dp)
                Switch(checked = meanings, onCheckedChange = { meanings = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = C.green))
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(lesson.items) { _, it ->
                    val mine = it.s == "A"
                    val on = Speech.speaking.value == it.zh
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = if (mine) 22.dp else 6.dp, bottomEnd = if (mine) 6.dp else 22.dp),
                            color = if (mine) C.green else C.card,
                            border = if (mine) null else BorderStroke(if (on) 2.dp else 1.dp, if (on) C.gold else C.line),
                            modifier = Modifier.widthIn(max = 310.dp).clickable { Speech.speak(it.zh) },
                        ) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(it.zh, fontSize = 21.sp, style = chineseStyle, color = if (mine) Color.White else C.ink, lineHeight = 30.sp)
                                Gap(4.dp)
                                if (mine) Text(it.py, fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f))
                                else Pinyin(it, size = 14.sp)
                                if (meanings) {
                                    Gap(6.dp)
                                    Text(Store.meaning(it), fontSize = 15.sp, color = if (mine) Color(0xFFCFE5DA) else C.muted)
                                }
                            }
                        }
                    }
                }
                item { Gap(8.dp) }
            }
            Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                if (done) OutlinedWide(tr("✓ Done · Back", "✓ শেষ · ফিরে যাও")) { Speech.stop(); nav.back() }
                else BigButton(tr("Mark as done ✓", "শেষ করেছি ✓")) { Speech.stop(); Store.markLesson(lesson.id); nav.back() }
            }
        }
    }
}

// ---------------- Grammar ----------------
@Composable
fun GrammarLesson(nav: Nav, lesson: Lesson) {
    val done = Store.lessonDone(lesson.id)
    Screenful {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            TopBack(nav, lesson.title, tr("${lesson.points.size} patterns · tap an example to hear it", "${lesson.points.size}টি নিয়ম · উদাহরণে চাপলে শুনবে"))
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                itemsIndexed(lesson.points) { i, p ->
                    Panel {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(30.dp).clip(CircleShape).background(C.goldSoft), contentAlignment = Alignment.Center) {
                                Text("${i + 1}", color = Color(0xFF9A6A00), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            RowSpacer(10.dp)
                            Text(p.t, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = C.green, modifier = Modifier.weight(1f))
                        }
                        Gap(10.dp)
                        Text(tr(p.en, p.bn.ifBlank { p.en }), fontSize = 15.sp, color = C.ink, lineHeight = 23.sp)
                        for (ex in p.ex) {
                            Gap(10.dp)
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.mint.copy(alpha = 0.6f))
                                .clickable { Speech.speak(ex.zh) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(ex.zh, fontSize = 19.sp, style = chineseStyle)
                                    Pinyin(ex, size = 14.sp)
                                    Text(Store.meaning(ex), fontSize = 14.sp, color = C.muted)
                                }
                                Icon(Icons.Filled.PlayArrow, null, tint = C.green2)
                            }
                        }
                    }
                }
            }
            Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                if (done) OutlinedWide(tr("✓ Learned · Back", "✓ শিখেছি · ফিরে যাও")) { nav.back() }
                else BigButton(tr("Mark as learned ✓", "শিখেছি ✓")) { Store.markLesson(lesson.id); nav.back() }
            }
        }
    }
}

// ---------------- Quiz ----------------
private class Question(val item: Item, val options: List<String>, val answer: Int)

@Composable
fun QuizScreen(nav: Nav, id: String) {
    val (level, lesson) = Data.lessonById(id) ?: return
    val questions = remember(id) {
        val pool = (lesson.items + level.lessons.flatMap { it.items }).distinctBy { Store.meaning(it) }
        lesson.items.distinctBy { Store.meaning(it) }.shuffled().take(10).map { q ->
            val right = Store.meaning(q)
            val opts = (pool.map { Store.meaning(it) }.filter { it != right }.shuffled().take(3) + right).shuffled()
            Question(q, opts, opts.indexOf(right))
        }
    }
    var qi by remember(id) { mutableIntStateOf(0) }
    var picked by remember(id) { mutableStateOf<Int?>(null) }
    var score by remember(id) { mutableIntStateOf(0) }
    val finished = qi >= questions.size
    LaunchedEffect(qi) { if (!finished) Speech.speak(questions[qi].item.zh) }
    LaunchedEffect(picked) {
        if (picked != null) { delay(1000); picked = null; qi++ }
    }
    Screenful {
        Column(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                IconButton(onClick = { Speech.stop(); nav.back() }) { Icon(Icons.Filled.Close, tr("Close", "বন্ধ"), tint = C.muted) }
                Bar(qi.toFloat() / questions.size.coerceAtLeast(1), Modifier.weight(1f), height = 8.dp, color = C.gold)
                RowSpacer()
                Text("⭐ $score", color = Color(0xFF9A6A00), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            if (finished) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Ring(score.toFloat() / questions.size.coerceAtLeast(1), 150.dp, stroke = 14.dp, color = C.gold) {
                        Text("$score/${questions.size}", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = C.green)
                    }
                    Gap(24.dp)
                    Text(if (score * 10 >= questions.size * 8) tr("Excellent!", "চমৎকার!") else tr("Good effort!", "ভালো চেষ্টা!"),
                        fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Gap(32.dp)
                    BigButton(tr("Try again", "আবার চেষ্টা")) { qi = 0; score = 0 }
                    Gap(10.dp)
                    OutlinedWide(tr("Done", "শেষ")) { nav.back() }
                }
                return@Column
            }
            val q = questions[qi]
            Spacer(Modifier.weight(0.6f))
            Text(tr("What does this mean?", "এর অর্থ কী?"), color = C.muted, fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Gap(14.dp)
            Text(q.item.zh, fontSize = cardSize(q.item.zh), style = chineseStyle, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { Speech.speak(q.item.zh) })
            Gap(6.dp)
            Pinyin(q.item, size = 19.sp, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.weight(0.6f))
            q.options.forEachIndexed { i, o ->
                val state = when {
                    picked == null -> 0
                    i == q.answer -> 1
                    i == picked -> 2
                    else -> 0
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = when (state) { 1 -> C.ok.copy(alpha = 0.12f); 2 -> C.bad.copy(alpha = 0.10f); else -> C.card },
                    border = BorderStroke(if (state == 0) 1.dp else 2.dp, when (state) { 1 -> C.ok; 2 -> C.bad; else -> C.line }),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(18.dp)).clickable(enabled = picked == null) {
                        picked = i
                        val right = i == q.answer
                        if (right) score++
                        Store.review(q.item.zh, right)
                    },
                ) {
                    Text(o, fontSize = 17.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        fontWeight = if (state == 1) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            Gap(18.dp)
        }
    }
}

// ---------------- Review ----------------
@Composable
fun ReviewScreen(nav: Nav) {
    val due = remember {
        Store.dueItems(Data.content.levels.flatMap { l -> l.lessons.flatMap { it.items } }).shuffled().take(30)
    }
    if (due.isEmpty()) {
        Screenful {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("🌙", fontSize = 64.sp)
                Gap(16.dp)
                Text(tr("Nothing to review now", "এখন রিভিউ করার কিছু নেই"), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Gap(8.dp)
                Text(tr("Learn new cards today; they come back here at the right time.", "আজ নতুন কার্ড শেখো; ঠিক সময়ে এগুলো এখানে ফিরে আসবে।"),
                    color = C.muted, textAlign = TextAlign.Center)
                Gap(28.dp)
                BigButton(tr("Back", "ফিরে যাও")) { nav.back() }
            }
        }
        return
    }
    CardsPlayer(nav, tr("Review", "রিভিউ"), "", due, listen = false, lessonId = null)
}

// ---------------- Book reader ----------------
private val hanRun = Regex("[\\u3400-\\u9fff][\\u3400-\\u9fff，。！？、：；·“”‘’（）0-9]*")
private val readerFont = FontFamily.Serif

/** Book text with every Chinese run tappable to hear it. */
private fun styled(spans: List<Pair<String, Int>>): AnnotatedString = buildAnnotatedString {
    for ((text, flags) in spans) {
        val style = SpanStyle(
            fontWeight = if (flags and 1 != 0) FontWeight.Bold else null,
            fontStyle = if (flags and 2 != 0) FontStyle.Italic else null,
        )
        withStyle(style) { appendTappable(text) }
    }
}

private fun AnnotatedString.Builder.appendTappable(text: String) {
    var last = 0
    for (m in hanRun.findAll(text)) {
        append(text.substring(last, m.range.first))
        val run = m.value
        withLink(LinkAnnotation.Clickable("zh", TextLinkStyles(SpanStyle(color = C.green2, fontWeight = FontWeight.Medium))) { Speech.speak(run) }) {
            append(run)
        }
        last = m.range.last + 1
    }
    append(text.substring(last))
}

private fun tappable(text: String) = buildAnnotatedString { appendTappable(text) }

@Composable
fun ReaderScreen(nav: Nav, part: String, sec: String) {
    val p = Data.book.firstOrNull { it.id == part } ?: return
    val s = p.sections.firstOrNull { it.id == sec } ?: return
    val all = Data.allSections
    val at = all.indexOfFirst { it.first.id == part && it.second.id == sec }
    val next = all.getOrNull(at + 1)
    val read = Store.sectionRead(part, sec)
    val list = remember(part, sec) { LazyListState() }
    val pos = p.sections.indexOf(s) + 1

    fun goNext() { Speech.stop(); if (next != null) nav.replace(Screen.Reader(next.first.id, next.second.id)) else nav.back() }

    Screenful {
        Column(Modifier.fillMaxSize()) {
            TopBack(nav, p.title, tr("Section $pos of ${p.sections.size}", "অংশ $pos / ${p.sections.size}"))
            Bar(pos.toFloat() / p.sections.size, Modifier.padding(horizontal = 20.dp), height = 3.dp, color = C.gold)
            LazyColumn(Modifier.weight(1f), state = list, contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp)) {
                item {
                    Text(s.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = C.green, lineHeight = 33.sp, fontFamily = readerFont)
                    Gap(6.dp)
                    Text(tr("Tap any green Chinese to hear it", "সবুজ চাইনিজ লেখায় চাপলে শুনতে পাবে"), fontSize = 13.sp, color = C.muted)
                    Gap(14.dp)
                }
                itemsIndexed(s.blocks) { _, b -> BlockView(b); Gap(14.dp) }
                item { Gap(20.dp) }
            }
            Surface(color = C.card, shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (read) {
                        Chip(tr("✓ Read", "✓ পড়া হয়েছে"), bg = C.ok.copy(alpha = 0.12f), fg = C.ok)
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { goNext() }, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = C.green)) {
                            Text(if (next != null) tr("Next section", "পরের অংশ") else tr("Finish", "শেষ"), fontSize = 16.sp)
                            RowSpacer(6.dp)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        Button(onClick = { Store.markSection(part, sec); goNext() }, shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = C.green)) {
                            Icon(Icons.Filled.Check, null, modifier = Modifier.size(20.dp))
                            RowSpacer(8.dp)
                            Text(tr("Mark as read · next", "পড়া শেষ · পরেরটা"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockView(b: Block) {
    when (b) {
        is Block.Heading -> Text(b.text, fontSize = if (b.level <= 2) 21.sp else 18.sp, fontWeight = FontWeight.Bold,
            color = C.green, fontFamily = readerFont, modifier = Modifier.padding(top = 8.dp))
        is Block.Para -> Text(styled(b.spans), fontSize = 17.sp, lineHeight = 28.sp, color = C.ink, fontFamily = readerFont)
        is Block.Bengali -> Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(C.gold))
            RowSpacer(12.dp)
            Text(b.text, fontSize = 16.sp, lineHeight = 26.sp, color = Color(0xFF5B4A1E))
        }
        is Block.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            b.items.forEachIndexed { i, item ->
                Row {
                    Text(if (b.ordered) "${i + 1}." else "•", fontSize = 17.sp, color = C.gold, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(26.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { item.forEach { BlockView(it) } }
                }
            }
        }
        is Block.Quote -> Surface(shape = RoundedCornerShape(18.dp), color = C.mint, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { b.blocks.forEach { BlockView(it) } }
        }
        is Block.Table -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in b.rows) {
                Surface(shape = RoundedCornerShape(14.dp), color = C.card, border = BorderStroke(1.dp, C.line), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        row.forEachIndexed { i, cell ->
                            if (cell.isBlank()) return@forEachIndexed
                            if (i == 0) Text(tappable(cell), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = C.ink)
                            else Row(Modifier.padding(top = 3.dp)) {
                                Text(b.head.getOrElse(i) { "" }, fontSize = 13.sp, color = C.muted, modifier = Modifier.width(110.dp))
                                Text(tappable(cell), fontSize = 15.sp, color = C.ink, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
