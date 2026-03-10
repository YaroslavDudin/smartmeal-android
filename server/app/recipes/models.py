from django.db import models
from django.core.validators import MinValueValidator, MaxValueValidator, URLValidator


calories_per_gram = {
    'protein': 4.0,
    'carbs': 4.0,
    'fat': 9.0,
}


class IngredientCategory(models.Model):
    '''Категория ингредиентов (например: фрукты, овощи, мясо, молочные продукты).'''
    name = models.CharField(max_length=100, unique=True)

    class Meta:
        db_table = 'ingredient_category'
        ordering = ['name']

    def __str__(self):
        return self.name
    

class Unit(models.Model):
    '''Единица измерения ингредиента (грамм, стакан, штука, столовая ложка и т.п.).'''
    name = models.CharField(max_length=100, unique=True)

    class Meta:
        db_table = 'unit'
        ordering = ['name']

    def __str__(self):
        return self.name


class UnitConversion(models.Model):
    '''Конвертация нестандартной единицы измерения в граммы для конкретного ингредиента.'''
    ingredient = models.ForeignKey('Ingredient', on_delete=models.CASCADE, related_name='unit_conversions')
    unit = models.ForeignKey(Unit, on_delete=models.CASCADE, related_name='in_grams')
    grams_per_unit = models.DecimalField(max_digits=8, decimal_places=2, help_text='Эквивалент в граммах')
    
    class Meta:
        db_table = 'unit_convertion'
        ordering = ['grams_per_unit']
        unique_together = ('ingredient', 'unit')
    
    def __str__(self):
        return f'Weight of {self.ingredient} in grams: {self.grams_per_unit}'


class Ingredient(models.Model):
    '''Базовый продукт/ингредиент (например: яблоко, куриная грудка, мука).'''
    name = models.CharField(max_length=255, unique=True)
    category = models.ForeignKey(IngredientCategory, on_delete=models.RESTRICT, related_name='ingredients')

    class Meta:
        db_table = 'ingredient'
        ordering = ['name']

    def __str__(self):
        return self.name


class IngredientNutrition(models.Model):
    '''Пищевая ценность ингредиента (КБЖУ) на указанную массу (обычно 100 г).'''
    ingredient = models.OneToOneField(Ingredient, on_delete=models.CASCADE, related_name='ingredient_nutrition')
    base_weight_g = models.PositiveSmallIntegerField(default=100, help_text='Масса, для которой указаны КБЖУ (в граммах)')
    protein = models.DecimalField(max_digits=5, decimal_places=1, validators=[MinValueValidator(0)])
    fat = models.DecimalField(max_digits=5, decimal_places=1, validators=[MinValueValidator(0)])
    carbs = models.DecimalField(max_digits=5, decimal_places=1, validators=[MinValueValidator(0)])
    
    class Meta:
        db_table = 'ingredient_nutrition'

    @property
    def calories(self):
        return self.protein * calories_per_gram['protein'] + \
            self.carbs * calories_per_gram['carbs'] + \
            self.fat * calories_per_gram['fat']

    def __str__(self):
        return f'{self.ingredient} имеет {self.protein} г белка, {self.carbs} г углеводов и {self.fat} г жира, калорийность на {self.base_weight_g}: {self.calories}'


