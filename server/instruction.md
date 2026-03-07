### Запуск Backend (Django, для бэкендеров)
Откройте терминал и перейдите в папку сервера: `cd server`

#### Установка зависимостей:
1. Создайте виртуальное окружение: 
   * Windows: `python -m venv venv`
   * Mac/Linux: `python3 -m venv venv`
2. Активируйте окружение:
   * Windows: `venv\Scripts\activate`
   * Mac/Linux: `source venv/bin/activate`
3. Установите зависимости: `pip install -r requirements.txt`

#### Задать переменные окружения:
1. Скопировать файл `cp .env.exampe .env`
2. Указать значения переменных окружения

#### База данных:
1. Существующая в PostgreSQL или создать вручную новую
2. Указать имя бд, пароль и пользователя в PostgreSQL, порт (если отличается от дефолтного) в файле .env

#### Генерация secret key:
1. Запустить консоль `python manage.py shell`
2. В консоли:
```bash
from django.core.management.utils import get_random_secret_key
get_random_secret_key()
``` 
3. Скопировать сгенерированный ключ и вставить в .env SECRET_KEY

#### Старт сервера:
1. Сделайте миграции `python manage.py migrate`
2. Запустите локальный сервер: `python manage.py runserver`
3. Сервер будет доступен по адресу: `http://127.0.0.1:8000/`

---

### Запуск Backend с помощью Docker (для фронтендеров):

Этот способ гарантирует одинаковое окружение для всех разработчиков и не требует установки Python или PostgreSQL вручную.

1. **Перейдите в папку сервера**  
  ```
  cd server
  ```

2. **Настройте переменные окружения**  
  В папке `server/` есть файл-образец `.env.example`. Скопируйте его в `.env`:
  - **Windows (PowerShell):**  
    ```powershell
    Copy-Item .env.example .env
    ```
  - **Mac/Linux:**  
    ```bash
    cp .env.example .env
    ```
  **Важно:** в этом файле **не должно быть кавычек** вокруг значений

3. **Сгенерируйте секретный ключ Django**  
  - Запустите однократно контейнер без базы данных (или используйте Python вручную):
    ```bash
    docker run --rm -it python:3.12-slim python -c "import secrets; print(secrets.token_urlsafe(32))"
    ```
  - Скопируйте полученный ключ и вставьте её в `.env` вместо `your_secret_key`.

4. **Запустите контейнеры**  
  В терминале (всё ещё в папке `server/`) выполните (и подождите пока всё скачается):
  ```bash
  docker-compose up
  ```
  При первом запуске будут скачаны образы PostgreSQL и Python, созданы и запущены контейнеры.

5. **Сервер готов!**  
  - API доступно по адресу: [http://localhost:8000](http://localhost:8000)  
  - Админка Django: [http://localhost:8000/admin](http://localhost:8000/admin)  
  - Из эмулятора Android используйте `http://10.0.2.2:8000` (этот адрес уже добавлен в `ALLOWED_HOSTS`).

**Посмотреть таблицы**
  `docker exec -it smartmeal_db psql -U postgres -d smart_meal_db -c "\dt"`

**Примените миграции (если они не выполнились автоматически)**  
  В другом окне терминала (или остановив `docker-compose` с помощью `Ctrl+C` и запустив снова) можно выполнить:
  `docker-compose exec web python manage.py migrate`
  Эта команда создаст все необходимые таблицы в базе данных.

**Остановка сервера**  
  Нажмите `Ctrl+C` в терминале с `docker-compose up`

**Удаление контейнера (вместе со всеми данными внутри бд)**
  ```bash
  docker-compose down -v
  ```

---