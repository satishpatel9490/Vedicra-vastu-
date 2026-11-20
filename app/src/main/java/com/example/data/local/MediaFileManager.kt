package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MediaFileManager(private val context: Context) {

    private val appFilesRoot: File
        get() = context.filesDir

    fun getReportOriginalsDir(reportId: String): File =
        File(appFilesRoot, "reports/$reportId/originals").apply { mkdirs() }

    fun getReportAnnotatedDir(reportId: String): File =
        File(appFilesRoot, "reports/$reportId/annotated").apply { mkdirs() }

    fun getPdfsDir(): File =
        File(appFilesRoot, "pdfs").apply { mkdirs() }

    fun getBackupsDir(): File =
        File(appFilesRoot, "backups").apply { mkdirs() }

    fun getBrandDir(): File =
        File(appFilesRoot, "brand").apply { mkdirs() }

    fun createOriginalImageFile(reportId: String, prefix: String = "img"): File {
        val dir = getReportOriginalsDir(reportId)
        val fileName = "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
        return File(dir, fileName)
    }

    fun createAnnotatedImageFile(reportId: String): File {
        val dir = getReportAnnotatedDir(reportId)
        val fileName = "annotated_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.png"
        return File(dir, fileName)
    }

    suspend fun importImageFromUri(reportId: String, uri: Uri, prefix: String = "gallery"): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                val targetFile = createOriginalImageFile(reportId, prefix)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null

                // Normalize EXIF orientation into upright high-quality JPEG so thumbnails, workspace & PDF match reliably
                normalizeExifOrientationInPlace(targetFile)
                targetFile
            }.getOrNull()
        }

    suspend fun normalizeCameraImage(file: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            normalizeExifOrientationInPlace(file)
            true
        }.getOrDefault(false)
    }

    private fun normalizeExifOrientationInPlace(file: File) {
        if (!file.exists()) return
        val exif = runCatching { ExifInterface(file.absolutePath) }.getOrNull() ?: return
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val flipHorizontal = orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL ||
            orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE

        if (degrees == 0f && !flipHorizontal) return

        val options = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(file.absolutePath, 2560, 2560)
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: return
        val matrix = Matrix().apply {
            if (flipHorizontal) postScale(-1f, 1f)
            if (degrees != 0f) postRotate(degrees)
        }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated !== decoded) decoded.recycle()

        FileOutputStream(file).use { out ->
            rotated.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        rotated.recycle()

        val updatedExif = ExifInterface(file.absolutePath)
        updatedExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
        updatedExif.saveAttributes()
    }

    suspend fun saveCustomCompanyLogo(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(getBrandDir(), "company_logo_${System.currentTimeMillis()}.png")
            // Clean old logos in brand dir
            getBrandDir().listFiles()?.forEach { old ->
                if (old.name.startsWith("company_logo_")) old.delete()
            }
            val bitmap = decodeSampledBitmap(uri, 512, 512) ?: return@withContext null
            FileOutputStream(target).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            bitmap.recycle()
            target.absolutePath
        }.getOrNull()
    }

    private fun decodeSampledBitmap(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val sampleSize = calculateInSampleSize(boundsOpts, reqWidth, reqHeight)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    }

    fun loadRotatedBitmap(
        filePath: String,
        extraRotationDegrees: Int = 0,
        maxDimension: Int = 1800
    ): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null

        val exifDegrees = runCatching {
            val exif = ExifInterface(filePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }.getOrDefault(0)

        val totalRotation = ((exifDegrees + extraRotationDegrees) % 360 + 360) % 360
        val opts = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(filePath, maxDimension, maxDimension)
        }
        val bitmap = BitmapFactory.decodeFile(filePath, opts) ?: return null
        if (totalRotation == 0) return bitmap

        val matrix = Matrix().apply { postRotate(totalRotation.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun computeSampleSize(filePath: String, reqWidth: Int, reqHeight: Int): Int {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, opts)
        return calculateInSampleSize(opts, reqWidth, reqHeight)
    }

    private fun calculateInSampleSize(opts: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = opts.outHeight to opts.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun isAppManagedFile(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val rootPath = appFilesRoot.canonicalPath
        return runCatching { File(path).canonicalPath.startsWith(rootPath) }.getOrDefault(false)
    }

    fun deleteAppManagedFile(path: String?) {
        if (isAppManagedFile(path)) {
            runCatching { File(path!!).delete() }
        }
    }

    fun deleteReportMediaDirectory(reportId: String) {
        val dir = File(appFilesRoot, "reports/$reportId")
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    fun copyFileWithinReport(sourcePath: String, targetFile: File): Boolean {
        val src = File(sourcePath)
        if (!src.exists()) return false
        return runCatching {
            src.copyTo(targetFile, overwrite = true)
            true
        }.getOrDefault(false)
    }
}
