from django.db import models
from django.conf import settings

class Ingredient(models.Model):
    name = models.CharField(max_length=255)
    category = models.CharField(max_length=100)
    base_unit = models.CharField(max_length=20) # g, ml, pcs

    def __str__(self):
        return self.name

class Recipe(models.Model):
    title = models.CharField(max_length=255)
    image = models.ImageField(upload_to='recipes/', null=True, blank=True)
    diet_type = models.CharField(max_length=50)
    cook_time = models.PositiveIntegerField() # в минутах
    calories = models.PositiveIntegerField()
    protein = models.FloatField()
    fat = models.FloatField()
    carbs = models.FloatField()
    ingredients = models.ManyToManyField(Ingredient, through='RecipeIngredient')

    def __str__(self):
        return self.title

class RecipeIngredient(models.Model):
    recipe = models.ForeignKey(Recipe, on_delete=models.CASCADE)
    ingredient = models.ForeignKey(Ingredient, on_delete=models.CASCADE)
    amount = models.FloatField()
    unit = models.CharField(max_length=20)

    def __str__(self):
        return f"{self.amount} {self.unit} of {self.ingredient.name} for {self.recipe.title}"

class Menu(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    period = models.CharField(max_length=50) # week, day
    start_date = models.DateField()
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Menu for {self.user.email} starting {self.start_date}"

class MenuItem(models.Model):
    DAY_CHOICES = [
        (1, 'Monday'), (2, 'Tuesday'), (3, 'Wednesday'), 
        (4, 'Thursday'), (5, 'Friday'), (6, 'Saturday'), (7, 'Sunday')
    ]
    MEAL_TYPES = [
        ('breakfast', 'Breakfast'),
        ('lunch', 'Lunch'),
        ('dinner', 'Dinner'),
    ]

    menu = models.ForeignKey(Menu, on_delete=models.CASCADE, related_name='items')
    day_of_week = models.IntegerField(choices=DAY_CHOICES)
    meal_type = models.CharField(max_length=20, choices=MEAL_TYPES)
    recipe = models.ForeignKey(Recipe, on_delete=models.CASCADE)

class Favorite(models.Model):
    user = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    recipe = models.ForeignKey(Recipe, on_delete=models.CASCADE)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        unique_together = ('user', 'recipe')
