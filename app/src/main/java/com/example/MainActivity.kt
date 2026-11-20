package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.di.VedicraAppContainer
import com.example.ui.VedicraApp

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { VedicraAppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VedicraApp(container = appContainer)
        }
    }
}
