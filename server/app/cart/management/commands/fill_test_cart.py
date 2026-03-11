import random

from django.core.management.base import BaseCommand
from django.contrib.auth import get_user_model

from app.cart.models import CartItem
from app.recipes.models import Recipe


User = get_user_model()


class Command(BaseCommand):
    help = 'Заполняет базу данных записями о продуктах в корзине'
    
    def add_arguments(self, parser):
        parser.add_argument(
            '--users',
            type=int,
            help='Количество случайных пользователей игнорируется, если указан --user'
        )
        parser.add_argument(
            '--user',
            type=int,
            help='Пользователь (id), которому нужно наполнить корзину'
        )
        parser.add_argument(
            '--recipe',
            type=int,
            help='Рецепт (id), ингредиентами которого нужно наполнить корзину'
        )

    def handle(self, *args, **options):
        users_count = options.get('users')
        user_id = options.get('user')
        recipe_id = options.get('recipe')
        add_to_users = []
        recipes = []

        all_users = list(User.objects.all())
        if not all_users:
            self.stdout.write(self.style.ERROR('Нет пользователей в базе'))
            return

        if user_id:
            try:
                add_to_users.append(User.objects.get(pk=user_id))
            except User.DoesNotExist:
                self.stdout.write(self.style.ERROR(f'Пользователь с id {user_id} не найден'))
                return
        elif users_count:
            add_to_users = random.sample(all_users, k=users_count)
        else:
            add_to_users = all_users
            
        if recipe_id:
            try:
                recipes.append(Recipe.objects.get(pk=recipe_id))
            except Recipe.DoesNotExist:
                self.stdout.write(self.style.ERROR(f'Рецепт с id {recipe_id} не найден'))
                return
        else:
            recipes = list(Recipe.objects.all())
            if not recipes:
                self.stdout.write(self.style.ERROR('Нет рецептов в базе'))
                return
        
        for user in add_to_users:
            recipe = random.choice(recipes)
            is_checked = random.choice([True, False])
            recipe_ingredients = recipe.recipe_ingredients.all()
            self.stdout.write(f'Обработка пользователя {user} с рецептом "{recipe}"')

            for recipe_ingredient in recipe_ingredients:
                cart_item, created = CartItem.objects.get_or_create(
                    user=user,
                    ingredient=recipe_ingredient.ingredient,
                    total_amount=recipe_ingredient.amount,
                    unit=recipe_ingredient.unit,
                    is_checked=is_checked,
                )
                if not created:
                    cart_item.total_amount = recipe_ingredient.amount
                    cart_item.unit = recipe_ingredient.unit
                    cart_item.save()
                    self.stdout.write(self.style.SUCCESS(f'Продукт {recipe_ingredient} обновлен'))
                else:
                    self.stdout.write(self.style.SUCCESS(f'Продукт {recipe_ingredient} добавлен в корзину'))
        