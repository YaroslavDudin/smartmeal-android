import random
import datetime

from django.core.management.base import BaseCommand
from django.contrib.auth import get_user_model

from app.menus.models import Menu, MenuItem
from app.recipes.models import Recipe


User = get_user_model()


def create_menu_item(menu, recipes, day):
    for meal_type in MenuItem.MEAL_TYPE_CHOICES:
        return MenuItem.objects.get_or_create(
            menu=menu,
            recipe=random.choice(recipes),
            day=day,
            meal_type=meal_type,
        )


class Command(BaseCommand):
    help = 'Заполняет базу данных рандомными меню для всех пользователей'
    
    def handle(self, *args, **options):
        users = User.objects.all()
        recipes = Recipe.objects.all()
        start_date = datetime.date.today()
        
        for user in users:
            period = random.choice(Menu.PERIOD_CHOICES)
            menu = Menu.objects.get_or_create(
                user=user,
                period=period,
                start_date=start_date
            )
            if period == 'day':
                create_menu_item(menu, recipes, start_date)
            elif period == 'week':
                for i in range(7):
                    create_menu_item(menu, recipes, start_date + datetime.timedelta(days=i))
