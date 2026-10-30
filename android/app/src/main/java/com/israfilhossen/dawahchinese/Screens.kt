package com.israfilhossen.dawahchinese

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

fun typeEmoji(t: String) = when (t) {
    "words" -> "🔤"; "listen" -> "🎧"; "dialogue" -> "🗣"; "grammar" -> "🧩"; "read" -> "📖"; else -> "💬"
}

fun lessonSub(u: Lesson) = when (u.type) {
    "read" -> tr("Read", "পড়ো")
    "grammar" -> tr("${u.size} patterns", "${u.size}টি নিয়ম")
    "dialogue" -> tr("${u.size} lines", "${u.size} লাইন")
    else -> tr("${u.size} cards", "${u.size}টি কার্ড")
}

fun openLesson(nav: Nav, u: Lesson) {
    if (u.type == "read") {
        val (p, s) = Data.sectionFor(u) ?: return
        nav.go(Screen.Reader(p.id, s.id))
    } else nav.go(Screen.LessonScr(u.id))
}

@Composable
fun Screenful(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(C.bg).statusBarsPadding()) { content() }
}

@Composable
fun TopBack(nav: Nav, title: String, sub: String? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { Speech.stop(); nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Back", "পেছনে"), tint = C.ink) }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, fontSize = 13.sp, color = C.muted, maxLines = 1)
        }
    }
}

