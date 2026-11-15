package com.chewanninja.matematikmaster

import java.math.BigDecimal
import java.util.Locale
import kotlin.random.Random

/** Satu soalan aneka pilihan. */
data class Q(
    val text: String,
    val options: List<String>,
    val answer: Int,
    val explanation: String,
    val topic: String,
    val tahun: Int,
    val kbat: Boolean = false
)

/** Pembantu untuk menjana soalan dengan jawapan dan pilihan pengalih yang betul. */
class Ctx(val t: Int, val topic: String, val r: Random, val kbat: Boolean) {

    fun rnd(a: Int, b: Int): Int = r.nextInt(a, b + 1)
    fun <T> pick(l: List<T>): T = l[r.nextInt(l.size)]

    fun pow10(k: Int): Int {
        var x = 1
        repeat(k) { x *= 10 }
        return x
    }

    fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    fun fmt(x: Double, dp: Int): String = String.format(Locale.US, "%.${dp}f", x)

    fun rm(sen: Int): String = "RM" + (sen / 100) + "." + (sen % 100).toString().padStart(2, '0')

    fun hr(x: Int): Int = (((x - 1) % 12) + 12) % 12 + 1

    fun clock(mins: Int): String {
        val h = ((mins / 60) % 12).let { if (it == 0) 12 else it }
        return "$h:" + (mins % 60).toString().padStart(2, '0')
    }

    fun f24(mins: Int): String =
        (mins / 60).toString().padStart(2, '0') + (mins % 60).toString().padStart(2, '0')

    fun mc(text: String, correct: String, wrongs: List<String>, exp: String): Q {
        val w = wrongs.filter { it != correct }.distinct().take(3).toMutableList()
        val pads = listOf("Tidak pasti", "Semua salah", "Tiada jawapan")
        var i = 0
        while (w.size < 3) {
            w.add(pads[i++])
        }
        val opts = (w + correct).shuffled(r)
        return Q(text, opts, opts.indexOf(correct), exp, topic, t, kbat)
    }

    fun num(text: String, ans: Int, exp: String, unit: String = "", prefix: String = ""): Q {
        val near = mutableListOf(1, -1, 2, -2, 5, -5, 10, -10)
        if (ans >= 200) near.addAll(listOf(20, -20, 50, -50, 100, -100))
        val cands = near.shuffled(r).map { ans + it }.filter { it >= 0 && it != ans }
        val wrongs = (cands + listOf(ans + 3, ans + 4, ans + 6)).distinct().take(3)
        return mc(text, "$prefix$ans$unit", wrongs.map { "$prefix$it$unit" }, exp)
    }

    fun dec(text: String, ans: Double, dp: Int, exp: String, unit: String = ""): Q {
        val s = Math.pow(10.0, -dp.toDouble())
        val c = listOf(ans + s, ans - s, ans + 2 * s, ans - 2 * s, ans + 10 * s, ans - 10 * s, ans + 3 * s)
            .filter { it >= 0.0 }
            .map { fmt(it, dp) + unit }
        return mc(text, fmt(ans, dp) + unit, c.shuffled(r), exp)
    }

    fun rmq(text: String, sen: Int, exp: String): Q {
        val c = listOf(10, -10, 20, -20, 50, -50, 100, -100, 5, 15).map { sen + it }
            .filter { it >= 0 && it != sen }.shuffled(r)
        return mc(text, rm(sen), c.take(4).map { rm(it) }, exp)
    }

    /** Soalan nilai tempat: digit bukan sifar pada satu tempat. */
    fun placeQ(n: Int): Q {
        val s = n.toString()
        val names = listOf("sa", "puluh", "ratus", "ribu", "puluh ribu", "ratus ribu", "juta")
        var pos = 0
        var digit = 0
        while (digit == 0) {
            pos = rnd(0, s.length - 1)
            digit = s[s.length - 1 - pos] - '0'
        }
        val ans = digit * pow10(pos)
        val wrongs = listOf(digit, digit * pow10(pos + 1), digit * pow10(maxOf(pos - 1, 0)), digit * pow10(pos + 2), digit * pow10(pos + 3))
        return mc(
            "Apakah nilai digit pada tempat ${names[pos]} dalam nombor $n?",
            "$ans", wrongs.map { "$it" },
            "Digit $digit berada pada tempat ${names[pos]}, jadi nilainya $ans."
        )
    }
}

internal class G(val topic: String, val fn: Ctx.(Int) -> Q)

object QuestionBank {

    val levels = listOf("Mudah", "Sederhana", "Sukar")

    fun topics(tahun: Int): List<String> = gens(tahun).map { it.topic }

    /**
     * Bina senarai soalan tanpa pengulangan.
     * level: 0 = Mudah, 1 = Sederhana, 2 = Sukar, -1 = Campuran.
     */
    fun build(tahun: Int, count: Int, level: Int, topic: String?, kbat: Boolean, seed: Long = System.nanoTime()): List<Q> {
        val r = Random(seed)
        val seen = HashSet<String>()
        val out = ArrayList<Q>()

        fun fill(pool: List<G>, isKbat: Boolean) {
            var guard = 0
            while (out.size < count && guard < count * 80) {
                guard++
                val g = pool[r.nextInt(pool.size)]
                val d = if (level < 0) r.nextInt(3) else level
                val q = g.fn(Ctx(tahun, g.topic, r, isKbat), d)
                if (seen.add(q.text)) out.add(q)
            }
        }

        if (kbat) {
            fill(kbatGens(tahun), true)
        } else {
            val all = gens(tahun)
            val sel = if (topic == null) all else all.filter { it.topic == topic }.ifEmpty { all }
            fill(sel, false)
            if (out.size < count) fill(all, false)
        }
        return out.shuffled(r)
    }

    private fun gens(t: Int): List<G> = when (t) {
        1 -> y1()
        2 -> y2()
        3 -> y3()
        4 -> y4()
        5 -> y5()
        else -> y6()
    }

