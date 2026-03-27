from decimal import Decimal
from django.db import models
from django.core.validators import MinValueValidator
from django.core.exceptions import ValidationError


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
    unit = models.ForeignKey(Unit, on_delete=models.CASCADE, related_name='in_other_units')
    base_unit = models.ForeignKey( # базовая единица, в которую конвертируется
        Unit, on_delete=models.RESTRICT, related_name='conversions_to',
        help_text='Единица, в которую конвертируется'
    )
    amount_per_unit = models.DecimalField( # базовой единицой измерения может быть не только грамм (тот же мл)
        max_digits=8, decimal_places=2,
        help_text='Количество в базовой единице'
    )

    class Meta:
        db_table = 'unit_conversion'  # исправлена опечатка: было unit_convertion
        verbose_name = 'Конвертация единицы измерения'
        verbose_name_plural = 'Конвертации единиц измерения'
        ordering = ['ingredient__name', 'amount_per_unit']
        constraints = [
            models.UniqueConstraint(fields=['ingredient', 'unit', 'base_unit'], name='unique_ingredient_unit_conversion')
        ]

    def __str__(self):
        return f'Вес ингредиента {self.ingredient} на 1 {self.unit} в {self.base_unit} равен {self.amount_per_unit}'


class Ingredient(models.Model):
    '''Базовый продукт/ингредиент (например: яблоко, куриная грудка, мука).'''
    name = models.CharField(max_length=255, unique=True)
    category = models.ForeignKey(IngredientCategory, on_delete=models.RESTRICT, related_name='ingredients')
    can_be_added_to_cart = models.BooleanField(default=True, help_text='Должен ли ингредиент добавляться в корзину')
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
    base_unit = models.ForeignKey(Unit, on_delete=models.RESTRICT, related_name='ingredient_nutritions')
    base_weight = models.PositiveSmallIntegerField(default=100, help_text='Масса, для которой указаны КБЖУ (в базовых единицах)')
    protein = models.DecimalField(max_digits=5, decimal_places=1, validators=[MinValueValidator(0)])
    fat = models.DecimalField(max_digits=5, decimal_places=1, validators=[MinValueValidator(0)])
    carbs = models.DecimalField(max_digits=5, decimal_places=1, validators=[MinValueValidator(0)])

    class Meta:
        db_table = 'ingredient_nutrition'
        verbose_name = 'Пищевая ценность ингредиента'
        verbose_name_plural = 'Пищевые ценности ингредиентов'
    
    def clean(self):
        if self.base_unit_id and not self.base_unit.is_base:
            raise ValidationError({
                'base_unit': f'Единица измерения "{self.base_unit}" не является базовой (is_base=False). '
                    f'Для пищевой ценности можно использовать только базовые единицы'
            })

    @property
    def calories(self):
        return (
            self.protein * CALORIES_PER_GRAM['protein']
            + self.carbs * CALORIES_PER_GRAM['carbs']
            + self.fat * CALORIES_PER_GRAM['fat']
        )

    def __str__(self):
        return (
            f'Пищевая ценность ингредиента {self.ingredient}: '
            f'{self.protein} г белка, {self.carbs} г углеводов, {self.fat} г жиров, '
            f'калорийность на {self.base_weight} {self.base_unit}: {self.calories}'
        )


class RecipeQuerySet(models.QuerySet):

    def with_prefetched_ingredients(self):
        '''Prefetch всех связанных данных для расчёта КБЖУ без N+1.'''
        return self.prefetch_related(
            'recipe_ingredients__ingredient__ingredient_nutrition',
            'recipe_ingredients__ingredient__unit_conversions',
            'recipe_ingredients__ingredient__allergies', # аллергии указанные для ингредиентов
            'recipe_ingredients__unit',
        )


# Построение пути для файла главного фото рецепта
def _get_recipe_photopath(instance, filename):
    return f'recipes/{instance.pk}/main/{filename}'


