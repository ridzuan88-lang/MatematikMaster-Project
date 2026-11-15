package com.chewanninja.matematikmaster

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// ===================== TETAPAN KUIZ =====================
@Composable
fun QuizSetupScreen(vm: AppViewModel, mode: Mode) {
    val u = vm.user ?: return
    var tahun by remember { mutableIntStateOf(if (u.isAdmin) 1 else u.tahun) }
    var level by remember { mutableIntStateOf(-1) }
    var topic by remember { mutableStateOf<String?>(null) }
    var count by remember { mutableIntStateOf(10) }
    Page("${mode.label} – Tetapan", { vm.back() }) {
        Panel {
            T("Tahun", 16, true)
            Flow { (1..6).forEach { t -> Chip("Tahun $t", tahun == t) { tahun = t; topic = null } } }
        }
        if (mode != Mode.KBAT) {
            Panel {
                T("Tahap kesukaran", 16, true)
                Flow {
                    QuestionBank.levels.forEachIndexed { i, n -> Chip(n, level == i) { level = i } }
                    Chip("Campuran", level == -1) { level = -1 }
                }
            }
        }
        if (mode == Mode.LATIHAN) {
            Panel {
                T("Topik", 16, true)
                Flow {
                    Chip("Semua topik", topic == null) { topic = null }
                    QuestionBank.topics(tahun).forEach { tp -> Chip(tp, topic == tp) { topic = tp } }
                }
            }
        }
        Panel {
            T("Bilangan soalan", 16, true)
            Flow { (10..100 step 10).forEach { c -> Chip("$c", count == c) { count = c } } }
        }
        if (mode == Mode.PEPERIKSAAN) {
            val secs = count * 45
            T("⏱ Masa peperiksaan: ${secs / 60} minit ${secs % 60} saat. Jawapan ditunjukkan di akhir.", 14)
        }
        if (mode == Mode.KBAT) T("🧠 Soalan KBAT memerlukan beberapa langkah pemikiran. Jawapan ditunjukkan terus.", 14)
        BigButton(if (vm.busy) "Menyediakan soalan…" else "▶ Mula", Mint, enabled = !vm.busy) {
            vm.startQuiz(mode, tahun, level, topic, count)
        }
    }
}

// ===================== KUIZ =====================
@Composable
fun QuizScreen(vm: AppViewModel) {
    val s = vm.session ?: return
    var confirmExit by remember { mutableStateOf(false) }
    var confirmSubmit by remember { mutableStateOf(false) }
    var remaining by remember { mutableIntStateOf(s.limitSec) }
    val exam = s.mode == Mode.PEPERIKSAAN
    BackHandler { confirmExit = true }
    if (exam) {
        LaunchedEffect(s) {
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
            }
            vm.finishQuiz()
        }
    }
    val q = s.questions[s.index]
    val last = s.index == s.questions.size - 1
    val chosen = s.chosen[s.index]

    Page("${s.mode.label} • Tahun ${s.tahun}", null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            T("Soalan ${s.index + 1}/${s.questions.size}", 16, true, modifier = Modifier.weight(1f))
            if (exam) {
                val low = remaining < 60
                T("⏱ %d:%02d".format(remaining / 60, remaining % 60), 18, true, if (low) Coral else Navy)
            }
        }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Soft)) {
            Box(
                Modifier.fillMaxWidth((s.index + 1) / s.questions.size.toFloat())
                    .height(10.dp).background(Sky)
            )
        }
        Panel {
            T(q.topic, 12, false, Color.Gray)
            T(q.text, 22, true)
        }
        val letters = listOf("A", "B", "C", "D")
        q.options.forEachIndexed { i, opt ->
            val bg: Color
            val bc: Color
            if (!exam && s.revealed) {
                bg = when {
                    i == q.answer -> Color(0xFFD4F5E4)
                    i == chosen -> Color(0xFFFFDADA)
                    else -> Color.White
                }
                bc = when {
                    i == q.answer -> Mint
                    i == chosen -> Coral
                    else -> Soft
                }
            } else {
                bg = if (exam && chosen == i) Color(0xFFCFE2FF) else Color.White
                bc = if (exam && chosen == i) Sky else Soft
            }
            val shape = RoundedCornerShape(16.dp)
            Row(
                Modifier.fillMaxWidth().focusRing(shape).clip(shape).background(bg)
                    .border(2.dp, bc, shape)
                    .clickable { vm.choose(i) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Navy), contentAlignment = Alignment.Center) {
                    T(letters[i], 16, true, Color.White)
                }
                Spacer(Modifier.width(12.dp))
                T(opt, 20, true)
            }
        }
        if (!exam) {
            if (s.revealed) {
                val ok = chosen == q.answer
                Panel(color = if (ok) Color(0xFFD4F5E4) else Color(0xFFFFEFD5)) {
                    T(if (ok) "✅ Betul! Syabas!" else "❌ Belum tepat. Jawapan: ${q.options[q.answer]}", 18, true)
                    T("💡 ${q.explanation}", 15)
                }
                if (last) BigButton("🏁 Selesai", Mint) { vm.finishQuiz() }
                else BigButton("Soalan Seterusnya ➡", Sky) { vm.nextQ() }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigButton("⬅ Sebelum", Navy, Modifier.weight(1f), enabled = s.index > 0) { vm.prevQ() }
                BigButton("Seterusnya ➡", Sky, Modifier.weight(1f), enabled = !last) { vm.nextQ() }
            }
            BigButton("📤 Hantar Jawapan", Mint) { confirmSubmit = true }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { T("Keluar daripada kuiz?", 20, true) },
            text = { T("Kemajuan kuiz ini tidak akan disimpan.", 15) },
            confirmButton = { SmallButton("Keluar", Coral) { confirmExit = false; vm.abortQuiz() } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Teruskan kuiz") } }
        )
    }
    if (confirmSubmit) {
        val answered = s.chosen.count { it >= 0 }
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { T("Hantar jawapan?", 20, true) },
            text = { T("Anda telah menjawab $answered daripada ${s.questions.size} soalan.", 15) },
            confirmButton = { SmallButton("Hantar", Mint) { confirmSubmit = false; vm.finishQuiz() } },
            dismissButton = { TextButton(onClick = { confirmSubmit = false }) { Text("Semak semula") } }
        )
    }
}