    private fun kbatGens(t: Int): List<G> = when (t) {
        1 -> k1()
        2 -> k2()
        3 -> k3()
        4 -> k4()
        5 -> k5()
        else -> k6()
    }
}

// ===================== TAHUN 1 =====================
private fun y1(): List<G> = listOf(
    G("Tambah") { d ->
        val m = listOf(9, 20, 50)[d]; val a = rnd(1, m); val b = rnd(1, m)
        num("$a + $b = ?", a + b, "$a tambah $b sama dengan ${a + b}.")
    },
    G("Tolak") { d ->
        val m = listOf(10, 20, 99)[d]; val a = rnd(2, m); val b = rnd(1, a)
        num("$a − $b = ?", a - b, "$a tolak $b sama dengan ${a - b}.")
    },
    G("Nilai Tempat") { d ->
        if (d < 2) placeQ(rnd(11, 99)) else placeQ(rnd(100, 999))
    },
    G("Wang") { d ->
        when (d) {
            0 -> {
                val a = pick(listOf(10, 20, 50)); val b = pick(listOf(10, 20, 50))
                num("Ali ada syiling $a sen dan syiling $b sen. Berapakah jumlah wangnya?", a + b, "$a sen + $b sen = ${a + b} sen.", unit = " sen")
            }
            1 -> {
                val a = rnd(2, 9); val b = rnd(1, 9)
                num("Siti ada RM$a. Ibu memberi RM$b lagi. Berapakah jumlah wang Siti?", a + b, "RM$a + RM$b = RM${a + b}.", prefix = "RM")
            }
            else -> {
                val a = rnd(10, 50); val b = rnd(1, a - 1)
                num("Adik ada RM$a. Dia membeli buku berharga RM$b. Berapakah bakinya?", a - b, "RM$a − RM$b = RM${a - b}.", prefix = "RM")
            }
        }
    },
    G("Masa") { d ->
        val h = rnd(1, 8); val k = rnd(1, listOf(2, 3, 4)[d]); val e = hr(h + k)
        mc("Sekarang pukul $h:00. Pukul berapakah selepas $k jam?", "$e:00",
            listOf("${hr(e + 1)}:00", "${hr(e - 1)}:00", "${hr(h - k)}:00", "${hr(e + 2)}:00"),
            "$h tambah $k jam = pukul $e:00.")
    },
    G("Pola Nombor") { d ->
        val step = listOf(1, 2, 5)[d]
        val down = d == 2 && rnd(0, 1) == 1
        val s = if (down) rnd(40, 90) else rnd(1, 20)
        val sg = if (down) -1 else 1
        val seq = (0..3).map { s + it * step * sg }
        val ans = s + 4 * step * sg
        num("Lengkapkan pola: ${seq.joinToString(", ")}, ___", ans,
            "Nombor ${if (down) "berkurang" else "bertambah"} $step setiap kali. Seterusnya ialah $ans.")
    }
)

// ===================== TAHUN 2 =====================
private fun y2(): List<G> = listOf(
    G("Tambah") { d ->
        val m = listOf(50, 300, 900)[d]; val a = rnd(10, m); val b = rnd(10, m)
        num("$a + $b = ?", a + b, "$a + $b = ${a + b}.")
    },
    G("Tolak") { d ->
        val m = listOf(50, 300, 900)[d]; val a = rnd(20, m); val b = rnd(10, a)
        num("$a − $b = ?", a - b, "$a − $b = ${a - b}.")
    },
    G("Darab") { d ->
        val n = pick(listOf(2, 3, 4, 5, 10)); val m = rnd(2, listOf(5, 10, 12)[d])
        if (d == 0) num("$m × $n = ?", m * n, "$m kumpulan $n = ${m * n}.")
        else num("Ada $m bakul. Setiap bakul mengandungi $n biji mangga. Berapakah jumlah mangga?", m * n, "$m × $n = ${m * n} biji.", unit = " biji")
    },
    G("Bahagi") { d ->
        val n = pick(listOf(2, 3, 4, 5, 10)); val q = rnd(2, listOf(5, 10, 12)[d]); val a = n * q
        if (d == 0) num("$a ÷ $n = ?", q, "$a ÷ $n = $q kerana $n × $q = $a.")
        else num("$a biji gula-gula dikongsi sama banyak kepada $n orang murid. Berapakah bahagian setiap murid?", q, "$a ÷ $n = $q biji.", unit = " biji")
    },
    G("Wang") { d ->
        when (d) {
            0 -> {
                val a = rnd(5, 40); val b = rnd(5, 40)
                num("Harga sebatang pensel RM$a dan sebuah buku RM$b. Berapakah jumlah harganya?", a + b, "RM$a + RM$b = RM${a + b}.", prefix = "RM")
            }
            1 -> {
                val p = rnd(11, 95) * 10
                rmq("Ali membeli sebuah mainan berharga ${rm(p)}. Dia membayar RM10. Berapakah baki wang Ali?", 1000 - p, "RM10.00 − ${rm(p)} = ${rm(1000 - p)}.")
            }
            else -> {
                val a = rnd(10, 90) * 5; val b = rnd(10, 90) * 5; val c = rnd(10, 90) * 5
                rmq("Harga sebuah kuih ${rm(a)}, sebiji epal ${rm(b)} dan segelas air ${rm(c)}. Berapakah jumlah harganya?", a + b + c, "${rm(a)} + ${rm(b)} + ${rm(c)} = ${rm(a + b + c)}.")
            }
        }
    },
    G("Pecahan") { d ->
        val den = pick(listOf(2, 3, 4)); val w = den * rnd(2, listOf(5, 8, 12)[d]); val ans = w / den
        num("Berapakah 1/$den daripada $w?", ans, "$w ÷ $den = $ans.")
    },
    G("Panjang dan Jisim") { d ->
        when (rnd(0, 2)) {
            0 -> { val m = rnd(1, listOf(5, 9, 20)[d]); num("$m m = ___ cm", m * 100, "1 m = 100 cm, jadi $m m = ${m * 100} cm.", unit = " cm") }
            1 -> { val k = rnd(1, listOf(5, 9, 20)[d]); num("$k kg = ___ g", k * 1000, "1 kg = 1 000 g, jadi $k kg = ${k * 1000} g.", unit = " g") }
            else -> {
                val a = rnd(50, 200); val b = rnd(10, a - 10)
                num("Seutas tali panjangnya $a cm. Sebanyak $b cm dipotong. Berapakah baki panjang tali itu?", a - b, "$a − $b = ${a - b} cm.", unit = " cm")
            }
        }
    }
)

