# SmartMeal API — справочник эндпоинтов

Base URL (эмулятор Android): `http://10.0.2.2:8000`
Base URL (физическое устройство): `http://<IP компьютера>:8000`

Все эндпоинты (кроме `register` и `token`) требуют заголовок:
```
Authorization: Bearer <access_token>
```

---

## Аутентификация (`/api/accounts/`)

### POST `/api/accounts/register/`
Регистрация нового пользователя.

**Тело запроса:**
```json
{
  "username": "ivan",
  "email": "ivan@example.com",
  "password": "SecurePass123!",
  "password_confirm": "SecurePass123!"
}
```

**Ответ 201:**
```json
{
  "user": {
    "id": 1,
    "username": "ivan",
    "email": "ivan@example.com",
    "portion_size": 1,
    "diet_type": null,
    "diet_type_name": null,
    "allergies": [],
    "allergies_names": []
  },
  "access": "<jwt_access_token>",
  "refresh": "<jwt_refresh_token>",
  "message": "Пользователь был успешно создан"
}
```

**Ошибки 400:** `username`, `email`, `password`, `password_confirm` — поля с описанием ошибки.

---

### POST `/api/accounts/token/`
Вход по email и паролю. Возвращает JWT-токены.

**Тело запроса:**
```json
{
  "email": "ivan@example.com",
  "password": "SecurePass123!"
}
```

**Ответ 200:**
```json
{
  "access": "<jwt_access_token>",
  "refresh": "<jwt_refresh_token>"
}
```

**Ошибка 401:**
```json
{ "detail": "no_active_account_found" }
```

---

### POST `/api/accounts/token/refresh/`
Обновление access-токена по refresh-токену (вызывается автоматически при 401).

**Тело запроса:**
```json
{ "refresh": "<jwt_refresh_token>" }
```

**Ответ 200:**
```json
{ "access": "<новый_access_token>" }
```

---

### POST `/api/accounts/logout/`
Выход из системы. Инвалидирует `refresh` токен, добавляя его в черный список.

**Тело запроса:**
```json
{
  "refresh": "<jwt_refresh_token>"
}
```

**Ответ 200:**
```json
{
  "detail": "Successfully logged out."
}
```

**Ошибки:**
- `401 Unauthorized`: если не передан валидный `access` токен в заголовке `Authorization`.
- `400 Bad Request`: если `refresh` токен не передан, невалиден или уже просрочен.

> **Важно:** После успешного запроса клиентское приложение должно удалить оба токена (`access` и `refresh`) из своего локального хранилища.

---

### GET `/api/accounts/me/`
Получить профиль текущего пользователя.

**Ответ 200:**
```json
{
  "id": 1,
  "username": "ivan",
  "email": "ivan@example.com",
  "portion_size": 2,
  "diet_type": 1,
  "diet_type_name": "Классическое",
  "allergies": [2, 5],
  "allergies_names": ["Орехи", "Лактоза"]
}
```

> `diet_type: null` означает, что пользователь ещё не прошёл настройку профиля.
> Используй это для определения, нужно ли показывать онбординг.

---

### PATCH `/api/accounts/me/`
Обновить профиль текущего пользователя. Принимает любое подмножество полей.

**Тело запроса:**
```json
{
  "diet_type": 1,
  "portion_size": 2,
  "allergies": [2, 5]
}
```

- `diet_type` — ID из `/api/accounts/diet-types/`
- `portion_size` — количество персон (целое число, min 1)
- `allergies` — массив ID из `/api/accounts/allergies/` (пустой массив `[]` — нет исключений)

**Ответ 200:** тот же формат что и GET `/api/accounts/me/`

---

### GET `/api/accounts/diet-types/`
Список доступных типов питания.

**Ответ 200:**
```json
[
  { "id": 1, "name": "Классическое" },
  { "id": 2, "name": "Вегетарианское" },
  { "id": 3, "name": "Кето" },
  { "id": 4, "name": "Без глютена" }
]
```

> Данные загружаются через Django Admin или фикстуры. Список статичный.

---

### GET `/api/accounts/allergies/`
Список доступных аллергенов/исключений.

**Ответ 200:**
```json
[
  { "id": 1, "name": "Рыба" },
  { "id": 2, "name": "Орехи" },
  { "id": 3, "name": "Лактоза" }
]
```

---

## Рецепты (`/api/recipes/`)

### GET `/api/recipes/`
Список всех рецептов. Поддерживает поиск: `?search=борщ`

