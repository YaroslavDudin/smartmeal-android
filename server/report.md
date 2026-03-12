# SmartMeal Backend - отчёт по рефакторингу

Привет команда. Прошёлся по всему бэку, нашёл несколько вещей которые нужно было починить прямо сейчас, и заодно привёл код в порядок. Ниже - что именно менял и почему.

---

## Что было сломано и как я это чинил

### Критика (то что вообще не работало)

**Django Admin падал при открытии любого пользователя**

В `accounts/admin.py` было написано `diet_types` вместо `diet_type`. Опечатка в одну букву, но Django Admin при этом бросал `ImproperlyConfigured` и вся страница пользователя была недоступна. Поправил на правильное имя поля.

**JWT-токены не генерировались**

В `settings.py` в блоке `SIMPLE_JWT` была строчка `'USER_ID_FIELD': 'email'`. Звучит логично - мы же логинимся по email - но это не тот параметр. `USER_ID_FIELD` говорит Django что положить в payload токена как `user_id`, и у User нет поля с именем `email` в роли первичного ключа. Авторизация через email настраивается совсем иначе - через `User.USERNAME_FIELD = 'email'`, что у нас уже было. Итог: строчку удалил, по дефолту используется `id` (PK), и всё заработало.

---

### Тесты (все были сломаны по одной причине)

Все тесты падали с 404 - оказалось, в них были захардкожены URL `/auth/register/` и `/auth/login/`, которых никогда не существовало. Реальные URL: `/api/accounts/register/` и `/api/accounts/token/`. Исправил.

Дополнительно нашёл два момента:

* В тесте регистрации не передавалось поле `password_confirm` - а сериализатор его требует. Тест на успешную регистрацию возвращал 400, при этом тест на слабый пароль случайно проходил (тоже получал 400, просто по другой причине). Добавил `password_confirm` в оба теста.
* `CurrentUserView` (`/me/`) был написан, но не был подключён в URLconf. Тест на защищённый маршрут получал 404 вместо 401. Добавил `me/` в `urls.py` и заодно подключил `UserViewSet` через router - он тоже висел в воздухе.

---

### Двойная аутентификация при логине

В `accounts/serializers.py` в методе `validate()` был такой код:

```python
user = authenticate(email=attrs['email'], password=attrs['password'])
# ... и потом ...
return super().validate(attrs)

```

Проблема в том, что `super().validate()` внутри сам вызывает `authenticate()`. То есть каждый логин - это два запроса аутентификации вместо одного. Убрал ручной вызов, оставил только `super().validate()` и обёртку для кастомного сообщения об ошибке.

---

### Схема базы данных

**`menus.Menu` - не было поля `created_at**`

В ER-диаграмме оно есть, в модели не было. Добавил `DateTimeField(auto_now_add=True)` и заодно выставил `ordering = ['-created_at']` чтобы меню по умолчанию шли от новых к старым. Написал миграцию `0005_audit_refactor.py`.

**`menus.MenuItem` - можно было добавить два завтрака в один день**

Не было `UniqueConstraint` на комбинацию `(menu, day_offset, meal_type)`. Добавил - теперь один слот на один тип приёма пищи в конкретный день.

**`recipes.UnitConversion` - опечатка в названии таблицы**

`db_table = 'unit_convertion'` - пропущена буква `s`. Казалось бы мелочь, но это ломает любые прямые SQL-запросы и документацию. Исправил на `unit_conversion`, добавил в миграцию `AlterModelTable`.

**`recipes.RecipeIngredient` - можно было добавить один ингредиент дважды**

Был `UniqueConstraint(fields=['recipe', 'ingredient', 'unit'])`. Это позволяло добавить, например, "100г муки" и "2 стакана муки" как две отдельные записи. Убрал `unit` из constraint - теперь один ингредиент = одна запись на рецепт.

---

### Производительность (N+1 в Recipe)

**Было:** каждое из свойств `total_proteins`, `total_fats`, `total_carbs`, `total_calories`, `total_weight_g` вызывало `self.recipe_ingredients.all()` отдельно. То есть при отображении полного рецепта - 5 одинаковых запросов к базе.

**Стало:** добавил приватный метод `_get_nutrition_totals()`, который проходит по ингредиентам **один раз** и кладёт результат в `self._nutrition_cache`. Все `total_*` и `per_serving_*` свойства теперь читают из кэша.

Также добавил `RecipeQuerySet.with_prefetched_ingredients()` - используй его в `RecipeViewSet.get_queryset()` чтобы не ловить N+1 при запросе списка рецептов.

**`RecipeIngredient._get_grams()` - хардкод строк**

