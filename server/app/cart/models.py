from django.db import models
from django.conf import settings

class CartItem(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='cart_items')
    ingredient = models.ForeignKey('recipes.Ingredient', on_delete=models.CASCADE, related_name='in_carts')
    total_amount = models.DecimalField(max_digits=8, decimal_places=2)
    unit = models.ForeignKey('recipes.Unit', on_delete=models.SET_NULL, null=True)
    is_checked = models.BooleanField(default=False)

    def __str__(self):
        return f"Cart: {self.ingredient.name} ({self.total_amount}) for {self.user}"
