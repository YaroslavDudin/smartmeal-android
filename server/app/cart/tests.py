from django.test import TestCase
from django.contrib.auth import get_user_model
from app.cart.models import CartItem
from app.recipes.models import Ingredient, Unit

User = get_user_model()

class CartItemModelTests(TestCase):

    def setUp(self):
        self.user = User.objects.create_user(
            username='test_buyer',
            email='buyer@test.com',
            password='password123'
        )
        
        self.unit = Unit.objects.create(name='кг')
        self.ingredient = Ingredient.objects.create(
            name='Картофель', 
            unit=self.unit
        )

    def test_create_cart_item(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            total_amount=2.50,
            unit=self.unit,
            is_checked=False
        )

        self.assertEqual(cart_item.total_amount, 2.50)
        self.assertFalse(cart_item.is_checked)
        self.assertEqual(cart_item.user.email, 'buyer@test.com')
        self.assertEqual(cart_item.ingredient.name, 'Картофель')
        
        expected_str = f"Cart: Картофель (2.5) for {self.user}"
        self.assertEqual(str(cart_item), expected_str)