// ===================== TAHUN 3 =====================
private fun y3(): List<G> = listOf(
    G("Operasi hingga 10 000") { d ->
        val m = listOf(1000, 5000, 9000)[d]
        if (rnd(0, 1) == 0) {
            val a = rnd(100, m); val b = rnd(100, m)
            num("$a + $b = ?", a + b, "$a + $b = ${a + b}.")
        } else {
            val a = rnd(500, m + 500); val b = rnd(100, a - 100)
            num("$a − $b = ?", a - b, "$a − $b = ${a - b}.")
        }
    },
    G("Darab") { d ->
        val a = if (d < 2) rnd(11, listOf(50, 99)[d]) else rnd(101, 300); val b = rnd(2, 9)
        num("$a × $b = ?", a * b, "$a × $b = ${a * b}.")
    },
    G("Bahagi") { d ->
        val b = rnd(2, 9); val q = rnd(11, listOf(30, 60, 99)[d]); val a = b * q
        num("$a ÷ $b = ?", q, "$a ÷ $b = $q kerana $b × $q = $a.")
    },
    G("Pecahan") { d ->
        val den = rnd(4, 12)
        when (d) {
            0 -> {
                val a = rnd(1, den - 2); val b = rnd(1, den - 1 - a)
                mc("$a/$den + $b/$den = ?", "${a + b}/$den",
                    listOf("${a + b}/${den * 2}", "${a * b + 1}/$den", "${a + b + 1}/$den", "${a + b}/${den + 1}"),
                    "Penyebut sama, jadi tambah pengangka sahaja: $a + $b = ${a + b}. Jawapan ${a + b}/$den.")
            }
            1 -> {
                val a = rnd(2, den - 1); val b = rnd(1, a - 1)
                mc("$a/$den − $b/$den = ?", "${a - b}/$den",
                    listOf("${a - b}/${den * 2}", "${a + b}/$den", "${a - b + 1}/$den", "${a - b}/${den - 1}"),
                    "Penyebut sama, tolak pengangka: $a − $b = ${a - b}. Jawapan ${a - b}/$den.")
            }
            else -> {
                val a = rnd(1, den - 1)
                mc("Sebiji kek dibahagi kepada $den bahagian sama. $a bahagian telah dimakan. Apakah pecahan kek yang tinggal?", "${den - a}/$den",
                    listOf("$a/$den", "${den - a}/${den + a}", "${den - a + 1}/$den", "${a}/${den - a}"),
                    "Kek yang tinggal = $den − $a = ${den - a} bahagian, iaitu ${den - a}/$den.")
            }
        }
    },
    G("Wang") { d ->
        when (d) {
            0 -> {
                val a = rnd(40, 400) * 5; val b = rnd(40, 400) * 5
                rmq("Harga sehelai baju ${rm(a)} dan sehelai seluar ${rm(b)}. Berapakah jumlah harganya?", a + b, "${rm(a)} + ${rm(b)} = ${rm(a + b)}.")
            }
            1 -> {
                val a = rnd(100, 900) * 5
                rmq("Ibu membayar RM50 untuk barang berharga ${rm(a)}. Berapakah baki yang diterima?", 5000 - a, "RM50.00 − ${rm(a)} = ${rm(5000 - a)}.")
            }
            else -> {
                val p = rnd(2, 9) * 50; val k = rnd(3, 8)
                rmq("Sebatang pen berharga ${rm(p)}. Berapakah harga $k batang pen?", p * k, "${rm(p)} × $k = ${rm(p * k)}.")
            }
        }
    },
    G("Ukuran") { _ ->
        when (rnd(0, 3)) {
            0 -> { val k = rnd(1, 9); num("$k km = ___ m", k * 1000, "1 km = 1 000 m. $k × 1 000 = ${k * 1000}.", unit = " m") }
            1 -> { val k = rnd(1, 9); num("$k kg = ___ g", k * 1000, "1 kg = 1 000 g. $k × 1 000 = ${k * 1000}.", unit = " g") }
            2 -> { val k = rnd(1, 9); num("$k liter = ___ ml", k * 1000, "1 liter = 1 000 ml. $k × 1 000 = ${k * 1000}.", unit = " ml") }
            else -> {
                val a = rnd(1, 9); val b = rnd(1, 9) * 10
                num("$a m $b cm = ___ cm", a * 100 + b, "$a m = ${a * 100} cm. ${a * 100} + $b = ${a * 100 + b} cm.", unit = " cm")
            }
        }
    },
    G("Masa") { d ->
        val h = rnd(1, 10); val m = pick(listOf(0, 15, 30, 45))
        val dur = rnd(2, listOf(6, 12, 20)[d]) * 5; val s = h * 60 + m
        num("Satu rancangan televisyen bermula pada ${clock(s)} dan tamat pada ${clock(s + dur)}. Berapa minitkah tempohnya?", dur,
            "Tempoh = $dur minit.", unit = " minit")
    }
)