**Ответ 200:**
```json
[
  {
    "id": 1,
    "title": "Борщ",
    "cook_time": 60,
    "servings": 4,
    "total_calories": 1200.0,
    "total_proteins": 45.0,
    "total_fats": 30.0,
    "total_carbs": 150.0
  }
]
```

---

### GET `/api/recipes/{id}/`
Полный рецепт с ингредиентами, шагами и КБЖУ.

**Ответ 200:**
```json
{
  "id": 1,
  "title": "Борщ",
  "cook_time": 60,
  "servings": 4,
  "total_calories": 1200.0,
  "total_proteins": 45.0,
  "total_fats": 30.0,
  "total_carbs": 150.0,
  "per_serving_calories": 300.0,
  "per_serving_proteins": 11.25,
  "per_serving_fats": 7.5,
  "per_serving_carbs": 37.5,
  "ingredients": [
    {
      "ingredient_name": "Свёкла",
      "amount": 300.0,
      "unit_name": "г"
    }
  ],
  "steps": [
    { "step_number": 1, "description": "Нарезать свёклу соломкой" }
  ]
}
```

---

### GET `/api/recipes/ingredients/`
Список ингредиентов. Поддерживает поиск: `?search=морковь`

**Ответ 200:**
```json
[
  { "id": 5, "name": "Морковь", "category": "Овощи" }
]
```

---

### GET `/api/recipes/categories/`
Категории ингредиентов.

**Ответ 200:**
```json
[
  { "id": 1, "name": "Овощи" },
  { "id": 2, "name": "Молочные продукты" }
]
```

---

### GET `/api/recipes/units/`
Единицы измерения.

**Ответ 200:**
```json
[
  { "id": 1, "name": "г", "is_base": true },
  { "id": 6, "name": "мл", "is_base": true }
]
```

---

## Меню (`/api/menus/`)

Все меню и элементы меню принадлежат текущему пользователю — чужие данные недоступны.

### GET `/api/menus/`
Список меню текущего пользователя. Каждое меню содержит вложенные items.

**Ответ 200:**
```json
[
  {
    "id": 1,
    "period": "week",
    "start_date": "2026-03-10",
    "created_at": "2026-03-10T10:00:00Z",
    "items": [
      {
        "id": 1,
        "recipe": 5,
        "recipe_title": "Борщ",
        "day_offset": 0,
        "meal_type": "breakfast",
        "actual_date": "2026-03-10"
      }
    ]
  }
]
```

- `period`: `"day"` — дневное, `"week"` — недельное
- `items` — все приёмы пищи этого меню (может быть пустым если меню только создано)

---

### POST `/api/menus/`
Создать пустое меню (без рецептов). Использовать для ручной сборки меню.
Для автоматической генерации — см. `POST /api/menus/generate/` ниже.

**Тело запроса:**
```json
{
  "period": "week",
  "start_date": "2026-03-10"
}
```

> `user` прописывается автоматически из токена — передавать не нужно.

**Ответ 201:** тот же формат что и GET (с пустым `items: []`)

---

### GET/PUT/PATCH/DELETE `/api/menus/{id}/`
CRUD для конкретного меню.

---

### POST `/api/menus/generate/`
Автоматически создаёт меню и заполняет его рецептами (breakfast/lunch/dinner × кол-во дней).

**Тело запроса:**
```json
{
  "period": "week",
  "start_date": "2026-03-10",
  "diet_type": 1,
  "max_cook_time": 30,
  "exclude_allergies": [2, 5]
}
```

- `period` — обязательный: `"day"` (1 день, 3 items) или `"week"` (7 дней, 21 item)
- `start_date` — обязательный: `"YYYY-MM-DD"`
- `diet_type` — опциональный: ID из `/api/accounts/diet-types/`. Если не передан — берётся из профиля пользователя
- `max_cook_time` — опциональный: максимальное время приготовления в минутах (`30` = «до 30 минут», `null` = без ограничения)
- `exclude_allergies` — опциональный: зарезервировано для будущего

**Ответ 201:**
```json
{
  "id": 42,
  "period": "week",
  "start_date": "2026-03-10",
  "created_at": "2026-03-10T10:00:00Z",
  "items": [
    {
      "id": 1,
      "recipe": 5,
      "recipe_title": "Борщ",
      "day_offset": 0,
      "meal_type": "breakfast",
      "actual_date": "2026-03-10"
    }
  ]
}
```

