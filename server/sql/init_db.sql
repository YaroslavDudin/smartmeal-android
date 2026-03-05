CREATE TABLE allergies(
	id SERIAL PRIMARY KEY NOT NULL,
	name VARCHAR(255) UNIQUE NOT NULL
);

-- новая таблица, вынесенные типы питания
CREATE TABLE diet_types(
	id SERIAL PRIMARY KEY NOT NULL,
	name VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE users(
	id SERIAL PRIMARY KEY NOT NULL,
	email VARCHAR(255) UNIQUE NOT NULL,
	password_hash VARCHAR(255) NOT NULL,
	portion_size INTEGER NOT NULL DEFAULT 1,
	created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE user_allergies(
	user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
	allergy_id INTEGER NOT NULL REFERENCES allergies(id) ON DELETE CASCADE,
	PRIMARY KEY(user_id, allergy_id)
);

-- новая таблица, пользователь + типы диет
CREATE TABLE user_diet_types(
	user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
	diet_type_id INTEGER NOT NULL REFERENCES diet_types(id) ON DELETE CASCADE,
	PRIMARY KEY(user_id, diet_type_id)
);

CREATE TABLE recipes(
	id SERIAL PRIMARY KEY NOT NULL,
	title VARCHAR(255) NOT NULL,
	image_url VARCHAR(255) NOT NULL,
	cook_time INTEGER NOT NULL, -- в минутах
	servings INTEGER NOT NULL DEFAULT 1, -- новое поле, количество порций
	calories INTEGER NOT NULL,
	protein DECIMAL(5, 1) NOT NULL,
	fat DECIMAL(5, 1) NOT NULL,
	carbs DECIMAL(5, 1) NOT NULL
);

-- новая таблица, рецепты + типы диет
CREATE TABLE recipe_diet_types(
	recipe_id INTEGER NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
	diet_type_id INTEGER NOT NULL REFERENCES diet_types(id) ON DELETE CASCADE,
	PRIMARY KEY(recipe_id, diet_type_id)
);

-- новая таблица, категории ингредиентов
CREATE TABLE ingredient_categories(
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);

-- новая таблица, единицы измерения
CREATE TABLE units(
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE ingredients(
	id SERIAL PRIMARY KEY NOT NULL,
	name VARCHAR(255) NOT NULL,
	category_id INTEGER NOT NULL REFERENCES ingredient_categories(id) ON DELETE RESTRICT, -- ссылка на новую ingredient_categories таблицу вместо значения
	unit_id INTEGER NOT NULL REFERENCES units(id) ON DELETE RESTRICT -- ссылка на новую таблицу units вместо значения
);

CREATE TABLE recipe_ingredients(
	recipe_id INTEGER NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
	ingredient_id INTEGER NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE,
	amount DECIMAL(8, 2) NOT NULL,
	unit_id INTEGER NOT NULL REFERENCES units(id) ON DELETE RESTRICT, -- ссылка на новую таблицу units вместо значения
	PRIMARY KEY (recipe_id, ingredient_id)
);

-- новая таблица с шагами для рецептов
CREATE TABLE recipe_steps(
    id SERIAL PRIMARY KEY,
    recipe_id INTEGER NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL,
    description TEXT NOT NULL,
    image_url VARCHAR(255),
    UNIQUE (recipe_id, step_number)
);

CREATE TABLE menus(
	id SERIAL PRIMARY KEY NOT NULL,
	user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
	period VARCHAR(20) NOT NULL CHECK (period IN ('day', 'week')),
	start_date DATE NOT NULL
);

CREATE TABLE menu_items(
	menu_id INTEGER NOT NULL REFERENCES menus(id) ON DELETE CASCADE,
	recipe_id INTEGER NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
	day DATE NOT NULL,
	meal_type VARCHAR(50) NOT NULL CHECK (meal_type IN ('breakfast', 'lunch', 'dinner', 'snack', 'drink')),
	PRIMARY KEY (menu_id, day, meal_type)
);

-- Дополнительные таблицы по тз:
CREATE TABLE user_favorites(
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipe_id INTEGER NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (user_id, recipe_id)
);

CREATE TABLE cart_items(
    id SERIAL PRIMARY KEY NOT NULL,
	user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
	ingredient_id INTEGER NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE,
	total_amount DECIMAL(8, 2) NOT NULL,
	unit_id INTEGER NOT NULL REFERENCES units(id) ON DELETE RESTRICT,
	is_checked BOOL NOT NULL DEFAULT True -- по умолчанию товары выбраны в корзине
);