// ===================== TAHUN 4 =====================
private fun y4(): List<G> = listOf(
    G("Nombor hingga 1 000 000") { d ->
        val len = listOf(4, 5, 6)[d]
        placeQ(rnd(pow10(len - 1), pow10(len) - 1))
    },
    G("Pembundaran") { d ->
        val place = listOf(10, 100, 1000)[d]; val nm = listOf("puluh", "ratus", "ribu")[d]
        val n = rnd(1000, 99999)
        val ans = ((n + place / 2) / place) * place
        val floor = (n / place) * place
        mc("Bundarkan $n kepada $nm yang terdekat.", "$ans",
            listOf("${floor}", "${floor + place}", "${ans + place}", "${ans - place}", "${ans + 2 * place}"),
            "Lihat digit di sebelah kanan tempat $nm. Jika 5 atau lebih, naikkan digit. Jawapan: $ans.")
    },
    G("Operasi Bercampur") { d ->
        when (d) {
            0 -> {
                val a = rnd(20, 99); val b = rnd(10, 99); val c = rnd(5, a + b - 1)
                num("$a + $b − $c = ?", a + b - c, "$a + $b = ${a + b}; ${a + b} − $c = ${a + b - c}.")
            }
            1 -> {
                val a = rnd(11, 60); val b = rnd(2, 9); val c = rnd(5, 99)
                num("$a × $b + $c = ?", a * b + c, "Darab dahulu: $a × $b = ${a * b}. Kemudian tambah $c = ${a * b + c}.")
            }
            else -> {
                val b = rnd(2, 9); val q = rnd(11, 60); val a = b * q; val c = rnd(2, 9); val e = rnd(2, 9)
                num("$a ÷ $b + $c × $e = ?", q + c * e, "Bahagi dan darab dahulu: $a ÷ $b = $q; $c × $e = ${c * e}. Kemudian tambah = ${q + c * e}.")
            }
        }
    },
    G("Pecahan dan Perpuluhan") { d ->
        when (d) {
            0 -> {
                val den = rnd(2, 6); val w = rnd(1, 4); val n = rnd(1, den - 1); val imp = w * den + n
                mc("Tukarkan $w $n/$den kepada pecahan tak wajar.", "$imp/$den",
                    listOf("${w + n}/$den", "${imp + 1}/$den", "$imp/${den + 1}", "${imp - 1}/$den"),
                    "$w × $den + $n = $imp, jadi $imp/$den.")
            }
            1 -> {
                val den = rnd(5, 12); val a = rnd(1, den - 2); val b = rnd(1, den - 1 - a)
                mc("$a/$den + $b/$den = ?", "${a + b}/$den",
                    listOf("${a + b}/${den * 2}", "${a * b + 1}/$den", "${a + b + 1}/$den", "${a + b}/${den + 1}"),
                    "Tambah pengangka: $a + $b = ${a + b}. Penyebut kekal $den.")
            }
            else -> {
                val a = rnd(100, 999); val b = rnd(100, 999)
                dec("${fmt(a / 100.0, 2)} + ${fmt(b / 100.0, 2)} = ?", (a + b) / 100.0, 2,
                    "Susun titik perpuluhan secara menegak dan tambah: ${fmt((a + b) / 100.0, 2)}.")
            }
        }
    },
    G("Wang") { d ->
        when (d) {
            0 -> {
                val a = rnd(100, 5000); val b = rnd(100, 5000)
                num("Encik Lee menyimpan RM$a pada bulan pertama dan RM$b pada bulan kedua. Berapakah jumlah simpanannya?", a + b, "RM$a + RM$b = RM${a + b}.", prefix = "RM")
            }
            1 -> {
                val a = rnd(1000, 9000); val b = rnd(100, a - 100)
                num("Puan Siti mempunyai RM$a. Dia membelanjakan RM$b. Berapakah baki wangnya?", a - b, "RM$a − RM$b = RM${a - b}.", prefix = "RM")
            }
            else -> {
                val p = rnd(20, 500); val k = rnd(2, 9)
                num("Harga sebuah beg ialah RM$p. Berapakah harga $k buah beg yang sama?", p * k, "RM$p × $k = RM${p * k}.", prefix = "RM")
            }
        }
    },
    G("Peratus") { d ->
        val a = rnd(1, 10) * 20; val p = pick(listOf(10, 20, 25, 50, 75)); val ans = a * p / 100
        if (d < 2) num("Berapakah $p% daripada $a?", ans, "$p% = $p/100. $p/100 × $a = $ans.")
        else num("Dalam sebuah kelas terdapat $a murid. $p% daripadanya memakai cermin mata. Berapakah bilangan murid yang memakai cermin mata?", ans,
            "$p/100 × $a = $ans murid.", unit = " murid")
    },
    G("Perimeter dan Luas") { d ->
        val l = rnd(4, listOf(12, 20, 30)[d]); val w = rnd(2, l - 1)
        when (d) {
            0 -> num("Sebuah segi empat tepat panjangnya $l cm dan lebarnya $w cm. Hitung perimeternya.", 2 * (l + w),
                "Perimeter = 2 × ($l + $w) = ${2 * (l + w)} cm.", unit = " cm")
            1 -> num("Sebuah segi empat tepat panjangnya $l cm dan lebarnya $w cm. Hitung luasnya.", l * w,
                "Luas = $l × $w = ${l * w} cm².", unit = " cm²")
            else -> num("Luas sebuah segi empat tepat ialah ${l * w} cm². Jika panjangnya $l cm, berapakah lebarnya?", w,
                "Lebar = ${l * w} ÷ $l = $w cm.", unit = " cm")
        }
    }
)

