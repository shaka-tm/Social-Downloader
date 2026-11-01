package com.socialdownloader.app

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.URLUtil
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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

private val bg = Color(0xFF090914)
private val violet = Color(0xFF9B7BFF)
private val cyan = Color(0xFF65D9F2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Home() }
    }
}

@Composable
fun Home() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var url by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Pega un enlace para comenzar") }
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(bg, Color(0xFF21143B), Color(0xFF092633)))).padding(22.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(42.dp))
            Text("✦", color = cyan, fontSize = 42.sp)
            Text("SOCIAL DOWNLOADER", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            Text("Tus videos favoritos, en un solo lugar", color = Color.LightGray, fontSize = 13.sp)
            Spacer(Modifier.height(38.dp))
            Column(Modifier.fillMaxWidth().border(1.dp, Color.White.copy(.2f), RoundedCornerShape(26.dp)).background(Color.White.copy(.08f), RoundedCornerShape(26.dp)).padding(18.dp)) {
                Text("ENLACE DEL VIDEO", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = url, onValueChange = { url = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Pega aquí el enlace...") }, singleLine = true, shape = RoundedCornerShape(15.dp))
                TextButton(onClick = { url = clipboard.getText()?.text.orEmpty() }, modifier = Modifier.align(Alignment.End)) { Text("PEGAR ENLACE", color = cyan) }
                Button(onClick = { message = download(context, url) }, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = violet), shape = RoundedCornerShape(15.dp)) {
                    Text("↓  DESCARGAR VIDEO", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text(message, color = cyan, fontSize = 12.sp)
            }
            Spacer(Modifier.height(22.dp))
            Text("✧  Descargas responsables", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("Guarda contenido que tengas permiso para descargar.", color = Color.LightGray, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text("DESCARGAR     •     HISTORIAL     •     ARCHIVOS", color = Color.LightGray, fontSize = 11.sp)
            Text("VERSION 1.0.0", color = Color.Gray, fontSize = 10.sp)
        }
    }
}

private fun download(context: Context, raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return "Primero pega un enlace."
    val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return "Enlace inválido."
    if (uri.scheme != "https" && uri.scheme != "http") return "Se requiere un enlace http o https."
    val file = uri.lastPathSegment?.lowercase().orEmpty()
    if (listOf(".mp4", ".m4v", ".webm", ".mov").none { file.endsWith(it) }) {
        return "Enlace detectado. Esta versión descarga archivos de video directos; las páginas sociales requieren integración específica."
    }
    return runCatching {
        val name = URLUtil.guessFileName(value, null, "video/mp4")
        val request = DownloadManager.Request(uri).setTitle(name).setDescription("Social Downloader")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        "Descarga iniciada. Revisa la carpeta Descargas."
    }.fold({ "Descarga iniciada. Revisa la carpeta Descargas." }, { "Error al iniciar la descarga." })
}