// ---------------- Today ----------------
@Composable
fun TodayScreen(nav: Nav) {
    var celebrate by remember { mutableStateOf(false) }
    LaunchedEffect(Store.tick.intValue) { if (Store.takeCelebration()) celebrate = true }
    val pts = Store.points(); val goal = Store.goal; val streak = Store.streak
    val next = Store.nextLesson(); val due = Store.dueItems(Data.content.levels.flatMap { l -> l.lessons.flatMap { it.items } }).size
    val sod = Data.sentences[(LocalDate.now().toEpochDay() % Data.sentences.size).toInt()]
    val read = Store.nextSection()
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(20.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Assalamu alaikum", "আসসালামু আলাইকুম"), color = C.muted, fontSize = 15.sp)
                    Text(tr("Today", "আজ"), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
                Chip(if (streak > 0) "🔥 $streak" else "🔥 0", bg = C.goldSoft, fg = Color(0xFF9A6A00))
            }
            Gap(16.dp)
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(pts.toFloat() / goal, 84.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$pts", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = C.green)
                            Text("/ $goal", fontSize = 12.sp, color = C.muted)
                        }
                    }
                    RowSpacer(18.dp)
                    Column {
                        Text(if (pts >= goal) tr("Goal reached!", "লক্ষ্য পূরণ!") else tr("Daily goal", "আজকের লক্ষ্য"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(if (pts >= goal) tr("Masha'Allah. Come back tomorrow.", "মাশাআল্লাহ। কাল আবার এসো।")
                        else tr("About 15 minutes: learn, review and read.", "প্রায় ১৫ মিনিট: শেখো, ঝালাই করো, পড়ো।"), color = C.muted, fontSize = 14.sp)
                    }
                }
            }
            Gap()
            if (next != null) {
                val (lv, u) = next
                Panel(color = C.green, onClick = { openLesson(nav, u) }) {
                    Text(tr("Continue · ${lv.title}", "চালিয়ে যাও · ${lv.bn}"), color = C.gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Gap(6.dp)
                    Text(tr(u.title, u.bn), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(lessonSub(u) + " · " + lv.hsk, color = Color(0xFFCFE3D8), fontSize = 14.sp)
                    Gap(14.dp)
                    Button(onClick = { openLesson(nav, u) }, colors = ButtonDefaults.buttonColors(containerColor = C.gold, contentColor = C.green)) {
                        Text(tr("Start", "শুরু করো"), fontWeight = FontWeight.Bold)
                    }
                }
                Gap()
            }
            if (due > 0) {
                Panel(onClick = { nav.go(Screen.Review) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble("🔁"); RowSpacer()
                        Column(Modifier.weight(1f)) {
                            Text(tr("Review", "ঝালাই"), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(tr("$due cards are due today", "আজ $due টি কার্ড ঝালাই করার"), color = C.muted, fontSize = 14.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = C.muted)
                    }
                }
                Gap()
            }
            Panel(color = C.goldSoft, onClick = { Speech.speak(sod.zh) }) {
                Text(tr("Sentence of the day", "আজকের বাক্য"), color = Color(0xFF8A6A1E), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Gap(8.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(sod.zh, fontSize = 24.sp, fontWeight = FontWeight.Medium, lineHeight = 32.sp)
                        Pinyin(sod, 15.sp)
                        Gap(4.dp)
                        Text(Store.meaning(sod), fontSize = 15.sp, color = C.ink)
                    }
                    Box(Modifier.size(48.dp).clip(CircleShape).background(C.green), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PlayArrow, tr("Play", "শোনো"), tint = Color.White)
                    }
                }
            }
            Gap()
            if (read != null) {
                Panel(onClick = { nav.go(Screen.Reader(read.first.id, read.second.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble("📖"); RowSpacer()
                        Column(Modifier.weight(1f)) {
                            Text(tr("Read from the book", "বই থেকে পড়ো"), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(read.second.title, color = C.muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = C.muted)
                    }
                }
            }
            Gap(24.dp)
        }
    }
    if (celebrate) AlertDialog(
        onDismissRequest = { celebrate = false },
        confirmButton = { TextButton(onClick = { celebrate = false }) { Text(tr("Alhamdulillah", "আলহামদুলিল্লাহ")) } },
        title = { Text("🎉 " + tr("Goal reached!", "আজকের লক্ষ্য পূরণ!")) },
        text = { Text(tr("Masha'Allah, you studied for dawah today. Your streak continues.", "মাশাআল্লাহ, আজ দাওয়াহর জন্য শিখলে। তোমার ধারাবাহিকতা চলছে।")) },
    )
}

// ---------------- Learn: the dawah path ----------------
@Composable
fun LearnScreen(nav: Nav) {
    val levels = Data.content.levels
    val current = Store.nextLesson()?.first?.id
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(20.dp)) {
        item { Title(tr("Your dawah path", "তোমার দাওয়াহর পথ"), tr("Learn Chinese to share your faith with wisdom.", "হিকমতের সাথে দাওয়াহ দিতে চাইনিজ শেখো।")) }
        items(levels) { l ->
            val done = l.lessons.count { Store.lessonDone(it.id) }; val total = l.lessons.size
            val isCur = l.id == current
            Panel(color = if (isCur) C.mint else C.card, onClick = { nav.go(Screen.LevelScr(l.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(done.toFloat() / total, 58.dp, 6.dp, if (done == total) C.ok else C.green) {
                        Text(if (done == total) "✓" else "${l.id}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = C.green)
                    }
                    RowSpacer(16.dp)
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tr(l.title, l.bn), fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
                            RowSpacer(8.dp); Chip(l.hsk)
                        }
                        Gap(4.dp)
                        Text(tr(l.goal, l.goalBn), color = C.muted, fontSize = 14.sp, lineHeight = 19.sp)
                        Gap(8.dp)
                        Text(tr("$done of $total lessons", "$total টির মধ্যে $done টি পাঠ"), color = C.green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (isCur) {
                    Gap(12.dp)
                    Text("● " + tr("You are here", "তুমি এখানে"), color = C.green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            Gap()
        }
    }
}

@Composable
fun LevelScreen(nav: Nav, id: Int) {
    val l = Data.content.levels.first { it.id == id }
    Screenful {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 30.dp)) {
            item {
                TopBack(nav, tr(l.title, l.bn), l.hsk)
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text(tr(l.goal, l.goalBn), fontSize = 16.sp, color = C.muted, lineHeight = 22.sp)
                    Gap(10.dp)
                    val done = l.lessons.count { Store.lessonDone(it.id) }
                    Bar(done.toFloat() / l.lessons.size)
                }
            }
            for (s in l.sections) {
                item { Box(Modifier.padding(horizontal = 20.dp)) { SectionLabel(tr(s.title, s.bn)) } }
                items(s.units) { u ->
                    val done = Store.lessonDone(u.id)
                    Row(
                        Modifier.fillMaxWidth().clickable { openLesson(nav, u) }.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBubble(typeEmoji(u.type), if (done) Color(0xFFE3F4EA) else C.mint)
                        RowSpacer(14.dp)
                        Column(Modifier.weight(1f)) {
                            Text(tr(u.title, u.bn), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(lessonSub(u), fontSize = 13.sp, color = C.muted)
                        }
                        if (done) Icon(Icons.Filled.Check, tr("Done", "শেষ"), tint = C.ok)
                        else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = C.line)
                    }
                }
            }
        }
    }
}

// ---------------- Book ----------------
@Composable
fun BookScreen(nav: Nav) {
    val total = Data.allSections.size; val read = Store.sectionsRead
    val next = Store.nextSection()
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(20.dp)) {
        item {
            Title(tr("The book", "বই"), "Knowing the Creator · 认识真主")
            Panel(color = C.green, onClick = next?.let { { nav.go(Screen.Reader(it.first.id, it.second.id)) } }) {
                Text(tr("Your reading", "তোমার পড়া"), color = C.gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Gap(6.dp)
                Text(tr("$read of $total sections read", "$total টির মধ্যে $read টি অংশ পড়া হয়েছে"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Gap(10.dp)
                Bar(read.toFloat() / total, color = C.gold)
                if (next != null) {
                    Gap(14.dp)
                    Text(tr("Next: ", "এরপর: ") + next.second.title, color = Color(0xFFCFE3D8), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Gap(10.dp)
                    Button(onClick = { nav.go(Screen.Reader(next.first.id, next.second.id)) }, colors = ButtonDefaults.buttonColors(containerColor = C.gold, contentColor = C.green)) {
                        Text(tr("Continue reading", "পড়া চালিয়ে যাও"), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Gap(8.dp)
        }
        items(Data.book) { p ->
            val n = p.sections.count { Store.sectionRead(p.id, it.id) }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { nav.go(Screen.PartScr(p.id)) }.padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(p.bn + " · " + tr("${n}/${p.sections.size} read", "${n}/${p.sections.size} পড়া"), fontSize = 13.sp, color = C.muted)
                    Gap(6.dp)
                    Bar(n.toFloat() / p.sections.size, Modifier.width(140.dp), height = 4.dp, color = if (n == p.sections.size) C.ok else C.green)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = C.muted)
            }
        }
        item { Gap(24.dp) }
    }
}

@Composable
fun PartScreen(nav: Nav, id: String) {
    val p = Data.book.first { it.id == id }
    Screenful {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 30.dp)) {
            item { TopBack(nav, p.title, p.bn) }
            items(p.sections) { s ->
                val read = Store.sectionRead(p.id, s.id)
                Row(Modifier.fillMaxWidth().clickable { nav.go(Screen.Reader(p.id, s.id)) }.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(s.title, fontSize = 16.sp, modifier = Modifier.weight(1f), color = if (read) C.muted else C.ink)
                    if (read) Icon(Icons.Filled.Check, null, tint = C.ok) else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = C.line)
                }
            }
        }
    }
}

// ---------------- Me: progress and settings ----------------
@Composable
fun MeScreen(nav: Nav) {
    val ctx = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ReminderReceiver.schedule(ctx) }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(20.dp)) {
        item {
            Title(tr("Me", "আমি"), tr("Your progress and settings", "তোমার অগ্রগতি আর সেটিংস"))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("🔥", "${Store.streak}", tr("day streak", "দিন টানা"), Modifier.weight(1f))
                Stat("🔤", "${Store.wordsKnown}", tr("cards known", "কার্ড জানা"), Modifier.weight(1f))
            }
            Gap(10.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("✅", "${Store.lessonsDone}", tr("lessons done", "পাঠ শেষ"), Modifier.weight(1f))
                Stat("📖", "${Store.sectionsRead}", tr("sections read", "অংশ পড়া"), Modifier.weight(1f))
            }
            Gap(10.dp)
            Panel {
                Text(tr("This week", "এই সপ্তাহ"), fontWeight = FontWeight.Bold)
                Gap(12.dp)
                Week()
            }
            SectionLabel(tr("Settings", "সেটিংস"))
            Panel {
                Text(tr("Show meanings in", "অর্থ দেখাও"), fontWeight = FontWeight.SemiBold)
                Gap(8.dp)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = Store.bengali, onClick = { Store.bengali = true }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("বাংলা") }
                    SegmentedButton(selected = !Store.bengali, onClick = { Store.bengali = false }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("English") }
                }
                Gap(18.dp)
                Text(tr("Daily goal", "দৈনিক লক্ষ্য"), fontWeight = FontWeight.SemiBold)
                Gap(8.dp)
                val goals = listOf(5, 10, 20)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    goals.forEachIndexed { i, g ->
                        SegmentedButton(selected = Store.goal == g, onClick = { Store.goal = g }, shape = SegmentedButtonDefaults.itemShape(i, goals.size)) {
                            Text(listOf(tr("Light", "হালকা"), tr("Steady", "নিয়মিত"), tr("Strong", "জোরালো"))[i])
                        }
                    }
                }
                Gap(18.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Daily reminder", "দৈনিক রিমাইন্ডার"), fontWeight = FontWeight.SemiBold)
                        Text(String.format("%02d:%02d", Store.reminderHour, Store.reminderMinute), color = C.green, fontSize = 15.sp,
                            modifier = Modifier.clickable {
                                TimePickerDialog(ctx, { _, h, m -> Store.reminderHour = h; Store.reminderMinute = m; ReminderReceiver.schedule(ctx) },
                                    Store.reminderHour, Store.reminderMinute, true).show()
                            })
                    }
                    Switch(checked = Store.reminderOn, onCheckedChange = {
                        Store.reminderOn = it
                        if (it && Build.VERSION.SDK_INT >= 33) notif.launch(Manifest.permission.POST_NOTIFICATIONS) else ReminderReceiver.schedule(ctx)
                    }, colors = SwitchDefaults.colors(checkedTrackColor = C.green))
                }
                Gap(18.dp)
                Text(tr("Voice speed", "উচ্চারণের গতি"), fontWeight = FontWeight.SemiBold)
                Slider(value = Store.rate, onValueChange = { Store.rate = it }, valueRange = 0.5f..1.2f,
                    onValueChangeFinished = { Speech.speak("你好，我是穆斯林。") })
                SettingRow("🎙", tr("Chinese voice settings", "চাইনিজ কণ্ঠের সেটিংস"), tr("Choose Google and install Chinese for the most natural voice", "সবচেয়ে স্বাভাবিক কণ্ঠের জন্য Google বেছে Chinese ইনস্টল করো")) { Speech.openSettings(ctx) }
            }
            SectionLabel(tr("About", "পরিচিতি"))
            Panel {
                SettingRow("☪", tr("Why this app", "কেন এই অ্যাপ"), null) { nav.go(Screen.About) }
                SettingRow("🌐", tr("Website and book PDF", "ওয়েবসাইট আর বইয়ের PDF"), null) { openUrl(ctx, SITE) }
                SettingRow("📤", tr("Share the app", "অ্যাপ শেয়ার করো"), null) {
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,
                        "Dawah Chinese · 认识真主: learn Chinese from zero to HSK 4 for dawah.\n$SITE"), null))
                }
                SettingRow("🔒", tr("Privacy policy", "গোপনীয়তা নীতি"), null) { openUrl(ctx, SITE + "privacy.html") }
                SettingRow("↺", tr("Reset progress", "অগ্রগতি মুছে ফেলো"), null) { confirmReset = true }
            }
            Gap(30.dp)
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        confirmButton = { TextButton(onClick = { Store.reset(); confirmReset = false }) { Text(tr("Reset", "মুছে ফেলো"), color = C.bad) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(tr("Cancel", "বাতিল")) } },
        title = { Text(tr("Reset all progress?", "সব অগ্রগতি মুছে ফেলবে?")) },
    )
}

@Composable
fun Stat(emoji: String, value: String, label: String, modifier: Modifier) {
    Panel(modifier) {
        Text(emoji, fontSize = 20.sp)
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = C.green)
        Text(label, fontSize = 13.sp, color = C.muted)
    }
}