// ===================== TAHUN 5 =====================
private fun y5(): List<G> = listOf(
    G("Operasi Nombor Besar") { d ->
        when (d) {
            0 -> { val a = rnd(101, 999); val b = rnd(11, 30); num("$a × $b = ?", a * b, "$a × $b = ${a * b}.") }
            1 -> { val a = rnd(1001, 9999); val b = rnd(11, 99); num("$a × $b = ?", a * b, "$a × $b = ${a * b}.") }
            else -> { val b = rnd(11, 99); val q = rnd(101, 500); num("${b * q} ÷ $b = ?", q, "${b * q} ÷ $b = $q kerana $b × $q = ${b * q}.") }
        }
    },
    G("Pecahan") { d ->
        when (d) {
            0, 1 -> {
                val den = rnd(3, 8); val n = rnd(1, den - 1); val k = rnd(2, listOf(8, 15)[d]) * den; val ans = k / den * n
                num("Berapakah $n/$den daripada $k?", ans, "$k ÷ $den = ${k / den}; ${k / den} × $n = $ans.")
            }
            else -> {
                val t = pick(listOf(
                    intArrayOf(1, 2, 1, 4, 3, 4), intArrayOf(1, 3, 1, 6, 1, 2), intArrayOf(2, 5, 1, 10, 1, 2),
                    intArrayOf(1, 4, 1, 8, 3, 8), intArrayOf(1, 2, 1, 3, 5, 6), intArrayOf(3, 4, 1, 8, 7, 8),
                    intArrayOf(2, 3, 1, 6, 5, 6), intArrayOf(1, 5, 3, 10, 1, 2)
                ))
                val a = t[0]; val b = t[1]; val c = t[2]; val dd = t[3]; val e = t[4]; val f = t[5]
                mc("${a}/${b} + ${c}/${dd} = ?", "${e}/${f}",
                    listOf("${a + c}/${b + dd}", "${a + c}/${maxOf(b, dd)}", "${e + 1}/$f", "${e}/${f * 2}"),
                    "Samakan penyebut dahulu, kemudian tambah pengangka. Jawapan ${e}/${f}.")
            }
        }
    },
    G("Perpuluhan") { d ->
        when (d) {
            0 -> {
                val a = rnd(11, 99); val b = rnd(2, 9)
                dec("${fmt(a / 10.0, 1)} × $b = ?", a * b / 10.0, 1, "${fmt(a / 10.0, 1)} × $b = ${fmt(a * b / 10.0, 1)}.")
            }
            1 -> {
                val a = rnd(101, 999); val sh = pick(listOf(10, 100, 1000)); val dp = if (sh == 10) 1 else 0
                dec("${fmt(a / 100.0, 2)} × $sh = ?", a * sh / 100.0, dp,
                    "Darab dengan $sh: alihkan titik perpuluhan ke kanan ${sh.toString().length - 1} tempat.")
            }
            else -> {
                val b = rnd(2, 9); val q = rnd(11, 99); val total = b * q
                dec("${fmt(total / 10.0, 1)} ÷ $b = ?", q / 10.0, 1, "${fmt(total / 10.0, 1)} ÷ $b = ${fmt(q / 10.0, 1)}.")
            }
        }
    },
    G("Peratus") { d ->
        when (d) {
            0, 1 -> {
                val a = rnd(1, 15) * 20
                val p = pick(if (d == 0) listOf(10, 20, 25, 50) else listOf(5, 15, 30, 40, 60, 75))
                num("Berapakah $p% daripada $a?", a * p / 100, "$a × $p/100 = ${a * p / 100}.")
            }
            else -> {
                var p = 10; var y = 20
                do {
                    p = pick(listOf(10, 20, 25, 40, 50, 60, 75, 80)); y = pick(listOf(20, 25, 40, 50, 100, 200))
                } while ((p * y) % 100 != 0)
                val x = p * y / 100
                num("Dalam sebuah kelas, $x daripada $y murid memakai cermin mata. Berapakah peratus murid yang memakai cermin mata?", p,
                    "$x/$y × 100 = $p%.", unit = "%")
            }
        }
    },
    G("Purata") { d ->
        val n = rnd(3, listOf(4, 5, 6)[d]); val m = rnd(10, 90)
        var nums: List<Int> = emptyList()
        do {
            val first = (1 until n).map { m + rnd(-9, 9) }
            nums = first + (m * n - first.sum())
        } while (nums.last() < 1)
        num("Cari purata bagi nombor berikut: ${nums.joinToString(", ")}.", m,
            "Jumlah = ${nums.sum()}. Purata = ${nums.sum()} ÷ $n = $m.")
    },
    G("Isi Padu") { d ->
        when (d) {
            0 -> { val l = rnd(2, 6); val w = rnd(2, 6); val h = rnd(2, 6)
                num("Sebuah kuboid berukuran $l cm × $w cm × $h cm. Berapakah isi padunya?", l * w * h, "Isi padu = $l × $w × $h = ${l * w * h} cm³.", unit = " cm³") }
            1 -> { val l = rnd(5, 15); val w = rnd(5, 15); val h = rnd(5, 15)
                num("Sebuah kuboid berukuran $l cm × $w cm × $h cm. Berapakah isi padunya?", l * w * h, "Isi padu = $l × $w × $h = ${l * w * h} cm³.", unit = " cm³") }
            else -> { val l = rnd(2, 10); val w = rnd(2, 10); val h = rnd(2, 10); val v = l * w * h
                num("Isi padu sebuah kuboid ialah $v cm³. Panjangnya $l cm dan lebarnya $w cm. Berapakah tingginya?", h, "Tinggi = $v ÷ ($l × $w) = $h cm.", unit = " cm") }
        }
    },
    G("Masa dan Waktu") { d ->
        when (d) {
            0 -> {
                val h = rnd(1, 10); val mm = pick(listOf("00", "15", "30", "45"))
                mc("Tukarkan $h:$mm p.m. kepada sistem 24 jam.", "${h + 12}$mm",
                    listOf(h.toString().padStart(2, '0') + mm, "${h + 11}$mm", "${h + 13}$mm"),
                    "Tambah 12 pada jam p.m.: $h + 12 = ${h + 12}. Jawapan ${h + 12}$mm.")
            }
            1 -> {
                val h = rnd(13, 23); val mm = pick(listOf("00", "15", "30", "45"))
                mc("Tukarkan $h$mm jam kepada sistem 12 jam.", "${h - 12}:$mm p.m.",
                    listOf("${h - 12}:$mm a.m.", "${h - 10}:$mm p.m.", "${h - 11}:$mm p.m."),
                    "Tolak 12 daripada jam: $h − 12 = ${h - 12}. Jawapan ${h - 12}:$mm p.m.")
            }
            else -> {
                val s = rnd(6, 14) * 60 + pick(listOf(0, 15, 30, 45)); val dur = rnd(3, 20) * 10
                num("Sebuah bas bertolak pada ${f24(s)} dan tiba pada ${f24(s + dur)}. Berapa minitkah tempoh perjalanan itu?", dur,
                    "Tempoh = $dur minit.", unit = " minit")
            }
        }
    }
)

