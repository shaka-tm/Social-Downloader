package com.socialdownloader.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.URLUtil
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.database.Cursor

private val bg = Color(0xFF090914)
private val violet = Color(0xFF9B7BFF)
private val cyan = Color(0xFF65D9F2)
private const val PREFS = "social_downloader"
private const val IDS = "download_ids"

data class DownloadItem(val id: Long, val title: String, val status: String, val localUri: String?)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SocialDownloader() }
    }
}

@Composable
fun SocialDownloader() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var url by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Pega un enlace para comenzar") }
    var tab by remember { mutableStateOf("DESCARGAR") }
    var downloads by remember { mutableStateOf(loadDownloads(context)) }

    fun refresh() { downloads = loadDownloads(context) }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.linearGradient(listOf(bg, Color(0xFF21143B), Color(0xFF092633))))
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Text("✦", color = cyan, fontSize = 42.sp)
            Text("SOCIAL DOWNLOADER", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            Text("Tus videos favoritos, en un solo lugar", color = Color.LightGray, fontSize = 13.sp)
            Spacer(Modifier.height(28.dp))

            when (tab) {
                "DESCARGAR" -> {
                    Column(
                        Modifier.fillMaxWidth()
                            .border(1.dp, Color.White.copy(.2f), RoundedCornerShape(26.dp))
                            .background(Color.White.copy(.08f), RoundedCornerShape(26.dp))
                            .padding(18.dp)
                    ) {
                        Text("ENLACE DEL VIDEO", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = url, onValueChange = { url = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Pega aquí el enlace...") },
                            singleLine = true, shape = RoundedCornerShape(15.dp)
                        )
                        TextButton(
                            onClick = { url = clipboard.getText()?.text.orEmpty() },
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("PEGAR ENLACE", color = cyan) }
                        Button(
                            onClick = {
                                val result = startDownload(context, url)
                                message = result.first
                                if (result.second) refresh()
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = violet),
                            shape = RoundedCornerShape(15.dp)
                        ) { Text("↓  DESCARGAR VIDEO", fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(12.dp))
                        Text(message, color = cyan, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(22.dp))
                    Text("✧  Descargas responsables", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Guarda contenido que tengas permiso para descargar.", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Admite enlaces directos a archivos MP4, M4V, WEBM y MOV.", color = Color.LightGray, fontSize = 11.sp)
                }
                "HISTORIAL" -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("TUS DESCARGAS", color = Color.White, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { refresh() }) { Text("ACTUALIZAR", color = cyan) }
                    }
                    if (downloads.isEmpty()) {
                        Text("Todavía no hay descargas.", color = Color.LightGray, modifier = Modifier.padding(24.dp))
                    } else {
                        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(downloads, key = { it.id }) { item ->
                                DownloadRow(item = item, onOpen = { openDownload(context, item) })
                            }
                        }
                    }
                }
                else -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("VIDEOS GUARDADOS", color = Color.White, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { refresh() }) { Text("ACTUALIZAR", color = cyan) }
                    }
                    val completed = downloads.filter { it.status == "Completada" }
                    if (completed.isEmpty()) {
                        Text("Aquí aparecerán los videos descargados.", color = Color.LightGray, modifier = Modifier.padding(24.dp))
                    } else {
                        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(completed, key = { it.id }) { item ->
                                DownloadRow(item = item, onOpen = { openDownload(context, item) })
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(.16f), RoundedCornerShape(18.dp)).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("DESCARGAR", "HISTORIAL", "ARCHIVOS").forEach { label ->
                    Text(
                        label,
                        color = if (tab == label) cyan else Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = if (tab == label) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.clickable { tab = label; refresh() }.padding(8.dp)
                    )
                }
            }
            Text("VERSION 1.1.0", color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun DownloadRow(item: DownloadItem, onOpen: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().border(1.dp, Color.White.copy(.14f), RoundedCornerShape(16.dp))
            .background(Color.White.copy(.07f), RoundedCornerShape(16.dp))
            .clickable(enabled = item.status == "Completada") { onOpen() }.padding(14.dp)
    ) {
        Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 2)
        Spacer(Modifier.height(5.dp))
        Text(item.status, color = cyan, fontSize = 12.sp)
        if (item.status == "Completada") Text("Toca para reproducir", color = Color.LightGray, fontSize = 11.sp)
    }
}

private fun loadDownloads(context: Context): List<DownloadItem> {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val ids = prefs.getString(IDS, "").orEmpty().split(",").mapNotNull { it.toLongOrNull() }
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    return ids.reversed().mapNotNull { id ->
        var cursor: Cursor? = null
        try {
            cursor = manager.query(DownloadManager.Query().setFilterById(id)) ?: return@mapNotNull null
            if (!cursor.moveToFirst()) return@mapNotNull null
            val title = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)) ?: "Video"
            val statusCode = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val status = when (statusCode) {
                DownloadManager.STATUS_SUCCESSFUL -> "Completada"
                DownloadManager.STATUS_FAILED -> "Fallida"
                DownloadManager.STATUS_RUNNING -> "Descargando"
                DownloadManager.STATUS_PENDING, DownloadManager.STATUS_PAUSED -> "En espera"
                else -> "Procesando"
            }
            val localIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val local = if (localIndex >= 0) cursor.getString(localIndex) else null
            DownloadItem(id, title, status, local)
        } catch (_: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }
}

private fun startDownload(context: Context, raw: String): Pair<String, Boolean> {
    val value = raw.trim()
    if (value.isBlank()) return "Primero pega un enlace." to false
    val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return "Enlace inválido." to false
    if ((uri.scheme != "https" && uri.scheme != "http") || uri.host.isNullOrBlank()) {
        return "Se requiere un enlace web válido (https)." to false
    }
    val path = uri.path.orEmpty().lowercase()
    if (listOf(".mp4", ".m4v", ".webm", ".mov").none { path.endsWith(it) }) {
        val host = uri.host.orEmpty()
        val platform = when {
            host.contains("tiktok") -> "TikTok"
            host.contains("instagram") -> "Instagram"
            host.contains("facebook") || host.contains("fb.watch") -> "Facebook"
            host.contains("youtube") || host.contains("youtu.be") -> "YouTube"
            host.contains("x.com") || host.contains("twitter") -> "X"
            else -> "esta página"
        }
        return "El enlace es de $platform. Esta versión necesita un método compatible con esa plataforma; prueba con el enlace directo al archivo de video." to false
    }
    return try {
        val name = URLUtil.guessFileName(value, null, "video/mp4")
        val request = DownloadManager.Request(uri)
            .setTitle(name)
            .setDescription("Social Downloader")
            .setMimeType("video/*")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = manager.enqueue(request)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = prefs.getString(IDS, "").orEmpty().split(",").filter { it.isNotBlank() }
        prefs.edit().putString(IDS, (old + id.toString()).distinct().joinToString(",")).apply()
        "Descarga iniciada. Puedes ver el progreso en Historial." to true
    } catch (_: Exception) {
        "No se pudo iniciar. Comprueba tu conexión y el enlace." to false
    }
}

private fun openDownload(context: Context, item: DownloadItem) {
    try {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = manager.getUriForDownloadedFile(item.id) ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Android will show an error if no compatible video player is installed.
    }
}