@Composable
fun Week() {
    val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
    val max = maxOf(Store.goal, days.maxOf { Store.points(it) })
    Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        for (d in days) {
            val v = Store.points(d)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth(0.7f).height((70 * v / max).coerceAtLeast(3).dp).clip(RoundedCornerShape(6.dp))
                    .background(if (v >= Store.goal) C.ok else if (v > 0) C.green2 else C.line))
                Gap(6.dp)
                Text(d.dayOfWeek.name.take(1), fontSize = 12.sp, color = C.muted)
            }
        }
    }
}

@Composable
fun SettingRow(icon: String, title: String, sub: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 18.sp, modifier = Modifier.width(32.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (sub != null) Text(sub, fontSize = 12.sp, color = C.muted)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = C.line)
    }
}

// ---------------- Why this app ----------------
data class Page(val emoji: String, val title: String, val titleBn: String, val body: String, val bodyBn: String)

val purposePages = listOf(
    Page("☪", "Why this app?", "কেন এই অ্যাপ?",
        "Dawah Chinese is for Muslim students in China. It teaches you Chinese from zero to HSK 4, and at the same time it teaches you to answer your Chinese friends' questions about God and Islam with knowledge, wisdom and kindness. Every word you learn here is learned for dawah.",
        "চীনে পড়া মুসলিম ছাত্রদের জন্য। শূন্য থেকে HSK 4 পর্যন্ত চাইনিজ শেখায়, আর একই সাথে শেখায় বন্ধুরা আল্লাহ ও ইসলাম নিয়ে প্রশ্ন করলে জ্ঞান, হিকমত আর ভালোবাসা দিয়ে উত্তর দিতে। এখানে শেখা প্রতিটা শব্দ দাওয়াহর নিয়তে শেখা।"),
    Page("🧭", "How to use it", "কীভাবে ব্যবহার করবে",
        "Open Today every day: continue your lesson, review the cards that are due, and read one section of the book. Follow the path from Start from zero to Level 4. Tap any Chinese text to hear it.",
        "প্রতিদিন \"আজ\" খোলো: পাঠ চালিয়ে যাও, দরকারি কার্ড ঝালাই করো, আর বইয়ের একটা অংশ পড়ো। শূন্য থেকে চতুর্থ ধাপ পর্যন্ত পথ ধরে এগোও। যেকোনো চাইনিজ লেখায় চাপ দিলে উচ্চারণ শোনা যাবে।"),
    Page("🔥", "Why every day?", "কেন প্রতিদিন?",
        "Fifteen minutes every day beats three hours once a week: memory keeps what it meets again and again. Speak about your faith when a friend asks, with good manners and within the law. Practise here, so you are ready when the question comes.",
        "প্রতিদিন ১৫ মিনিট, সপ্তাহে একদিন তিন ঘণ্টার চেয়ে বেশি কাজে আসে। বন্ধু জিজ্ঞেস করলে আদব আর আইন মেনে উত্তর দাও। প্রশ্ন আসার আগেই এখানে প্রস্তুত হও।"),
)

