package com.purrfolio.mco1

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Profile document users/{uid} in Cloud Firestore.
 *
 * Firebase Storage needs the paid Blaze plan, so the profile photo is stored in the document itself
 * as a small Base64 JPEG (field "photo"); a Firestore document can hold up to 1 MiB.
 */
class UserRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    private fun doc(uid: String) = db.collection("users").document(uid)

    suspend fun create(uid: String, fullName: String, email: String) {
        doc(uid).set(mapOf(
            "fullName" to fullName.trim(),
            "email" to email.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "notifications" to mapOf(
                "careReminders" to true, "appUpdates" to true, "tips" to true,
                "careCircle" to true, "productNews" to false
            )
        )).await()
    }

    /** Returns an empty map when the document does not exist yet (e.g. first social sign-in). */
    suspend fun load(uid: String): Map<String, Any?> = doc(uid).get().await().data ?: emptyMap()

    /** Merges only the given fields; dotted keys such as "notifications.tips" update one entry of a map. */
    suspend fun update(uid: String, fields: Map<String, Any?>) {
        doc(uid).set(fields, SetOptions.merge()).await()
    }

    /** Shrinks the picked image, saves it to the profile and returns the Base64 string. */
    suspend fun savePhoto(uid: String, resolver: ContentResolver, uri: Uri): String {
        val photo = withContext(Dispatchers.IO) { encodePhoto(resolver, uri) }
        update(uid, mapOf("photo" to photo))
        return photo
    }

    suspend fun deleteAll(uid: String) {
        doc(uid).delete().await()
    }

    companion object {
        private const val MAX_SIDE = 512
        private const val MAX_BYTES = 700 * 1024 // leaves room for the other fields under the 1 MiB limit

        /** Downscales to at most 512 px, fixes camera rotation and JPEG-encodes until it fits. */
        fun encodePhoto(resolver: ContentResolver, uri: Uri): String {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val decoded = resolver.openInputStream(uri)!!.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: throw IllegalArgumentException("That file isn't an image.")
            val bitmap = scaleAndRotate(decoded, rotation(resolver, uri))
            var quality = 80
            while (true) {
                val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
                val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                if (encoded.length <= MAX_BYTES || quality <= 30) return encoded
                quality -= 10
            }
        }

        fun decodePhoto(photo: String): Bitmap? = runCatching {
            val bytes = Base64.decode(photo, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()

        private fun rotation(resolver: ContentResolver, uri: Uri): Int = runCatching {
            resolver.openInputStream(uri)!!.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        }.getOrDefault(0)

        private fun scaleAndRotate(source: Bitmap, degrees: Int): Bitmap {
            val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(source.width, source.height))
            if (scale == 1f && degrees == 0) return source
            val matrix = Matrix().apply { postScale(scale, scale); postRotate(degrees.toFloat()) }
            return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }
    }
}
