package com.abaye.derechkohanim.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.abaye.derechkohanim.ui.theme.DkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Loads profile pictures (Google account photos on lh3.googleusercontent.com) without an image
 * library: OkHttp fetches the bytes, BitmapFactory decodes them downsampled to the size shown,
 * and the result is cached in memory and as the raw file in cacheDir, keyed by a hash of the URL.
 *
 * A plain client on purpose: the DI client carries the AuthInterceptor, which would send the
 * user's bearer token to Google. Any failure (NetFree blocking the host, no network, a bad
 * image) yields null and the caller keeps its fallback.
 */
object AvatarLoader {
    private val memory = LruCache<String, Bitmap>(16)

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    suspend fun load(context: Context, url: String, sizePx: Int): Bitmap? {
        val key = "$url@$sizePx"
        memory.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val file = File(File(context.cacheDir, "avatars").apply { mkdirs() }, sha1(url))
                if (!file.exists() || file.length() == 0L) {
                    client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                        if (!response.isSuccessful) return@runCatching null
                        val bytes = response.body?.bytes() ?: return@runCatching null
                        file.writeBytes(bytes)
                    }
                }
                decodeSampled(file, sizePx)?.also { memory.put(key, it) }
            }.getOrNull()
        }
    }

    private fun decodeSampled(file: File, sizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            file.delete()
            return null
        }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= sizePx && bounds.outHeight / (sample * 2) >= sizePx) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}

/**
 * The signed-in user as a circle: the profile picture when there is one and it loads, otherwise
 * the initials of the name (or email), otherwise a person icon. The fallback shows while the
 * picture loads, so nothing jumps.
 */
@Composable
fun UserAvatar(
    avatarUrl: String?,
    name: String?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    var bitmap by remember(avatarUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(avatarUrl, sizePx) {
        bitmap = avatarUrl?.takeIf { it.isNotBlank() }?.let { AvatarLoader.load(context, it, sizePx) }
    }
    val colors = DkTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.primarySoft),
    ) {
        val image = bitmap
        val initials = initialsOf(name)
        when {
            image != null -> Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
            initials != null -> Text(
                text = initials,
                color = colors.primary,
                fontWeight = FontWeight.Medium,
                fontSize = (size.value * 0.4f).sp,
            )
            else -> Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(size * 0.6f),
            )
        }
    }
}

/** Up to two initials from a display name or an email's local part; null when there is none. */
fun initialsOf(name: String?): String? {
    val source = name?.trim()?.substringBefore('@')?.takeIf { it.isNotBlank() } ?: return null
    val parts = source.split(' ', '.', '_', '-').filter { it.isNotBlank() }
    return parts.take(2).joinToString("") { it.first().uppercase() }.takeIf { it.isNotBlank() }
}
