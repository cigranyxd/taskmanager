package com.example.szemelyes_penzugyi_menedzser

import KategoriaAdapter
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth

class KategoriaHozzaadasActivity : AppCompatActivity() {

    private lateinit var kategoriaNevEditText: EditText
    private lateinit var tipusRadioGroup: RadioGroup
    private lateinit var ikonGridView: GridView
    private lateinit var hozzadasGomb: Button

    // Például 21 ikon resource ID
    private val ikonLista = listOf(
        R.drawable.ikon1, R.drawable.ikon2, R.drawable.ikon3, R.drawable.ikon4,
        R.drawable.ikon5, R.drawable.ikon6, R.drawable.ikon7, R.drawable.ikon8,
        R.drawable.ikon9, R.drawable.ikon10, R.drawable.ikon11, R.drawable.ikon12,
        R.drawable.ikon13, R.drawable.ikon14, R.drawable.ikon15, R.drawable.ikon16,
        R.drawable.ikon17, R.drawable.ikon18, R.drawable.ikon19, R.drawable.ikon20,
        R.drawable.ikon21
    )

    private var kivalasztottIkonResId: Int? = null
    // Tároljuk a kiválasztott ikon pozícióját; ha nincs kiválasztva, -1
    private var selectedIconPosition: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kategoriahozzadas)

        kategoriaNevEditText = findViewById(R.id.categoryNameEditText)
        tipusRadioGroup = findViewById(R.id.typeRadioGroup)
        ikonGridView = findViewById(R.id.iconGridView)
        hozzadasGomb = findViewById(R.id.addCategoryButton)

        // Custom kategóriák betöltése Firestore-ból, ha be van jelentkezve
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            EgyediKategoriak.betoltFirestore(this, currentUser.uid)
        } else {
            EgyediKategoriak.betolt(this)
        }

        // Az adapterben csak az ikonok jelennek meg, a nevek üresek, mivel a felhasználó írja be a kategória nevet.
        val adapter = KategoriaAdapter(this, List(ikonLista.size) { "" }, ikonLista)
        ikonGridView.adapter = adapter

        ikonGridView.setOnItemClickListener { parent, view, position, _ ->
            // Ha ugyanarra az elemre kattintanak, visszavonjuk a kijelölést
            if (selectedIconPosition == position) {
                selectedIconPosition = -1
                kivalasztottIkonResId = null
            } else {
                selectedIconPosition = position
                kivalasztottIkonResId = ikonLista[position]
            }
            // Frissítjük az adapter kiválasztott pozícióját
            adapter.selectedPosition = selectedIconPosition
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

            if (kategoriaNev.isNotEmpty() && kivalasztottIkonResId != null && kategoriaTipus.isNotEmpty()) {
                val ujKategoria = EgyediKategoria(kategoriaNev, kivalasztottIkonResId!!, kategoriaTipus)
                EgyediKategoriak.kategoriak.add(ujKategoria)
                EgyediKategoriak.ment(this)
                if (currentUser != null) {
                    EgyediKategoriak.mentFirestore(this, currentUser.uid) {
                        Toast.makeText(this, "Kategória hozzáadva: $kategoriaNev, $kategoriaTipus", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Kategória hozzáadva lokálisan: $kategoriaNev, $kategoriaTipus", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        ellenorizMezok()
    }

    private fun ellenorizMezok() {
        val isNevValid = kategoriaNevEditText.text.toString().trim().isNotEmpty()
        val isIkonKivalasztva = kivalasztottIkonResId != null
        hozzadasGomb.isEnabled = isNevValid && isIkonKivalasztva
        val colorRes = if (isNevValid && isIkonKivalasztva) android.R.color.holo_red_dark else android.R.color.darker_gray
        hozzadasGomb.setBackgroundColor(ContextCompat.getColor(this, colorRes))
    }
}
