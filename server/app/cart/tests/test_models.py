from decimal import Decimal
from app.cart.models import CartItem
from .setup import Setup


class CartItemModelTests(Setup):

    def test_create_cart_item(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.potato,
            total_amount=Decimal(2.5),
            unit=self.unit_kg,
        )

        self.assertEqual(cart_item.total_amount, 2.50)
        self.assertEqual(cart_item.user.email, self.user.email)
        self.assertEqual(cart_item.ingredient.name, self.potato.name)
        
        expected_str = f'Ingredient ID {self.potato.id} (2.5 Unit ID {self.unit_kg.id}) for User ID {self.user.id}'
        self.assertEqual(str(cart_item), expected_str)
