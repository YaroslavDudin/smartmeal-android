import random
import datetime

from django.core.management.base import BaseCommand
from django.contrib.auth import get_user_model

from app.menus.models import MealType, Menu, MenuItem, Period
from app.recipes.models import Recipe


User = get_user_model()


class Command(BaseCommand):
    help = 'Заполняет базу данных случайными меню для всех пользователей'
    
    def add_arguments(self, parser):
        parser.add_argument(
            '--force',
            action='store_true',
            help='Обновлять существующие элементы меню новыми случайными рецептами'
        )

    def handle(self, *args, **options):
        force = options['force']
        users = User.objects.all()
        recipes = Recipe.objects.all()

        if not recipes:
            self.stdout.write(self.style.ERROR('Нет рецептов в базе. Создайте рецепты сначала.'))
            return

        start_date = datetime.date.today()

        for user in users:
            period_key = random.choice(Period.values)
            menu, menu_created = Menu.objects.get_or_create(
                user=user,
                period=period_key,
                start_date=start_date
            )
            if menu_created:
                self.stdout.write(f'Создано новое меню: {menu}')
            else:
                self.stdout.write(f'Меню уже существует: {menu}')

            days_range = 1 if period_key == Period.DAY else 7

            for day_offset in range(days_range):
                for meal_type in MealType.values:
                    recipe = random.choice(recipes)
                    item, item_created = MenuItem.objects.get_or_create(
                        menu=menu,
                        day_offset=day_offset,
                        meal_type=meal_type,
                        recipe=recipe
                    )
                    if item_created:
                        self.stdout.write(f'Прием пищи {item} добавлен')
                    elif force:
                        item.recipe = recipe
                        item.save()
                        self.stdout.write(f'Прием пищи {item} обновлен')
