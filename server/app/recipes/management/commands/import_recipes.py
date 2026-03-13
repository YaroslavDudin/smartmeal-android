import json
import os
from django.core.management.base import BaseCommand
from django.db import transaction
from django.conf import settings
from app.recipes.models import Unit, Ingredient, Recipe, RecipeIngredient, RecipeStep
from app.accounts.models import DietType

class Command(BaseCommand):
    help = 'Импорт рецептов из JSON-файла (Data Seeder)'

    def add_arguments(self, parser):
        parser.add_argument(
            '--file',
            type=str,
            default='data/seed_recipes.json',
            help='Путь к JSON файлу с рецептами'
        )

    def handle(self, *args, **options):
        if not Ingredient.objects.exists():
            self.stdout.write(self.style.ERROR('В базе нет ингредиентов! Сначала запустите: python manage.py fill_recipes_static'))
            return

        file_path = os.path.join(settings.BASE_DIR, options['file'])
        
        if not os.path.exists(file_path):
            self.stdout.write(self.style.ERROR(f'Файл не найден: {file_path}'))
            return

        with open(file_path, 'r', encoding='utf-8') as file:
            try:
                raw_recipes = json.load(file)
            except json.JSONDecodeError:
                self.stdout.write(self.style.ERROR('Ошибка чтения JSON файла.'))
                return

        self.stdout.write(f"Найден файл. Начинаем импорт {len(raw_recipes)} рецептов...")

        with transaction.atomic():
            for r_data in raw_recipes:
                recipe, created = Recipe.objects.update_or_create(
                    title=r_data['title'],
                    defaults={
                        'cook_time': r_data['cook_time'],
                        'servings': r_data['servings'],
                        'image_url': r_data.get('image_url', '')
                    }
                )

                diet_types = []
                for dt_name in r_data.get('diet_types', []):
                    dt = DietType.objects.filter(name=dt_name).first()
                    if dt:
                        diet_types.append(dt)
                recipe.diet_types.set(diet_types)

                if not created:
                    recipe.recipe_ingredients.all().delete()
                    recipe.steps.all().delete()

                for ing_data in r_data['ingredients']:
                    try:
                        ingredient = Ingredient.objects.get(name=ing_data['name'])
                        unit = Unit.objects.get(name=ing_data['unit'])
                        
                        RecipeIngredient.objects.create(
                            recipe=recipe,
                            ingredient=ingredient,
                            amount=ing_data['amount'],
                            unit=unit
                        )
                    except Ingredient.DoesNotExist:
                        self.stdout.write(self.style.WARNING(f"Пропущен ингредиент '{ing_data['name']}' (не найден в справочнике)"))
                    except Unit.DoesNotExist:
                        self.stdout.write(self.style.WARNING(f"Пропущена единица '{ing_data['unit']}' (не найдена в справочнике)"))

                for step_data in r_data['steps']:
                    RecipeStep.objects.create(
                        recipe=recipe,
                        step_number=step_data['num'],
                        description=step_data['desc'],
                        timer=step_data.get('timer')
                    )

                status = "Создан" if created else "Обновлен"
                self.stdout.write(self.style.SUCCESS(f'{status} рецепт: {recipe.title}'))

        self.stdout.write(self.style.SUCCESS("✅ Импорт успешно завершен!"))