// ===================== TAHUN 6 =====================
private fun y6(): List<G> = listOf(
    G("Operasi Bergabung") { d ->
        when (d) {
            0 -> { val a = rnd(100, 500); val b = rnd(2, 9); val c = rnd(10, 99)
                num("$a + $b × $c = ?", a + b * c, "Darab dahulu: $b × $c = ${b * c}. Kemudian $a + ${b * c} = ${a + b * c}.") }
            1 -> { val a = rnd(10, 99); val b = rnd(10, 99); val c = rnd(2, 9)
                num("($a + $b) × $c = ?", (a + b) * c, "Kira dalam kurungan dahulu: $a + $b = ${a + b}. Kemudian × $c = ${(a + b) * c}.") }
            else -> { val b = rnd(2, 9); val q = rnd(20, 99); val a = b * q; val c = rnd(11, 30); val e = rnd(2, 9)
                num("$a ÷ $b + $c × $e = ?", q + c * e, "$a ÷ $b = $q; $c × $e = ${c * e}. Jumlah = ${q + c * e}.") }
        }
    },
    G("Pecahan, Perpuluhan dan Peratus") { d ->
        val pool = listOf(
            intArrayOf(1, 2), intArrayOf(1, 4), intArrayOf(3, 4), intArrayOf(1, 5), intArrayOf(2, 5), intArrayOf(3, 5),
            intArrayOf(1, 10), intArrayOf(7, 10), intArrayOf(9, 10), intArrayOf(1, 20), intArrayOf(3, 20),
            intArrayOf(7, 20), intArrayOf(1, 25), intArrayOf(3, 25)
        )
        fun decOf(p: Int) = BigDecimal(p).movePointLeft(2).stripTrailingZeros().toPlainString()
        val item = pick(pool); val n = item[0]; val den = item[1]; val p = n * 100 / den
        val others = pool.shuffled(r).map { it[0] * 100 / it[1] }
        when (d) {
            0 -> mc("Tukarkan $n/$den kepada perpuluhan.", decOf(p), others.map { decOf(it) } + listOf("0.0$n"),
                "$n/$den = $p/100 = ${decOf(p)}.")
            1 -> mc("Tukarkan $n/$den kepada peratus.", "$p%", others.map { "$it%" },
                "$n ÷ $den × 100 = $p%.")
            else -> {
                val g = gcd(p, 100)
                mc("Tukarkan $p% kepada pecahan paling ringkas.", "${p / g}/${100 / g}",
                    listOf("$p/100", "${100 / g}/${p / g}", "${p / g}/${100 / g + 1}", "$p/10"),
                    "$p% = $p/100. Bahagi pengangka dan penyebut dengan $g: ${p / g}/${100 / g}.")
            }
        }
    },
    G("Peratus dan Diskaun") { d ->
        when (d) {
            0 -> { val a = rnd(1, 10) * 20; val p = pick(listOf(10, 20, 25, 50)); val x = a * p / 100
                num("Harga sehelai baju ialah RM$a. Diskaun $p% diberikan. Berapakah jumlah diskaun?", x, "$p/100 × RM$a = RM$x.", prefix = "RM") }
            1 -> { val a = rnd(1, 10) * 20; val p = pick(listOf(10, 20, 25, 50)); val x = a * p / 100
                num("Harga sebuah kasut ialah RM$a. Diskaun $p% diberikan. Berapakah harga selepas diskaun?", a - x, "Diskaun = RM$x. Harga = RM$a − RM$x = RM${a - x}.", prefix = "RM") }
            else -> { val a = rnd(1, 10) * 50; val tax = a * 6 / 100
                num("Harga sebuah hidangan ialah RM$a. Cukai perkhidmatan 6% dikenakan. Berapakah jumlah yang perlu dibayar?", a + tax, "6% × RM$a = RM$tax. Jumlah = RM$a + RM$tax = RM${a + tax}.", prefix = "RM") }
        }
    },
    G("Laju") { d ->
        val s = rnd(4, 20) * 5; val t = rnd(2, 6)
        when (d) {
            0 -> num("Sebuah kereta bergerak sejauh ${s * t} km dalam $t jam. Berapakah lajunya?", s, "Laju = jarak ÷ masa = ${s * t} ÷ $t = $s km/j.", unit = " km/j")
            1 -> num("Sebuah bas bergerak dengan laju $s km/j selama $t jam. Berapakah jarak yang dilalui?", s * t, "Jarak = laju × masa = $s × $t = ${s * t} km.", unit = " km")
            else -> num("Jarak antara dua bandar ialah ${s * t} km. Sebuah kereta bergerak dengan laju $s km/j. Berapa jamkah masa yang diambil?", t, "Masa = jarak ÷ laju = ${s * t} ÷ $s = $t jam.", unit = " jam")
        }
    },
    G("Nisbah dan Kadaran") { d ->
        val a = rnd(1, 5); val b = rnd(a + 1, 8)
        when (d) {
            0 -> { val k = rnd(2, 9)
                num("Nisbah bilangan murid lelaki kepada murid perempuan ialah $a : $b. Jika terdapat ${a * k} murid lelaki, berapakah bilangan murid perempuan?", b * k,
                    "$a bahagian = ${a * k}, jadi 1 bahagian = $k. $b bahagian = ${b * k}.", unit = " murid") }
            1 -> { val k = rnd(2, 12); val tot = (a + b) * k
                num("RM$tot dikongsi antara Ali dan Abu dalam nisbah $a : $b. Berapakah bahagian Abu?", b * k,
                    "Jumlah bahagian = ${a + b}. 1 bahagian = RM$k. Bahagian Abu = $b × $k = RM${b * k}.", prefix = "RM") }
            else -> { val c = rnd(1, 6); val k = rnd(2, 9); val tot = (a + b + c) * k
                num("Jumlah wang Aina, Bala dan Chen ialah RM$tot dalam nisbah $a : $b : $c. Berapakah wang Bala?", b * k,
                    "Jumlah bahagian = ${a + b + c}. 1 bahagian = RM$k. Wang Bala = $b × $k = RM${b * k}.", prefix = "RM") }
        }
    },
    G("Kebarangkalian") { _ ->
        val r1 = rnd(1, 6); val b1 = rnd(1, 6); val tot = r1 + b1
        mc("Sebuah beg mengandungi $r1 biji guli merah dan $b1 biji guli biru. Sebiji guli dipilih secara rawak. Berapakah kebarangkalian guli itu merah?", "$r1/$tot",
            listOf("$b1/$tot", "$r1/$b1", "$tot/$r1", "${r1 + 1}/$tot"),
            "Guli merah = $r1, jumlah guli = $tot. Kebarangkalian = $r1/$tot.")
    },
    G("Isi Padu Cecair") { _ ->
        var l = 10; var w = 10; var h = 10
        do {
            l = pick(listOf(10, 20, 25, 40, 50)); w = pick(listOf(10, 20, 25, 40, 50)); h = pick(listOf(10, 20, 25, 40, 50))
        } while ((l * w * h) % 1000 != 0)
        num("Sebuah tangki berbentuk kuboid berukuran $l cm × $w cm × $h cm. Berapa literkah isi padu maksimum tangki itu? (1 liter = 1 000 cm³)", l * w * h / 1000,
            "Isi padu = $l × $w × $h = ${l * w * h} cm³ = ${l * w * h / 1000} liter.", unit = " liter")
    }
)

