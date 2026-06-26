package com.example.smartmeal.feature.economics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.smartmeal.feature.economics.data.local.EconomicsPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ─────────────────────────────────────────────
// Модели данных
// ─────────────────────────────────────────────

enum class EconomicsPeriod(val label: String) {
    WEEK("Неделя"),
    MONTH("Месяц"),
    YEAR("Год")
}

data class SpendingBar(
    val label: String,   // Пн, Вт, … / Нед1, … / Янв, …
    val amount: Float    // Потрачено (руб)
)

data class SpendingCategory(
    val name: String,
    val emoji: String,
    val percent: Float,
    val amount: Float,
    val color: Long      // ARGB hex
)

data class EconomicsUiState(
    val dailyBudget: Float = 500f,
    val budgetInput: String = "500",
    val todaySpent: Float = 0f,
    val period: EconomicsPeriod = EconomicsPeriod.WEEK,
    val bars: List<SpendingBar> = emptyList(),
    val categories: List<SpendingCategory> = emptyList(),
    val isBudgetSaved: Boolean = false
)

// ─────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────

class EconomicsViewModel(
    private val prefs: EconomicsPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(EconomicsUiState())
    val state: StateFlow<EconomicsUiState> = _state.asStateFlow()

    init {
        val savedBudget = prefs.getDailyBudget()
        _state.update {
            it.copy(
                dailyBudget = savedBudget,
                budgetInput = savedBudget.toInt().toString()
            )
        }
        refreshMockData()
    }

    fun onBudgetInputChange(input: String) {
        _state.update { it.copy(budgetInput = input, isBudgetSaved = false) }
    }

    fun saveBudget() {
        val amount = _state.value.budgetInput.toFloatOrNull() ?: return
        if (amount <= 0f) return
        prefs.setDailyBudget(amount)
        _state.update { it.copy(dailyBudget = amount, isBudgetSaved = true) }
        refreshMockData()
    }

    fun selectPeriod(period: EconomicsPeriod) {
        _state.update { it.copy(period = period) }
        refreshMockData()
    }

    // ─────────────────────────────────────────────
    // Mock-данные
    // TODO: Заменить на реальный API (Яндекс.Лавка, Сбер и пр.)
    //       Пример точки замены:
    //       val realData = yandexLavkaApi.getSpendingHistory(period, userId)
    // ─────────────────────────────────────────────
    private fun refreshMockData() {
        val budget = _state.value.dailyBudget
        val period = _state.value.period

        // Коэффициенты расходов относительно дневного бюджета (шаблонные)
        val bars: List<SpendingBar> = when (period) {
            EconomicsPeriod.WEEK -> listOf(
                SpendingBar("Пн", budget * 0.82f),
                SpendingBar("Вт", budget * 1.10f),
                SpendingBar("Ср", budget * 0.65f),
                SpendingBar("Чт", budget * 0.95f),
                SpendingBar("Пт", budget * 1.25f),
                SpendingBar("Сб", budget * 1.45f),
                SpendingBar("Вс", budget * 0.70f)
            )
            EconomicsPeriod.MONTH -> listOf(
                SpendingBar("Нед 1", budget * 7 * 0.88f),
                SpendingBar("Нед 2", budget * 7 * 1.05f),
                SpendingBar("Нед 3", budget * 7 * 0.92f),
                SpendingBar("Нед 4", budget * 7 * 1.15f)
            )
            EconomicsPeriod.YEAR -> listOf(
                SpendingBar("Янв", budget * 30 * 0.78f),
                SpendingBar("Фев", budget * 28 * 0.85f),
                SpendingBar("Мар", budget * 31 * 0.90f),
                SpendingBar("Апр", budget * 30 * 0.95f),
                SpendingBar("Май", budget * 31 * 1.00f),
                SpendingBar("Июн", budget * 30 * 1.10f),
                SpendingBar("Июл", budget * 31 * 1.20f),
                SpendingBar("Авг", budget * 31 * 1.15f),
                SpendingBar("Сен", budget * 30 * 1.05f),
                SpendingBar("Окт", budget * 31 * 0.98f),
                SpendingBar("Ноя", budget * 30 * 0.88f),
                SpendingBar("Дек", budget * 31 * 1.30f)
            )
        }

        // Сегодняшние расходы — второй по счёту элемент (вторник) для WEEK, иначе средний
        // TODO: Заменить на реальные данные за сегодня
        val todaySpent = bars.getOrNull(1)?.amount ?: (budget * 0.9f)

        // Статья расходов (mock-категории)
        // TODO: Заменить на реальные транзакции из API чеков/банка
        val totalSpent = bars.sumOf { it.amount.toDouble() }.toFloat()
        val categories = listOf(
            SpendingCategory("Мясо и рыба",    "🥩", 32f, totalSpent * 0.32f, 0xFFFF5738),
            SpendingCategory("Овощи и фрукты", "🥦", 22f, totalSpent * 0.22f, 0xFF24C76A),
            SpendingCategory("Молочное",        "🥛", 18f, totalSpent * 0.18f, 0xFF28A7E8),
            SpendingCategory("Бакалея",         "🌾", 14f, totalSpent * 0.14f, 0xFFE9A23B),
            SpendingCategory("Специи и соусы",  "🧂",  8f, totalSpent * 0.08f, 0xFF8E6BE8),
            SpendingCategory("Прочее",          "🛒",  6f, totalSpent * 0.06f, 0xFF9B8E88)
        )

        _state.update {
            it.copy(
                bars = bars,
                todaySpent = todaySpent,
                categories = categories
            )
        }
    }
}

// ─────────────────────────────────────────────
// Factory
// ─────────────────────────────────────────────

class EconomicsViewModelFactory(
    private val prefs: EconomicsPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EconomicsViewModel::class.java)) {
            return EconomicsViewModel(prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: $modelClass")
    }
}
