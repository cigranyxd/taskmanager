package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object EgyediKategoriak {
    val kategoriak: MutableList<EgyediKategoria> = mutableListOf()

    fun load(context: Context) {
        val prefs = context.getSharedPreferences("custom_categories", Context.MODE_PRIVATE)
        val json = prefs.getString("kategoriak", null)
        if (json != null) {
            val type = object : TypeToken<List<EgyediKategoria>>() {}.type
            val lista: List<EgyediKategoria> = Gson().fromJson(json, type)
            kategoriak.clear()
            kategoriak.addAll(lista)
        }
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences("custom_categories", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json = Gson().toJson(kategoriak)
        editor.putString("kategoriak", json)
        editor.apply()
    }
}
