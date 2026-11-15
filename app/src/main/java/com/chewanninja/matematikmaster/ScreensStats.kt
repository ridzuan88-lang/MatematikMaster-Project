package com.chewanninja.matematikmaster

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun masteryColor(p: Int): Color = when {
    p >= 80 -> Mint
    p >= 50 -> Sun
    else -> Coral
}

private fun masteryLabel(p: Int): String = when {
    p >= 80 -> "Mahir"
    p >= 50 -> "Sederhana"
    else -> "Perlu latihan"
}

@Composable
private fun StatCard(icon: String, label: String, value: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        T(icon, 24)
        T(value, 22, true, Sky)
        T(label, 12)
    }
}

// ===================== PRESTASI =====================
@Composable
fun StatsScreen(vm: AppViewModel, profileId: Long) {
    val p = vm.profiles.firstOrNull { it.id == profileId } ?: return
    var data by remember { mutableStateOf<StatsData?>(null) }
    LaunchedEffect(profileId) { data = vm.loadStats(profileId) }
    val fmt = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    Page("Prestasi: ${p.displayName}", { vm.back() }) {
        val d = data
        if (d == null) {
            T("Memuatkan…", 16)
        } else {
            val sessions = d.results.size
            val questions = d.results.sumOf { it.total }
            val correct = d.results.sumOf { it.correct }
            val points = d.results.sumOf { it.points }
            val acc = if (questions == 0) 0 else correct * 100 / questions
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("📚", "Kuiz", "$sessions", Modifier.weight(1f))
                StatCard("🎯", "Ketepatan", "$acc%", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("❓", "Soalan dijawab", "$questions", Modifier.weight(1f))
                StatCard("⭐", "Jumlah mata", "$points", Modifier.weight(1f))
            }
            if (sessions == 0) {
                Panel { T("Belum ada kuiz diselesaikan. Mulakan Latihan untuk melihat prestasi di sini!", 15) }
            } else {
                Panel {
                    T("Ketepatan mengikut Tahun", 18, true)
                    (1..6).forEach { t ->
                        val rs = d.results.filter { it.tahun == t }
                        if (rs.isNotEmpty()) {
                            val tq = rs.sumOf { it.total }
                            val pc = if (tq == 0) 0 else rs.sumOf { it.correct } * 100 / tq
                            T("Tahun $t – $pc% (${rs.size} kuiz)", 14, true)
                            Bar(pc, masteryColor(pc))
                        }
                    }
                }
                Panel {
                    T("Penguasaan Topik", 18, true)
                    d.topics.forEach { tr ->
                        val pc = if (tr.total == 0) 0 else tr.correct * 100 / tr.total
                        T("T${tr.tahun} • ${tr.topic} – $pc% (${tr.correct}/${tr.total}) • ${masteryLabel(pc)}", 14, true)
                        Bar(pc, masteryColor(pc))
                    }
                }
                Panel {
                    T("Kuiz terkini", 18, true)
                    d.results.take(10).forEach { r ->
                        val pc = if (r.total == 0) 0 else r.correct * 100 / r.total
                        val m = Mode.values().firstOrNull { it.name == r.mode }?.label ?: r.mode
                        T("${fmt.format(Date(r.timestamp))} • $m • T${r.tahun} • ${r.correct}/${r.total} ($pc%) • +${r.points}", 13)
                    }
                }
            }
            Panel {
                T("Lencana: ${d.badges.size}/${BADGES.size}", 16, true)
                T(BADGES.filter { it.code in d.badges }.joinToString("  ") { it.icon }.ifEmpty { "Belum ada lencana" }, 24)
            }
        }
    }
}

// ===================== PAPAN PENDAHULU =====================
@Composable
fun LeaderboardScreen(vm: AppViewModel) {
    var rows by remember { mutableStateOf<List<BoardRow>>(emptyList()) }
    var byAcc by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { rows = vm.loadBoard() }
    val sorted = if (byAcc) rows.sortedWith(compareByDescending<BoardRow> { it.accuracy }.thenByDescending { it.points })
    else rows.sortedByDescending { it.points }
    val me = vm.user?.id
    Page("Papan Pendahulu 🏅", { vm.back() }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("⭐ Mengikut Mata", !byAcc) { byAcc = false }
            Chip("🎯 Mengikut Ketepatan", byAcc) { byAcc = true }
        }
        if (sorted.isEmpty()) T("Tiada data lagi.", 15)
        sorted.forEachIndexed { i, r ->
            val medal = when (i) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "#${i + 1}" }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(if (r.profile.id == me) Color(0xFFFFF3CD) else Color.White).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                T(medal, 22, true, modifier = Modifier.width(48.dp))
                Avatar(r.profile, 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    T(r.profile.displayName + if (r.profile.isAdmin) " (Ibu Bapa)" else "", 16, true)
                    T("${r.points} mata • ${r.accuracy}% tepat • ${r.questions} soalan", 13)
                }
            }
        }
    }
}

