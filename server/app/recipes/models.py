from decimal import Decimal
from django.db import models
from django.db.models import Sum, F
from django.core.validators import MinValueValidator, URLValidator


CALORIES_PER_GRAM = {
    'protein': Decimal('4.0'),
    'carbs': Decimal('4.0'),
    'fat': Decimal('9.0'),
}


class IngredientCategory(models.Model):
    '''Категория ингредиентов (например: фрукты, овощи, мясо, молочные продукты).'''
    name = models.CharField(max_length=100, unique=True)

    class Meta:
        db_table = 'ingredient_category'
        verbose_name = 'Категория ингредиента'
        verbose_name_plural = 'Категории ингредиентов'
        ordering = ['name']

    def __str__(self):
        return self.name


class Unit(models.Model):
    '''Единица измерения ингредиента (грамм, стакан, штука, столовая ложка и т.п.).'''
    name = models.CharField(max_length=100, unique=True)
    # Если is_base=True, конверсия в граммы не нужна — единица уже является базовой (граммы/мл).
    is_base = models.BooleanField(default=False)

    class Meta:
        db_table = 'unit'
        verbose_name = 'Единица измерения'
        verbose_name_plural = 'Единицы измерения'
        ordering = ['name']

    def __str__(self):
        return self.name


class UnitConversion(models.Model):
    '''Конвертация нестандартной единицы измерения в граммы для конкретного ингредиента.'''
    ingredient = models.ForeignKey('Ingredient', on_delete=models.CASCADE, related_name='unit_conversions')
    unit = models.ForeignKey(Unit, on_delete=models.CASCADE, related_name='in_grams')
    grams_per_unit = models.DecimalField(max_digits=8, decimal_places=2, help_text='Эквивалент в граммах')

    class Meta:
        db_table = 'unit_conversion'  # исправлена опечатка: было unit_convertion
        verbose_name = 'Конвертация единицы измерения'
        verbose_name_plural = 'Конвертации единиц измерения'
        ordering = ['grams_per_unit']
        constraints = [
            models.UniqueConstraint(fields=['ingredient', 'unit'], name='unique_ingredient_unit_conversion')
        ]

    def __str__(self):
        return f'Weight of Ingredient ID {self.ingredient_id} in Unit ID {self.unit_id} in grams: {self.grams_per_unit}'


class Ingredient(models.Model):
    '''Базовый продукт/ингредиент (например: яблоко, куриная грудка, мука).'''
    name = models.CharField(max_length=255, unique=True)
    category = models.ForeignKey(IngredientCategory, on_delete=models.RESTRICT, related_name='ingredients')
    allergies = models.ManyToManyField('accounts.Allergy', blank=True, related_name='ingredients')

    class Meta:
        db_table = 'ingredient'
        verbose_name = 'Ингредиент'
        verbose_name_plural = 'Ингредиенты'
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
        verbose_name = 'Пищевая ценность ингредиента'
        verbose_name_plural = 'Пищевые ценности ингредиентов'

    @property
    def calories(self):
        return (
            self.protein * CALORIES_PER_GRAM['protein']
            + self.carbs * CALORIES_PER_GRAM['carbs']
            + self.fat * CALORIES_PER_GRAM['fat']
        )

    def __str__(self):
        return (
            f'Nutrition for Ingredient ID {self.ingredient_id}: '
            f'{self.protein}g protein, {self.carbs}g carbs, {self.fat}g fat, '
            f'calories per {self.base_weight_g}g: {self.calories}'
        )


class RecipeQuerySet(models.QuerySet):
    def with_nutrition(self):
        '''
        Аннотирует queryset суммарным весом рецепта на уровне БД (один запрос вместо N+1).
        Используй этот метод вместо свойств total_* при работе со списками рецептов.
        '''
        return self.annotate(
            total_weight=Sum(
                F('recipe_ingredients__amount') * F('recipe_ingredients__ingredient__unit_conversions__grams_per_unit'),
                output_field=models.DecimalField()
            )
        )

    def with_prefetched_ingredients(self):
        '''Prefetch всех связанных данных для расчёта КБЖУ без N+1.'''
        return self.prefetch_related(
            'recipe_ingredients__ingredient__ingredient_nutrition',
            'recipe_ingredients__ingredient__unit_conversions',
            'recipe_ingredients__ingredient__allergies', # аллергии указанные для ингредиентов
            'recipe_ingredients__unit',
        )


