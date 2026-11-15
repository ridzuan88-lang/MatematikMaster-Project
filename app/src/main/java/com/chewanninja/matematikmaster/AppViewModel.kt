package com.chewanninja.matematikmaster

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

enum class Mode(val label: String, val mult: Int) {
    LATIHAN("Latihan", 10),
    PEPERIKSAAN("Peperiksaan", 15),
    KBAT("KBAT", 20)
}

sealed class Screen {
    object Loading : Screen()
    object Setup : Screen()
    object Login : Screen()
    object Home : Screen()
    data class QuizSetup(val mode: Mode) : Screen()
    object Quiz : Screen()
    object Result : Screen()
    data class Stats(val profileId: Long) : Screen()
    object Leaderboard : Screen()
    object Badges : Screen()
    object Parent : Screen()
}

data class BadgeDef(val code: String, val icon: String, val title: String, val desc: String)

val BADGES = listOf(
    BadgeDef("first", "🌟", "Langkah Pertama", "Selesaikan kuiz pertama anda"),
    BadgeDef("perfect", "💯", "Skor Sempurna", "Dapat 100% dalam kuiz sekurang-kurangnya 10 soalan"),
    BadgeDef("kbat", "🧠", "Pakar KBAT", "Skor 80% ke atas dalam KBAT (10 soalan atau lebih)"),
    BadgeDef("exam", "🏆", "Juara Peperiksaan", "Skor 90% ke atas dalam Peperiksaan (20 soalan atau lebih)"),
    BadgeDef("diligent", "🔥", "Rajin Berlatih", "Selesaikan 10 kuiz"),
    BadgeDef("marathon", "🏃", "Pelari Maraton", "Selesaikan satu kuiz 100 soalan"),
    BadgeDef("hundred", "🎯", "Seratus Betul", "Kumpul 100 jawapan betul"),
    BadgeDef("master", "🎓", "Penguasaan Topik", "Capai 90% ke atas dalam satu topik (20 jawapan atau lebih)")
)

class QuizSession(val mode: Mode, val tahun: Int, val questions: List<Q>, val limitSec: Int) {
    var index by mutableIntStateOf(0)
    val chosen = mutableStateListOf<Int>().apply { repeat(questions.size) { add(-1) } }
    var revealed by mutableStateOf(false)
    val startMs: Long = System.currentTimeMillis()
}

data class ResultSummary(
    val session: QuizSession,
    val correct: Int,
    val total: Int,
    val points: Int,
    val seconds: Int,
    val newBadges: List<BadgeDef>
)

data class StatsData(
    val results: List<QuizResult>,
    val topics: List<TopicRow>,
    val badges: Set<String>
)

data class BoardRow(val profile: Profile, val sessions: Int, val questions: Int, val correct: Int, val points: Int) {
    val accuracy: Int get() = if (questions == 0) 0 else correct * 100 / questions
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDb.get(app).dao()
    private val stack = ArrayList<Screen>()
    private var finishing = false

