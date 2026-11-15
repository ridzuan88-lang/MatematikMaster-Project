package com.chewanninja.matematikmaster

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ===================== SETUP IBU BAPA =====================
@Composable
fun SetupScreen(vm: AppViewModel) {
    var name by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    Page("Selamat Datang! 👋", null) {
        Panel {
            T("Cipta akaun Ibu Bapa / Admin. Selepas ini anda boleh menambah profil anak di Dashboard Ibu Bapa.", 15)
            Field("Nama (cth: Ayah Ali)", name) { name = it }
            Field("Nama pengguna", user) { user = it }
            Field("Kata laluan", pass, true) { pass = it }
            Field("Ulang kata laluan", pass2, true) { pass2 = it }
            val e = err
            if (e != null) T(e, 15, true, Coral)
            BigButton("Cipta Akaun", Mint) {
                if (pass != pass2) err = "Kata laluan tidak sama"
                else vm.createParent(name, user, pass) { err = it }
            }
        }
    }
}

// ===================== LOG MASUK =====================
@Composable
fun LoginScreen(vm: AppViewModel) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    val shape = RoundedCornerShape(16.dp)
    Page("Matematik Master 🎓", null) {
        T("Pilih profil, atau taip nama pengguna:", 16)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            vm.profiles.forEach { p ->
                Column(
                    Modifier.focusRing(shape).clip(shape).clickable { user = p.username; err = null }.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Avatar(p, 64.dp)
                    T(p.displayName, 14, true)
                }
            }
        }
        Panel {
            Field("Nama pengguna", user) { user = it }
            Field("Kata laluan", pass, true) { pass = it }
            val e = err
            if (e != null) T(e, 15, true, Coral)
            BigButton("Masuk", Sky) { vm.login(user, pass) { err = it } }
        }
    }
}

// ===================== MENU UTAMA =====================
private class MenuItem(val icon: String, val title: String, val sub: String, val color: Color, val action: () -> Unit)

@Composable
fun HomeScreen(vm: AppViewModel) {
    val u = vm.user ?: return
    var editAvatar by remember { mutableStateOf(false) }
    val shape = CircleShape
    Page("Matematik Master", null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.focusRing(shape).clip(shape).clickable { editAvatar = true }) { Avatar(u, 72.dp) }
            Column {
                T("Hai, ${u.displayName}! 👋", 22, true)
                T(if (u.isAdmin) "Ibu Bapa / Admin" else "Tahun ${u.tahun}", 14)
                T("Ketik gambar untuk menukar", 12, false, Color.Gray)
            }
        }
        val items = mutableListOf(
            MenuItem("✏️", "Latihan", "Jawapan terus + penerangan", Sky) { vm.go(Screen.QuizSetup(Mode.LATIHAN)) },
            MenuItem("📝", "Peperiksaan", "Bermasa, markah di akhir", Coral) { vm.go(Screen.QuizSetup(Mode.PEPERIKSAAN)) },
            MenuItem("🧠", "KBAT", "Soalan pemikiran aras tinggi", Grape) { vm.go(Screen.QuizSetup(Mode.KBAT)) },
            MenuItem("📊", "Prestasi Saya", "Penguasaan topik", Mint) { vm.go(Screen.Stats(u.id)) },
            MenuItem("🏅", "Papan Pendahulu", "Kedudukan keluarga", Color(0xFFFF9800)) { vm.go(Screen.Leaderboard) },
            MenuItem("🎖️", "Lencana", "Pencapaian anda", Color(0xFF00ACC1)) { vm.go(Screen.Badges) }
        )
        if (u.isAdmin) items.add(MenuItem("👨‍👩‍👧", "Dashboard Ibu Bapa", "Urus profil & prestasi", Navy) { vm.go(Screen.Parent) })
        items.add(MenuItem("🚪", "Log Keluar", "Tukar pengguna", Color.Gray) { vm.logout() })
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { it.Card(this) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    if (editAvatar) {
        AvatarDialog(u, { editAvatar = false }) { emoji, photo ->
            vm.updateAvatar(u, emoji, photo)
            editAvatar = false
        }
    }
}

@Composable
private fun MenuItem.Card(scope: RowScope) {
    with(scope) { MenuCard(icon, title, sub, color, Modifier.weight(1f), action) }
}
