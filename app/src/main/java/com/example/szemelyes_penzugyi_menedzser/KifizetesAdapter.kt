package com.example.szemelyes_penzugyi_menedzser

import Kifizetesitem
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class KifizetesAdapter(private val kifizetesek: MutableList<Kifizetesitem>) :
    RecyclerView.Adapter<KifizetesAdapter.KifizetesViewHolder>() {

    inner class KifizetesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nevTextView: TextView = itemView.findViewById(R.id.nevTextView)
        val osszegTextView: TextView = itemView.findViewById(R.id.osszegTextView)
        val periodTextView: TextView = itemView.findViewById(R.id.periodTextView)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KifizetesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.kifizetes_item, parent, false)
        return KifizetesViewHolder(view)
    }

    override fun onBindViewHolder(holder: KifizetesViewHolder, position: Int) {
        val kifizetes = kifizetesek[position]
        holder.nevTextView.text = kifizetes.nev
        holder.osszegTextView.text = "${kifizetes.osszeg.toInt()} Ft"

        // 🆕 Periódus szép formázással
        val periodFormatted = kifizetes.period.replaceFirstChar { it.uppercaseChar() }
        holder.periodTextView.text = "Gyakoriság: $periodFormatted"

        // Fekete szövegszínek
        holder.nevTextView.setTextColor(Color.BLACK)
        holder.osszegTextView.setTextColor(Color.BLACK)
        holder.periodTextView.setTextColor(Color.DKGRAY)

        // CheckBox stílus
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val colors = intArrayOf(Color.WHITE, Color.BLACK)
        holder.checkBox.buttonTintList = ColorStateList(states, colors)

        holder.checkBox.isChecked = kifizetes.isChecked
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            kifizetesek[position].isChecked = isChecked
        }
    }

    override fun getItemCount(): Int = kifizetesek.size

    fun getKijeloltElemek(): List<Kifizetesitem> {
        return kifizetesek.filter { it.isChecked }
    }
}