package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
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
        // Feltételezzük, hogy a "kategoria_item.xml" a következő elemeket tartalmazza:
        // - ImageView: categoryIconImageView
        // - TextView: categoryNameTextView
        // - LinearLayout: expenseContainer, benne ProgressBar (expenseProgressBar) és TextView (expenseAmountTextView)
        // - LinearLayout: incomeContainer, benne ProgressBar (incomeProgressBar) és TextView (incomeAmountTextView)
        val view = convertView ?: inflater.inflate(R.layout.kategoria_item, parent, false)

        val categoryIcon = view.findViewById<ImageView>(R.id.categoryIconImageView)
        val categoryName = view.findViewById<TextView>(R.id.categoryNameTextView)
        val expenseContainer = view.findViewById<View>(R.id.expenseContainer)
        val incomeContainer = view.findViewById<View>(R.id.incomeContainer)
        val expenseProgressBar = view.findViewById<ProgressBar>(R.id.expenseProgressBar)
        val incomeProgressBar = view.findViewById<ProgressBar>(R.id.incomeProgressBar)
        val expenseAmountText = view.findViewById<TextView>(R.id.expenseAmountTextView)
        val incomeAmountText = view.findViewById<TextView>(R.id.incomeAmountTextView)

        if (categoryIcon == null || categoryName == null || expenseContainer == null || incomeContainer == null ||
            expenseProgressBar == null || incomeProgressBar == null || expenseAmountText == null || incomeAmountText == null) {
            Log.e("KategoriakAdapter", "Hibás nézetek!")
            return view
        }

        categoryIcon.setImageResource(kategoriakIkonok.getOrElse(position) { R.drawable.placeholder_icon })
        categoryName.text = kategoriakNevek.getOrElse(position) { "Ismeretlen" }

        // Állítsuk be az összegeket és a progressbar értékeket
        val expOsszeg = kategoriakExpenseAmount.getOrElse(position) { 0f }
        if (expOsszeg > 0) {
            expenseContainer.visibility = View.VISIBLE
            expenseProgressBar.progress = kategoriakExpenseProgress.getOrElse(position) { 0 }
            expenseAmountText.text = "${expOsszeg.toInt()} Ft"
        } else {
            expenseContainer.visibility = View.GONE
        }

        val incOsszeg = kategoriakIncomeAmount.getOrElse(position) { 0f }
        if (incOsszeg > 0) {
            incomeContainer.visibility = View.VISIBLE
            incomeProgressBar.progress = kategoriakIncomeProgress.getOrElse(position) { 0 }
            incomeAmountText.text = "${incOsszeg.toInt()} Ft"
        } else {
            incomeContainer.visibility = View.GONE
        }

        return view
    }
}