    var screen by mutableStateOf<Screen>(Screen.Loading)
        private set
    var user by mutableStateOf<Profile?>(null)
        private set
    var profiles by mutableStateOf<List<Profile>>(emptyList())
        private set
    var session by mutableStateOf<QuizSession?>(null)
        private set
    var summary by mutableStateOf<ResultSummary?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            refresh()
            screen = if (profiles.isEmpty()) Screen.Setup else Screen.Login
        }
    }

    // ---------- navigasi ----------
    fun go(s: Screen) {
        stack.add(screen)
        screen = s
    }

    fun back() {
        if (stack.isNotEmpty()) screen = stack.removeAt(stack.size - 1)
    }

    fun canGoBack(): Boolean = stack.isNotEmpty()

    private fun resetTo(s: Screen) {
        stack.clear()
        screen = s
    }

    private suspend fun refresh() {
        profiles = dao.profiles()
        val u = user
        if (u != null) user = profiles.firstOrNull { it.id == u.id }
    }

    // ---------- akaun ----------
    private fun hash(username: String, pass: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(("mm|" + username.trim().lowercase() + "|" + pass).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    }

    private fun validate(name: String, username: String, pass: String): String? = when {
        name.isBlank() -> "Sila isi nama"
        username.trim().length < 3 -> "Nama pengguna sekurang-kurangnya 3 aksara"
        username.trim().any { it.isWhitespace() } -> "Nama pengguna tidak boleh ada ruang kosong"
        pass.length < 4 -> "Kata laluan sekurang-kurangnya 4 aksara"
        else -> null
    }

    fun createParent(name: String, username: String, pass: String, onResult: (String?) -> Unit) {
        val err = validate(name, username, pass)
        if (err != null) {
            onResult(err)
            return
        }
        viewModelScope.launch {
            if (dao.byUsername(username.trim()) != null) {
                onResult("Nama pengguna sudah digunakan")
                return@launch
            }
            val id = dao.insertProfile(
                Profile(
                    username = username.trim(), displayName = name.trim(),
                    passwordHash = hash(username, pass), tahun = 1, avatar = "👨‍👩‍👧", isAdmin = true
                )
            )
            refresh()
            user = profiles.firstOrNull { it.id == id }
            resetTo(Screen.Home)
            onResult(null)
        }
    }

    fun login(username: String, pass: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val p = dao.byUsername(username.trim())
            if (p != null && p.passwordHash == hash(p.username, pass)) {
                user = p
                resetTo(Screen.Home)
                onResult(null)
            } else {
                onResult("Nama pengguna atau kata laluan salah")
            }
        }
    }

    fun logout() {
        user = null
        session = null
        summary = null
        viewModelScope.launch {
            refresh()
            resetTo(Screen.Login)
        }
    }

    fun addChild(
        name: String, username: String, pass: String, tahun: Int, avatar: String, photo: Uri?,
        onResult: (String?) -> Unit
    ) {
        val err = validate(name, username, pass)
        if (err != null) {
            onResult(err)
            return
        }
        viewModelScope.launch {
            if (dao.byUsername(username.trim()) != null) {
                onResult("Nama pengguna sudah digunakan")
                return@launch
            }
            val path = if (photo != null) withContext(Dispatchers.IO) { savePhoto(photo) } else null
            dao.insertProfile(
                Profile(
                    username = username.trim(), displayName = name.trim(),
                    passwordHash = hash(username, pass), tahun = tahun, avatar = avatar,
                    photoPath = path, isAdmin = false
                )
            )
            refresh()
            onResult(null)
        }
    }

    /** emoji / photo = null bermaksud tidak diubah. */
    fun updateAvatar(p: Profile, emoji: String?, photo: Uri?) {
        viewModelScope.launch {
            val path = if (photo != null) withContext(Dispatchers.IO) { savePhoto(photo) } else null
            val updated = when {
                path != null -> p.copy(photoPath = path)
                emoji != null -> p.copy(avatar = emoji, photoPath = null)
                else -> p
            }
            dao.updateProfile(updated)
            refresh()
        }
    }

    fun setTahun(p: Profile, tahun: Int) {
        viewModelScope.launch {
            dao.updateProfile(p.copy(tahun = tahun))
            refresh()
        }
    }

    fun resetPassword(p: Profile, newPass: String, onResult: (String?) -> Unit) {
        if (newPass.length < 4) {
            onResult("Kata laluan sekurang-kurangnya 4 aksara")
            return
        }
        viewModelScope.launch {
            dao.updateProfile(p.copy(passwordHash = hash(p.username, newPass)))
            refresh()
            onResult(null)
        }
    }

    fun deleteProfile(p: Profile) {
        if (p.isAdmin) return
        viewModelScope.launch {
            dao.deleteResults(p.id)
            dao.deleteAnswers(p.id)
            dao.deleteBadges(p.id)
            dao.deleteProfile(p.id)
            p.photoPath?.let { File(it).delete() }
            refresh()
        }
    }

    private fun savePhoto(uri: Uri): String? {
        return try {
            val resolver = getApplication<Application>().contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 512) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
            val side = minOf(bmp.width, bmp.height)
            val square = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
            val out = Bitmap.createScaledBitmap(square, 256, 256, true)
            val file = File(getApplication<Application>().filesDir, "avatar_" + System.currentTimeMillis() + ".jpg")
            FileOutputStream(file).use { out.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    // ---------- kuiz ----------
    fun startQuiz(mode: Mode, tahun: Int, level: Int, topic: String?, count: Int) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            val qs = withContext(Dispatchers.Default) {
                QuestionBank.build(tahun, count, level, topic, mode == Mode.KBAT)
            }
            busy = false
            if (qs.isEmpty()) {
                message = "Tiada soalan dapat dijana"
                return@launch
            }
            session = QuizSession(mode, tahun, qs, if (mode == Mode.PEPERIKSAAN) qs.size * 45 else 0)
            go(Screen.Quiz)
        }
    }

    fun choose(i: Int) {
        val s = session ?: return
        if (s.mode == Mode.PEPERIKSAAN) {
            s.chosen[s.index] = i
        } else {
            if (s.revealed) return
            s.chosen[s.index] = i
            s.revealed = true
        }
    }

    fun nextQ() {
        val s = session ?: return
        if (s.index < s.questions.size - 1) {
            s.index = s.index + 1
            s.revealed = false
        }
    }

    fun prevQ() {
        val s = session ?: return
        if (s.index > 0) {
            s.index = s.index - 1
            s.revealed = false
        }
    }

    fun abortQuiz() {
        session = null
        resetTo(Screen.Home)
    }

    fun finishQuiz() {
        val s = session ?: return
        val u = user ?: return
        if (finishing) return
        finishing = true
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            var correct = 0
            val recs = ArrayList<AnswerRec>()
            s.questions.forEachIndexed { i, q ->
                val ok = s.chosen[i] == q.answer
                if (ok) correct++
                recs.add(AnswerRec(profileId = u.id, tahun = q.tahun, topic = q.topic, correct = ok, timestamp = now))
            }
            val total = s.questions.size
            val points = correct * s.mode.mult
            val secs = ((now - s.startMs) / 1000).toInt()
            dao.insertResult(
                QuizResult(
                    profileId = u.id, tahun = s.tahun, mode = s.mode.name, total = total,
                    correct = correct, points = points, seconds = secs, timestamp = now
                )
            )
            dao.insertAnswers(recs)
            val newBadges = awardBadges(u.id)
            summary = ResultSummary(s, correct, total, points, secs, newBadges)
            session = null
            stack.clear()
            stack.add(Screen.Home)
            screen = Screen.Result
            finishing = false
        }
    }

    private suspend fun awardBadges(id: Long): List<BadgeDef> {
        val res = dao.allResults(id)
        val have = dao.badges(id).map { it.code }.toSet()
        val topics = dao.topicStats(id)
        val earned = HashSet<String>()
        if (res.isNotEmpty()) earned.add("first")
        if (res.any { it.total >= 10 && it.correct == it.total }) earned.add("perfect")
        if (res.any { it.mode == "KBAT" && it.total >= 10 && it.correct * 100 >= it.total * 80 }) earned.add("kbat")
        if (res.any { it.mode == "PEPERIKSAAN" && it.total >= 20 && it.correct * 100 >= it.total * 90 }) earned.add("exam")
        if (res.size >= 10) earned.add("diligent")
        if (res.any { it.total >= 100 }) earned.add("marathon")
        if (res.sumOf { it.correct } >= 100) earned.add("hundred")
        if (topics.any { it.total >= 20 && it.correct * 100 >= it.total * 90 }) earned.add("master")
        val fresh = earned - have
        val now = System.currentTimeMillis()
        fresh.forEach { dao.insertBadge(BadgeRec(id, it, now)) }
        return BADGES.filter { it.code in fresh }
    }

    // ---------- data paparan ----------
    suspend fun loadStats(id: Long): StatsData =
        StatsData(dao.allResults(id), dao.topicStats(id), dao.badges(id).map { it.code }.toSet())

    suspend fun loadBoard(): List<BoardRow> {
        val t = dao.totals().associateBy { it.profileId }
        return dao.profiles().map { p ->
            val x = t[p.id]
            BoardRow(p, x?.sessions ?: 0, x?.questions ?: 0, x?.correct ?: 0, x?.points ?: 0)
        }
    }
}
