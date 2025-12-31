package com.mviiiax.m8ax_diariogimnasio

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

fun copiarDBADownloads(context: Context, nombreDB: String, db: AppDatabase) {
    try {
        val dbFile = context.getDatabasePath(nombreDB)
        if (!dbFile.exists()) {
            Toast.makeText(context, "Base De Datos No Encontrada", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            db.close()
        } catch (_: Exception) {
        }
        val timestamp = System.currentTimeMillis()
        val nombreDestino = "M8AX-Gimnasio_DB-$timestamp.db"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, nombreDestino)
                put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = context.contentResolver.insert(collection, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    dbFile.inputStream().use { input -> input.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                Toast.makeText(
                    context, "Base De Datos Copiada A Downloads. Reiniciando", Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destinoFile = File(downloadsDir, nombreDestino)
            dbFile.inputStream().use { input ->
                FileOutputStream(destinoFile).use { output -> input.copyTo(output) }
            }
            Toast.makeText(
                context, "Base De Datos Copiada A Downloads. Reiniciando", Toast.LENGTH_SHORT
            ).show()
        }
        reiniciarAplicacion(context)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error Al Copiar Base De Datos: ${e.message}", Toast.LENGTH_LONG)
            .show()
    }
}

private fun reiniciarAplicacion(context: Context) {
    Handler(Looper.getMainLooper()).postDelayed({
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }, 7000L)
}