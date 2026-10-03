package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MafiaDarkBg
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MafiaViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MafiaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MafiaDarkBg
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