Раньше там была проверка `if self.unit.name.lower() in ['г', 'г.', 'грамм', 'g', 'gram']`. Это хрупко - добавишь новую базовую единицу и придётся лезть в код. Добавил поле `Unit.is_base = BooleanField(default=False)`. Теперь базовые единицы помечаются в админке или через фикстуру.

---

### Мелочи в settings и views

* `ALLOWED_HOSTS` - `'*'` теперь добавляется только когда `DEBUG=True`. В production задаёшь точный список через env-переменную `ALLOWED_HOSTS`. Комментарий с объяснением там же.
* `STATIC_ROOT` - без него `collectstatic` падает. Добавил `BASE_DIR / 'staticfiles'`.
* `RegisterView` возвращал вручную собранный `{'username': ..., 'email': ...}`. Теперь возвращает `UserSerializer(user).data` - тот же формат, что и `/me/`. Не надо синхронизировать два места при изменении сериализатора.

---

## Что добавил после рефакторинга

### CORS

Добавил `django-cors-headers`. В `MIDDLEWARE` - выше `CommonMiddleware`, как того требует документация. В DEBUG разрешены все origins (удобно при тестировании с реального телефона). В production - только `10.0.2.2:8000`, `localhost:8000`, `127.0.0.1:8000`.

### API для рецептов, меню и корзины

Все три `views.py` были пустыми заглушками. Реализовал:

**Рецепты** - `/api/recipes/`

| Эндпоинт | Что делает |
| --- | --- |
| `GET /api/recipes/` | Список рецептов (поиск по `?search=`) |
| `GET /api/recipes/{id}/` | Полный рецепт с ингредиентами, шагами и КБЖУ |
| `GET /api/recipes/ingredients/` | Список ингредиентов (поиск по `?search=`) |
| `GET /api/recipes/categories/` | Категории ингредиентов |
| `GET /api/recipes/units/` | Единицы измерения |

Рецепты пока только на чтение (`ReadOnlyModelViewSet`) - добавлять рецепты через API не предполагается, они грузятся через фикстуры или Django Admin.

**Меню** - `/api/menus/`

| Эндпоинт | Что делает |
| --- | --- |
| `GET /api/menus/` | Меню текущего пользователя |
| `POST /api/menus/` | Создать меню (user прописывается автоматически из токена) |
| `GET/PUT/PATCH/DELETE /api/menus/{id}/` | CRUD для меню |
| `GET /api/menus/items/` | Элементы меню текущего пользователя |
| `POST /api/menus/items/` | Добавить рецепт в меню |
| `GET/PUT/PATCH/DELETE /api/menus/items/{id}/` | CRUD для элементов меню |

**Корзина** - `/api/cart/`

| Эндпоинт | Что делает |
| --- | --- |
| `GET /api/cart/` | Список покупок текущего пользователя |
| `POST /api/cart/` | Добавить ингредиент (user из токена) |
| `PATCH /api/cart/{id}/` | Обновить количество или отметить купленным (`is_checked`) |
| `DELETE /api/cart/{id}/` | Удалить из корзины |

Все вьюхи фильтруют данные по текущему пользователю - никто не видит чужие меню и корзины.

---

## Сводная таблица всех правок

| Приоритет | Файл | Что сделал |
| --- | --- | --- |
| P0 | `accounts/admin.py` | `diet_types` → `diet_type` (опечатка роняла Django Admin) |
| P0 | `config/settings.py` | Удалил `USER_ID_FIELD: 'email'` из SIMPLE_JWT (ломал генерацию токенов) |
| P1 | `accounts/serializers.py` | Убрал двойную аутентификацию при логине |
| P1 | `accounts/tests/test_registration.py` | Исправил URL, добавил `password_confirm` |
| P1 | `accounts/tests/test_login.py` | Исправил URL (`/auth/login/` → `/api/accounts/token/`) |
| P2 | `menus/models.py` | Добавил `Menu.created_at`, UniqueConstraint для `MenuItem` |
| P2 | `menus/migrations/0005_audit_refactor.py` | Миграция для изменений Menu/MenuItem |
| P2 | `recipes/models.py` | Опечатка `unit_convertion`, упростил constraint RecipeIngredient |
| P3 | `recipes/models.py` | N+1: кэш `_nutrition_cache`, `with_prefetched_ingredients()`, `Unit.is_base` |
| P3 | `recipes/migrations/0008_audit_refactor.py` | Миграция для is_base, table rename, constraint |
| P4 | `config/settings.py` | `*` в ALLOWED_HOSTS только при DEBUG, STATIC_ROOT, CORS |
| P4 | `accounts/views.py` | `RegisterView` использует `UserSerializer` |
| P4 | `accounts/urls.py` | Подключил `CurrentUserView` (`me/`) и `UserViewSet` |
| NEW | `recipes/serializers.py` | Новый файл - сериализаторы для рецептов и ингредиентов |
| NEW | `recipes/views.py` | ViewSet-ы для Recipe, Ingredient, Unit, Category |
| NEW | `recipes/urls.py` | URL-ы для recipes API |
| NEW | `menus/serializers.py` | Новый файл - сериализаторы для Menu и MenuItem |
| NEW | `menus/views.py` | ViewSet-ы для Menu и MenuItem |
| NEW | `menus/urls.py` | URL-ы для menus API |
| NEW | `cart/serializers.py` | Новый файл - сериализатор для CartItem |
| NEW | `cart/views.py` | ViewSet для CartItem |
| NEW | `cart/urls.py` | URL-ы для cart API |
| NEW | `requirements.txt` | Добавлен `django-cors-headers` |
| FIX | `requirements.txt` | `psycopg2` → `psycopg2-binary` |
| FIX | `fixtures/menus_data.json` | Добавлено поле `created_at` во все записи `menus.menu` |
| FIX | `fixtures/recipes_data.json` | Исправлены единицы измерения (стакан -> г) и добавлены недостающие данные |
| FIX | `recipes/models.py` | `_get_grams()` и `protein/fat/carbs` больше не падают при отсутствии данных |

