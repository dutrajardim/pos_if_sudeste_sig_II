package com.example.navegacao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.example.navegacao.ui.theme.NavegacaoTheme
import io.github.cdimascio.dotenv.dotenv

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dotenv = dotenv {
            directory = "/assets"
            filename = "env"
        }
        ArcGISEnvironment.apiKey = ApiKey.create(dotenv["API_KEY"])

        setContent {
            NavegacaoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    Column (
        modifier = modifier.fillMaxSize(),
        horizontalAlignment =  Alignment.CenterHorizontally
    ) {
        MapViewCompose()
    }
}