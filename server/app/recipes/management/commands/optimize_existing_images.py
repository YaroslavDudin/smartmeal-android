from django.core.management.base import BaseCommand
from app.recipes.models import Recipe, RecipeStep
from django.db import transaction

class Command(BaseCommand):
    help = 'Оптимизирует все существующие изображения рецептов и шагов'

    def handle(self, *args, **options):
        self.stdout.write('Начало оптимизации изображений рецептов...')
        
        recipes = Recipe.objects.exclude(image_url='')
        total_recipes = recipes.count()
        
        for i, recipe in enumerate(recipes):
            try:
                # Вызов save() триггерит нашу новую логику сжатия
                recipe.save()
                self.stdout.write(f'[{i+1}/{total_recipes}] Оптимизирован рецепт: {recipe.title}')
            except Exception as e:
                self.stdout.write(self.style.ERROR(f'Ошибка в рецепте {recipe.id}: {e}'))

        self.stdout.write('\nНачало оптимизации изображений шагов...')
        steps = RecipeStep.objects.exclude(image_url='')
        total_steps = steps.count()

        for i, step in enumerate(steps):
            try:
                step.save()
                self.stdout.write(f'[{i+1}/{total_steps}] Оптимизирован шаг {step.step_number} для рецепта {step.recipe_id}')
            except Exception as e:
                self.stdout.write(self.style.ERROR(f'Ошибка в шаге {step.id}: {e}'))

        self.stdout.write(self.style.SUCCESS('\nВся оптимизация успешно завершена!'))
