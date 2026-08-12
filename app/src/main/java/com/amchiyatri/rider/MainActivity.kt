package com.amchiyatri.rider

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.amchiyatri.rider.ui.navigation.AmchiYatriNavGraph
import com.amchiyatri.rider.ui.theme.AmchiYatriTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmchiYatriTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AmchiYatriNavGraph()
                }
            }
        }
    }
}
