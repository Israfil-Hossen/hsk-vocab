package com.israfilhossen.dawahchinese

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Where the reader is. The stack's last entry is on screen. */
sealed class Screen {
    data class Tab(val index: Int) : Screen()
    data class LevelScr(val id: Int) : Screen()
    data class LessonScr(val id: String) : Screen()
    data class Quiz(val id: String) : Screen()
    data class PartScr(val id: String) : Screen()
    data class Reader(val part: String, val sec: String) : Screen()
    data object Review : Screen()
    data object About : Screen()
    data object Onboarding : Screen()
}

class Nav(start: List<Screen>) {
    val stack = mutableStateListOf<Screen>().apply { addAll(start) }
    val top: Screen get() = stack.last()
    fun go(s: Screen) { stack.add(s) }
    fun back(): Boolean = if (stack.size > 1) { stack.removeAt(stack.lastIndex); true } else false
    fun tab(i: Int) { stack.clear(); stack.add(Screen.Tab(i)) }
    fun replace(s: Screen) { stack[stack.lastIndex] = s }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always light: dark status and navigation icons on a transparent bar.
        val bars = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        Store.init(this); Data.load(this); Speech.init(this)
        val start = startScreens(intent)
        setContent { AppTheme { App(remember { Nav(start) }) } }
    }

    override fun onDestroy() { Speech.stop(); super.onDestroy() }

    /** A "screen" extra opens one screen directly (used for store screenshots and tests). */
    private fun startScreens(i: Intent?): List<Screen> {
        val s = i?.getStringExtra("screen") ?: return listOf(if (Store.onboarded) Screen.Tab(0) else Screen.Onboarding)
        if (s != "onboarding") Store.onboarded = true
        val l1 = Data.content.levels[1]; val l2 = Data.content.levels[2]
        val book = Data.book[1]
        return when (s) {
            "onboarding" -> listOf(Screen.Onboarding)
            "learn" -> listOf(Screen.Tab(1))
            "book" -> listOf(Screen.Tab(2))
            "me" -> listOf(Screen.Tab(3))
            "level" -> listOf(Screen.Tab(1), Screen.LevelScr(1))
            "cards" -> listOf(Screen.Tab(1), Screen.LevelScr(1), Screen.LessonScr(l1.lessons[1].id))
            "dialogue" -> listOf(Screen.Tab(1), Screen.LevelScr(2), Screen.LessonScr(l2.lessons.first { it.type == "dialogue" }.id))
            "grammar" -> listOf(Screen.Tab(1), Screen.LevelScr(1), Screen.LessonScr(l1.lessons.first { it.type == "grammar" }.id))
            "reader" -> listOf(Screen.Tab(2), Screen.PartScr(book.id), Screen.Reader(book.id, book.sections[2].id))
            "about" -> listOf(Screen.Tab(3), Screen.About)
            else -> listOf(Screen.Tab(0))
        }
    }
}

fun openUrl(c: android.content.Context, url: String) {
    runCatching { c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

const val SITE = "https://israfil-hossen.github.io/hsk-vocab/dawah/"

@Composable
fun App(nav: Nav) {
    Store.tick.intValue // refresh when progress changes
    BackHandler(enabled = nav.stack.size > 1) { Speech.stop(); nav.back() }
    val top = nav.top
    if (top is Screen.Onboarding) { Onboarding(nav); return }
    if (top is Screen.Tab) {
        Scaffold(
            containerColor = C.bg,
            bottomBar = {
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    val items = listOf(
                        Triple(Icons.Filled.Home, tr("Today", "আজ"), 0),
                        Triple(Icons.Filled.Star, tr("Learn", "শেখা"), 1),
                        Triple(Icons.AutoMirrored.Filled.List, tr("Book", "বই"), 2),
                        Triple(Icons.Filled.Person, tr("Me", "আমি"), 3),
                    )
                    for ((icon, label, i) in items) NavigationBarItem(
                        selected = top.index == i, onClick = { nav.tab(i) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontWeight = if (top.index == i) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = C.green, selectedTextColor = C.green, indicatorColor = C.mint),
                    )
                }
            },
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                AnimatedContent(targetState = top.index, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { i ->
                    when (i) {
                        0 -> TodayScreen(nav)
                        1 -> LearnScreen(nav)
                        2 -> BookScreen(nav)
                        else -> MeScreen(nav)
                    }
                }
            }
        }
        return
    }
    Box(Modifier.fillMaxSize()) {
        when (top) {
            is Screen.LevelScr -> LevelScreen(nav, top.id)
            is Screen.LessonScr -> LessonScreen(nav, top.id)
            is Screen.Quiz -> QuizScreen(nav, top.id)
            is Screen.PartScr -> PartScreen(nav, top.id)
            is Screen.Reader -> ReaderScreen(nav, top.part, top.sec)
            Screen.Review -> ReviewScreen(nav)
            Screen.About -> AboutScreen(nav)
            else -> {}
        }
    }
}