// ===================== LENCANA =====================
@Composable
fun BadgesScreen(vm: AppViewModel) {
    val u = vm.user ?: return
    var earned by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(u.id) { earned = vm.loadStats(u.id).badges }
    Page("Lencana 🎖️", { vm.back() }) {
        val e = earned
        if (e == null) {
            T("Memuatkan…", 15)
        } else {
            T("Anda memperoleh ${e.size} daripada ${BADGES.size} lencana", 15, true)
            BADGES.forEach { b ->
                val got = b.code in e
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(if (got) Color(0xFFFFF3CD) else Color(0xFFE9E9EF)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    T(if (got) b.icon else "🔒", 34)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        T(b.title, 17, true, if (got) Navy else Color.Gray)
                        T(b.desc, 13, false, if (got) Navy else Color.Gray)
                    }
                }
            }
        }
    }
}

// ===================== DASHBOARD IBU BAPA =====================
@Composable
fun ParentScreen(vm: AppViewModel) {
    var rows by remember { mutableStateOf<List<BoardRow>>(emptyList()) }
    var reload by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var resetFor by remember { mutableStateOf<Profile?>(null) }
    var deleteFor by remember { mutableStateOf<Profile?>(null) }
    var tahunFor by remember { mutableStateOf<Profile?>(null) }
    LaunchedEffect(vm.profiles, reload) { rows = vm.loadBoard() }

    Page("Dashboard Ibu Bapa 👨‍👩‍👧", { vm.back() }) {
        val children = rows.filter { !it.profile.isAdmin }
        val q = children.sumOf { it.questions }
        val c = children.sumOf { it.correct }
        Panel {
            T("Ringkasan keluarga", 18, true)
            T("Bilangan anak: ${children.size}", 15)
            T("Kuiz diselesaikan: ${children.sumOf { it.sessions }}", 15)
            T("Ketepatan keseluruhan: ${if (q == 0) 0 else c * 100 / q}%", 15)
        }
        BigButton("➕ Tambah Profil Anak", Mint) { showAdd = true }
        rows.forEach { r ->
            val p = r.profile
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(p, 56.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        T(p.displayName + if (p.isAdmin) " (Ibu Bapa)" else "", 17, true)
                        T("@${p.username}" + if (!p.isAdmin) " • Tahun ${p.tahun}" else "", 13)
                        T("${r.points} mata • ${r.accuracy}% tepat • ${r.sessions} kuiz", 13)
                    }
                }
                Flow {
                    SmallButton("📊 Prestasi", Sky) { vm.go(Screen.Stats(p.id)) }
                    SmallButton("🔑 Kata laluan", Sun) { resetFor = p }
                    if (!p.isAdmin) {
                        SmallButton("🎓 Tukar Tahun", Grape) { tahunFor = p }
                        SmallButton("🗑 Padam", Coral) { deleteFor = p }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddChildDialog(vm, onDismiss = { showAdd = false; reload++ })
    }
    val rf = resetFor
    if (rf != null) {
        var np by remember(rf.id) { mutableStateOf("") }
        var err by remember(rf.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { resetFor = null },
            title = { T("Kata laluan baharu: ${rf.displayName}", 18, true) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Kata laluan baharu", np, true) { np = it }
                    val e = err
                    if (e != null) T(e, 14, true, Coral)
                }
            },
            confirmButton = {
                SmallButton("Simpan", Mint) {
                    vm.resetPassword(rf, np) { res -> if (res == null) { resetFor = null; vm.message = "Kata laluan dikemas kini" } else err = res }
                }
            },
            dismissButton = { TextButton(onClick = { resetFor = null }) { Text("Batal") } }
        )
    }
    val tf = tahunFor
    if (tf != null) {
        AlertDialog(
            onDismissRequest = { tahunFor = null },
            title = { T("Tahun untuk ${tf.displayName}", 18, true) },
            text = { Flow { (1..6).forEach { t -> Chip("Tahun $t", tf.tahun == t) { vm.setTahun(tf, t); tahunFor = null } } } },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { tahunFor = null }) { Text("Tutup") } }
        )
    }
    val df = deleteFor
    if (df != null) {
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { T("Padam ${df.displayName}?", 18, true) },
            text = { T("Profil dan semua rekod kuiz akan dipadam secara kekal.", 15) },
            confirmButton = { SmallButton("Padam", Coral) { vm.deleteProfile(df); deleteFor = null } },
            dismissButton = { TextButton(onClick = { deleteFor = null }) { Text("Batal") } }
        )
    }
}

@Composable
fun AddChildDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var tahun by remember { mutableIntStateOf(1) }
    var emoji by remember { mutableStateOf<String?>("🦁") }
    var photo by remember { mutableStateOf<Uri?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { T("Tambah Profil Anak", 20, true) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Field("Nama anak", name) { name = it }
                Field("Nama pengguna", user) { user = it }
                Field("Kata laluan", pass, true) { pass = it }
                T("Tahun", 14, true)
                Flow { (1..6).forEach { t -> Chip("$t", tahun == t) { tahun = t } } }
                T("Gambar profil", 14, true)
                AvatarPreview(emoji, photo, null)
                AvatarEditor(emoji, { emoji = it; photo = null }, { photo = it; emoji = null })
                val e = err
                if (e != null) T(e, 14, true, Coral)
            }
        },
        confirmButton = {
            SmallButton("Simpan", Mint) {
                vm.addChild(name, user, pass, tahun, emoji ?: "🙂", photo) { res ->
                    if (res == null) onDismiss() else err = res
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
