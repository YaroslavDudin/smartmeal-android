# SmartMeal: Экосистема умного планирования питания

**SmartMeal** — это современное Full-stack решение для автоматизации диетического планирования, управления рецептами и оптимизации процесса покупки продуктов. Проект объединяет высокопроизводительный **REST API** на Django и нативное **Android-приложение** на Jetpack Compose.

---

## 🏗 Архитектура системы

Проект спроектирован с разделением ответственности (Separation of Concerns) и готов к масштабированию:

- **Backend (API)**: Модульное Django-приложение. Бизнес-логика инкапсулирована в сервисный слой и модели, обеспечивая чистоту контроллеров (ViewSets).
- **Frontend (Android)**: Нативное приложение с использованием декларативного UI. Архитектура построена на принципах Single Activity и реактивного управления состоянием.
- **Инфраструктура**: Полная контейнеризация через Docker, автоматизация рутинных задач через Makefile и строгий контроль качества через CI/CD.

---

## 🛠 Технологический стек

### Бэкенд (Server)
- **Runtime**: Python 3.12+
- **Framework**: Django 6.0 + Django REST Framework (DRF)
- **Database**: PostgreSQL (реляционное хранилище со сложными связями ингредиентов и нутриентов)
- **Auth**: Stateless JWT (SimpleJWT)
- **Tools**: Docker & Docker Compose, Makefile для автоматизации (Task Runner)
- **Testing**: Pytest + pytest-django (Unit & Integration tests)

### Мобильный клиент (Android)
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3 (современный адаптивный дизайн)
- **Networking**: Retrofit 2 + OkHttp + Gson
- **DI/Architecture**: ViewModel, Navigation Compose
- **Build System**: Gradle (Kotlin DSL) + Version Catalogs (libs.versions.toml)

---

## 📂 Структура репозитория

```text
smartmeal-android/
├── client/SmartMeal/      # Нативное Android-приложение (Kotlin)
│   ├── app/               # Основной модуль приложения
│   └── gradle/            # Конфигурация сборки и каталог зависимостей
├── server/                # Серверная часть (Django REST API)
│   ├── app/               # Доменные приложения (accounts, recipes, menus, cart)
│   ├── config/            # Системные настройки и маршрутизация
│   ├── fixtures/          # Тестовые данные (JSON) для быстрой инициализации
│   └── docker-compose.yml # Описание контейнеров (App, DB)
└── .github/workflows/     # Автоматизация (Android lint/build & Backend tests)
```

---

## 🚦 Быстрый старт (Development)

### 1. Подготовка Бэкенда
Убедитесь, что у вас установлен Docker и Make.

```bash
cd server
# Создание конфигурации окружения
cp .env.example .env

# Сборка и запуск контейнеров в фоне
make docker_up

# Применение миграций и загрузка начальных данных (фикстур)
make docker_migrate
make docker_load_data
```
*API будет доступен по адресу: `http://localhost:8000`*

### 2. Подготовка Android-клиента
1. Откройте директорию `client/SmartMeal` в **Android Studio (Koala+)**.
2. Дождитесь завершения Gradle Sync.
3. Убедитесь, что в эмуляторе настроен проброс портов (для локального API используйте `10.0.2.2`).
4. Запустите конфигурацию `app`.

---

## 🧪 Качество и тестирование

Проект следует стандартам автоматизированного тестирования:

- **Backend**: Используется `pytest`. Покрыты ключевые сценарии: регистрация, генерация меню и логика корзины.
  - Запуск: `cd server && pytest`
- **Frontend**: Интегрированы статические анализаторы кода (lint).
- **CI/CD**: При каждом Pull Request запускаются workflow:
  - `android-tests.yml`: сборка и верификация APK.
  - `backend-tests.yml`: запуск тестов Django и проверка стиля кода.

---

## 📖 Документация API
Подробное описание эндпоинтов, форматов запросов и схем ответов находится в файле [`server/api.md`](server/api.md).

---

## 🛡 Безопасность и конвенции
- **JWT**: Все запросы к защищенным эндпоинтам требуют `Authorization: Bearer <token>`.
- **Environment**: Секреты (SECRET_KEY, DB_PASS) никогда не коммитятся в репозиторий и управляются через `.env`.
- **Code Style**: Применяются стандарты PEP8 для Python и Kotlin Style Guide.

---
© 2026 SmartMeal Team. Разработано с фокусом на качество и производительность.
