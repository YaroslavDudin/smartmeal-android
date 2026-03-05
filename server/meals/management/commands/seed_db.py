import random
from django.core.management.base import BaseCommand
from accounts.models import Allergy
from meals.models import Ingredient, Recipe, RecipeIngredient

class Command(BaseCommand):
    help = 'Seeds the database with initial data'

    def handle(self, *args, **kwargs):
        self.stdout.write('Seeding data...')

        # 1. Allergies
        allergies_data = ['Nuts', 'Dairy', 'Gluten', 'Seafood', 'Soy']
        allergies = []
        for name in allergies_data:
            allergy, created = Allergy.objects.get_or_create(name=name)
            allergies.append(allergy)
        self.stdout.write(f'Created {len(allergies)} allergies.')

        # 2. Ingredients
        ingredients_data = [
            ('Chicken Breast', 'Meat', 'g'),
            ('Salmon', 'Fish', 'g'),
            ('Beef', 'Meat', 'g'),
            ('Rice', 'Grain', 'g'),
            ('Pasta', 'Grain', 'g'),
            ('Avocado', 'Fruit', 'pcs'),
            ('Tomato', 'Vegetable', 'pcs'),
            ('Spinach', 'Vegetable', 'g'),
            ('Eggs', 'Dairy', 'pcs'),
            ('Milk', 'Dairy', 'ml'),
            ('Olive Oil', 'Oil', 'ml'),
            ('Garlic', 'Vegetable', 'pcs'),
            ('Onion', 'Vegetable', 'pcs'),
            ('Quinoa', 'Grain', 'g'),
            ('Tofu', 'Protein', 'g'),
            ('Lentils', 'Legume', 'g'),
            ('Almonds', 'Nut', 'g'),
            ('Greek Yogurt', 'Dairy', 'g'),
            ('Bell Pepper', 'Vegetable', 'pcs'),
            ('Broccoli', 'Vegetable', 'g'),
        ]
        ingredients = []
        for name, category, unit in ingredients_data:
            ing, created = Ingredient.objects.get_or_create(name=name, category=category, base_unit=unit)
            ingredients.append(ing)
        self.stdout.write(f'Created {len(ingredients)} ingredients.')

        # 3. Recipes (20)
        diet_types = ['classic', 'vegetarian', 'vegan', 'keto', 'paleo']
        
        for i in range(1, 21):
            recipe_title = f'Meal Recipe {i}'
            diet = random.choice(diet_types)
            cook_time = random.randint(15, 60)
            calories = random.randint(300, 800)
            protein = random.uniform(15, 50)
            fat = random.uniform(10, 30)
            carbs = random.uniform(20, 100)

            recipe, created = Recipe.objects.get_or_create(
                title=recipe_title,
                defaults={
                    'diet_type': diet,
                    'cook_time': cook_time,
                    'calories': calories,
                    'protein': round(protein, 1),
                    'fat': round(fat, 1),
                    'carbs': round(carbs, 1),
                }
            )

            if created:
                # Add 3-5 random ingredients to each recipe
                num_ingredients = random.randint(3, 5)
                selected_ingredients = random.sample(ingredients, num_ingredients)
                for ing in selected_ingredients:
                    RecipeIngredient.objects.create(
                        recipe=recipe,
                        ingredient=ing,
                        amount=random.uniform(10, 500) if ing.base_unit in ['g', 'ml'] else random.randint(1, 3),
                        unit=ing.base_unit
                    )

        self.stdout.write(self.style.SUCCESS('Successfully seeded database!'))
