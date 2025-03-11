package com.example.szemelyes_penzugyi_menedzser

import Kifizetesitem
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
        holder.osszegTextView.text = "${kifizetes.osszeg} Ft"
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