// ===================== KEPUTUSAN =====================
@Composable
fun ResultScreen(vm: AppViewModel) {
    val r = vm.summary ?: return
    val pct = if (r.total == 0) 0 else r.correct * 100 / r.total
    val (emoji, msg) = when {
        pct >= 90 -> "🏆" to "Cemerlang!"
        pct >= 70 -> "🌟" to "Bagus!"
        pct >= 50 -> "👍" to "Boleh tahan, teruskan berlatih!"
        else -> "💪" to "Jangan putus asa, cuba lagi!"
    }
    Page("Keputusan", { vm.back() }) {
        Panel {
            T(emoji, 56, modifier = Modifier.fillMaxWidth(), align = androidx.compose.ui.text.style.TextAlign.Center)
            T("$pct%", 44, true, Sky, Modifier.fillMaxWidth(), androidx.compose.ui.text.style.TextAlign.Center)
            T(msg, 18, true, Navy, Modifier.fillMaxWidth(), androidx.compose.ui.text.style.TextAlign.Center)
            T("${r.correct} / ${r.total} betul  •  +${r.points} mata  •  ${r.seconds / 60}m ${r.seconds % 60}s", 15,
                false, Navy, Modifier.fillMaxWidth(), androidx.compose.ui.text.style.TextAlign.Center)
        }
        if (r.newBadges.isNotEmpty()) {
            Panel(color = Color(0xFFFFF3CD)) {
                T("🎉 Lencana baharu!", 18, true)
                r.newBadges.forEach { b -> T("${b.icon} ${b.title} – ${b.desc}", 15) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigButton("🔁 Main Lagi", Sky, Modifier.weight(1f)) { vm.go(Screen.QuizSetup(r.session.mode)) }
            BigButton("🏠 Menu", Navy, Modifier.weight(1f)) { vm.back() }
        }
        val wrong = r.session.questions.indices.filter { r.session.chosen[it] != r.session.questions[it].answer }
        if (wrong.isNotEmpty()) {
            T("Semakan jawapan salah (${wrong.size})", 18, true)
            wrong.forEach { i ->
                val q = r.session.questions[i]
                val c = r.session.chosen[i]
                Panel {
                    T("${i + 1}. ${q.text}", 16, true)
                    T("Jawapan anda: ${if (c >= 0) q.options[c] else "— tiada jawapan —"}", 14, false, Coral)
                    T("Jawapan betul: ${q.options[q.answer]}", 14, true, Mint)
                    T("💡 ${q.explanation}", 14)
                }
            }
        } else if (r.total > 0) {
            T("🎯 Semua jawapan betul!", 18, true, Mint)
        }
    }
}