class Recipe(models.Model):
    '''Рецепт блюда с заголовком, временем приготовления, количеством порций и связанными диетическими типами.'''
    title = models.CharField(max_length=255)
    # Локальная загрузка фото
    image_url = models.ImageField(upload_to=_get_recipe_photopath, null=True, blank=True)
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
            protein = Decimal(0)
            fat = Decimal(0)
            carbs = Decimal(0)
            for ri in self.recipe_ingredients.all():
                protein += ri.protein
                fat += ri.fat
                carbs += ri.carbs
            calories = (
                protein * CALORIES_PER_GRAM['protein']
                + carbs * CALORIES_PER_GRAM['carbs']
                + fat * CALORIES_PER_GRAM['fat']
            )
            self._nutrition_cache = {
                'protein': protein,
                'fat': fat,
                'carbs': carbs,
                'calories': calories,
            }
        return self._nutrition_cache

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
    
    def _find_conversion(self, target_unit):
        conversions = self.ingredient.unit_conversions.filter(unit=self.unit)
        
        # Прямая: self.unit → target_unit
        direct = conversions.filter(base_unit=target_unit).first()
        if direct:
            return self.amount * direct.amount_per_unit, target_unit
        
        # Обратная: target_unit → self.unit
        reverse = self.ingredient.unit_conversions.filter(
            unit=target_unit, base_unit=self.unit
        ).first()
        if reverse:
            return self.amount / reverse.amount_per_unit, target_unit
        
        return None

    def _get_in_base_units(self):
        if hasattr(self, '_in_base_units_cache'):
            return self._in_base_units_cache
        
        target_units = []

        try:
            nutrition = self.nutrition
            # Если есть пищевая ценность — проверяем совпадение
            if self.unit.pk == nutrition.base_unit.pk:
                self._in_base_units_cache = (self.amount, self.unit)
                return self._in_base_units_cache
            # если не совпадает кладем в target_units
            target_units.append(nutrition.base_unit)
        except ValueError:
            # Единица уже базовая и конверсия не нужна
            if self.unit.is_base:
                self._in_base_units_cache = (self.amount, self.unit)
                return self._in_base_units_cache

        # Добавляем базовые единицы как запасные варианты
        base_units = list(Unit.objects.filter(is_base=True) \
            .exclude(pk=target_units[0].pk if target_units else None)) # исключаем единицу nutrition если есть
        target_units.extend(base_units)

        for target_unit in target_units:
            result = self._find_conversion(target_unit)
            if result:
                self._in_base_units_cache = result
                return self._in_base_units_cache

        # Конверсия не найдена — возвращаем 0
        self._in_base_units_cache = (Decimal(0), self.unit)
        return self._in_base_units_cache

    @property
    def base_unit(self):
        _, base_unit = self._get_in_base_units()
        return base_unit

    @property
    def amount_in_base_units(self):
        amount, _ = self._get_in_base_units()
        return amount

    @property
    def nutrition(self):
        if not hasattr(self, '_nutrition_cache'):
            try:
                self._nutrition_cache = self.ingredient.ingredient_nutrition
            except IngredientNutrition.DoesNotExist:
                raise ValueError(f'Отсутствует пищевая ценность для ингредиента "{self.ingredient}"')
        return self._nutrition_cache

    @property
    def protein(self):
        amount = self.amount_in_base_units
        try:
            nutrition = self.nutrition
        except ValueError:
            return Decimal(0)
        return (amount / Decimal(nutrition.base_weight)) * nutrition.protein

    @property
    def fat(self):
        amount = self.amount_in_base_units
        try:
            nutrition = self.nutrition
        except ValueError:
            return Decimal(0)
        return (amount / Decimal(nutrition.base_weight)) * nutrition.fat

    @property
    def carbs(self):
        amount = self.amount_in_base_units
        try:
            nutrition = self.nutrition
        except ValueError:
            return Decimal(0)
        return (amount / Decimal(nutrition.base_weight)) * nutrition.carbs

    @property
    def calories(self):
        return (
            self.protein * CALORIES_PER_GRAM['protein']
            + self.carbs * CALORIES_PER_GRAM['carbs']
            + self.fat * CALORIES_PER_GRAM['fat']
        )

    def __str__(self):
        return f'{self.amount} (Unit ID {self.unit_id}) of Ingredient ID {self.ingredient_id} for Recipe ID {self.recipe_id}'


# Построение пути для файла фото шага рецепта
def _get_recipe_step_photopath(instance, filename):
    return f'recipes/{instance.recipe.pk}/steps/step_{instance.step_number}_{filename}'


class RecipeStep(models.Model):
    '''Шаг приготовления рецепта: порядковый номер, описание и опциональное изображение.'''
    recipe = models.ForeignKey(Recipe, on_delete=models.CASCADE, related_name='steps')
    step_number = models.PositiveSmallIntegerField(validators=[MinValueValidator(1)])
    description = models.TextField()
    # Локальная загрузка фото
    image_url = models.ImageField(upload_to=_get_recipe_step_photopath, null=True, blank=True)
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
