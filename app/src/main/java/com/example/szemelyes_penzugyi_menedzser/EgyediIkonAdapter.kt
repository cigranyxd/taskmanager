package com.example.szemelyes_penzugyi_menedzser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class EgyediIkonAdapter(
    private val context: Context,
    private val iconList: List<Int>,
    private val onIconClick: (Int?) -> Unit // Nullable Int, hogy törölhessük a kijelölést
) : RecyclerView.Adapter<EgyediIkonAdapter.IconViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION // Kezdetben nincs kiválasztott elem

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_icon, parent, false)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val iconResId = iconList[position]
        holder.iconImageView.setImageResource(iconResId)

        // Ha kiválasztották az elemet, akkor vizuálisan megjelöljük
        if (position == selectedPosition) {
            holder.itemView.setBackgroundColor(Color.LTGRAY) // Kiválasztott ikon
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT) // Nem kiválasztott ikon
        }

        // Kattintás kezelés
        holder.itemView.setOnClickListener {
            if (selectedPosition == position) {
                // Ha ugyanarra kattintanak, vonjuk vissza a kijelölést
                selectedPosition = RecyclerView.NO_POSITION // Nincs többé kiválasztott elem
                onIconClick(null) // Küldjük vissza, hogy nincs kiválasztott ikon
                notifyItemChanged(position) // Mivel itt frissítjük a kijelölést, ezt is jelezni kell
            } else {
                // Másik elemre kattintva frissítjük a kijelölést
                val previousPosition = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousPosition) // Frissítjük az előző elemet
                notifyItemChanged(selectedPosition) // Frissítjük az új elemet
                onIconClick(iconResId) // Kiválasztott ikon visszaküldése
            }
        }
    }

    override fun getItemCount(): Int = iconList.size

    class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconImageView: ImageView = itemView.findViewById(R.id.iconImageView)
    }
}