class Recipe(models.Model):
    '''Рецепт блюда с заголовком, временем приготовления, количеством порций и связанными диетическими типами.'''
    title = models.CharField(max_length=255)
    image_url = models.CharField(max_length=255, blank=True, null=True, validators=[URLValidator()])
    cook_time = models.PositiveIntegerField(help_text='Время приготовления в минутах')
    servings = models.PositiveSmallIntegerField(default=1)

    diet_types = models.ManyToManyField('accounts.DietType', related_name='recipes')
    meal_types = models.ManyToManyField('menus.MealType', related_name='recipes', blank=True)

    objects = RecipeQuerySet.as_manager()

    class Meta:
        db_table = 'recipe'
        verbose_name = 'Рецепт'
        verbose_name_plural = 'Рецепты'
        indexes = [
            models.Index(fields=['title']),
        ]
        ordering = ['title']

    def _get_nutrition_totals(self):
        '''
        Итерирует recipe_ingredients ровно один раз и кэширует результат на экземпляре.
        Это устраняет N+1: вместо 5 отдельных вызовов .all() — один проход по кэшу.
        Для списков рецептов предпочтительнее использовать with_prefetched_ingredients().
        '''
        if not hasattr(self, '_nutrition_cache'):
            weight = Decimal(0)
            protein = Decimal(0)
            fat = Decimal(0)
            carbs = Decimal(0)
            for ri in self.recipe_ingredients.all():
                weight += ri.amount_in_grams
                protein += ri.protein
                fat += ri.fat
                carbs += ri.carbs
            calories = (
                protein * CALORIES_PER_GRAM['protein']
                + carbs * CALORIES_PER_GRAM['carbs']
                + fat * CALORIES_PER_GRAM['fat']
            )
            self._nutrition_cache = {
                'weight': weight,
                'protein': protein,
                'fat': fat,
                'carbs': carbs,
                'calories': calories,
            }
        return self._nutrition_cache

    @property
    def total_weight_g(self):
        '''Общий вес рецепта в граммах'''
        return self._get_nutrition_totals()['weight']

    @property
    def total_proteins(self):
        '''Общее количество белков в рецепте (г)'''
        return self._get_nutrition_totals()['protein']

    @property
    def total_fats(self):
        '''Общее количество жиров в рецепте (г)'''
        return self._get_nutrition_totals()['fat']

    @property
    def total_carbs(self):
        '''Общее количество углеводов в рецепте (г)'''
        return self._get_nutrition_totals()['carbs']

    @property
    def total_calories(self):
        '''Общая калорийность рецепта (ккал)'''
        return self._get_nutrition_totals()['calories']

    @property
    def per_serving_proteins(self):
        return self.total_proteins / self.servings if self.servings else Decimal(0)

    @property
    def per_serving_fats(self):
        return self.total_fats / self.servings if self.servings else Decimal(0)

    @property
    def per_serving_carbs(self):
        return self.total_carbs / self.servings if self.servings else Decimal(0)

    @property
    def per_serving_calories(self):
        return self.total_calories / self.servings if self.servings else Decimal(0)

    def __str__(self):
        return self.title


class RecipeIngredient(models.Model):
    '''Промежуточная модель для связи рецепта с ингредиентами, их количеством и единицей измерения.'''
    recipe = models.ForeignKey(Recipe, on_delete=models.CASCADE, related_name='recipe_ingredients')
    ingredient = models.ForeignKey(Ingredient, on_delete=models.CASCADE, related_name='used_in_recipes')
    amount = models.DecimalField(max_digits=8, decimal_places=2, validators=[MinValueValidator(Decimal('0.01'))])
    unit = models.ForeignKey(Unit, on_delete=models.RESTRICT)

    class Meta:
        db_table = 'recipe_ingredient'
        verbose_name = 'Ингредиент в рецепте'
        verbose_name_plural = 'Ингредиенты в рецептах'
        constraints = [
            # Один ингредиент — одна запись на рецепт (независимо от единицы).
            models.UniqueConstraint(fields=['recipe', 'ingredient'], name='unique_recipe_ingredient')
        ]

    def _get_grams(self):
        # Если единица помечена как базовая — конверсия не нужна.
        if self.unit.is_base:
            return self.amount

        try:
            conversion = self.ingredient.unit_conversions.get(unit=self.unit)
            return self.amount * conversion.grams_per_unit
        except UnitConversion.DoesNotExist:
            return Decimal(0)

    @property
    def amount_in_grams(self):
        return self._get_grams()

    @property
    def nutrition(self):
        try:
            return self.ingredient.ingredient_nutrition
        except IngredientNutrition.DoesNotExist:
            raise ValueError(f'Отсутствует пищевая ценность для ингредиента "{self.ingredient}"')

    @property
    def protein(self):
        grams = self.amount_in_grams
        try:
            nutrition = self.nutrition
        except ValueError:
            return Decimal(0)
        return (grams / Decimal(nutrition.base_weight_g)) * nutrition.protein

    @property
    def fat(self):
        grams = self.amount_in_grams
        try:
            nutrition = self.nutrition
        except ValueError:
            return Decimal(0)
        return (grams / Decimal(nutrition.base_weight_g)) * nutrition.fat

    @property
    def carbs(self):
        grams = self.amount_in_grams
        try:
            nutrition = self.nutrition
        except ValueError:
            return Decimal(0)
        return (grams / Decimal(nutrition.base_weight_g)) * nutrition.carbs

    @property
    def calories(self):
        return (
            self.protein * CALORIES_PER_GRAM['protein']
            + self.carbs * CALORIES_PER_GRAM['carbs']
            + self.fat * CALORIES_PER_GRAM['fat']
        )

    def __str__(self):
        return f'{self.amount} (Unit ID {self.unit_id}) of Ingredient ID {self.ingredient_id} for Recipe ID {self.recipe_id}'


class RecipeStep(models.Model):
    '''Шаг приготовления рецепта: порядковый номер, описание и опциональное изображение.'''
    recipe = models.ForeignKey(Recipe, on_delete=models.CASCADE, related_name='steps')
    step_number = models.PositiveSmallIntegerField(validators=[MinValueValidator(1)])
    description = models.TextField()
    image_url = models.CharField(max_length=255, blank=True, null=True, validators=[URLValidator()])
    timer = models.IntegerField(blank=True, null=True, help_text="Время таймера в минутах")

    class Meta:
        db_table = 'recipe_step'
        verbose_name = 'Шаг притовления рецепта'
        verbose_name_plural = 'Шаги приготовления рецептов'
        ordering = ['step_number']
        constraints = [
            models.UniqueConstraint(fields=['recipe', 'step_number'], name='unique_recipe_step_number')
        ]

    def __str__(self):
        return f'Step №{self.step_number} for Recipe ID {self.recipe_id}'
