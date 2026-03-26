package com.example.smartmeal.feature.menu_generator.data.models

// ─── Создание меню (ручная сборка, шаг 1) ────────────────────────────────────

/**
 * POST /api/menus/
 * Создаёт пустое меню. После этого нужно наполнить его рецептами через /api/menus/items/
 */
data class CreateMenuRequest(
    val period: String,      // "day" или "week" — из PeriodType.apiValue
    val start_date: String   // дата начала в формате "YYYY-MM-DD" (из calendar state)
)

// ─── Добавление рецептов в меню (шаг 2 ручной сборки) ───────────────────────

/**
 * POST /api/menus/items/
 * Добавляет один рецепт в конкретный слот меню.
 * Ошибка 400 если слот (menu + day_offset + meal_type) уже занят.
 */
data class CreateMenuItemRequest(
    val menu: Int,           // ID меню из CreateMenuResponse
    val day_offset: Int,     // 0 = первый день, 1 = второй и т.д.
    val meal_type: String,   // "breakfast" | "lunch" | "dinner" | "snack" | "drink"
    val recipe: Int          // ID рецепта из /api/recipes/
)

// ─── Добавление в корзину (шаг 3 ручной сборки) ─────────────────────────────

/**
 * POST /api/cart/
 * Добавляет ингредиент в список покупок пользователя.
 */
data class AddToCartRequest(
    val ingredient: Int,     // ID ингредиента
    val unit: Int,           // ID единицы измерения (1=г, 6=мл, ...)
    val amount: String       // количество как строка: "300.00"
)

// ─── Автоматическая генерация меню ──────────────────────────────────────────

/**
 * POST /api/menus/generate/
 *
 * Автоматически генерирует меню: создаёт Menu и заполняет его рецептами
 * (breakfast/lunch/dinner × кол-во дней периода) с учётом предпочтений.
 *
 * Параметры diet_type и max_cook_time — опциональные, при отсутствии
 * бэкенд берёт значения из профиля пользователя (diet_type).
 *
 * Пример:
 * ```
 * autoGenerate(AutoGenerateRequest(period = "week", start_date = "2026-03-10"))
 * // → GeneratedMenuDto(id=42, period="week", items=[...21 items...])
 * ```
 */
data class AutoGenerateRequest(
    val period: String,                          // "day" или "week"
    val start_date: String,                      // "YYYY-MM-DD"
    val days: Int? = null,                       // количество дней для генерации (1-31)
    val diet_type: Int? = null,                  // null → берётся из профиля пользователя
    val cook_time_range: String? = null,         // "short" | "medium" | "long" | "any"
    val max_cook_time: Int? = null,              // null = без ограничения; 30 = «до 30 минут»
    val exclude_allergies: List<Int> = emptyList() // зарезервировано для будущего
)

/**
 * Ответ на POST /api/menus/generate/
 *
 * Возвращает полное меню с вложенными items.
 * Структура совпадает с MenuSerializer на бэкенде.
 */
data class GeneratedMenuDto(
    val id: Int,
    val period: String,      // "day" | "week"
    val start_date: String,  // "YYYY-MM-DD"
    val created_at: String,  // "2026-03-10T10:00:00Z"
    val items: List<GeneratedMenuItemDto>
)

/**
 * Один элемент сгенерированного меню.
 * Вложен в [GeneratedMenuDto.items].
 */
data class GeneratedMenuItemDto(
    val id: Int,
    val recipe: Int,           // ID рецепта
    val recipe_title: String,  // название рецепта (для отображения без доп. запроса)
    val day_offset: Int,       // 0 = первый день, 1 = второй и т.д.
    val meal_type: String,     // "breakfast" | "lunch" | "dinner" | "snack" | "drink"
    val actual_date: String    // "YYYY-MM-DD" — реальная дата (start_date + day_offset)
)
