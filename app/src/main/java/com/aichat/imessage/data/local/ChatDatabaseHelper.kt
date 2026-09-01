package com.aichat.imessage.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Base de datos local en SQLite, guardada dentro del almacenamiento privado
 * del teléfono (no depende de internet ni se borra al cerrar la app).
 * Aquí viven las conversaciones, los mensajes y los metadatos de los
 * archivos adjuntos (el contenido de los archivos se guarda aparte, en
 * filesDir/attachments, y aquí solo se referencia la ruta).
 */
class ChatDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE conversations (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                avatarColor TEXT NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                conversationId TEXT NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                time INTEGER NOT NULL,
                error INTEGER NOT NULL DEFAULT 0,
                displayMode TEXT NOT NULL DEFAULT 'AUTO'
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE attachments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                messageId INTEGER NOT NULL,
                fileName TEXT NOT NULL,
                localPath TEXT NOT NULL,
                mimeType TEXT NOT NULL,
                sizeBytes INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_messages_conv ON messages(conversationId)")
        db.execSQL("CREATE INDEX idx_attachments_msg ON attachments(messageId)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS attachments")
        db.execSQL("DROP TABLE IF EXISTS messages")
        db.execSQL("DROP TABLE IF EXISTS conversations")
        onCreate(db)
    }

    companion object {
        private const val DB_NAME = "aichat_local.db"
        private const val DB_VERSION = 1
    }
}