@Composable
fun AboutScreen(nav: Nav) {
    Screenful {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            TopBack(nav, tr("Why this app", "কেন এই অ্যাপ"))
            for (p in purposePages) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Text(p.emoji, fontSize = 34.sp)
                    Text(tr(p.title, p.titleBn), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Gap(6.dp)
                    Text(tr(p.body, p.bodyBn), fontSize = 16.sp, lineHeight = 24.sp, color = C.ink)
                }
            }
            Gap(30.dp)
        }
    }
}

// ---------------- First launch ----------------
@Composable
fun Onboarding(nav: Nav) {
    val ctx = LocalContext.current
    val pager = rememberPagerState(pageCount = { purposePages.size + 1 })
    val scope = rememberCoroutineScope()
    var reminder by remember { mutableStateOf(true) }
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ReminderReceiver.schedule(ctx) }
    fun finish() {
        Store.onboarded = true
        Store.reminderOn = reminder
        if (reminder && Build.VERSION.SDK_INT >= 33) notif.launch(Manifest.permission.POST_NOTIFICATIONS) else ReminderReceiver.schedule(ctx)
        nav.replace(Screen.Tab(0))
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0C3A2D), Color(0xFF155A45))))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            HorizontalPager(pager, Modifier.weight(1f)) { i ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.Center) {
                    if (i < purposePages.size) {
                        val p = purposePages[i]
                        Text(p.emoji, fontSize = 52.sp)
                        Gap(10.dp)
                        Text(p.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(p.titleBn, fontSize = 18.sp, color = C.gold, fontWeight = FontWeight.SemiBold)
                        Gap(16.dp)
                        Text(p.body, fontSize = 16.sp, lineHeight = 24.sp, color = Color(0xFFE9E2C8))
                        Gap(12.dp)
                        Text(p.bodyBn, fontSize = 15.sp, lineHeight = 23.sp, color = Color(0xFFCFE6DA))
                    } else Setup(reminder) { reminder = it }
                }
            }
            Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(purposePages.size + 1) {
                        Box(Modifier.height(8.dp).width(if (it == pager.currentPage) 24.dp else 8.dp).clip(CircleShape)
                            .background(if (it == pager.currentPage) C.gold else Color(0x55F4EBD0)))
                    }
                }
                Button(onClick = { if (pager.currentPage < purposePages.size) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } else finish() },
                    colors = ButtonDefaults.buttonColors(containerColor = C.gold, contentColor = C.green)) {
                    Text(if (pager.currentPage < purposePages.size) "Next →" else "Start · শুরু করো", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun Setup(reminder: Boolean, onReminder: (Boolean) -> Unit) {
    Text("Let's set up", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Text("শুরু করার আগে", fontSize = 18.sp, color = C.gold, fontWeight = FontWeight.SemiBold)
    Gap(20.dp)
    Text("Meanings in · অর্থ দেখাও", color = Color.White, fontWeight = FontWeight.SemiBold)
    Gap(8.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Choice("বাংলা", Store.bengali, Modifier.weight(1f)) { Store.bengali = true }
        Choice("English", !Store.bengali, Modifier.weight(1f)) { Store.bengali = false }
    }
    Gap(20.dp)
    Text("Where do you start? · কোথা থেকে শুরু?", color = Color.White, fontWeight = FontWeight.SemiBold)
    Gap(8.dp)
    val starts = listOf("I'm new · একদম নতুন" to 0, "I know HSK 1 · HSK 1 জানি" to 1, "HSK 2" to 2, "HSK 3 or more · HSK 3+" to 3)
    for ((label, lv) in starts) {
        Choice(label, Store.startLevel == lv, Modifier.fillMaxWidth()) { Store.startLevel = lv }
        Gap(8.dp)
    }
    Gap(12.dp)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Daily reminder at 20:00", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("প্রতিদিন রাত ৮টায় মনে করিয়ে দেবে", color = Color(0xFFCFE6DA), fontSize = 14.sp)
        }
        Switch(checked = reminder, onCheckedChange = onReminder, colors = SwitchDefaults.colors(checkedTrackColor = C.gold, checkedThumbColor = C.green))
    }
}

@Composable
fun Choice(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) C.gold else Color(0x22F4EBD0)).clickable(onClick = onClick).padding(14.dp)) {
        Text(label, color = if (selected) C.green else Color.White, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun SpacerFill() = Spacer(Modifier.height(1.dp))

@Composable
fun OutlinedWide(text: String, onClick: () -> Unit) = OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.green)
}

fun reminderTime(): LocalTime = LocalTime.of(Store.reminderHour, Store.reminderMinute)
