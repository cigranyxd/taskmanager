import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.szemelyes_penzugyi_menedzser.R

class KategoriaAdapter(
    context: Context,
    private val kategoriakNevek: List<String>,
    private val kategoriakIkonok: List<Int>
) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = kategoriakNevek.size

    override fun getItem(position: Int): Any = kategoriakNevek[position]

    override fun getItemId(position: Int): Long = position.toLong()

    // Változó a kiválasztott pozíció tárolására, alapértelmezett: nincs kiválasztva (-1)
    var selectedPosition: Int = -1

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: inflater.inflate(R.layout.kategoria_item_elemzeshez, parent, false)
        val categoryImage = view.findViewById<ImageView>(R.id.categoryIcon)
        val categoryName = view.findViewById<TextView>(R.id.categoryName)

        // Ikon és név beállítása
        categoryImage.setImageResource(kategoriakIkonok[position])
        categoryName.text = kategoriakNevek[position]
        // Kényszerítjük a fekete színt a kategória nevéhez
        categoryName.setTextColor(Color.BLACK)

        // Ha ez a pozíció van kiválasztva, állítsuk például világosszürkére a háttérszínt, egyébként átlátszóra
        if (position == selectedPosition) {
            view.setBackgroundColor(Color.LTGRAY)
        } else {
            view.setBackgroundColor(Color.TRANSPARENT)
        }

        return view
    }
}
