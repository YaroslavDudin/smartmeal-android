from decimal import Decimal
from django.db import models
from django.contrib.auth.models import AbstractUser
from django.core.validators import MaxValueValidator, MinValueValidator
from django.core.exceptions import ValidationError
from app.recipes.models import IngredientNutrition
from app.recipes.utils import convert_amount


class Allergy(models.Model):
    name = models.CharField(max_length=255, unique=True)

    class Meta:
        db_table = 'allergy'
        verbose_name = 'Аллергия'
        verbose_name_plural = 'Аллергии'
        ordering = ['-id']

    def __str__(self):
        return self.name


class DietType(models.Model):
    name = models.CharField(max_length=255, unique=True)

    class Meta:
        db_table = 'diet_type'
        verbose_name = 'Тип питания'
        verbose_name_plural = 'Типы питания'
        ordering = ['-id']

    def __str__(self):
        return self.name


class CookTimeRange(models.TextChoices):
    SHORT = 'short', 'До 30 минут'
    MEDIUM = 'medium', 'От 30 до 60 минут'
    LONG = 'long', '60 минут и более'
    ANY = 'any', 'Любое время'


class User(AbstractUser):
    email = models.EmailField(unique=True, max_length=255)
    avatar = models.ImageField(upload_to='avatars/', null=True, blank=True, verbose_name="Аватар")
    portion_size = models.IntegerField(default=1)
    birth_date = models.DateField(null=True, blank=True, verbose_name="Дата рождения")
    gender = models.CharField(
        max_length=10,
        choices=[('male', 'Мужской'), ('female', 'Женский')],
        null=True,
        blank=True,
        verbose_name="Пол"
    )
    created_at = models.DateTimeField(auto_now_add=True)
    
    allergies = models.ManyToManyField(Allergy, blank=True, related_name='users')
    diet_type = models.ForeignKey('DietType', null=True, blank=True, on_delete=models.SET_NULL, related_name='users')
    preferred_cook_time = models.CharField(
        max_length=10,
        choices=CookTimeRange.choices,
        default=CookTimeRange.ANY,
        verbose_name="Предпочитаемое время готовки"
    )
    calories_enabled = models.BooleanField(default=False, verbose_name="Планировать по калориям")
    target_calories = models.PositiveSmallIntegerField(
        default=2000,
        validators=[MinValueValidator(1200), MaxValueValidator(3000)],
        verbose_name="Целевая калорийность"
    )
    calorie_margin = models.PositiveSmallIntegerField(
        default=100,
        validators=[MinValueValidator(50), MaxValueValidator(500)],
        verbose_name="Допустимый разброс калорий"
    )
    protein_percent = models.PositiveSmallIntegerField(
        default=20,
        validators=[MinValueValidator(10), MaxValueValidator(80)],
        verbose_name="Белки, %"
    )
    fat_percent = models.PositiveSmallIntegerField(
        default=30,
        validators=[MinValueValidator(10), MaxValueValidator(80)],
        verbose_name="Жиры, %"
    )
    carbs_percent = models.PositiveSmallIntegerField(
        default=50,
        validators=[MinValueValidator(10), MaxValueValidator(80)],
        verbose_name="Углеводы, %"
    )

    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = ['username']

    class Meta:
        db_table = 'user'
        verbose_name = 'Пользователь'
        verbose_name_plural = 'Пользователи'
        ordering = ['-created_at']

    def clean(self):
        super().clean()
        macro_sum = self.protein_percent + self.fat_percent + self.carbs_percent
        if macro_sum != 100:
            raise ValidationError({
                'carbs_percent': 'Сумма белков, жиров и углеводов должна быть равна 100%.'
            })

    def __str__(self):
        return f'Имя пользователя: {self.username}, email: {self.email}'


class UserFavorite(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='favorites')
    recipe = models.ForeignKey('recipes.Recipe', on_delete=models.CASCADE, related_name='favorited_by')
    meal_type = models.ForeignKey('menus.MealType', on_delete=models.SET_NULL, null=True, blank=True, related_name='favorited_as')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'user_favorite'
        verbose_name = 'Избранный рецепт пользователя'
        verbose_name_plural = 'Избранные рецепты пользователей'
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['user', 'created_at']),
        ]
        constraints = [
            models.UniqueConstraint(fields=['user', 'recipe', 'meal_type'], name='unique_user_recipe_meal_favorite')
        ]

    def __str__(self):
        return f'User ID: {self.user_id} - Recipe ID: {self.recipe_id} (MT: {self.meal_type_id})'


class UserStock(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='ingredients_in_stock')
    ingredient = models.ForeignKey('recipes.Ingredient', on_delete=models.CASCADE, related_name='in_users_stock')
    amount = models.DecimalField(max_digits=8, decimal_places=2, validators=[MinValueValidator(Decimal('0.01'))])
    unit = models.ForeignKey('recipes.Unit', on_delete=models.CASCADE)
    
    class Meta:
        db_table = 'user_stock'
        verbose_name = 'Ингредиент в наличии у пользователя'
        verbose_name_plural = 'Ингредиенты в наличии у пользователей'
        ordering = ['user', 'ingredient', 'amount']
        constraints = [
            models.UniqueConstraint(fields=['user', 'ingredient'], name='unique_user_ingredient_in_stock')
        ]
    
    def clean(self):
        super().clean()
        if not self.ingredient_id or not self.unit_id:
            return
        
        try:
            self.amount_in_base_units
        except (ValueError, ValidationError) as e:
            raise ValidationError({'unit': str(e)})
    
    def save(self, *args, **kwargs):
        self.full_clean()
        super().save(*args, **kwargs)
    
    @property
    def amount_in_base_units(self):
        if hasattr(self, '_in_base_units_cache'):
            return self._in_base_units_cache

        try:
            nutrition = self.ingredient.ingredient_nutrition
            base_unit = nutrition.base_unit
        except IngredientNutrition.DoesNotExist:
            raise ValidationError({
                'ingredient': f'Для ингредиента "{self.ingredient.name}" не указана пищевая ценность'
            })

        if self.unit_id == base_unit.pk:
            self._in_base_units_cache = self.amount
            return self._in_base_units_cache

        result = convert_amount(self.ingredient, self.amount, self.unit, base_unit)
        if result is None:
            raise ValueError(
                f'Невозможно конвертировать "{self.unit}" в "{base_unit}" '
                f'для ингредиента "{self.ingredient}"'
            )
        self._in_base_units_cache = result
        return self._in_base_units_cache
        
    def __str__(self):
        return f'Пользователь ID {self.user_id} имеет {self.amount} {self.unit.name} {self.ingredient.name}'
