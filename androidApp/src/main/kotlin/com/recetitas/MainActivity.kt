package com.recetitas

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.recetitas.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        iniciarAndroid(this)
        // Si la app se abrió tocando un .receta en WhatsApp, se importa
        if (savedInstanceState == null) recibirIntent(this, intent)
        enableEdgeToEdge()
        setContent { App() }
    }

    // Con la app ya abierta, llega otro .receta
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        recibirIntent(this, intent)
    }
}
