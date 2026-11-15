package com.chewanninja.matematikmaster

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Navy = Color(0xFF1E2A5A)
val Sky = Color(0xFF3F8CFF)
val Sun = Color(0xFFFFB300)
val Mint = Color(0xFF2EB67D)
val Coral = Color(0xFFFF6B6B)
val Grape = Color(0xFF7E57C2)
val Cream = Color(0xFFF6F8FF)
val Soft = Color(0xFFE3ECFF)

@Composable
fun MmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Sky, onPrimary = Color.White, secondary = Sun,
            background = Cream, surface = Color.White, onSurface = Navy, onBackground = Navy
        ),
        content = content
    )
}

/** Bingkai kuning apabila elemen difokus (remote Android TV / papan kekunci). */
fun Modifier.focusRing(shape: Shape = RoundedCornerShape(16.dp)): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { focused = it.hasFocus }
        .then(if (focused) Modifier.border(4.dp, Sun, shape) else Modifier)
}

@Composable
fun T(
    text: String, size: Int = 16, bold: Boolean = false, color: Color = Navy,
    modifier: Modifier = Modifier, align: TextAlign? = null
) {
    Text(
        text = text, fontSize = size.sp, color = color, modifier = modifier, textAlign = align,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Flow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}

@Composable
fun Page(title: String, onBack: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Cream), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 900.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    SmallButton("← Kembali", Navy, onBack)
                    Spacer(Modifier.width(12.dp))
                }
                T(title, 24, true)
            }
            content()
        }
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(color).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
fun BigButton(
    text: String, color: Color = Sky, modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true, onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Button(
        onClick = onClick, enabled = enabled, shape = shape,
        modifier = modifier.heightIn(min = 56.dp).focusRing(shape),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
    ) { Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun SmallButton(text: String, color: Color = Navy, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Button(
        onClick = onClick, shape = shape, modifier = Modifier.focusRing(shape),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
    ) { Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.focusRing(shape).clip(shape)
            .background(if (selected) Sky else Soft)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) { T(text, 15, true, if (selected) Color.White else Navy) }
}

@Composable
fun MenuCard(icon: String, title: String, sub: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.focusRing(shape).clip(shape).background(color)
            .clickable(onClick = onClick).padding(16.dp).heightIn(min = 110.dp),
        verticalArrangement = Arrangement.Center
    ) {
        T(icon, 34)
        T(title, 20, true, Color.White)
        T(sub, 13, false, Color.White.copy(alpha = 0.9f))
    }
}

@Composable
fun Field(label: String, value: String, password: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) }, singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun Bar(percent: Int, color: Color) {
    Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(Soft)) {
        Box(
            Modifier.fillMaxWidth(percent.coerceIn(0, 100) / 100f).fillMaxSize()
                .clip(RoundedCornerShape(6.dp)).background(color)
        )
    }
}

// ---------- Avatar ----------
@Composable
fun AvatarBox(emoji: String, bmp: androidx.compose.ui.graphics.ImageBitmap?, size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Soft), contentAlignment = Alignment.Center) {
        if (bmp != null) {
            Image(bitmap = bmp, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(emoji, fontSize = (size.value * 0.55f).sp)
        }
    }
}

@Composable
fun Avatar(p: Profile, size: Dp = 56.dp) {
    val bmp = remember(p.photoPath) {
        p.photoPath?.let { path -> runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull() }
    }
    AvatarBox(p.avatar, bmp, size)
}

@Composable
fun AvatarPreview(emoji: String?, photo: Uri?, profile: Profile?, size: Dp = 72.dp) {
    val ctx = LocalContext.current
    val bmp = remember(photo) {
        photo?.let { u ->
            runCatching {
                val o = BitmapFactory.Options().apply { inSampleSize = 4 }
                ctx.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, o) }?.asImageBitmap()
            }.getOrNull()
        }
    }
    when {
        bmp != null -> AvatarBox("", bmp, size)
        emoji != null -> AvatarBox(emoji, null, size)
        profile != null -> Avatar(profile, size)
        else -> AvatarBox("🙂", null, size)
    }
}

val EMOJIS = listOf("🦁", "🐯", "🐼", "🐰", "🦊", "🐸", "🐵", "🦄", "🐧", "🚀", "⚽", "🌈")

@Composable
fun AvatarEditor(selected: String?, onEmoji: (String) -> Unit, onPhoto: (Uri?) -> Unit) {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onPhoto(uri)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallButton("📷 Pilih gambar daripada galeri", Grape) {
            try {
                launcher.launch("image/*")
            } catch (e: Exception) {
                Toast.makeText(ctx, "Tiada aplikasi galeri. Sila pilih emoji.", Toast.LENGTH_LONG).show()
            }
        }
        T("atau pilih emoji:", 13)
        Flow { EMOJIS.forEach { e -> Chip(e, selected == e) { onEmoji(e) } } }
    }
}

/** Dialog tukar gambar profil (untuk profil sedia ada). */
@Composable
fun AvatarDialog(p: Profile, onDismiss: () -> Unit, onSave: (String?, Uri?) -> Unit) {
    var emoji by remember { mutableStateOf<String?>(null) }
    var photo by remember { mutableStateOf<Uri?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { T("Tukar Gambar Profil", 20, true) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AvatarPreview(emoji, photo, p)
                AvatarEditor(emoji, { emoji = it; photo = null }, { photo = it; emoji = null })
            }
        },
        confirmButton = { SmallButton("Simpan", Mint) { onSave(emoji, photo) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
