package com.example.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object PdfShareHelper {

    fun getFileProviderUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun viewPdf(context: Context, pdfFile: File) {
        if (!pdfFile.exists()) {
            Toast.makeText(context, "PDF file not found.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = getFileProviderUri(context, pdfFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Open Vastu Report PDF"))
        }.onFailure {
            Toast.makeText(context, "No PDF viewer app found. You can share or save the PDF.", Toast.LENGTH_LONG).show()
        }
    }

    fun sharePdf(context: Context, pdfFile: File, clientName: String) {
        if (!pdfFile.exists()) {
            Toast.makeText(context, "PDF file not found.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = getFileProviderUri(context, pdfFile)
        val subject = if (clientName.isBlank()) {
            "Vedicra Vastu Consultancy Report"
        } else {
            "Vedicra Vastu Consultancy Report - $clientName"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "Please find attached your Vastu Inspection & Consultancy Report from VEDICRA VASTU SOLUTIONS.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Vastu Report PDF"))
    }

    fun sharePdfViaWhatsApp(context: Context, pdfFile: File, clientName: String) {
        if (!pdfFile.exists()) {
            Toast.makeText(context, "PDF file not found.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = getFileProviderUri(context, pdfFile)
        val message = buildString {
            append("Namaste")
            if (clientName.isNotBlank()) append(" $clientName")
            append(",\nPlease find attached your Vastu Inspection & Consultancy Report prepared by VEDICRA VASTU SOLUTIONS.")
        }

        val pm = context.packageManager
        val whatsAppPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
        val installedPkg = whatsAppPackages.firstOrNull { pkg ->
            runCatching {
                pm.getPackageInfo(pkg, 0)
                true
            }.getOrDefault(false)
        }

        if (installedPkg != null) {
            val waIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                setPackage(installedPkg)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching {
                context.startActivity(waIntent)
            }.onFailure {
                sharePdf(context, pdfFile, clientName)
            }
        } else {
            Toast.makeText(context, "WhatsApp not installed. Opening share sheet.", Toast.LENGTH_SHORT).show()
            sharePdf(context, pdfFile, clientName)
        }
    }

    fun shareImage(context: Context, imageFile: File, title: String = "Share Layout") {
        if (!imageFile.exists()) {
            Toast.makeText(context, "Image file not found.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = getFileProviderUri(context, imageFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
