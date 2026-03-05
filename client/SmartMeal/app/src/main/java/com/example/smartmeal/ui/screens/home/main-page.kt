package com.example.smartmeal.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmeal.data.api.SmartMealApi
import com.example.smartmeal.data.models.Ingredient
import kotlinx.coroutines.launch

class HomeViewModel(private val api: SmartMealApi = SmartMealApi.create()) : ViewModel() {
    var ingredients by mutableStateOf<List<Ingredient>>(emptyList())
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    init {
        loadIngredients()
    }

    fun loadIngredients() {
        viewModelScope.launch {
            isLoading = true
            try {
                ingredients = api.getIngredients()
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Failed to load ingredients"
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Ingredients") })
        }
    ) { padding ->
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (viewModel.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(text = viewModel.error!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.ingredients) { ingredient ->
                    IngredientItem(ingredient)
                }
            }
        }
    }
}

@Composable
fun IngredientItem(ingredient: Ingredient) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = ingredient.name, style = MaterialTheme.typography.titleLarge)
            Text(text = "Category: ${ingredient.category}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Unit: ${ingredient.baseUnit}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
