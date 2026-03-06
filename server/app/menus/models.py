from django.db import models
from django.conf import settings

class Menu(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name='menus')
    period = models.CharField(max_length=20)
    start_date = models.DateField()

    def __str__(self):
        return f"Menu for {self.user} ({self.period})"

class MenuItem(models.Model):
    menu = models.ForeignKey(Menu, on_delete=models.CASCADE, related_name='items')
    recipe = models.ForeignKey('recipes.Recipe', on_delete=models.CASCADE, related_name='menu_items')
    day = models.DateField()
    meal_type = models.CharField(max_length=50)

    def __str__(self):
        return f"{self.meal_type} on {self.day} - {self.recipe.title}"