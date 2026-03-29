import logging
from decimal import Decimal
from rest_framework import serializers
from app.recipes.models import (
    IngredientCategory, Unit, Ingredient, IngredientNutrition,
    Recipe, RecipeIngredient, RecipeStep,
)
from app.accounts.serializers import AllergySerializer


logger = logging.getLogger(__name__)


class IngredientCategorySerializer(serializers.ModelSerializer):
    class Meta:
        model = IngredientCategory
        fields = ('id', 'name')


class UnitSerializer(serializers.ModelSerializer):
    class Meta:
        model = Unit
        fields = ('id', 'name', 'is_base')


class IngredientNutritionSerializer(serializers.ModelSerializer):
    calories = serializers.DecimalField(max_digits=6, decimal_places=1, read_only=True)

    class Meta:
        model = IngredientNutrition
        fields = ('base_weight', 'base_unit', 'protein', 'fat', 'carbs', 'calories')


class IngredientSerializer(serializers.ModelSerializer):
    category_name = serializers.CharField(source='category.name', read_only=True)
    nutrition = IngredientNutritionSerializer(source='ingredient_nutrition', read_only=True)

    class Meta:
        model = Ingredient
        fields = ('id', 'name', 'category', 'category_name', 'nutrition', 'can_be_added_to_cart')


class RecipeIngredientSerializer(serializers.ModelSerializer):
    ingredient_name = serializers.CharField(source='ingredient.name', read_only=True)
    unit_name = serializers.CharField(source='unit.name', read_only=True)
    category_name = serializers.CharField(source='ingredient.category.name', read_only=True)
    base_unit_name = serializers.CharField(source='base_unit.name', read_only=True)
    # переопределение количества под нужное количество персон
    amount = serializers.SerializerMethodField()
    amount_in_base_units = serializers.SerializerMethodField()

    class Meta:
        model = RecipeIngredient
        fields = ('id', 'ingredient', 'ingredient_name', 'amount', 'amount_in_base_units', 'base_unit_name', 'unit', 'unit_name', 'category_name')

    # отношение количества персон к количеству порций в рецепте
    def _get_scale(self, obj):
        cache_key = f'_scale_{obj.pk}'
        if not hasattr(self, cache_key):
            target_servings = self.context.get('target_servings')
            recipe_servings = self.context.get('recipe_servings') or obj.recipe.servings
            if not target_servings or not recipe_servings:
                scale = Decimal(1)
            else:
                scale = Decimal(target_servings) / Decimal(recipe_servings)
            setattr(self, cache_key, scale)
        return getattr(self, cache_key)

    # перерасчет количества под указанное количество персон
    # если нету в контексте передается как есть в рецепте
    def get_amount(self, obj):
        scale = self._get_scale(obj)
        scaled_amount = round(obj.amount * scale, 2)
        logger.debug(
            "Scaling recipe ingredient amount recipe_ingredient_id=%s ingredient_id=%s target_servings=%s recipe_servings=%s original_amount=%s scaled_amount=%s",
            obj.id,
            obj.ingredient_id,
            self.context.get('target_servings'),
            obj.recipe.servings,
            obj.amount,
            scaled_amount,
        )
        return scaled_amount

    # перерасчет количества под указанное количество персон в базовых единицах
    def get_amount_in_base_units(self, obj):
        scale = self._get_scale(obj)
        scaled_amount = round(obj.amount_in_base_units * scale, 2)
        return scaled_amount

class RecipeStepSerializer(serializers.ModelSerializer):
    class Meta:
        model = RecipeStep
        fields = ('step_number', 'description', 'image_url', 'timer')


class RecipeSerializer(serializers.ModelSerializer):
    ingredients = RecipeIngredientSerializer(source='recipe_ingredients', many=True, read_only=True)
    steps = RecipeStepSerializer(many=True, read_only=True)
    allergies = serializers.SerializerMethodField()
    is_favorite = serializers.SerializerMethodField()

    # Все КБЖУ-поля — это @property на модели, DRF достаёт их через getattr.
    # Указываем явно, чтобы контролировать точность вывода.
    per_serving_calories = serializers.DecimalField(max_digits=8, decimal_places=1, read_only=True)
    per_serving_proteins = serializers.DecimalField(max_digits=8, decimal_places=1, read_only=True)
    per_serving_fats = serializers.DecimalField(max_digits=8, decimal_places=1, read_only=True)
    per_serving_carbs = serializers.DecimalField(max_digits=8, decimal_places=1, read_only=True)
    # кбжу и вес в граммах также пересчитывается с учетом количества персон выбранных у пользователя
    total_calories = serializers.SerializerMethodField()
    total_proteins = serializers.SerializerMethodField()
    total_fats = serializers.SerializerMethodField()
    total_carbs = serializers.SerializerMethodField()
    total_weight = serializers.SerializerMethodField()

    class Meta:
        model = Recipe
        fields = (
            'id', 'title', 'image_url', 'cook_time', 'servings',
            'ingredients', 'steps', 'allergies', 'is_favorite',
            'total_calories', 'total_proteins', 'total_fats', 'total_carbs', 'total_weight',
            'per_serving_calories', 'per_serving_proteins', 'per_serving_fats', 'per_serving_carbs',
        )

    def get_is_favorite(self, obj):
        user = self.context.get('request').user
        if user.is_authenticated:
            return obj.favorited_by.filter(user=user).exists()
        return False
        
    # отношение количества персон к количеству порций в рецепте
    def _get_scale(self, obj):
        cache_key = f'_scale_{obj.pk}'
        if not hasattr(self, cache_key):
            target_servings = self.context.get('target_servings')
            if not target_servings:
                scale = Decimal(1)
            else:
                scale = Decimal(target_servings) / Decimal(obj.servings)
            setattr(self, cache_key, scale)
        return getattr(self, cache_key)
    
    # собираем уникальные аллергии из всех ингредиентов
    def get_allergies(self, obj):
        seen = set()
        result = []
        for ri in obj.recipe_ingredients.all():
            for allergy in ri.ingredient.allergies.all():
                if allergy.id not in seen:
                    seen.add(allergy.id)
                    result.append(AllergySerializer(allergy).data)
        return result

    def get_total_calories(self, obj):
        cache_key = f'_calories_{obj.pk}'
        if not hasattr(self, cache_key):
            calories = round(obj.total_calories * self._get_scale(obj), 2)
            setattr(self, cache_key, calories)
        return getattr(self, cache_key)

    def get_total_proteins(self, obj):
        cache_key = f'_proteins_{obj.pk}'
        if not hasattr(self, cache_key):
            proteins = round(obj.total_proteins * self._get_scale(obj), 2)
            setattr(self, cache_key, proteins)
        return getattr(self, cache_key)

    def get_total_fats(self, obj):
        cache_key = f'_fats_{obj.pk}'
        if not hasattr(self, cache_key):
            fats = round(obj.total_fats * self._get_scale(obj), 2)
            setattr(self, cache_key, fats)
        return getattr(self, cache_key)

    def get_total_carbs(self, obj):
        cache_key = f'_carbs_{obj.pk}'
        if not hasattr(self, cache_key):
            carbs = round(obj.total_carbs * self._get_scale(obj), 2)
            setattr(self, cache_key, carbs)
        return getattr(self, cache_key)

    def get_total_weight(self, obj):
        cache_key = f'_weight_{obj.pk}'
        if not hasattr(self, cache_key):
            total_weight = sum(ri.amount_in_base_units for ri in obj.recipe_ingredients.all())
            weight = round(total_weight * self._get_scale(obj), 0)
            setattr(self, cache_key, weight)
        return getattr(self, cache_key)
