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

    override fun getCount(): Int {
        return kategoriakNevek.size
    }

    override fun getItem(position: Int): Any {
        return kategoriakNevek[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
    // Változó a kiválasztott pozíció tárolására, alapértelmezett: nincs kiválasztva (-1)
    var selectedPosition: Int = -1

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // Inflate-oljuk a nézetet, ha szükséges
        val view = convertView ?: inflater.inflate(R.layout.kategoria_item_elemzeshez, parent, false)

        val categoryImage = view.findViewById<ImageView>(R.id.categoryIcon)
        val categoryName = view.findViewById<TextView>(R.id.categoryName)

        // Állítsuk be az ikonokat és a neveket
        categoryImage.setImageResource(kategoriakIkonok[position])
        categoryName.text = kategoriakNevek[position]

        // Ha ez a pozíció van kiválasztva, világosszürke háttér, egyébként átlátszó
        if (position == selectedPosition) {
            view.setBackgroundColor(Color.RED)
        } else {
            view.setBackgroundColor(Color.TRANSPARENT)
        }

        return view
    }
}
