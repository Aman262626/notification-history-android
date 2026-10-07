package com.aman.notifhistory

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotifAdapter(
    private val ctx: Context,
    private val onLong: (Notif) -> Unit
) : RecyclerView.Adapter<NotifAdapter.VH>() {

    private var items: List<Notif> = emptyList()
    private val icons = HashMap<String, Drawable?>()
    private val fmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    fun submit(list: List<Notif>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val app: TextView = v.findViewById(R.id.app)
        val time: TextView = v.findViewById(R.id.time)
        val title: TextView = v.findViewById(R.id.title)
        val text: TextView = v.findViewById(R.id.text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(ctx).inflate(R.layout.item_notif, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val n = items[position]
        h.app.text = n.app
        h.time.text = fmt.format(Date(n.ts))
        h.title.text = n.title
        h.title.visibility = if (n.title.isBlank()) View.GONE else View.VISIBLE
        h.text.text = n.text
        h.text.maxLines = 3
        val icon = if (icons.containsKey(n.pkg)) {
            icons[n.pkg]
        } else {
            val d = try {
                ctx.packageManager.getApplicationIcon(n.pkg)
            } catch (e: Exception) {
                null
            }
            icons[n.pkg] = d
            d
        }
        h.icon.setImageDrawable(icon)
        h.itemView.setOnClickListener {
            h.text.maxLines = if (h.text.maxLines == 3) Int.MAX_VALUE else 3
        }
        h.itemView.setOnLongClickListener {
            onLong(n)
            true
        }
    }
}
