package com.example.prueba3.form

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class FormRecepcionViewModel : ViewModel() {
    val latitud = mutableStateOf(0.0)
    val longitud = mutableStateOf(0.0)
    val fotoRecepcion = mutableStateOf<Uri?>(null)
}