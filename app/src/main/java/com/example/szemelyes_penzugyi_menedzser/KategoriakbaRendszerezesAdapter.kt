package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class KategoriakbaRendszerezesAdapter(
    private val context: Context,
    private val kategoriakNevek: List<String>,
    private val kategoriakIkonok: List<Int>,
    private val kategoriakExpenseProgress: List<Int>,
    private val kategoriakIncomeProgress: List<Int>,
    private val kategoriakExpenseAmount: List<Float>,
    private val kategoriakIncomeAmount: List<Float>
) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = kategoriakNevek.size

    override fun getItem(position: Int): Any = kategoriakNevek[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.kategoria_item, parent, false)

        // Nézetek keresése
        val categoryImage = view.findViewById<ImageView>(R.id.categoryIconImageView)
        val categoryName = view.findViewById<TextView>(R.id.categoryNameTextView)
        val expenseContainer = view.findViewById<LinearLayout>(R.id.expenseContainer)
        val incomeContainer = view.findViewById<LinearLayout>(R.id.incomeContainer)
        val expenseProgressBar = view.findViewById<ProgressBar>(R.id.expenseProgressBar)
        val incomeProgressBar = view.findViewById<ProgressBar>(R.id.incomeProgressBar)
        val expenseAmountTextView = view.findViewById<TextView>(R.id.expenseAmountTextView)
        val incomeAmountTextView = view.findViewById<TextView>(R.id.incomeAmountTextView)

        if (categoryImage == null || categoryName == null || expenseContainer == null || incomeContainer == null ||
            expenseProgressBar == null || incomeProgressBar == null || expenseAmountTextView == null || incomeAmountTextView == null) {
            Log.e("KategoriakAdapter", "View elemek inicializálása sikertelen a $position pozíciónál.")
            return view
        }

        try {
            val icon = kategoriakIkonok.getOrElse(position) { R.drawable.placeholder_icon }
            val name = kategoriakNevek.getOrElse(position) { "Ismeretlen" }
            val expenseProgress = kategoriakExpenseProgress.getOrElse(position) { 0 }
            val incomeProgress = kategoriakIncomeProgress.getOrElse(position) { 0 }
            val expenseAmount = kategoriakExpenseAmount.getOrElse(position) { 0f }
            val incomeAmount = kategoriakIncomeAmount.getOrElse(position) { 0f }

            categoryImage.setImageResource(icon)
            categoryName.text = name
            expenseProgressBar.progress = expenseProgress
            incomeProgressBar.progress = incomeProgress
            expenseAmountTextView.text = "${expenseAmount.toInt()} Ft"
            incomeAmountTextView.text = "${incomeAmount.toInt()} Ft"

            // A kategória neve alapján állítjuk be a láthatóságot:
            when (name) {
                "Fizetési csekk" -> {
                    // Csak a bevétel (income) sor látszik
                    expenseContainer.visibility = View.GONE
                    incomeContainer.visibility = View.VISIBLE
                }
                "Ajándékok", "Egyéb" -> {
                    // Mindkét sor látszik
                    expenseContainer.visibility = View.VISIBLE
                    incomeContainer.visibility = View.VISIBLE
                }
                else -> {
                    // Minden más kategóriánál csak a kiadás (expense) sor látszik
                    expenseContainer.visibility = View.VISIBLE
                    incomeContainer.visibility = View.GONE
                }
            }

            Log.d("KategoriakAdapter", "Nézet inicializálva a $position pozíciónál")
        } catch (e: Exception) {
            Log.e("KategoriakAdapter", "Hiba a getView-ben: ${e.message}")
        }

        return view
    }
}
