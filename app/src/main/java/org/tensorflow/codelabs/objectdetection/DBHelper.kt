package org.tensorflow.codelabs.objectdetection

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val context: Context = context

    override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
        // 초기 데이터 삽입은 필요 없으므로 삭제
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 데이터베이스 업그레이드 로직을 여기에 구현
    }

    fun initializeDatabase() {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) {
            this.writableDatabase // 데이터베이스를 생성합니다.
            copyDatabase(dbFile)
        }
    }

    private fun copyDatabase(dbFile: File) {
        val inputStream: InputStream = context.assets.open(ASSET_DATABASE_NAME)
        val outputStream: OutputStream = FileOutputStream(dbFile)

        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
            outputStream.write(buffer, 0, length)
        }

        outputStream.flush()
        outputStream.close()
        inputStream.close()
    }

    fun getMenuNames(): ArrayList<String> {
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT menu_name FROM Recipes", null)
        val result = ArrayList<String>()

        while (cursor.moveToNext()) {
            result.add(cursor.getString(0))
        }
        cursor.close()
        return result
    }

    // 재료를 기반으로 메뉴 이름과 조리법을 검색하는 메서드
    fun getRecipesByIngredient(ingredient: String): List<Pair<String, String>> {
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT menu_name, instructions FROM Recipes WHERE ingredients LIKE ?", arrayOf("%$ingredient%"))
        val result = mutableListOf<Pair<String, String>>()

        while (cursor.moveToNext()) {
            val menuName = cursor.getString(0)
            val instructions = cursor.getString(1)
            result.add(Pair(menuName, instructions))
        }
        cursor.close()
        return result
    }

    companion object {
        private const val DATABASE_NAME = "database223.sqlite3" // 데이터베이스 파일 이름 변경
        private const val ASSET_DATABASE_NAME = "database223.sqlite3" // assets에서 사용할 파일 이름
        private const val DATABASE_VERSION = 1
    }
}