class Recipe(models.Model):
    '''Рецепт блюда с заголовком, временем приготовления, количеством порций и связанными диетическими типами.'''
    title = models.CharField(max_length=255)
    image_url = models.CharField(max_length=255, blank=True, null=True, validators=[URLValidator()])
    cook_time = models.PositiveIntegerField(help_text='Время приготовления в минутах')
    servings = models.PositiveSmallIntegerField(default=1)

    diet_types = models.ManyToManyField('accounts.DietType', related_name='recipes')

    class Meta:
        db_table = 'recipe'
        indexes = [
            models.Index(fields=['title']),
        ]
        ordering = ['title']

    @property
    def total_weight_g(self):
        '''Общий вес рецепта в граммах (сумма весов ингредиентов)'''
        return sum(ri.amount_in_grams for ri in self.recipe_ingredients.all())

    @property
    def total_proteins(self):
        '''Общее количество белков в рецепте (в граммах)'''
        return sum(ri.protein for ri in self.recipe_ingredients.all())

    @property
    def total_fats(self):
        '''Общее количество жиров в рецепте'''
        return sum(ri.fat for ri in self.recipe_ingredients.all())

    @property
    def total_carbs(self):
        '''Общее количество углеводов в рецепте'''
        return sum(ri.carbs for ri in self.recipe_ingredients.all())

    @property
    def total_calories(self):
        '''Общая калорийность рецепта (ккал)'''
        return sum(ri.calories for ri in self.recipe_ingredients.all())

    @property
    def per_serving_proteins(self):
        '''Белки на одну порцию'''
        return self.total_proteins / self.servings if self.servings else 0

    @property
    def per_serving_fats(self):
        '''Жиры на одну порцию'''
        return self.total_fats / self.servings if self.servings else 0

    @property
    def per_serving_carbs(self):
        '''Углеводы на одну порцию'''
        return self.total_carbs / self.servings if self.servings else 0

    @property
    def per_serving_calories(self):
        '''Калории на одну порцию'''
        return self.total_calories / self.servings if self.servings else 0

    def __str__(self):
        return self.title


class RecipeIngredient(models.Model):
    '''Промежуточная модель для связи рецепта с ингредиентами, их количеством и единицей измерения.'''
    recipe = models.ForeignKey(Recipe, on_delete=models.CASCADE, related_name='recipe_ingredients')
    ingredient = models.ForeignKey(Ingredient, on_delete=models.CASCADE, related_name='used_in_recipes')
    amount = models.DecimalField(max_digits=8, decimal_places=2, validators=[MinValueValidator(0.01)])
    unit = models.ForeignKey(Unit, on_delete=models.RESTRICT)

    class Meta:
        db_table = 'recipe_ingredient'
        unique_together = ('recipe', 'ingredient', 'unit')

    def _get_grams(self):
        try:
            conversion = UnitConversion.objects.get(ingredient=self.ingredient, unit=self.unit)
            return float(self.amount) * float(conversion.grams_per_unit)
        except UnitConversion.DoesNotExist:
                raise ValueError(f'Нет конверсии для {self.ingredient} в {self.unit}')

    @property
    def amount_in_grams(self):
        return self._get_grams()
    
    @property
    def nutrition(self):
        try:
            return self.ingredient.nutrition
        except IngredientNutrition.DoesNotExist:
            raise ValueError(f'Отсутствует пищевая ценность для ингредиента {self.ingredient}')

    @property
    def protein(self):
        grams = self.amount_in_grams
        nutrition = self.ingredient.ingredient_nutrition
        return (grams / nutrition.base_weight_g) * nutrition.protein

    @property
    def fat(self):
        grams = self.amount_in_grams
        nutrition = self.ingredient.ingredient_nutrition
        return (grams / nutrition.base_weight_g) * nutrition.fat

    @property
    def carbs(self):
        grams = self.amount_in_grams
        nutrition = self.ingredient.ingredient_nutrition
        return (grams / nutrition.base_weight_g) * nutrition.carbs

    @property
    def calories(self):
        return self.protein * calories_per_gram['protein'] + \
            self.carbs * calories_per_gram['carbs'] + \
            self.fat * calories_per_gram['fat']

    def __str__(self):
        return f'{self.amount} {self.unit} ингредиента {self.ingredient} для {self.recipe.title}'


class RecipeStep(models.Model):
    '''Шаг приготовления рецепта: порядковый номер, описание и опциональное изображение.'''
    recipe = models.ForeignKey(Recipe, on_delete=models.CASCADE, related_name='steps')
    step_number = models.PositiveSmallIntegerField(validators=[MinValueValidator(1)])
    description = models.TextField()
    image_url = models.CharField(max_length=255, blank=True, null=True, validators=[URLValidator()])

    class Meta:
        db_table = 'recipe_step'
        ordering = ['step_number']
        unique_together = ('recipe', 'step_number')

    def __str__(self):
        return f'Шаг №{self.step_number} для {self.recipe.title}'
