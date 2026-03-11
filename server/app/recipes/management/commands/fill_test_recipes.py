import random

from django.core.management.base import BaseCommand
from django.contrib.auth import get_user_model

from app.recipes.models import Unit, UnitConversion, Ingredient, Recipe, RecipeIngredient, RecipeStep
from app.accounts.models import DietType


User = get_user_model()
cooking_adj = [
    'Жареный', 'Тушёный', 'Запечённый', 'Варёный', 'Солёный',
    'Маринованный', 'Копчёный', 'Вяленый', 'Паровой', 'Фаршированный',
    'Свежий', 'Острый', 'Сладкий', 'Кислый', 'Пряный', 'Сливочный',
    'Овощной', 'Мясной', 'Рыбный', 'Грибной', 'Сырный', 'Шоколадный',
    'Фруктовый', 'Ягодный', 'Ореховый', 'Медовый','Карамельный',
    'Пикантный', 'Нежный', 'Хрустящий'
]
cooking_verbs = [
    'Нарежьте', 'Измельчите', 'Обжарьте', 'Отварите', 'Запеките', 
    'Добавьте', 'Перемешайте', 'Взбейте', 'Посолите', 'Поперчите',
    'Посыпьте', 'Полейте', 'Накройте крышкой', 'Тушите', 'Остудите',
    'Готовьте', 'Дайте настояться', 'Влейте', 'Подавайте'
]
title_template = [
    '{adj} {ingredient} под {adj2} {ingredient2}',
    '{adj} {ingredient} с {ingredient2}',
    '{adj} {ingredient}'
]
step_templates = [
    '{action} {ingredient} в течение {time}.',
    '{action} {ingredient} и {adj2} {ingredient2}.',
    '{action} {ingredient2} при температуре {temperature} на протяжении {time} .'
]


class Command(BaseCommand):
    help = 'Заполняет базу данных сгенерированными рецептами'
    
    def add_arguments(self, parser):
        parser.add_argument(
            '--recipes',
            type=int,
            default=10,
            help='Количество обычных рецептов для создания (по умолчанию 10)'
        )

    def handle(self, *args, **options):
        diet_types_list = list(DietType.objects.all())
        if not diet_types_list:
            self.stdout.write(self.style.WARNING('Типы питания не найдены'))

        ingredients_list = list(Ingredient.objects.all())
        if not ingredients_list:
            self.stdout.write(self.style.ERROR('В базе нет ингредиентов'))
            return
        
        unit_conversions = UnitConversion.objects.all()
        if not unit_conversions:
            unit = Unit.objects.get_or_create(name='г')
        else:
            unit = random.choice(unit_conversions).unit

        recipes_count = options.get('recipes')

        for _ in range(recipes_count):
            ingredients_count = random.randint(3, 7)
            ingredients_for_recipe = random.sample(ingredients_list, k=ingredients_count)
            ingredients_names = [ing.name for ing in ingredients_for_recipe]
            title = random.choice(title_template).format(**{
                'ingredient': ingredients_names[0],
                'ingredient2': ingredients_names[1],
                'adj': random.choice(cooking_adj),
                'adj2': random.choice(cooking_adj),
            })
            recipe, _ = Recipe.objects.get_or_create(
                title=title,
                image_url='',
                cook_time=random.randint(15, 90),
                servings=random.randint(1, 6),
            )
            diet_types_count = random.randint(0, 3)
            recipe.diet_types.set(random.sample(diet_types_list, k=diet_types_count))

            for ingredient in ingredients_for_recipe:
                RecipeIngredient.objects.create(
                    recipe=recipe,
                    ingredient=ingredient,
                    amount=round(random.uniform(50, 300), 1),
                    unit=unit
                )

            steps_count = random.randint(5, 10)
            for step_num in range(1, steps_count):
                description = random.choice(step_templates).format(**{
                    'ingredient': random.choice(ingredients_names),
                    'ingredient2': random.choice(ingredients_names),
                    'adj': random.choice(cooking_adj),
                    'adj2': random.choice(cooking_adj),
                    'time': random.randint(1, 60),
                    'temperature': random.randint(50, 300),
                    'action': random.choice(cooking_verbs),
                })
                RecipeStep.objects.get_or_create(
                    recipe=recipe,
                    step_number=step_num,
                    description=description,
                    image_url=''
                )
