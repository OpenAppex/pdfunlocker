package io.openappex.pdfunlocker.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import io.openappex.pdfunlocker.data.settings.AppSettings
import io.openappex.pdfunlocker.data.settings.DefaultOutputFolderLabel
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import java.io.IOException
import java.io.OutputStream

class PdfUnlockRepository(
    private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver

    suspend fun unlockPdf(
        sourceUri: Uri,
        sourceName: String,
        password: String,
        settings: AppSettings
    ): PdfUnlockResult {
        val inputFile = copySourceToCache(sourceUri)
            ?: return PdfUnlockResult.Error("PDF cannot be opened.")

        return try {
            PDFBoxResourceLoader.init(context)

            PDDocument.load(inputFile, password).use { document ->
                document.isAllSecurityToBeRemoved = true
                saveUnlockedDocument(
                    document = document,
                    outputName = unlockedFileName(sourceName, settings.outputFilenameSuffix),
                    settings = settings
                )
            }
        } catch (exception: IOException) {
            if (exception.isPasswordFailure()) {
                PdfUnlockResult.InvalidPassword
            } else {
                PdfUnlockResult.Error("PDF cannot be opened.")
            }
        } catch (exception: SecurityException) {
            PdfUnlockResult.Error("PDF cannot be opened.")
        } finally {
            inputFile.delete()
        }
    }

    private fun copySourceToCache(sourceUri: Uri): File? {
        val inputFile = File.createTempFile("pdf_unlocker_input", ".pdf", context.cacheDir)
        return try {
            contentResolver.openInputStream(sourceUri)?.use { input ->
                inputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            inputFile
        } catch (exception: IOException) {
            inputFile.delete()
            null
        } catch (exception: SecurityException) {
            inputFile.delete()
            null
        }
    }

    private fun saveUnlockedDocument(
        document: PDDocument,
        outputName: String,
        settings: AppSettings
    ): PdfUnlockResult {
        val folderUri = settings.outputFolderUri
        if (folderUri != null) {
            return saveWithTreeUri(
                document = document,
                outputName = outputName,
                folderUri = Uri.parse(folderUri),
                folderLabel = settings.outputFolderLabel
            )
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(document, outputName)
        } else {
            saveWithPublicDownloadsFile(document, outputName)
        }
    }

    private fun saveWithMediaStore(
        document: PDDocument,
        outputName: String
    ): PdfUnlockResult {
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/PdfUnlocker"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, outputName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val outputUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return PdfUnlockResult.Error("Could not create the output PDF.")

        return try {
            contentResolver.openOutputStream(outputUri)?.use { output ->
                document.save(output)
            } ?: return PdfUnlockResult.Error("Could not write the output PDF.")

            val completedValues = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            contentResolver.update(outputUri, completedValues, null, null)
            PdfUnlockResult.Success("$DefaultOutputFolderLabel/$outputName")
        } catch (exception: IOException) {
            contentResolver.delete(outputUri, null, null)
            PdfUnlockResult.Error("Could not write the output PDF.")
        } catch (exception: SecurityException) {
            contentResolver.delete(outputUri, null, null)
            PdfUnlockResult.Error("Could not write the output PDF.")
        }
    }

    private fun saveWithTreeUri(
        document: PDDocument,
        outputName: String,
        folderUri: Uri,
        folderLabel: String
    ): PdfUnlockResult {
        val documentUri = try {
            contentResolver.takePersistableUriPermission(
                folderUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            DocumentsContract.createDocument(
                contentResolver,
                folderUri,
                "application/pdf",
                outputName
            ) ?: return PdfUnlockResult.Error("Could not create the output PDF.")
        } catch (exception: SecurityException) {
            return PdfUnlockResult.Error("Could not access the selected output folder.")
        } catch (exception: IllegalArgumentException) {
            return PdfUnlockResult.Error("Could not access the selected output folder.")
        }

        return try {
            contentResolver.openOutputStream(documentUri)?.use { output ->
                document.save(output)
            } ?: return PdfUnlockResult.Error("Could not write the output PDF.")

            PdfUnlockResult.Success("$folderLabel/$outputName")
        } catch (exception: IOException) {
            contentResolver.delete(documentUri, null, null)
            PdfUnlockResult.Error("Could not write the output PDF.")
        } catch (exception: SecurityException) {
            contentResolver.delete(documentUri, null, null)
            PdfUnlockResult.Error("Could not write the output PDF.")
        }
    }

    private fun saveWithPublicDownloadsFile(
        document: PDDocument,
        outputName: String
    ): PdfUnlockResult {
        val outputDirectory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "PdfUnlocker"
        )
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            return PdfUnlockResult.Error("Could not create the output folder.")
        }

        val outputFile = File(outputDirectory, outputName)
        return try {
            outputFile.outputStream().use { output: OutputStream ->
                document.save(output)
            }
            PdfUnlockResult.Success(outputFile.absolutePath)
        } catch (exception: IOException) {
            PdfUnlockResult.Error("Could not write the output PDF.")
        } catch (exception: SecurityException) {
            PdfUnlockResult.Error("Could not write the output PDF.")
        }
    }

    private fun unlockedFileName(sourceName: String, suffix: String): String {
        val cleanName = sourceName.substringAfterLast('/').ifBlank { "document.pdf" }
        val cleanSuffix = suffix.ifBlank { "_unlocked" }
        return if (cleanName.endsWith(".pdf", ignoreCase = true)) {
            cleanName.dropLast(4) + cleanSuffix + ".pdf"
        } else {
            cleanName + cleanSuffix + ".pdf"
        }
    }

    private fun IOException.isPasswordFailure(): Boolean {
        val text = listOfNotNull(message, cause?.message)
            .joinToString(" ")
            .lowercase()

        return "password" in text ||
            "decrypt" in text ||
            "encrypted" in text ||
            "cannot be decrypted" in text
    }
}
