package com.example.smartmeal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.smartmeal.ui.navigation.SmartMealNavGraph
import com.example.smartmeal.ui.theme.SmartMealTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartMealTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Создаем контроллер навигации на самом верхнем уровне
                    val navController = rememberNavController()

                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            // Вызываем наш вынесенный граф навигации!
                            SmartMealNavGraph(navController = navController)
                        }
                    }
                }
            }
        }
    }
}