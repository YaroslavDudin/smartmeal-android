import random

from django.core.management.base import BaseCommand
from django.contrib.auth import get_user_model
from faker import Faker

from app.recipes.models import Ingredient, Recipe, RecipeIngredient, RecipeStep
from app.accounts.models import DietType


User = get_user_model()
fake = Faker('ru-RU')


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
        ingredients_list = list(Ingredient.objects.all())
        recipe_names = [
            'Овощной суп', 'Паста карбонара', 'Куриный суп', 'Плов', 'Гречка с грибами',
            'Салат Цезарь', 'Омлет', 'Борщ', 'Щи', 'Уха',
            'Жаркое по-домашнему', 'Котлеты с пюре', 'Рыба запечённая', 'Рис с овощами',
            'Спагетти болоньезе', 'Тыквенный суп', 'Греческий салат', 'Шашлык', 'Лазанья', 'Ризотто',
            'Пельмени', 'Вареники', 'Драники', 'Запеканка творожная', 'Сырники',
            'Блины', 'Оладьи', 'Компот', 'Морс', 'Чай с лимоном'
        ]
        recipes_count = options.get('recipes')

        for _ in range(recipes_count):
            title = random.choice(recipe_names)
            recipe, _ = Recipe.objects.get_or_create(
                title=title,
                image_url=fake.image_url(),
                cook_time=random.randint(15, 90),
                servings=random.randint(1, 6),
                calories=random.randint(150, 800),
                protein=round(random.uniform(5, 40), 1),
                fat=round(random.uniform(2, 30), 1),
                carbs=round(random.uniform(10, 80), 1),
            )
            recipe.diet_types.set(random.sample(diet_types_list, k=random.randint(0, 3)))

            recipe_ingredients = random.sample(ingredients_list, k=random.randint(3, 7))
            for ingredient in recipe_ingredients:
                RecipeIngredient.objects.get_or_create(
                    recipe=recipe,
                    ingredient=ingredient,
                    amount=round(random.uniform(50, 300), 1),
                    unit=ingredient.unit
                )

            steps_count = random.randint(3, 7)
            for step_num in range(1, steps_count):
                RecipeStep.objects.get_or_create(
                    recipe=recipe,
                    step_number=step_num,
                    description=fake.paragraph(nb_sentences=2),
                    image_url=fake.image_url()
                )
