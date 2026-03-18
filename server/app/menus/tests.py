from django.test import TestCase
from django.contrib.auth import get_user_model
from datetime import date
from app.menus.models import Menu, MenuItem
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
            servings=1,
            calories=250,
            protein=10.0,
            fat=5.5,
            carbs=40.0
        )

        #  Создаем само меню (оно понадобится для теста MenuItem)
        self.menu = Menu.objects.create(
            user=self.user,
            period="Неделя 1",
            start_date=date.today()
        )

    def test_create_menu(self):
        # Проверяем, что меню из setUp создалось корректно
        self.assertEqual(self.menu.period, "Неделя 1")
        self.assertEqual(self.menu.user.email, 'diet@test.com')
        self.assertEqual(str(self.menu), f"Menu for {self.user} (Неделя 1)")

    def test_create_menu_item(self):
        #  Создаем пункт меню (завтрак), привязывая его к меню и рецепту
        menu_item = MenuItem.objects.create(
            menu=self.menu,
            recipe=self.recipe,
            day=date.today(),
            meal_type="Завтрак"
        )

        #  Проверяем, что пункт меню сохранился со всеми связями
        self.assertEqual(menu_item.meal_type, "Завтрак")
        self.assertEqual(menu_item.recipe.title, "Овсянка с ягодами")
        self.assertEqual(menu_item.menu.period, "Неделя 1")
        
        expected_str = f"Завтрак on {date.today()} - Овсянка с ягодами"
        self.assertEqual(str(menu_item), expected_str)

