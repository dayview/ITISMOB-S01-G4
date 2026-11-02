package com.purrfolio.mco1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Keeps pet photos in the app's own private folder.
 *
 * Why copy? The Android photo picker only grants temporary access to the picked image, so saving its
 * URI would break later. A small JPEG copy always works, needs no permission, and is removed with the app.
 */
object PetPhotoStore {
    private const val FOLDER = "pets"
    private const val MAX_SIDE = 1024

    private fun folder(context: Context): File = File(context.filesDir,FOLDER).apply { mkdirs() }

    /** Copies [source] into private storage and returns a file:// URI string, or null if it couldn't be read. */
    fun import(context: Context, source: Uri, petId: String): String? = runCatching {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > MAX_SIDE * 2 || bounds.outHeight / sample > MAX_SIDE * 2) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return@runCatching null

        val orientation = resolver.openInputStream(source)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        val largest = maxOf(decoded.width, decoded.height)
        if (largest > MAX_SIDE) {
            val scale = MAX_SIDE.toFloat() / largest
            matrix.postScale(scale, scale)
        }

        val finished = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        // A new file name each time means the picture refreshes on screen and old copies can be deleted.
        val file = File(folder(context), "$petId-${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { finished.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        Uri.fromFile(file).toString()
    }.getOrNull()

    /** Deletes [photoUri] only if it is one of our own copies. Anything else is left alone. */
    fun deleteIfOurs(context: Context, photoUri: String) {
        if (photoUri.isBlank()) return
        runCatching {
            val path = Uri.parse(photoUri).path ?: return
            val file = File(path)
            val inside = file.canonicalPath.startsWith(folder(context).canonicalPath + File.separator)
            if (inside) file.delete()
        }
    }
}