// ===================== KBAT =====================
private fun k1(): List<G> = listOf(
    G("KBAT: Cerita Nombor") { _ ->
        val a = rnd(10, 30); val b = rnd(1, a - 1); val c = rnd(1, 15)
        num("Ali ada $a biji guli. Dia memberi $b biji kepada adik, kemudian mendapat $c biji lagi daripada kakak. Berapakah guli Ali sekarang?", a - b + c,
            "$a − $b = ${a - b}. Kemudian ${a - b} + $c = ${a - b + c}.")
    },
    G("KBAT: Nombor Hilang") { _ ->
        val a = rnd(5, 40); val c = rnd(a + 1, a + 40)
        num("Apakah nombor yang sesuai dalam petak kosong?   ☐ + $a = $c", c - a, "$c − $a = ${c - a}, kerana ${c - a} + $a = $c.")
    },
    G("KBAT: Banding Bilangan") { _ ->
        val a = rnd(5, 20); val b = rnd(2, 10); val c = rnd(1, a + b - 1)
        num("Siti ada $a biji gula-gula. Ben ada $b biji lebih banyak daripada Siti. Cik ada $c biji kurang daripada Ben. Berapakah gula-gula Cik?", a + b - c,
            "Ben = $a + $b = ${a + b}. Cik = ${a + b} − $c = ${a + b - c}.")
    }
)

private fun k2(): List<G> = listOf(
    G("KBAT: Beli Barang") { _ ->
        val x = rnd(2, 9); val k = rnd(2, 4); val tot = x * k
        val pay = if (tot <= 10) 10 else if (tot <= 20) 20 else 50
        num("Sebuah buku berharga RM$x. Aina membeli $k buah buku dan membayar dengan not RM$pay. Berapakah bakinya?", pay - tot,
            "Jumlah harga = $k × RM$x = RM$tot. Baki = RM$pay − RM$tot = RM${pay - tot}.", prefix = "RM")
    },
    G("KBAT: Bilangan Meja") { _ ->
        val n = rnd(3, 6); val tables = rnd(4, 9); val extra = rnd(1, n - 1); val a = n * tables + extra
        num("$a orang murid hendak duduk. Setiap meja boleh memuatkan $n orang. Berapakah bilangan meja minimum yang diperlukan?", tables + 1,
            "$a ÷ $n = $tables baki $extra. Murid yang tinggal perlukan satu meja lagi, jadi ${tables + 1} meja.", unit = " meja")
    },
    G("KBAT: Baki Buah") { _ ->
        val boxes = rnd(3, 6); val n = pick(listOf(2, 3, 4, 5, 10)); val total = boxes * n; val eaten = rnd(1, total - 1)
        num("Terdapat $boxes kotak epal. Setiap kotak ada $n biji epal. Sebanyak $eaten biji epal dimakan. Berapakah epal yang tinggal?", total - eaten,
            "Jumlah = $boxes × $n = $total. Baki = $total − $eaten = ${total - eaten}.", unit = " biji")
    }
)

private fun k3(): List<G> = listOf(
    G("KBAT: Wang Dua Barang") { _ ->
        val p = rnd(1, 5); val a = rnd(2, 5); val q = rnd(3, 9); val b = rnd(2, 4)
        val tot = a * p + b * q; val pay = (tot / 10 + 1) * 10
        num("Sebatang pen berharga RM$p dan sebuah buku berharga RM$q. Zara membeli $a batang pen dan $b buah buku. Dia membayar RM$pay. Berapakah bakinya?", pay - tot,
            "Pen = $a × RM$p = RM${a * p}. Buku = $b × RM$q = RM${b * q}. Jumlah = RM$tot. Baki = RM$pay − RM$tot = RM${pay - tot}.", prefix = "RM")
    },
    G("KBAT: Umur") { _ ->
        val n = rnd(5, 12); val mult = rnd(3, 4); val plus = rnd(2, 6)
        num("Umur Aina ialah $n tahun. Umur ibunya ialah $mult kali umur Aina. Ayahnya $plus tahun lebih tua daripada ibunya. Berapakah umur ayah?", n * mult + plus,
            "Ibu = $mult × $n = ${n * mult}. Ayah = ${n * mult} + $plus = ${n * mult + plus} tahun.", unit = " tahun")
    },
    G("KBAT: Pecahan Kek") { _ ->
        val den = rnd(6, 12); val a = rnd(1, 3); val b = rnd(1, 3)
        mc("Sebiji kek dibahagi kepada $den bahagian sama. Ali makan $a bahagian dan Siti makan $b bahagian. Apakah pecahan kek yang tinggal?", "${den - a - b}/$den",
            listOf("${a + b}/$den", "${den - a}/$den", "${den - b}/$den", "${den - a - b}/${den - 1}"),
            "Dimakan = $a + $b = ${a + b} bahagian. Tinggal = $den − ${a + b} = ${den - a - b}, iaitu ${den - a - b}/$den.")
    }
)

