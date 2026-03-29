from django.db import models
from django.conf import settings
from django.core.validators import MinValueValidator


class CartItem(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='cart_items')
    ingredient = models.ForeignKey('recipes.Ingredient', on_delete=models.CASCADE, related_name='in_carts')
    total_amount = models.DecimalField(max_digits=8, decimal_places=2, validators=[MinValueValidator(0.01)])
    unit = models.ForeignKey('recipes.Unit', on_delete=models.RESTRICT)
    is_checked = models.BooleanField(default=False)
    
    class Meta:
        db_table = 'cart_item'
        verbose_name = 'Продукт в корзине'
        verbose_name_plural = 'Продукты в корзинах'
        indexes = [
            models.Index(fields=['user', 'ingredient']),
            models.Index(fields=['is_checked']),
        ]
        constraints = [
            models.UniqueConstraint(fields=['user', 'ingredient'], name='unique_user_cart_item')
        ]

    def __str__(self):
        return f'Ingredient ID {self.ingredient_id} ({self.total_amount} Unit ID {self.unit_id}) for User ID {self.user_id}'