**Ответ 400:** если нет рецептов для заданных параметров:
```json
{ "detail": "Нет рецептов для заданных параметров. Попробуйте изменить фильтры." }
```

> Алгоритм детерминирован: одни и те же параметры + один пользователь = одно и то же меню.

---

### GET `/api/menus/items/`
Все элементы меню текущего пользователя (все дни, все приёмы пищи).

**Ответ 200:**
```json
[
  {
    "id": 1,
    "recipe": 5,
    "recipe_title": "Борщ",
    "day_offset": 0,
    "meal_type": "breakfast",
    "actual_date": "2026-03-10"
  }
]
```

- `meal_type`: `"breakfast"`, `"lunch"`, `"dinner"`, `"snack"`, `"drink"`
- `day_offset`: 0 = первый день меню, 1 = второй день, и т.д.
- `recipe_title` — название рецепта (чтобы не делать отдельный запрос за названием)

> Внимание: поле `menu` (ID меню) в этом ответе НЕ возвращается. Чтобы получить items
> конкретного меню с ID — используй `GET /api/menus/{id}/` (items вложены в ответ).

---

### POST `/api/menus/items/`
Добавить рецепт в слот меню вручную.

**Тело запроса:**
```json
{
  "menu": 1,
  "day_offset": 0,
  "meal_type": "breakfast",
  "recipe": 5
}
```

**Ошибка 400** если слот уже занят (UniqueConstraint: menu + day_offset + meal_type).

---

### GET/PUT/PATCH/DELETE `/api/menus/items/{id}/`
CRUD для элемента меню.

---

## Корзина (`/api/cart/`)

### GET `/api/cart/`
Список покупок текущего пользователя (фильтрует отмеченные is_checked=True по умолчанию).

**Ответ 200:**
```json
[
  {
    "id": 1,
    "ingredient": 5,
    "ingredient_name": "Морковь",
    "unit": 1,
    "unit_name": "г",
    "amount": "300.00",
    "is_checked": false
  }
]
```

---

### GET `/api/cart/show_checked=true`
Продукты корзины текущего пользователя, включая те, что "есть дома" (is_checked=True).

**Ответ 200:**
```json
[
  {
    "id": 1,
    "ingredient": 5,
    "ingredient_name": "Морковь",
    "unit": 1,
    "unit_name": "г",
    "amount": "300.00",
    "is_checked": true
  }
]
```

---

### POST `/api/cart/`
Добавить ингредиент в корзину.

**Тело запроса:**
```json
{
  "ingredient": 5,
  "unit": 1,
  "amount": "300.00"
}
```

> `user` прописывается из токена автоматически.

---

### PATCH `/api/cart/{id}/`
Обновить позицию в корзине (количество или отметка «куплено»).

**Тело запроса:**
```json
{ "is_checked": true }
```

или

```json
{ "amount": "500.00" }
```

---

### DELETE `/api/cart/{id}/`
Удалить позицию из корзины.

---

### POST `/api/cart/recalculate`
Добавляет в корзину продукты из меню пользователя

**Ответ 204:**
```json
[{ "detail": "Корзина обновлена" }]
```

- `menu_id` — ID меню, ингредиенты из которого загрузить в корзину (если не указано, берется текущее меню или ближайшее будущее)

> Внимание: не возвращает продукты из корзины, нужен отдельный get-запрос на /api/cart

---

## Типичный флоу онбординга

1. `POST /api/accounts/register/` или `POST /api/accounts/token/` — получить токены
2. `GET /api/accounts/me/` — проверить `diet_type == null` (нужен онбординг?)
3. `GET /api/accounts/diet-types/` + `GET /api/accounts/allergies/` — загрузить справочники
4. `PATCH /api/accounts/me/` — сохранить выбор пользователя
5. Перейти на главный экран

## Типичный флоу генерации меню

### Быстрый (рекомендуемый):
1. `POST /api/menus/generate/` — создать и заполнить меню одним запросом
2. `GET /api/recipes/{id}/` — по каждому рецепту из items получить ингредиенты
3. `POST /api/cart/` x M — добавить ингредиенты в корзину

### Ручной (по шагам):
1. `POST /api/menus/` — создать пустое меню
2. `GET /api/recipes/` — выбрать рецепты из каталога (поиск: `?search=`)
3. `POST /api/menus/items/` x N — добавить рецепты по дням
4. `GET /api/recipes/{id}/` — получить ингредиенты для каждого рецепта
5. `POST /api/cart/` x M — добавить ингредиенты в корзину
