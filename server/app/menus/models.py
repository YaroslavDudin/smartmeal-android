from datetime import timedelta

from django.db import models
from django.conf import settings
from django.core.validators import MinValueValidator, MaxValueValidator


class Period(models.TextChoices):
    DAY = 'day', 'День'
    WEEK = 'week', 'Неделя'


class MealType(models.Model):
    name = models.CharField(max_length=50, unique=True)
    order = models.IntegerField(default=0)

    def __str__(self):
        return self.name


class Menu(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='menus')
    period = models.CharField(max_length=20, choices=Period.choices)
    start_date = models.DateField()
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f'Menu for User ID {self.user_id} (start: {self.start_date}, period: {self.period})'

    class Meta:
        db_table = 'menu'
        ordering = ['-created_at']


class MenuItem(models.Model):
    menu = models.ForeignKey(Menu, on_delete=models.CASCADE, related_name='items')
    recipe = models.ForeignKey('recipes.Recipe', on_delete=models.CASCADE, related_name='menu_items')
    day_offset = models.PositiveSmallIntegerField(validators=[MinValueValidator(0), MaxValueValidator(6)])
    meal_type = models.ForeignKey(MealType, on_delete=models.PROTECT)

    class Meta:
        db_table = 'menu_item'
        indexes = [
            models.Index(fields=['menu', 'day_offset'], name='menu_day_idx'),
        ]
        constraints = [
            models.UniqueConstraint(
                fields=['menu', 'day_offset', 'meal_type'],
                name='unique_menu_day_meal_slot'
            ),
        ]

    @property
    def actual_date(self):
        return self.menu.start_date + timedelta(days=self.day_offset)

    def __str__(self):
        return f'{self.meal_type.name} on Day Offset {self.day_offset} for Menu ID {self.menu_id} - Recipe ID {self.recipe_id}'
