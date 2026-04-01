from decimal import Decimal
from django.db import models
from django.db.models import Max
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
    from_unit = models.ForeignKey(Unit, on_delete=models.CASCADE, related_name='conversions_from')
    to_unit = models.ForeignKey( # единица измерения, в которую конвертируется
        Unit, on_delete=models.CASCADE, related_name='conversions_to',
        help_text='Единица, в которую конвертируется'
    )
    amount_per_unit = models.DecimalField(
        max_digits=8, decimal_places=2,
        help_text='Количество в базовой единице'
    )

    class Meta:
        db_table = 'unit_conversion'  # исправлена опечатка: было unit_convertion
        verbose_name = 'Конвертация единицы измерения'
        verbose_name_plural = 'Конвертации единиц измерения'
        ordering = ['ingredient__name', 'amount_per_unit']
        constraints = [
            models.UniqueConstraint(fields=['ingredient', 'from_unit', 'to_unit'], name='unique_ingredient_unit_conversion')
        ]

    def __str__(self):
        return f'Вес ингредиента {self.ingredient} на 1 {self.from_unit} в {self.to_unit} равен {self.amount_per_unit}'


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
    
    def save(self, *args, **kwargs):
        self.full_clean()  # вызывает clean() и валидацию полей
        super().save(*args, **kwargs)

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
            'recipe_ingredients__ingredient__ingredient_nutrition__base_unit',
            'recipe_ingredients__ingredient__unit_conversions__from_unit',
            'recipe_ingredients__ingredient__unit_conversions__to_unit',
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
            protein = fat = carbs = Decimal(0)
            for ri in self.recipe_ingredients.all():
                p, f, c = ri.get_macros()
                protein += p
                fat += f
                carbs += c
            calories = (
                protein * CALORIES_PER_GRAM['protein']
                + carbs * CALORIES_PER_GRAM['carbs']
                + fat * CALORIES_PER_GRAM['fat']
            )
            self._nutrition_cache = {'protein': protein, 'fat': fat, 'carbs': carbs, 'calories': calories}
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
        return self.total_proteins / self.servings

    @property
    def per_serving_fats(self):
        return self.total_fats / self.servings

    @property
    def per_serving_carbs(self):
        return self.total_carbs / self.servings

    @property
    def per_serving_calories(self):
        return self.total_calories / self.servings

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
    
    def clean(self):
        if not self.ingredient_id or not self.unit_id:
            return

        try:
            # Проверяем одновременно наличие пищевой ценности и конвертации в единицы измерения пищевой ценности
            self.get_amount_in_target_units(self.nutrition.base_unit)
        except ValueError as e:
            raise ValidationError(str(e))
    
    def __setattr__(self, name, value):
        if name in ('amount', 'unit', 'unit_id'):
            for attr in ('_macros_cache', '_in_base_units_cache'):
                self.__dict__.pop(attr, None)
        elif name in ('ingredient', 'ingredient_id'):
            # При смене ингредиента сбрасываем всё включая nutrition
            for attr in ('_macros_cache', '_nutrition_cache', '_in_base_units_cache'):
                self.__dict__.pop(attr, None)
        super().__setattr__(name, value)
    
    def save(self, *args, **kwargs):
        self.full_clean()  # вызывает clean() и валидацию полей
        super().save(*args, **kwargs)
        # Сбрасываем кэш рецепта если он уже загружен в память
        if hasattr(self, 'recipe') and hasattr(self.recipe, '_nutrition_cache'):
            del self.recipe._nutrition_cache

    def _convert_to(self, target_unit):
        if target_unit.pk == self.unit.pk:
            return self.amount

        conversions = self.ingredient.unit_conversions.all()
        
        for conv in conversions:  # из prefetch-кеша
            # Прямая: self.unit → target_unit
            if conv.from_unit_id == self.unit_id and conv.to_unit_id == target_unit.pk:
                return self.amount * conv.amount_per_unit
            # Обратная: target_unit → self.unit
            if conv.from_unit_id == target_unit.pk and conv.to_unit_id == self.unit_id:
                return self.amount / conv.amount_per_unit
        
        return None

    def get_amount_in_target_units(self, target_unit):
        '''Метод, чтобы получить количества ингредиента в указанной единице измерения

        Args:
            target_unit (Unit): Экземпляр класса Unit

        Raises:
            ValueError: Ошибка, возникающая, если нет доступных конвертаций в переданную единицу измерения

        Returns:
            Decimal: Количество ингредиента в переданных единицах измерения
        '''     
        if not hasattr(self, '_in_base_units_cache'):
            self._in_base_units_cache = {}

        if target_unit.pk in self._in_base_units_cache:
            return self._in_base_units_cache[target_unit.pk]

        amount = self._convert_to(target_unit)
        if amount is None:
            raise ValueError(
                f'Нет конвертации из "{self.unit}" в "{target_unit}" '
                f'для ингредиента "{self.ingredient}"'
            )

        self._in_base_units_cache[target_unit.pk] = amount
        return self._in_base_units_cache[target_unit.pk]

    @property
    def base_unit(self):
        # Берется базовая единица измерения пищевой ценности
        return self.nutrition.base_unit
    
    @property
    def amount_in_base_units(self):
        # Переводим в те же базовые единицы, что в пищевой ценности ингредиента
        return self.get_amount_in_target_units(self.nutrition.base_unit)

    @property
    def nutrition(self):
        if not hasattr(self, '_nutrition_cache'):
            try:
                self._nutrition_cache = self.ingredient.ingredient_nutrition
            except IngredientNutrition.DoesNotExist:
                raise ValueError(f'Отсутствует пищевая ценность для ингредиента "{self.ingredient}"')
        return self._nutrition_cache
    
    def get_macros(self):
        # Возвращает (protein, fat, carbs) за один проход
        if not hasattr(self, '_macros_cache'):
            amount = self.get_amount_in_target_units(self.nutrition.base_unit)
            nutrition = self.nutrition
            scale = amount / Decimal(nutrition.base_weight)
            self._macros_cache = (
                scale * nutrition.protein,
                scale * nutrition.fat,
                scale * nutrition.carbs
            )
        return self._macros_cache

    @property
    def protein(self):
        return self.get_macros()[0]

    @property
    def fat(self):
        return self.get_macros()[1]

    @property
    def carbs(self):
        return self.get_macros()[2]

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
    
    def save(self, *args, **kwargs):
        if not self.step_number:
            # Автоматически присваиваем следующий номер
            max_number = RecipeStep.objects.filter(recipe=self.recipe).aggregate(Max('step_number'))['step_number__max']
            self.step_number = (max_number or 0) + 1
        super().save(*args, **kwargs)

    def delete(self, *args, **kwargs):
        # Запоминаем номер удаляемого шага
        deleted_number = self.step_number
        recipe = self.recipe
        super().delete(*args, **kwargs)
        # Сдвигаем номера всех последующих шагов
        RecipeStep.objects.filter(recipe=recipe, step_number__gt=deleted_number).update(step_number=models.F('step_number') - 1)

    def __str__(self):
        return f'Step №{self.step_number} for Recipe ID {self.recipe_id}'
