package com.example.visionhuntonline.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.visionhuntonline.model.CapturedItem

class CapturedItemDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_NAME (" +
                    "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_TASK_ID TEXT, " +
                    "$COLUMN_TASK_NAME_AR TEXT, " +
                    "$COLUMN_TASK_NAME_EN TEXT, " +
                    "$COLUMN_IMAGE_PATH TEXT, " +
                    "$COLUMN_TIMESTAMP INTEGER, " +
                    "$COLUMN_SCORE REAL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertItem(item: CapturedItem): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TASK_ID, item.taskId)
            put(COLUMN_TASK_NAME_AR, item.taskNameAr)
            put(COLUMN_TASK_NAME_EN, item.taskNameEn)
            put(COLUMN_IMAGE_PATH, item.imagePath)
            put(COLUMN_TIMESTAMP, item.timestamp)
            put(COLUMN_SCORE, item.score)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    fun getAllItems(): List<CapturedItem> {
        val list = mutableListOf<CapturedItem>()
        val db = readableDatabase
        val cursor = db.query(TABLE_NAME, null, null, null, null, null, "$COLUMN_TIMESTAMP DESC")
        cursor.use {
            val idCol = it.getColumnIndex(COLUMN_ID)
            val taskIdCol = it.getColumnIndex(COLUMN_TASK_ID)
            val nameArCol = it.getColumnIndex(COLUMN_TASK_NAME_AR)
            val nameEnCol = it.getColumnIndex(COLUMN_TASK_NAME_EN)
            val pathCol = it.getColumnIndex(COLUMN_IMAGE_PATH)
            val timeCol = it.getColumnIndex(COLUMN_TIMESTAMP)
            val scoreCol = it.getColumnIndex(COLUMN_SCORE)

            while (it.moveToNext()) {
                list.add(
                    CapturedItem(
                        id = if (idCol >= 0) it.getLong(idCol) else 0L,
                        taskId = if (taskIdCol >= 0) it.getString(taskIdCol) ?: "" else "",
                        taskNameAr = if (nameArCol >= 0) it.getString(nameArCol) ?: "" else "",
                        taskNameEn = if (nameEnCol >= 0) it.getString(nameEnCol) ?: "" else "",
                        imagePath = if (pathCol >= 0) it.getString(pathCol) ?: "" else "",
                        timestamp = if (timeCol >= 0) it.getLong(timeCol) else 0L,
                        score = if (scoreCol >= 0) it.getFloat(scoreCol) else 0f
                    )
                )
            }
        }
        return list
    }

    fun deleteAllItems() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_NAME")
    }

    companion object {
        private const val DATABASE_NAME = "vision_hunt_captured.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_NAME = "captured_items"
        const val COLUMN_ID = "id"
        const val COLUMN_TASK_ID = "task_id"
        const val COLUMN_TASK_NAME_AR = "task_name_ar"
        const val COLUMN_TASK_NAME_EN = "task_name_en"
        const val COLUMN_IMAGE_PATH = "image_path"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_SCORE = "score"
    }
}
