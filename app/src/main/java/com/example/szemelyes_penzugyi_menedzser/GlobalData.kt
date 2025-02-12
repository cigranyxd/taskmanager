package com.example.szemelyes_penzugyi_menedzser

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object GlobalData {
    private val _aktualisPenz = MutableLiveData<Double>(0.0)
    val aktualisPenz: LiveData<Double> = _aktualisPenz

    fun setAktualisPenz(osszeg: Double) {
        _aktualisPenz.value = osszeg
    }
}
