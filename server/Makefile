define dump_to_file
	$(VENV_PYTHON) -c "import os; os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings'); import django; django.setup(); from django.core import management; args = '$(2)'.split(); f = open('fixtures/$(1).json', 'w', encoding='utf-8'); management.call_command('dumpdata', *args, indent=4, stdout=f); f.close()"
endef

define docker_dump_to_file
	docker-compose exec web python -c "import os; os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings'); import django; django.setup(); from django.core import management; args = '$(2)'.split(); f = open('/tmp/$(1).json', 'w', encoding='utf-8'); management.call_command('dumpdata', *args, indent=4, stdout=f); f.close()"
	docker-compose cp web:/tmp/$(1).json fixtures/$(1).json
endef

ifeq ($(OS),Linux)
    PYTHON = python3
    VENV_PYTHON = venv/bin/python
	ENV_CREATE = cp .env.example .env
else ifeq ($(OS),Windows_NT)
    PYTHON = python
    VENV_PYTHON = .\venv\Scripts\python
	ENV_CREATE = copy .env.example .env
else
    $(error Неподдерживаемая операционная система: $(OS). Сетап настроен на Linux или Windows.)
endif

help:
	@echo Доступные команды:
	@echo
	@echo Сетап для бэкендеров:
	@echo make env                 - создать .env файл из .env.example если его нет
	@echo make venv                - создать виртуальное окружение
	@echo make install             - установить зависимости из requirements.txt в виртуальное окружение
	@echo make secret_key          - сгенерировать SECRET_KEY (если установлены зависимости)
	@echo make migrate             - выполнить миграции базы данных
	@echo make load_data           - загрузить фикстуры локально
	@echo make save_data           - выгрузить данные локально в фикстуры
	@echo make run                 - запустить сервер разработки
	@echo make shell               - запустить консоль django
	@echo make test                - запустить все тесты или конкретный указав APP_NAME=app.<app_name>
	@echo Сетап с Докером:
	@echo make docker_env          - создать .env файл из .env.example для Docker (проверьте DB_HOST=db)
	@echo make docker_secret_key   - сгенерировать SECRET_KEY для Docker (нужен только докер)
	@echo make docker_up           - запустить докер контейнер
	@echo make docker_tables       - посмотреть список таблиц из бд
	@echo make docker_migrate      - применить миграции (если автоматически не применились)
	@echo make docker_load_data    - загрузить все фикстуры (из папки fixtures) в Docker
	@echo make docker_save_data    - выгрузить текущие данные из Docker в фикстуры (папка fixtures)
	@echo make docker_del          - удалить докер контейнер (с удалением томов)
	@echo
	@echo Управление данными базы данных:
	@echo make gen_10_users        - генерирует 10 тестовых пользователей и избранные для них
	@echo make del_all_users       - удаляет всех пользователей из бд
	@echo
	@echo Другие команды:
	@echo make pwrshell_utf        - переключить кодировку консоли Windows на UTF-8 (if instead of text gibberish)


# БД в PostgreSQL:
env:
	$(ENV_CREATE)
	@echo "создайте или укажите параметры существующей бд DB_NAME, DB_USER, DB_PASSWORD"
	@echo "проверьте, что DB_HOST=localhost, DB_PORT=5432 (если вы не меняли стандартный порт PostgreSQL)"

venv:
	$(PYTHON) -m venv venv

install: venv
	$(VENV_PYTHON) -m pip install -r requirements.txt

secret_key:
	$(VENV_PYTHON) manage.py shell -c "from django.core.management.utils import get_random_secret_key; print('SECRET_KEY=' + get_random_secret_key())"
	@echo "замените строку с SECRET_KEY в .env на сгенерированную"

migrate:
	$(VENV_PYTHON) manage.py makemigrations
	$(VENV_PYTHON) manage.py migrate

load_data:
	$(VENV_PYTHON) manage.py loaddata fixtures/accounts_static.json
	$(VENV_PYTHON) manage.py loaddata fixtures/meal_types_data.json
	$(VENV_PYTHON) manage.py loaddata fixtures/recipes_data.json
	$(VENV_PYTHON) manage.py loaddata fixtures/accounts_users.json
	$(VENV_PYTHON) manage.py loaddata fixtures/menus_data.json
	$(VENV_PYTHON) manage.py loaddata fixtures/cart_data.json

run:
	$(VENV_PYTHON) manage.py runserver

save_data:
	$(call dump_to_file,accounts_static,accounts.Allergy accounts.DietType)
	$(call dump_to_file,meal_types_data,menus.MealType)
	$(call dump_to_file,recipes_data,recipes)
	$(call dump_to_file,accounts_users,accounts.User accounts.UserFavorite accounts.UserStock)
	$(call dump_to_file,menus_data,menus.Menu menus.MenuItem)
	$(call dump_to_file,cart_data,cart)

shell:
	$(VENV_PYTHON) manage.py shell

test:
	$(VENV_PYTHON) manage.py test $(APP_NAME)


# Docker:
docker_env:
	$(ENV_CREATE)
	@echo "проверьте, что DB_HOST=db, остальные параметры базы данных можно не менять"

docker_secret_key:
	docker run --rm -it python:3.12-slim python -c "import secrets; print('SECRET_KEY=' + secrets.token_urlsafe(32))"
	@echo "замените строку с SECRET_KEY в .env на сгенерированную"

docker_migrate:
	docker-compose exec web python manage.py migrate

docker_load_data:
	docker-compose exec web python manage.py loaddata fixtures/accounts_static.json
	docker-compose exec web python manage.py loaddata fixtures/meal_types_data.json
	docker-compose exec web python manage.py loaddata fixtures/recipes_data.json
	docker-compose exec web python manage.py loaddata fixtures/accounts_users.json
	docker-compose exec web python manage.py loaddata fixtures/menus_data.json
	docker-compose exec web python manage.py loaddata fixtures/cart_data.json

docker_save_data:
	$(call dump_to_file,accounts_static,accounts.Allergy accounts.DietType)
	$(call dump_to_file,meal_types_data,menus.MealType)
	$(call dump_to_file,recipes_data,recipes)
	$(call dump_to_file,accounts_users,accounts.User accounts.UserFavorite accounts.UserStock)
	$(call dump_to_file,menus_data,menus.Menu menus.MenuItem)
	$(call dump_to_file,cart_data,cart)
	docker-compose exec web rm /tmp/accounts_static.json /tmp/accounts_users.json /tmp/cart_data.json /tmp/menus_data.json /tmp/recipes_data.json

docker_up:
	docker-compose up

docker_tables:
	docker exec -it smartmeal_db psql -U postgres -d smart_meal_db -c "\dt"

docker_del:
	docker-compose down -v


# Тестовые пользователи:
gen_10_users:
	$(VENV_PYTHON) manage.py fill_test_users --users 10

del_all_users:
	$(VENV_PYTHON) manage.py delete_users_data

# Другие
pwrshell_utf:
	chcp 65001
