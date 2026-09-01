package com.aichat.imessage.tools

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtil {
    /** Comprime [files] dentro de [outputFile]. Ignora silenciosamente archivos que ya no existan. */
    fun createZip(outputFile: File, files: List<File>): File {
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            files.forEach { file ->
                if (file.exists() && file.isFile) {
                    FileInputStream(file).use { fis ->
                        zos.putNextEntry(ZipEntry(file.name))
                        fis.copyTo(zos)
                        zos.closeEntry()
                    }
                }
            }
        }
        return outputFile
    }
}
