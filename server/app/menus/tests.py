from django.test import TestCase
from django.contrib.auth import get_user_model
from datetime import date
from app.menus.models import Menu, MenuItem, MealType, Period
from app.recipes.models import Recipe

User = get_user_model()

class MenusModelTests(TestCase):
    def setUp(self):
        #  Создаем пользователя, для которого будем делать меню
        self.user = User.objects.create_user(
            username='diet_user',
            email='diet@test.com',
            password='password123'
        )
        
        #  Создаем рецепт, который добавим в меню
        self.recipe = Recipe.objects.create(
            title="Овсянка с ягодами",
            cook_time=15,
            servings=1
        )

        #  Создаем само меню (оно понадобится для теста MenuItem)
        self.menu = Menu.objects.create(
            user=self.user,
            period=Period.WEEK,
            start_date=date.today()
        )

    def test_create_menu(self):
        # Проверяем, что меню из setUp создалось корректно
        self.assertEqual(self.menu.period, Period.WEEK)
        self.assertEqual(self.menu.user.email, 'diet@test.com')
        expected_str = f'Меню для {self.user} (начало: {date.today()}, длительность: {Period.WEEK})'
        self.assertEqual(str(self.menu), expected_str)

    def test_create_menu_item(self):
        #  Создаем пункт меню (завтрак), привязывая его к меню и рецепту
        menu_item = MenuItem.objects.create(
            menu=self.menu,
            recipe=self.recipe,
            day_offset=0,
            meal_type=MealType.BREAKFAST
        )

        #  Проверяем, что пункт меню сохранился со всеми связями
        self.assertEqual(menu_item.meal_type, MealType.BREAKFAST)
        self.assertEqual(menu_item.recipe.title, "Овсянка с ягодами")
        self.assertEqual(menu_item.menu.period, Period.WEEK)
        
        expected_str = f"Завтрак на {date.today()} - Овсянка с ягодами"
        self.assertEqual(str(menu_item), expected_str)

