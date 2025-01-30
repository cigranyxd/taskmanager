import android.content.Context
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

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        if (view == null) {
            view = inflater.inflate(R.layout.kategoria_item, parent, false)
        }

        val categoryImage = view?.findViewById<ImageView>(R.id.categoryIcon)
        val categoryName = view?.findViewById<TextView>(R.id.categoryName)

        categoryImage?.setImageResource(kategoriakIkonok[position])
        categoryName?.text = kategoriakNevek[position]

        return view!!
    }
}