### Фикстура `menus_data.json` - добавлено поле `created_at`

Фикстура была создана до того как мы добавили поле `created_at` в модель `Menu`, поэтому при `loaddata` падала с ошибкой `null value in column "created_at"`. Причина: `auto_now_add=True` при загрузке фикстур не срабатывает - Django вставляет данные напрямую в базу минуя логику модели.

Добавили `"created_at": "2026-03-10T00:00:00Z"` во все записи `menus.menu` скриптом. Если будете пересоздавать фикстуру через `dumpdata` - поле уже будет там автоматически.

**Правило на будущее:** если добавляете поле с `auto_now_add=True` или `default` - не забудьте обновить существующие фикстуры, иначе `loaddata` упадёт.

---

---

### Django Admin падал на странице рецептов

После загрузки фикстур `/admin/recipes/recipe/` возвращал 500. Проблема была не одна - их оказалось три слоя.

**Слой 1 - фикстура `recipes_data.json` была сгенерирована с рандомными единицами**

131 запись в `RecipeIngredient` использовала единицу "стакан" (unit_id=7), но без соответствующей `UnitConversion`. Ещё 15 записей использовали "стакан" с существующей конверсией, но с суммами вроде "233.4 стакана гречки" или "254.4 стакана молока" - это явно граммы, не стаканы.

Короче, фикстура была сгенерирована автоматически и единицы расставлены рандомно. Все такие записи переведены на unit_id=1 (г) скриптом.

**Слой 2 - `is_base` не сохранялся в фикстуру**

Поле `Unit.is_base` было добавлено при рефакторинге, но в `recipes_data.json` оно не сериализовалось (по умолчанию `False`). После `loaddata` даже единица "г" считалась не базовой, `_get_grams()` искал несуществующую конверсию и возвращал 0. Добавили `"is_base": true` для г (pk=1) и мл (pk=6) прямо в фикстуру.

**Слой 3 - у ингредиента "Манка" не было данных о питательности**

Единственный ингредиент в рецептах без `IngredientNutrition`. Добавили запись в фикстуру: protein=10.3, fat=1.0, carbs=73.3, base_weight_g=100.

**Защита в коде**

Помимо фикса данных - сделали код устойчивым к неполным данным:
- `_get_grams()`: при отсутствии конверсии возвращает `Decimal(0)` вместо `raise ValueError`
- `protein / fat / carbs` в `RecipeIngredient`: при отсутствии `IngredientNutrition` возвращают `Decimal(0)` вместо крашить всё

**Правило на будущее:** если добавляете поле с `default` или `BooleanField` - проверьте что фикстуры его сериализуют. Django не добавляет поля в фикстуру если они имеют значение по умолчанию и не были явно выставлены.

---

### Почему `psycopg2` заменён на `psycopg2-binary`

`psycopg2` компилируется из C-исходников при установке и требует системные заголовки PostgreSQL (`libpq-dev`, `pg_config.h`). Если их нет - `pip install` падает с ошибкой компилятора.

`psycopg2-binary` - это тот же пакет, только с уже скомпилированным бинарником внутри. Никакой компиляции, никаких системных зависимостей. Интерфейс и импорты абсолютно идентичны - в коде ничего менять не нужно.

Для production-деплоя на серверах где есть `libpq-dev` можно вернуть обычный `psycopg2` - он чуть надёжнее на специфичных Linux-окружениях. Но для разработки `psycopg2-binary` - стандартная практика.