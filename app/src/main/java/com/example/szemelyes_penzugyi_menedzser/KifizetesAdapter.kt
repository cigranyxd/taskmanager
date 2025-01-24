package com.example.szemelyes_penzugyi_menedzser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class KifizetesAdapter(
    private val kifizetesek: List<Kifizetes>, // A kifizetések listája
    private val torlesListener: (String) -> Unit // Lambda a törlés kezelésére
) : RecyclerView.Adapter<KifizetesAdapter.KifizetesViewHolder>() {

    // ViewHolder: Egy RecyclerView elem nézeteit reprezentálja
    class KifizetesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nevTextView: TextView = view.findViewById(R.id.nevTextView)
        val osszegTextView: TextView = view.findViewById(R.id.osszegTextView)
        val torlesButton: Button = view.findViewById(R.id.torlesButton)
    }

    // Létrehozza az egyes elemekhez tartozó nézeteket
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KifizetesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.kifizetes_item, parent, false) // Elem layout fájl
        return KifizetesViewHolder(view)
    }

    // Az adatok megjelenítése az egyes elemekben
    override fun onBindViewHolder(holder: KifizetesViewHolder, position: Int) {
        val kifizetes = kifizetesek[position]
        holder.nevTextView.text = kifizetes.nev
        holder.osszegTextView.text = "${kifizetes.osszeg} Ft"

        // A törlés gomb eseménykezelője
        holder.torlesButton.setOnClickListener {
            torlesListener(kifizetes.docId) // Meghívja a törlési műveletet
        }
    }

    // Az elemek számának visszaadása
    override fun getItemCount() = kifizetesek.size
}
