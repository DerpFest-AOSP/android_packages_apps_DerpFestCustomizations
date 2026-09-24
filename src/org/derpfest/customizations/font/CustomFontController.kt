/*
 * Copyright (C) 2026 The uwuAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.derpfest.customizations.font

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFileUtil
import android.graphics.fonts.FontManager
import android.graphics.fonts.FontStyle
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.channels.FileChannel
import java.util.Locale
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

internal class CustomFontController(private val context: Context) {
    private val fontManager = context.getSystemService(FontManager::class.java)
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val importDirectory = File(context.filesDir, IMPORT_DIRECTORY)

    fun activeFontName(): String? {
        val postScriptName = fontManager.getCustomFontName() ?: return null
        return preferences.getString(KEY_DISPLAY_NAME, null) ?: postScriptName
    }

    fun preparedFonts(): List<FontCandidate> {
        return importDirectory
            .listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isFile && isSupportedFontName(it.name) }
            .mapNotNull(::inspectFont)
            .distinctBy { it.postScriptName }
            .sortedBy { it.displayName.lowercase(Locale.ROOT) }
            .toList()
    }

    fun prepare(uri: Uri): FontImportResult {
        val sourceName = displayName(uri)
        val createdFiles = mutableListOf<File>()
        return try {
            ensureImportDirectory()
            val existingFiles = storedFontFiles()
            val existingBytes = existingFiles.sumOf(File::length)
            context.contentResolver.openInputStream(uri)?.use { rawInput ->
                val input = BufferedInputStream(rawInput)
                if (isZip(input, sourceName)) {
                    extractArchive(
                        input,
                        createdFiles,
                        existingFiles.size,
                        existingBytes,
                    )
                } else {
                    val extension =
                        fontExtension(input, sourceName) ?: return FontImportResult.UnsupportedFile
                    if (existingFiles.size >= MAX_FONT_FILES) throw ImportLimitException()
                    val destination = uniqueDestination(sourceName, extension)
                    createdFiles += destination
                    val copied = copyLimited(input, destination, MAX_FONT_BYTES)
                    if (existingBytes + copied > MAX_TOTAL_FONT_BYTES) {
                        throw ImportLimitException()
                    }
                }
            } ?: return FontImportResult.Failed

            val importedFonts = createdFiles.mapNotNull(::inspectFont)
            val validFiles = importedFonts.mapTo(mutableSetOf()) { it.file }
            createdFiles.filterNot(validFiles::contains).forEach(File::delete)
            if (importedFonts.isEmpty()) return FontImportResult.NoFonts
            val fonts = preparedFonts()
            FontImportResult.Success(fonts)
        } catch (_: ImportLimitException) {
            discardFiles(createdFiles)
            FontImportResult.TooLarge
        } catch (_: ZipException) {
            discardFiles(createdFiles)
            FontImportResult.InvalidArchive
        } catch (_: IOException) {
            discardFiles(createdFiles)
            FontImportResult.Failed
        }
    }

    fun install(candidate: FontCandidate): FontOperationResult {
        if (!isPreparedFont(candidate.file)) return FontOperationResult.InvalidFile
        val result =
            ParcelFileDescriptor.open(candidate.file, ParcelFileDescriptor.MODE_READ_ONLY).use {
                fontManager.installCustomFont(it)
            }
        if (result != FontManager.RESULT_SUCCESS) return FontOperationResult.Failed(result)

        preferences.edit().putString(KEY_DISPLAY_NAME, candidate.displayName).apply()
        return FontOperationResult.Success(candidate.displayName)
    }

    fun restoreDefault(): Int {
        val result = fontManager.clearCustomFont()
        if (result == FontManager.RESULT_SUCCESS) {
            preferences.edit().remove(KEY_DISPLAY_NAME).apply()
            discardImportedFonts()
        }
        return result
    }

    private fun extractArchive(
        input: InputStream,
        createdFiles: MutableList<File>,
        existingCount: Int,
        existingBytes: Long,
    ) {
        ZipInputStream(input).use { zip ->
            var entryCount = 0
            var fontCount = 0
            var totalBytes = existingBytes
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                if (entryCount > MAX_ARCHIVE_ENTRIES) throw ImportLimitException()
                if (!entry.isDirectory && isSupportedFontName(entry.name)) {
                    fontCount++
                    if (existingCount + fontCount > MAX_FONT_FILES) throw ImportLimitException()
                    val destination = uniqueDestination(File(entry.name).name)
                    createdFiles += destination
                    totalBytes += copyLimited(zip, destination, MAX_FONT_BYTES)
                    if (totalBytes > MAX_TOTAL_FONT_BYTES) throw ImportLimitException()
                }
                zip.closeEntry()
            }
        }
    }

    private fun copyLimited(input: InputStream, destination: File, byteLimit: Long): Long {
        var copied = 0L
        val buffer = ByteArray(COPY_BUFFER_SIZE)
        FileOutputStream(destination).use { output ->
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                copied += count
                if (copied > byteLimit) throw ImportLimitException()
                output.write(buffer, 0, count)
            }
        }
        return copied
    }

    private fun inspectFont(file: File): FontCandidate? {
        return runCatching {
                val frameworkFont = Font.Builder(file).build()
                val postScriptName =
                    FileInputStream(file).channel.use { channel ->
                        val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
                        FontFileUtil.getPostScriptName(buffer, 0)
                    } ?: return null
                FontCandidate(
                    file = file,
                    displayName = postScriptName.replace('-', ' '),
                    postScriptName = postScriptName,
                    sourceName = file.name,
                    weight = frameworkFont.style.weight,
                    italic = frameworkFont.style.slant == FontStyle.FONT_SLANT_ITALIC,
                    typeface = Typeface.Builder(file).build(),
                )
            }
            .getOrNull()
    }

    private fun isPreparedFont(file: File): Boolean {
        return runCatching {
                file.isFile &&
                    file.canonicalPath.startsWith(importDirectory.canonicalPath + File.separator) &&
                    inspectFont(file) != null
            }
            .getOrDefault(false)
    }

    private fun isZip(input: BufferedInputStream, sourceName: String): Boolean {
        if (sourceName.endsWith(".zip", ignoreCase = true)) return true
        input.mark(ZIP_SIGNATURE_SIZE)
        val signature = ByteArray(ZIP_SIGNATURE_SIZE)
        val count = input.read(signature)
        input.reset()
        if (
            count != ZIP_SIGNATURE_SIZE ||
                signature[0] != 'P'.code.toByte() ||
                signature[1] != 'K'.code.toByte()
        ) {
            return false
        }
        return (signature[2] == 3.toByte() && signature[3] == 4.toByte()) ||
            (signature[2] == 5.toByte() && signature[3] == 6.toByte()) ||
            (signature[2] == 7.toByte() && signature[3] == 8.toByte())
    }

    private fun uniqueDestination(sourceName: String): File {
        val extension = if (sourceName.endsWith(".otf", ignoreCase = true)) "otf" else "ttf"
        return uniqueDestination(sourceName, extension)
    }

    private fun uniqueDestination(sourceName: String, extension: String): File {
        val safeStem =
            sourceName
                .substringAfterLast('/')
                .substringAfterLast('\\')
                .substringBeforeLast('.')
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .take(MAX_FILE_NAME_LENGTH - extension.length - 1)
                .ifBlank { "font" }
        val safeName = "$safeStem.$extension"
        var destination = File(importDirectory, safeName)
        var suffix = 2
        while (destination.exists()) {
            destination = File(importDirectory, "${safeStem}_${suffix++}.$extension")
        }
        return destination
    }

    private fun ensureImportDirectory() {
        if (!importDirectory.mkdirs() && !importDirectory.isDirectory) {
            throw IOException("Unable to create font import directory")
        }
    }

    private fun discardImportedFonts() {
        runCatching {
            if (importDirectory.exists() && !importDirectory.deleteRecursively()) {
                throw IOException("Unable to clear font import directory")
            }
        }
    }

    private fun discardFiles(files: Iterable<File>) {
        files.forEach { runCatching { it.delete() } }
    }

    private fun storedFontFiles(): List<File> {
        return importDirectory
            .listFiles()
            .orEmpty()
            .filter { it.isFile && isSupportedFontName(it.name) }
    }

    private fun displayName(uri: Uri): String {
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) return cursor.getString(0).orEmpty() }
        return uri.lastPathSegment.orEmpty()
    }

    private fun isSupportedFontName(name: String): Boolean {
        return name.endsWith(".ttf", ignoreCase = true) || name.endsWith(".otf", ignoreCase = true)
    }

    private fun fontExtension(input: BufferedInputStream, sourceName: String): String? {
        if (sourceName.endsWith(".ttf", ignoreCase = true)) return "ttf"
        if (sourceName.endsWith(".otf", ignoreCase = true)) return "otf"

        input.mark(FONT_SIGNATURE_SIZE)
        val signature = ByteArray(FONT_SIGNATURE_SIZE)
        val count = input.read(signature)
        input.reset()
        if (count != FONT_SIGNATURE_SIZE) return null
        return when {
            signature.contentEquals(TRUE_TYPE_SIGNATURE) -> "ttf"
            signature.contentEquals(OPEN_TYPE_SIGNATURE) -> "otf"
            else -> null
        }
    }

    private class ImportLimitException : IOException()

    private companion object {
        const val PREFERENCES = "custom_font"
        const val KEY_DISPLAY_NAME = "display_name"
        const val IMPORT_DIRECTORY = "CustomTTF"
        const val MAX_ARCHIVE_ENTRIES = 256
        const val MAX_FONT_FILES = 128
        const val MAX_FILE_NAME_LENGTH = 96
        const val ZIP_SIGNATURE_SIZE = 4
        const val FONT_SIGNATURE_SIZE = 4
        const val COPY_BUFFER_SIZE = 64 * 1024
        const val MAX_FONT_BYTES = 64L * 1024 * 1024
        const val MAX_TOTAL_FONT_BYTES = 256L * 1024 * 1024
        val TRUE_TYPE_SIGNATURE = byteArrayOf(0, 1, 0, 0)
        val OPEN_TYPE_SIGNATURE =
            byteArrayOf('O'.code.toByte(), 'T'.code.toByte(), 'T'.code.toByte(), 'O'.code.toByte())
    }
}

internal data class FontCandidate(
    val file: File,
    val displayName: String,
    val postScriptName: String,
    val sourceName: String,
    val weight: Int,
    val italic: Boolean,
    val typeface: Typeface,
) {
    val id: String = postScriptName + ':' + file.length()
}

internal sealed interface FontImportResult {
    data class Success(val fonts: List<FontCandidate>) : FontImportResult

    object UnsupportedFile : FontImportResult

    object NoFonts : FontImportResult

    object InvalidArchive : FontImportResult

    object TooLarge : FontImportResult

    object Failed : FontImportResult
}

internal sealed interface FontOperationResult {
    data class Success(val displayName: String) : FontOperationResult

    data class Failed(val errorCode: Int) : FontOperationResult

    object InvalidFile : FontOperationResult
}
