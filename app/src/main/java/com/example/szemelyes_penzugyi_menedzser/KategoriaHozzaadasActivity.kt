package com.example.szemelyes_penzugyi_menedzser

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class KategoriaHozzaadasActivity : AppCompatActivity() {

    private lateinit var kategoriaNevEditText: EditText
    private lateinit var tipusRadioGroup: RadioGroup
    private lateinit var ikonGridView: GridView
    private lateinit var hozzadasGomb: Button

    // Például 21 ikon resource ID (győződj meg róla, hogy ezek léteznek a drawable mappában)
    private val ikonLista = listOf(
        R.drawable.ikon1, R.drawable.ikon2, R.drawable.ikon3, R.drawable.ikon4,
        R.drawable.ikon5, R.drawable.ikon6, R.drawable.ikon7, R.drawable.ikon8,
        R.drawable.ikon9, R.drawable.ikon10, R.drawable.ikon11, R.drawable.ikon12,
        R.drawable.ikon13, R.drawable.ikon14, R.drawable.ikon15, R.drawable.ikon16,
        R.drawable.ikon17, R.drawable.ikon18, R.drawable.ikon19, R.drawable.ikon20,
        R.drawable.ikon21
    )

    private var kivalasztottIkonResId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kategoriahozzadas)

        kategoriaNevEditText = findViewById(R.id.categoryNameEditText)
        tipusRadioGroup = findViewById(R.id.typeRadioGroup)
        ikonGridView = findViewById(R.id.iconGridView)
        hozzadasGomb = findViewById(R.id.addCategoryButton)

        // Ikonok megjelenítése a GridView-ban
        val adapter = IkonAdapter(this, ikonLista)
        ikonGridView.adapter = adapter

        ikonGridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            kivalasztottIkonResId = ikonLista[position]
            adapter.setSelectedPosition(position)
            adapter.notifyDataSetChanged()
            ellenorizMezok()
        }

        kategoriaNevEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                ellenorizMezok()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        tipusRadioGroup.setOnCheckedChangeListener { _, _ ->
            ellenorizMezok()
        }

        hozzadasGomb.setOnClickListener {
            val kategoriaNev = kategoriaNevEditText.text.toString().trim()
            val kategoriaTipus = when (tipusRadioGroup.checkedRadioButtonId) {
                R.id.incomeRadioButton -> "Bevétel"
                R.id.expenseRadioButton -> "Kiadás"
                else -> ""
            }
            val ikonRes = kivalasztottIkonResId

            if (kategoriaNev.isNotEmpty() && ikonRes != null && kategoriaTipus.isNotEmpty()) {
                // Új kategória mentése a globális EgyediKategoriak objektumba
                val ujKategoria = EgyediKategoria(kategoriaNev, ikonRes, kategoriaTipus)
                EgyediKategoriak.kategoriak.add(ujKategoria)
                // Mentés SharedPreferences-be, hogy újraindítás után is megmaradjanak
                EgyediKategoriak.save(this)
                Toast.makeText(this, "Kategória hozzáadva: $kategoriaNev, $kategoriaTipus", Toast.LENGTH_SHORT).show()
                finish()  // Visszalépés a főképernyőre
            }
        }

        ellenorizMezok()
    }

    private fun ellenorizMezok() {
        val isNevValid = kategoriaNevEditText.text.toString().trim().isNotEmpty()
        val isIkonKivalasztva = kivalasztottIkonResId != null

        if (isNevValid && isIkonKivalasztva) {
            hozzadasGomb.isEnabled = true
            hozzadasGomb.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        } else {
            hozzadasGomb.isEnabled = false
            hozzadasGomb.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }
}
