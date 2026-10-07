package com.aman.notifhistory

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

data class Notif(
    val id: Long,
    val pkg: String,
    val app: String,
    val title: String,
    val text: String,
    val ts: Long
)

class NotifDb private constructor(ctx: Context) : SQLiteOpenHelper(ctx, "notifs.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE notifs(id INTEGER PRIMARY KEY AUTOINCREMENT, pkg TEXT, app TEXT, title TEXT, text TEXT, ts INTEGER)")
        db.execSQL("CREATE INDEX idx_ts ON notifs(ts)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun insert(n: Notif) {
        val cv = ContentValues()
        cv.put("pkg", n.pkg)
        cv.put("app", n.app)
        cv.put("title", n.title)
        cv.put("text", n.text)
        cv.put("ts", n.ts)
        writableDatabase.insert("notifs", null, cv)
    }

    fun query(q: String, pkg: String?, limit: Int): List<Notif> {
        val where = StringBuilder("1=1")
        val args = ArrayList<String>()
        if (pkg != null) {
            where.append(" AND pkg=?")
            args.add(pkg)
        }
        if (q.isNotBlank()) {
            where.append(" AND (title LIKE ? OR text LIKE ? OR app LIKE ?)")
            repeat(3) { args.add("%" + q.trim() + "%") }
        }
        val out = ArrayList<Notif>()
        readableDatabase.query(
            "notifs", null, where.toString(), args.toTypedArray(),
            null, null, "ts DESC", limit.toString()
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    Notif(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("pkg")) ?: "",
                        c.getString(c.getColumnIndexOrThrow("app")) ?: "",
                        c.getString(c.getColumnIndexOrThrow("title")) ?: "",
                        c.getString(c.getColumnIndexOrThrow("text")) ?: "",
                        c.getLong(c.getColumnIndexOrThrow("ts"))
                    )
                )
            }
        }
        return out
    }

    fun apps(): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        readableDatabase.rawQuery(
            "SELECT pkg, MAX(app) FROM notifs GROUP BY pkg ORDER BY MAX(app) COLLATE NOCASE", null
        ).use { c ->
            while (c.moveToNext()) {
                out.add(Pair(c.getString(0) ?: "", c.getString(1) ?: ""))
            }
        }
        return out
    }

    fun count(): Long = DatabaseUtils.queryNumEntries(readableDatabase, "notifs")

    fun delete(id: Long) {
        writableDatabase.delete("notifs", "id=?", arrayOf(id.toString()))
    }

    fun clear() {
        writableDatabase.delete("notifs", null, null)
    }

    companion object {
        @Volatile
        private var inst: NotifDb? = null

        val io = Executors.newSingleThreadExecutor()
        val main = Handler(Looper.getMainLooper())

        @Volatile
        var onChange: (() -> Unit)? = null

        fun get(c: Context): NotifDb =
            inst ?: synchronized(this) {
                inst ?: NotifDb(c.applicationContext).also { inst = it }
            }
    }
}
