from decimal import Decimal
from datetime import date, timedelta
from django.test import TestCase
from django.contrib.auth import get_user_model
from rest_framework.test import APITestCase
from rest_framework import status
from app.cart.models import CartItem
from app.recipes.models import Ingredient, IngredientNutrition, Unit, UnitConversion, IngredientCategory, Recipe, RecipeIngredient
from app.menus.models import Menu, MenuItem, MealType, Period

User = get_user_model()

class CartItemModelTests(TestCase):

    def setUp(self):
        self.user = User.objects.create_user(
            username='test_buyer',
            email='buyer@test.com',
            password='password123'
        )
        
        self.unit = Unit.objects.create(name='кг')
        self.category = IngredientCategory.objects.create(name='Овощи')
        self.ingredient = Ingredient.objects.create(
            name='Картофель', 
            category=self.category,
            can_be_added_to_cart=True,
        )

    def test_create_cart_item(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            total_amount=Decimal(2.5),
            unit=self.unit,
            is_checked=False
        )

        self.assertEqual(cart_item.total_amount, 2.50)
        self.assertFalse(cart_item.is_checked)
        self.assertEqual(cart_item.user.email, 'buyer@test.com')
        self.assertEqual(cart_item.ingredient.name, 'Картофель')
        
        expected_str = f'Ingredient ID {self.ingredient.id} (2.5 Unit ID {self.unit.id}) for User ID {self.user.id}'
        self.assertEqual(str(cart_item), expected_str)


