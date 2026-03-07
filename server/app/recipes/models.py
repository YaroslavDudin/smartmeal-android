from django.db import models


class IngredientCategory(models.Model):
    name = models.CharField(max_length=100, unique=True)

    class Meta:
        db_table = 'ingredient_category'

    def __str__(self):
        return self.name
    

class Unit(models.Model):
    name = models.CharField(max_length=100, unique=True)

    class Meta:
        db_table = 'unit'

    def __str__(self):
        return self.name


class Ingredient(models.Model):
    name = models.CharField(max_length=255)
    category = models.ForeignKey(IngredientCategory, on_delete=models.RESTRICT, related_name='ingredients')
    unit = models.ForeignKey(Unit, on_delete=models.RESTRICT, related_name='ingredients')

    class Meta:
        db_table = 'ingredient'

    def __str__(self):
        return self.name


class Recipe(models.Model):
    title = models.CharField(max_length=255)
    image_url = models.CharField(max_length=255, blank=True, null=True)
    cook_time = models.IntegerField(help_text="Время приготовления в минутах")
    servings = models.IntegerField(default=1)
    calories = models.IntegerField()
    protein = models.DecimalField(max_digits=5, decimal_places=1)
    fat = models.DecimalField(max_digits=5, decimal_places=1)
    carbs = models.DecimalField(max_digits=5, decimal_places=1)
    
    diet_types = models.ManyToManyField('accounts.DietType', related_name='recipes')

    class Meta:
        db_table = 'recipe'

    def __str__(self):
        return self.title


class RecipeIngredient(models.Model):
    recipe = models.ForeignKey(Recipe, on_delete=models.CASCADE, related_name='recipe_ingredients')
    ingredient = models.ForeignKey(Ingredient, on_delete=models.CASCADE, related_name='used_in_recipes')
    amount = models.DecimalField(max_digits=8, decimal_places=2)
    unit = models.ForeignKey(Unit, on_delete=models.RESTRICT)

    class Meta:
        db_table = 'recipe_ingredient'
        unique_together = [['recipe', 'ingredient']]

    def __str__(self):
        return f"{self.amount} {self.unit} of {self.ingredient.name} for {self.recipe.title}"


class RecipeStep(models.Model):
    recipe = models.ForeignKey(Recipe, on_delete=models.CASCADE, related_name='steps')
    step_number = models.IntegerField()
    description = models.TextField()
    image_url = models.CharField(max_length=255, blank=True, null=True)

    class Meta:
        db_table = 'recipe_step'
        ordering = ['step_number']
        unique_together = [['recipe', 'step_number']]

    def __str__(self):
        return f"Step {self.step_number} for {self.recipe.title}"
