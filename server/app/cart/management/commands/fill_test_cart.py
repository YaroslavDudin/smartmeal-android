import random

from django.core.management.base import BaseCommand
from django.contrib.auth import get_user_model

from app.cart.models import CartItem
from app.recipes.models import Recipe


User = get_user_model()


class Command(BaseCommand):
    help = 'Заполняет базу данных записями о продуктах в корзине (случайные пользователи, случайные продукты)'
    
    def add_arguments(self, parser):
        parser .add_argument(
            '--users',
            type=int,
            default=5,
            help='Количество случайных пользователей которым добавлят продукты в корзину (по умолчанию 5)'
        )

    def handle(self, *args, **options):
        users_list = list(User.objects.all())
        recipes = Recipe.objects.all()
        add_to_users = random.sample(users_list, k=options.get('users'))
        
        for user in add_to_users:
            recipe = random.choice(recipes)
            is_checked = random.choice([True, False])
            recipe_ingredients = recipe.recipe_ingredients.all()
            for recipe_ingredient in recipe_ingredients:
                CartItem.objects.get_or_create(
                    user=user,
                    ingredient=recipe_ingredient.ingredient,
                    total_amount=recipe_ingredient.amount,
                    unit=recipe_ingredient.ingredient.unit,
                    is_checked=is_checked,
                )
        