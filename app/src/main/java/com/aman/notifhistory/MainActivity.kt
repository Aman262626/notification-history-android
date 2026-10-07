package com.aman.notifhistory

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: NotifAdapter
    private lateinit var banner: View
    private lateinit var empty: TextView
    private lateinit var spinner: Spinner
    private lateinit var search: EditText
    private lateinit var db: NotifDb

    private var pkgFilter: String? = null
    private var query: String = ""
    private var appList: List<Pair<String, String>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = NotifDb.get(this)

        banner = findViewById(R.id.banner)
        empty = findViewById(R.id.empty)
        spinner = findViewById(R.id.appSpinner)
        search = findViewById(R.id.search)
        val list = findViewById<RecyclerView>(R.id.list)

        adapter = NotifAdapter(this) { confirmDelete(it) }
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        findViewById<Button>(R.id.grantBtn).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString() ?: ""
                refresh()
            }
        })

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val newPkg = if (pos == 0) null else appList.getOrNull(pos - 1)?.first
                if (newPkg != pkgFilter) {
                    pkgFilter = newPkg
                    refresh()
                }
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    override fun onResume() {
        super.onResume()
        val enabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        banner.visibility = if (enabled) View.GONE else View.VISIBLE
        if (enabled) {
            try {
                NotificationListenerService.requestRebind(ComponentName(this, NotifListener::class.java))
            } catch (e: Exception) {
            }
        }
        NotifDb.onChange = { loadApps(); refresh() }
        loadApps()
        refresh()
    }

    override fun onPause() {
        super.onPause()
        NotifDb.onChange = null
    }

    private fun loadApps() {
        NotifDb.io.execute {
            val apps = db.apps()
            runOnUiThread {
                appList = apps
                val names = ArrayList<String>()
                names.add("All apps")
                apps.forEach { names.add(it.second) }
                spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
                val idx = apps.indexOfFirst { it.first == pkgFilter }
                if (idx >= 0) {
                    spinner.setSelection(idx + 1)
                } else if (pkgFilter != null) {
                    pkgFilter = null
                    refresh()
                }
            }
        }
    }

    private fun refresh() {
        val q = query
        val p = pkgFilter
        NotifDb.io.execute {
            val items = db.query(q, p, 1000)
            val total = db.count()
            runOnUiThread {
                adapter.submit(items)
                empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                supportActionBar?.subtitle = "$total saved"
            }
        }
    }

    private fun confirmDelete(n: Notif) {
        AlertDialog.Builder(this)
            .setTitle("Delete this entry?")
            .setMessage(n.app + ": " + n.title)
            .setPositiveButton("Delete") { _, _ ->
                NotifDb.io.execute {
                    db.delete(n.id)
                    runOnUiThread { loadApps(); refresh() }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_ignore_ongoing).isChecked =
            getSharedPreferences("prefs", MODE_PRIVATE).getBoolean("ignore_ongoing", true)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_ignore_ongoing -> {
                val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
                prefs.edit().putBoolean("ignore_ongoing", !prefs.getBoolean("ignore_ongoing", true)).apply()
                return true
            }
            R.id.action_export -> {
                exportCsv()
                return true
            }
            R.id.action_clear -> {
                AlertDialog.Builder(this)
                    .setTitle("Clear all history?")
                    .setMessage("Saari saved notifications delete ho jayengi.")
                    .setPositiveButton("Clear") { _, _ ->
                        NotifDb.io.execute {
                            db.clear()
                            runOnUiThread { loadApps(); refresh() }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun csv(s: String): String {
        val q = 34.toChar().toString()
        return q + s.replace(q, q + q) + q
    }

    private fun exportCsv() {
        NotifDb.io.execute {
            try {
                val all = db.query("", null, 1000000)
                val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val nl = System.lineSeparator()
                val sb = StringBuilder("time,app,package,title,text").append(nl)
                for (n in all) {
                    sb.append(csv(fmt.format(Date(n.ts)))).append(',')
                        .append(csv(n.app)).append(',')
                        .append(csv(n.pkg)).append(',')
                        .append(csv(n.title)).append(',')
                        .append(csv(n.text)).append(nl)
                }
                val dir = File(cacheDir, "exports")
                dir.mkdirs()
                val f = File(dir, "notifications.csv")
                f.writeText(sb.toString())
                val uri = FileProvider.getUriForFile(this, packageName + ".fileprovider", f)
                runOnUiThread {
                    val i = Intent(Intent.ACTION_SEND)
                    i.type = "text/csv"
                    i.putExtra(Intent.EXTRA_STREAM, uri)
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(Intent.createChooser(i, "Export notifications"))
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Export failed: " + e.message, Toast.LENGTH_LONG).show() }
            }
        }
    }
}
