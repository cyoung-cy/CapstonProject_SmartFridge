package org.tensorflow.codelabs.objectdetection

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ingredients.sqlite3"
        private const val DATABASE_VERSION = 5

        const val TABLE_CATEGORY = "category"
        const val COLUMN_CATEGORY_ID = "category_id"
        const val COLUMN_CATEGORY_NAME = "category_name"

        const val TABLE_REFRIGERATED = "냉장"
        const val TABLE_FROZEN = "냉동"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "이름"
        const val COLUMN_PHOTO = "사진"
        const val COLUMN_REGISTRATION_DATE = "등록한_날짜"
        const val COLUMN_EXPIRY_DATE = "유통기한"
        const val COLUMN_CATEGORY_NUMBER = "카테고리_넘버"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 카테고리 테이블 생성
        val createCategoryTableQuery = """
        CREATE TABLE IF NOT EXISTS $TABLE_CATEGORY (
            $COLUMN_CATEGORY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_CATEGORY_NAME TEXT
        )
    """.trimIndent()
        db.execSQL(createCategoryTableQuery)

        // 냉장 테이블 생성
        val createRefrigeratedTableQuery = """
        CREATE TABLE IF NOT EXISTS $TABLE_REFRIGERATED (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_NAME TEXT,
            $COLUMN_PHOTO TEXT,
            $COLUMN_REGISTRATION_DATE TEXT,
            $COLUMN_EXPIRY_DATE TEXT,
            $COLUMN_CATEGORY_NUMBER INTEGER
        )
    """.trimIndent()
        db.execSQL(createRefrigeratedTableQuery)

        // 냉동 테이블 생성
        val createFrozenTableQuery = """
        CREATE TABLE IF NOT EXISTS $TABLE_FROZEN (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_NAME TEXT,
            $COLUMN_PHOTO TEXT,
            $COLUMN_REGISTRATION_DATE TEXT,
            $COLUMN_EXPIRY_DATE TEXT,
            $COLUMN_CATEGORY_NUMBER INTEGER
        )
    """.trimIndent()
        db.execSQL(createFrozenTableQuery)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 데이터베이스 업그레이드 시 호출 (모든 테이블 삭제 후 재생성)
        db.execSQL("DROP TABLE IF EXISTS category")
        db.execSQL("DROP TABLE IF EXISTS 냉장")
        db.execSQL("DROP TABLE IF EXISTS 냉동")
        onCreate(db)
    }

    fun insertRefrigeratedData(
        name: String,
        photo: String,
        registrationDate: String,
        expiryDate: String,
        categoryNumber: Int
    ) {
        val db = writableDatabase
        val contentValues = ContentValues().apply {
            put("이름", name)
            put("사진", photo)
            put("등록한_날짜", registrationDate)
            put("유통기한", expiryDate)
            put("카테고리_넘버", categoryNumber)
        }
        db.insert("냉장", null, contentValues)
        db.close()
    }

    fun insertFrozenData(
        name: String,
        photo: String,
        registrationDate: String,
        expiryDate: String,
        categoryNumber: Int
    ) {
        val db = writableDatabase
        val contentValues = ContentValues().apply {
            put("이름", name)
            put("사진", photo)
            put("등록한_날짜", registrationDate)
            put("유통기한", expiryDate)
            put("카테고리_넘버", categoryNumber)
        }
        db.insert("냉동", null, contentValues)
        db.close()
    }

    // 저장된 재료를 가져오는 메서드 추가
    @SuppressLint("Range")
    fun getAllIngredients(): List<String> {
        val ingredients = mutableListOf<String>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COLUMN_NAME FROM $TABLE_REFRIGERATED UNION SELECT $COLUMN_NAME FROM $TABLE_FROZEN", null)

        while (cursor.moveToNext()) {
            ingredients.add(cursor.getString(0))
        }
        cursor.close()
        db.close()
        return ingredients
    }

    fun getIngredientsSortedByExpiryDate(): List<String> {
        val db = this.readableDatabase
        val query = """
        SELECT $COLUMN_NAME 
        FROM (
            SELECT $COLUMN_NAME, $COLUMN_EXPIRY_DATE 
            FROM $TABLE_REFRIGERATED
            UNION ALL
            SELECT $COLUMN_NAME, $COLUMN_EXPIRY_DATE 
            FROM $TABLE_FROZEN
        )
        ORDER BY $COLUMN_EXPIRY_DATE ASC
    """.trimIndent()

        val cursor = db.rawQuery(query, null)
        val ingredients = mutableListOf<String>()
        while (cursor.moveToNext()) {
            ingredients.add(cursor.getString(0))
        }
        cursor.close()
        db.close()
        return ingredients
    }


    @SuppressLint("Range")
    fun getIngredientDetails(name: String): Map<String, String>? {
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT $COLUMN_PHOTO, $COLUMN_REGISTRATION_DATE, $COLUMN_EXPIRY_DATE, $COLUMN_CATEGORY_NUMBER FROM $TABLE_REFRIGERATED WHERE $COLUMN_NAME = ?",
            arrayOf(name)
        )

        return if (cursor.moveToFirst()) {
            val details = mapOf(
                "photo" to cursor.getString(cursor.getColumnIndex(COLUMN_PHOTO)),
                "registrationDate" to cursor.getString(
                    cursor.getColumnIndex(
                        COLUMN_REGISTRATION_DATE
                    )
                ),
                "expiryDate" to cursor.getString(cursor.getColumnIndex(COLUMN_EXPIRY_DATE)),
                "categoryNumber" to cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY_NUMBER))
            )
            cursor.close()
            db.close()
            details
        } else {
            cursor.close()
            db.close()
            null
        }
    }

    @SuppressLint("Range")
    fun getAllIngredientsFromRefrigerated(): List<String> {
        val ingredients = mutableListOf<String>()
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT $COLUMN_NAME FROM $TABLE_REFRIGERATED", null)

        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME))
                ingredients.add(name)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        return ingredients
    }

    @SuppressLint("Range")
    fun getAllIngredientsFromFrozen(): List<String> {
        val ingredients = mutableListOf<String>()
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT $COLUMN_NAME FROM $TABLE_FROZEN", null)

        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME))
                ingredients.add(name)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        return ingredients
    }

    @SuppressLint("Range")
    fun getIngredientDetailsFromRefrigerated(name: String): Map<String, String>? {
        // 기존의 getIngredientDetails 메서드에서 냉장 테이블을 조회하도록 수정
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT $COLUMN_PHOTO, $COLUMN_REGISTRATION_DATE, $COLUMN_EXPIRY_DATE, $COLUMN_CATEGORY_NUMBER FROM $TABLE_REFRIGERATED WHERE $COLUMN_NAME = ?",
            arrayOf(name)
        )

        return if (cursor.moveToFirst()) {
            val details = mapOf(
                "photo" to cursor.getString(cursor.getColumnIndex(COLUMN_PHOTO)),
                "registrationDate" to cursor.getString(cursor.getColumnIndex(COLUMN_REGISTRATION_DATE)),
                "expiryDate" to cursor.getString(cursor.getColumnIndex(COLUMN_EXPIRY_DATE)),
                "categoryNumber" to cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY_NUMBER))
            )
            cursor.close()
            db.close()
            details
        } else {
            cursor.close()
            db.close()
            null
        }
    }

    @SuppressLint("Range")
    fun getIngredientDetailsFromFrozen(name: String): Map<String, String>? {
        // 새로운 메서드 추가: 냉동 테이블에서 세부 정보 조회
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT $COLUMN_PHOTO, $COLUMN_REGISTRATION_DATE, $COLUMN_EXPIRY_DATE, $COLUMN_CATEGORY_NUMBER FROM $TABLE_FROZEN WHERE $COLUMN_NAME = ?",
            arrayOf(name)
        )

        return if (cursor.moveToFirst()) {
            val details = mapOf(
                "photo" to cursor.getString(cursor.getColumnIndex(COLUMN_PHOTO)),
                "registrationDate" to cursor.getString(
                    cursor.getColumnIndex(
                        COLUMN_REGISTRATION_DATE
                    )
                ),
                "expiryDate" to cursor.getString(cursor.getColumnIndex(COLUMN_EXPIRY_DATE)),
                "categoryNumber" to cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY_NUMBER))
            )
            cursor.close()
            db.close()
            details
        } else {
            cursor.close()
            db.close()
            null


        }
    }

    fun deleteIngredient(name: String, category: String) {
        val tableName = if (category == "냉장") "냉장" else "냉동"
        val db = this.writableDatabase

        // '이름' 열을 사용하도록 수정
        db.delete(tableName, "이름 = ?", arrayOf(name))
        db.close()
    }
}