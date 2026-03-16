# SmartMeal Admin Panel

## 1. Настройка бэкенда (Django)

```bash
cd server
python -m venv venv
source venv/bin/activate       # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

Создай `server/.env` по образцу из `server/.env.example` — там описаны все переменные.

Применяем миграции и запускаем:

```bash
python manage.py migrate
python manage.py createsuperuser   # создаёт учётку для входа в админку
python manage.py runserver         # http://localhost:8000
```

---

## 2. Запуск фронтенда

```bash
cd server/admin-panel
npm install
npm run dev    # http://localhost:5173
```

Vite автоматически проксирует `/api/` на `http://localhost:8000`, поэтому бэкенд должен быть запущен.

---

## Вход

Открой `http://localhost:5173` и войди с учёткой суперпользователя, созданной через `createsuperuser`.