private fun k4(): List<G> = listOf(
    G("KBAT: Kos Pagar") { _ ->
        val l = rnd(10, 40); val w = rnd(5, l - 1); val c = rnd(2, 9)
        num("Sebuah taman berbentuk segi empat tepat berukuran $l m panjang dan $w m lebar. Pagar dipasang di sekelilingnya dengan kos RM$c semeter. Berapakah jumlah kos memagar taman itu?", 2 * (l + w) * c,
            "Perimeter = 2 × ($l + $w) = ${2 * (l + w)} m. Kos = ${2 * (l + w)} × RM$c = RM${2 * (l + w) * c}.", prefix = "RM")
    },
    G("KBAT: Peratus Kelas") { _ ->
        val n = rnd(1, 10) * 20; val p = pick(listOf(20, 25, 40, 50, 60, 75)); val boys = n * p / 100
        num("Sebuah kelas mempunyai $n orang murid. $p% daripadanya ialah murid lelaki. Berapakah bilangan murid perempuan?", n - boys,
            "Lelaki = $p% × $n = $boys. Perempuan = $n − $boys = ${n - boys}.", unit = " orang")
    },
    G("KBAT: Dua Nombor") { _ ->
        val a = rnd(20, 90); val b = rnd(5, a - 5); val s = a + b; val df = a - b
        num("Jumlah dua nombor ialah $s dan beza antara keduanya ialah $df. Apakah nombor yang lebih besar?", a,
            "Nombor besar = ($s + $df) ÷ 2 = ${s + df} ÷ 2 = $a.")
    }
)

private fun k5(): List<G> = listOf(
    G("KBAT: Harga Diskaun") { _ ->
        val p = rnd(1, 10) * 20; val disc = pick(listOf(10, 20, 25, 50)); val k = rnd(2, 5)
        val after = p - p * disc / 100
        num("Harga asal sehelai kemeja ialah RM$p. Kedai memberi diskaun $disc%. Ahmad membeli $k helai kemeja itu. Berapakah jumlah yang perlu dibayar?", after * k,
            "Diskaun = RM${p * disc / 100}. Harga sehelai = RM$after. Jumlah = $k × RM$after = RM${after * k}.", prefix = "RM")
    },
    G("KBAT: Purata Markah") { _ ->
        var marks = listOf(0, 0, 0, 0)
        do {
            marks = List(4) { rnd(40, 100) }
        } while (marks.sum() % 4 != 0)
        val m = marks.sum() / 4
        num("Purata markah empat ujian Dina ialah $m. Markah tiga ujian pertama ialah ${marks[0]}, ${marks[1]} dan ${marks[2]}. Berapakah markah ujian keempat?", marks[3],
            "Jumlah markah = 4 × $m = ${4 * m}. Markah keempat = ${4 * m} − (${marks[0]} + ${marks[1]} + ${marks[2]}) = ${marks[3]}.")
    },
    G("KBAT: Tinggi Air") { _ ->
        val p = pick(listOf(intArrayOf(20, 50, 1000), intArrayOf(40, 25, 1000), intArrayOf(20, 25, 500), intArrayOf(10, 50, 500)))
        val k = rnd(2, 20); val h = 1000 * k / (p[0] * p[1])
        num("Sebuah tangki kosong berdasar ${p[0]} cm × ${p[1]} cm. Sebanyak $k liter air dituang ke dalamnya. Berapakah tinggi air dalam tangki itu? (1 liter = 1 000 cm³)", h,
            "Isi padu = ${k * 1000} cm³. Tinggi = ${k * 1000} ÷ (${p[0]} × ${p[1]}) = $h cm.", unit = " cm")
    }
)

private fun k6(): List<G> = listOf(
    G("KBAT: Jarak Perjalanan") { _ ->
        val s1 = rnd(4, 10) * 10; val t1 = rnd(1, 4); val s2 = rnd(4, 10) * 10; val t2 = rnd(1, 4)
        num("Sebuah kereta bergerak dengan laju $s1 km/j selama $t1 jam, kemudian dengan laju $s2 km/j selama $t2 jam. Berapakah jumlah jarak perjalanan itu?", s1 * t1 + s2 * t2,
            "Jarak pertama = $s1 × $t1 = ${s1 * t1} km. Jarak kedua = $s2 × $t2 = ${s2 * t2} km. Jumlah = ${s1 * t1 + s2 * t2} km.", unit = " km")
    },
    G("KBAT: Beza Nisbah") { _ ->
        val a = rnd(2, 7); val b = rnd(1, a - 1); val k = rnd(3, 12); val tot = (a + b) * k
        num("Nisbah umur ayah kepada umur anaknya ialah $a : $b. Jumlah umur mereka ialah $tot tahun. Berapakah beza umur mereka?", (a - b) * k,
            "Jumlah bahagian = ${a + b}. 1 bahagian = $k tahun. Beza = ($a − $b) × $k = ${(a - b) * k} tahun.", unit = " tahun")
    },
    G("KBAT: Belanjawan Gaji") { _ ->
        val g = rnd(20, 60) * 100; val p = pick(listOf(10, 20, 25, 30)); val q = pick(listOf(10, 15, 20, 25))
        num("Gaji Encik Lim ialah RM$g sebulan. Sebanyak $p% digunakan untuk sewa rumah dan $q% untuk makanan. Berapakah baki gajinya?", g / 100 * (100 - p - q),
            "Peratus digunakan = $p% + $q% = ${p + q}%. Baki = ${100 - p - q}% × RM$g = RM${g / 100 * (100 - p - q)}.", prefix = "RM")
    }
)