class CartItemAPITest(APITestCase):

    def setUp(self):
        self.user = User.objects.create_user(
            username='test_buyer',
            email='buyer@test.com',
            password='password123',
        )
        
        self.unit = Unit.objects.create(name='кг')
        self.base_unit_g = Unit.objects.create(name='г', is_base=True)
        self.category = IngredientCategory.objects.create(name='Овощи')
        self.ingredient = Ingredient.objects.create(
            name='Картофель', 
            category=self.category,
            can_be_added_to_cart=True,
        )
        IngredientNutrition.objects.create(
            ingredient=self.ingredient,
            base_unit=self.base_unit_g,
            base_weight=100,
            protein=2,
            fat=0.1,
            carbs=16,
        )
        self.unit_convertion = UnitConversion.objects.create(
            ingredient=self.ingredient,
            from_unit=self.unit,
            to_unit=self.base_unit_g,
            amount_per_unit=1000,
        )
        self.recipe = Recipe.objects.create(title='Суп', cook_time=30, servings=2)
        RecipeIngredient.objects.create(
            recipe=self.recipe,
            ingredient=self.ingredient,
            amount = 2,
            unit=self.unit
        )
        self.lunch_type = MealType.objects.create(name='lunch', order=1)

        self.client.force_authenticate(user=self.user)
        self.base_url = '/api/cart/'

    def test_get_cart_items_grouped(self):
        response_empty = self.client.get(self.base_url)
        self.assertEqual(response_empty.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response_empty.data), 0)
        
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            total_amount=1,
            unit=self.unit,
            is_checked=False,
        )
        response = self.client.get(self.base_url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('Овощи', response.data)
        self.assertEqual(len(response.data['Овощи']), 1)
        item_data = response.data['Овощи'][0]
        self.assertEqual(item_data['id'], cart_item.pk)
        self.assertEqual(item_data['ingredient_name'], cart_item.ingredient.name)
        self.assertEqual(Decimal(item_data['total_amount']), cart_item.total_amount)
        self.assertEqual(item_data['unit_name'], cart_item.unit.name)
        self.assertEqual(item_data['is_checked'], cart_item.is_checked)

    def test_get_cart_items_filter_checked(self):
        ingredient2 = Ingredient.objects.create(name='Морковь', category=self.category)
        CartItem.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            total_amount=1,
            unit=self.unit,
            is_checked=True
        )
        unchecked_item = CartItem.objects.create(
            user=self.user,
            ingredient=ingredient2,
            total_amount=2,
            unit=self.unit,
            is_checked=False
        )

        response = self.client.get(self.base_url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('Овощи', response.data)
        self.assertEqual(len(response.data['Овощи']), 1)
        self.assertEqual(response.data['Овощи'][0]['id'], unchecked_item.pk)
    
    def test_get_cart_items_not_filter_checked_if_query_param(self):
        ingredient2 = Ingredient.objects.create(name='Морковь', category=self.category)
        CartItem.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            total_amount=1,
            unit=self.unit,
            is_checked=False
        )
        CartItem.objects.create(
            user=self.user,
            ingredient=ingredient2,
            total_amount=2,
            unit=self.unit,
            is_checked=True
        )

        response = self.client.get(f'{self.base_url}?show_checked=true')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('Овощи', response.data)
        self.assertEqual(len(response.data['Овощи']), 2)

    def test_get_cart_items_unauthenticated(self):
        self.client.force_authenticate(user=None)

        response = self.client.get(self.base_url)
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_create_cart_item_success(self):
        data = {
            'ingredient': self.ingredient.id,
            'total_amount': 1.5,
            'unit': self.unit.id,
            'is_checked': False,
        }

        response = self.client.post(self.base_url, data, format='json')

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(CartItem.objects.count(), 1)

        cart_item = CartItem.objects.get(pk=response.data['id'])
        self.assertEqual(cart_item.user, self.user)
        self.assertEqual(cart_item.ingredient, self.ingredient)
        self.assertEqual(cart_item.total_amount, 1.5)
        self.assertEqual(cart_item.unit, self.unit)
        self.assertFalse(cart_item.is_checked)
    
    def test_create_cart_item_default_is_checked(self):
        data = {
            'ingredient': self.ingredient.id,
            'total_amount': 1.5,
            'unit': self.unit.id,
        }
        
        response = self.client.post(self.base_url, data, format='json')
        cart_item = CartItem.objects.get(pk=response.data['id'])
        self.assertFalse(cart_item.is_checked)
    
    def test_create_cart_item_with_ingredient_add_to_cart_false(self):
        ingredient = Ingredient.objects.create(
            name='Грустная свекла', 
            category=self.category,
            can_be_added_to_cart=False,
        )
        data = {
            'ingredient': ingredient.id,
            'total_amount': 1.5,
            'unit': self.unit.id,
            'is_checked': False,
        }

        response = self.client.post(self.base_url, data, format='json')

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('can_be_added_to_cart', response.data)
        self.assertEqual(CartItem.objects.count(), 0)

    def test_create_cart_item_missing_ingredient(self):
        data = {
            'total_amount': 1.0,
            'unit': self.unit.id,
            'is_checked': True,
        }
        response = self.client.post(self.base_url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('ingredient', response.data)

    def test_create_cart_item_duplicate(self):
        CartItem.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            total_amount=1,
            unit=self.unit,
            is_checked=True,
        )
        data = {
            'ingredient': self.ingredient.id,
            'total_amount': 2.0,
            'unit': self.unit.id,
            'is_checked': True,
        }

        response = self.client.post(self.base_url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
    
    def test_patch_cart_item_amount(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            total_amount=1,
            unit=self.unit,
            is_checked=True,
        )

        cart_item_url = f'{self.base_url}{cart_item.pk}/'
        response = self.client.patch(cart_item_url, {'total_amount': 6.0}, format='json')
        cart_item.refresh_from_db()

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(cart_item.total_amount, 6.0)
        
    def test_patch_cart_item_is_checked(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            total_amount=1,
            unit=self.unit,
            is_checked=False,
        )

        cart_item_url = f'{self.base_url}{cart_item.pk}/'
        response = self.client.patch(cart_item_url, {'is_checked': True}, format='json')
        cart_item.refresh_from_db()
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(cart_item.is_checked)
        
    def test_delete_cart_item(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            total_amount=1,
            unit=self.unit,
            is_checked=True,
        )
        self.assertEqual(CartItem.objects.count(), 1)
        
        cart_item_url = f'{self.base_url}{cart_item.pk}/'
        response = self.client.delete(cart_item_url)
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(CartItem.objects.filter(pk=cart_item.pk).exists())

    def test_cart_recalculate_for_week_plan(self):
        menu = Menu.objects.create(
            user=self.user, 
            period=Period.WEEK, 
            start_date=date.today(),
        )
        MenuItem.objects.create(
            menu=menu, 
            recipe=self.recipe, 
            day_offset=0, 
            meal_type=self.lunch_type
        )
        CartItem.objects.all().delete()

        recalculate_url = f'{self.base_url}recalculate/'
        response = self.client.post(recalculate_url)

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertEqual(CartItem.objects.count(), 1)
        cart_item = CartItem.objects.first()
        self.assertEqual(cart_item.ingredient, self.ingredient)
        self.assertEqual(cart_item.total_amount, 2000.0)
        
        response_cart = self.client.get(self.base_url)
        self.assertIn('Овощи', response_cart.data)
        self.assertEqual(len(response_cart.data['Овощи']), 1)
        self.assertEqual(response_cart.data['Овощи'][0]['ingredient_name'], 'Картофель')

    def test_cart_recalculate_for_day_plan(self):
        menu = Menu.objects.create(
            user=self.user,
            period=Period.DAY,
            start_date=date.today(),
        )
        MenuItem.objects.create(
            menu=menu,
            recipe=self.recipe,
            day_offset=0,
            meal_type=self.lunch_type
        )

        recalculate_url = f'{self.base_url}recalculate/'
        response = self.client.post(recalculate_url)

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertEqual(CartItem.objects.count(), 1)
        cart_item = CartItem.objects.first()
        self.assertEqual(cart_item.ingredient, self.ingredient)
        self.assertEqual(cart_item.total_amount, 2000.0) # 2.0 кг = 2000 г

    def test_cart_recalculate_for_week_plan_early_start_date(self):
        timedelta_days = 3
        start_date = date.today() - timedelta(days=timedelta_days)
        menu = Menu.objects.create(
            user=self.user,
            period=Period.WEEK,
            start_date=start_date,
        )
        for day_offset in range(7):
            MenuItem.objects.create(
                menu=menu,
                recipe=self.recipe,
                day_offset=day_offset,
                meal_type=self.lunch_type
            )

        recalculate_url = f'{self.base_url}recalculate/'
        response = self.client.post(recalculate_url)

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        # Каждый рецепт содержит 2 кг картошки, и этот рецепт добавлен на каждый день
        # Должно добавиться картошки на сегодняшний и оставшиеся дни
        expected_total = 2000 * (7 - timedelta_days)  # 8000 г
        self.assertEqual(CartItem.objects.count(), 1)
        cart_item = CartItem.objects.first()
        self.assertEqual(cart_item.ingredient, self.ingredient)
        self.assertEqual(cart_item.total_amount, expected_total)

    def test_cart_recalculate_updates_existing_unchecked_item(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            total_amount=1000,
            unit=self.base_unit_g,
            is_checked=False,
        )

        menu = Menu.objects.create(
            user=self.user,
            period=Period.DAY,
            start_date=date.today(),
        )
        MenuItem.objects.create(
            menu=menu,
            recipe=self.recipe,
            day_offset=0,
            meal_type=self.lunch_type,
        )

        recalculate_url = f'{self.base_url}recalculate/'
        response = self.client.post(recalculate_url)

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)

        cart_item.refresh_from_db()
        self.assertEqual(cart_item.total_amount, 3000) # было 1000 г, прибавилось 2 кг
        self.assertEqual(cart_item.unit, self.base_unit_g)
        self.assertFalse(cart_item.is_checked)
        self.assertEqual(CartItem.objects.count(), 1)
        
    def test_cart_recalculate_replaces_existing_checked_item(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            total_amount=1000,
            unit=self.base_unit_g,
            is_checked=True,
        )

        menu = Menu.objects.create(
            user=self.user,
            period=Period.DAY,
            start_date=date.today(),
        )
        MenuItem.objects.create(
            menu=menu,
            recipe=self.recipe,
            day_offset=0,
            meal_type=self.lunch_type,
        )

        recalculate_url = f'{self.base_url}recalculate/'
        response = self.client.post(recalculate_url)

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)

        cart_item.refresh_from_db()
        self.assertEqual(cart_item.total_amount, 2000) # было 1000 г, заменилось на 2 кг в г
        self.assertEqual(CartItem.objects.count(), 1)

    def test_cart_recalculate_no_unit_convertion(self):
        new_ingredient = Ingredient.objects.create(
            name='Мука',
            category=self.category,
            can_be_added_to_cart=True,
        )

        IngredientNutrition.objects.create(
            ingredient=new_ingredient,
            base_unit=self.base_unit_g,
            base_weight=100,
            protein=10,
            fat=1,
            carbs=70
        )

        cup_unit = Unit.objects.create(name='стакан')
        new_recipe = Recipe.objects.create(title='Блины', cook_time=20, servings=4)
        RecipeIngredient.objects.create(
            recipe=new_recipe,
            ingredient=new_ingredient,
            amount=2,
            unit=cup_unit,
        )
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=new_ingredient,
            total_amount=1000,
            unit=self.base_unit_g,
            is_checked=False,
        )
        # Создаём меню с этим рецептом
        menu = Menu.objects.create(
            user=self.user,
            period=Period.DAY,
            start_date=date.today(),
        )
        MenuItem.objects.create(
            menu=menu,
            recipe=new_recipe,
            day_offset=0,
            meal_type=self.lunch_type
        )

        recalculate_url = f'{self.base_url}recalculate/'
        response = self.client.post(recalculate_url)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        cart_item.refresh_from_db()
        self.assertEqual(cart_item.total_amount, 1000)
        self.assertEqual(cart_item.unit, self.base_unit_g)
    
    def test_cart_recalculate_exclude_ingredient_with_add_to_cart_false(self):
        not_cart_ingredient = Ingredient.objects.create(
            name='Грустная свекла', 
            category=self.category,
            can_be_added_to_cart=False,
        )
        recipe = Recipe.objects.create(title='Борщ', cook_time=30, servings=2)
        RecipeIngredient.objects.create(
            recipe=recipe,
            ingredient=not_cart_ingredient,
            amount=2,
            unit=self.unit
        )
        RecipeIngredient.objects.create(
            recipe=recipe,
            ingredient=self.ingredient,
            amount=2,
            unit=self.unit
        )
        menu = Menu.objects.create(
            user=self.user,
            period=Period.DAY,
            start_date=date.today(),
        )
        MenuItem.objects.create(
            menu=menu,
            recipe=recipe,
            day_offset=0,
            meal_type=self.lunch_type,
        )

        recalculate_url = f'{self.base_url}recalculate/'
        response = self.client.post(recalculate_url)
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertEqual(CartItem.objects.count(), 1)

        response_cart = self.client.get(self.base_url)
        self.assertIn('Овощи', response_cart.data)
        vegetables = response_cart.data['Овощи']
        vegetables_names = [v['ingredient_name'] for v in vegetables]
        self.assertEqual(len(vegetables_names), 1)
        self.assertIn(self.ingredient.name, vegetables_names)
        self.assertNotIn(not_cart_ingredient.name, vegetables_names)
