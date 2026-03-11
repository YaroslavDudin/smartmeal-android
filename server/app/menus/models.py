from datetime import timedelta

from django.db import models
from django.conf import settings
from django.core.validators import MinValueValidator, MaxValueValidator


class Period(models.TextChoices):
    DAY = 'day', 'День'
    WEEK = 'week', 'Неделя'


class MealType(models.TextChoices):
    BREAKFAST = 'breakfast', 'Завтрак'
    LUNCH = 'lunch', 'Обед'
    DINNER = 'dinner', 'Ужин'
    SNACK = 'snack', 'Перекус'
    DRINK = 'drink', 'Напиток'


class Menu(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='menus')
    period = models.CharField(max_length=20, choices=Period.choices)
    start_date = models.DateField()

    def __str__(self):
        return f'Меню для {self.user} (начало: {self.start_date}, длительность: {self.period})'
    
    class Meta:
        db_table = 'menu'


class MenuItem(models.Model):
    MEAL_TYPE_CHOICES = [
        ('beakfast', 'Breakfast'),
        ('lunch', 'Lunch'),
        ('dinner', 'Dinner'),
        ('snack', 'Snack'),
        ('drink', 'Drink'),
    ]

    menu = models.ForeignKey(Menu, on_delete=models.CASCADE, related_name='items')
    recipe = models.ForeignKey('recipes.Recipe', on_delete=models.CASCADE, related_name='menu_items')
    day_offset = models.PositiveSmallIntegerField(validators=[MinValueValidator(0), MaxValueValidator(6)])
    meal_type = models.CharField(max_length=50, choices=MealType.choices)

    class Meta:
        db_table = 'menu_item'
        indexes = [
            models.Index(fields=['menu', 'day_offset'], name='menu_day_idx'),
        ]
    
    @property
    def actual_date(self):
        start_date = self.menu.start_date
        return start_date + timedelta(days=self.day_offset)

    def __str__(self):
        return f'{self.get_meal_type_display()} на {self.actual_date} - {self.recipe.title}'
