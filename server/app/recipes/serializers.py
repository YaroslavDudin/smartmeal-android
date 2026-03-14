from rest_framework import serializers
from app.recipes.models import (
    IngredientCategory, Unit, Ingredient, IngredientNutrition,
    Recipe, RecipeIngredient, RecipeStep,
)


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
        fields = ('base_weight_g', 'protein', 'fat', 'carbs', 'calories')


class IngredientSerializer(serializers.ModelSerializer):
    category_name = serializers.CharField(source='category.name', read_only=True)
    nutrition = IngredientNutritionSerializer(source='ingredient_nutrition', read_only=True)

    class Meta:
        model = Ingredient
        fields = ('id', 'name', 'category', 'category_name', 'nutrition')


class RecipeIngredientSerializer(serializers.ModelSerializer):
    ingredient_name = serializers.CharField(source='ingredient.name', read_only=True)
    unit_name = serializers.CharField(source='unit.name', read_only=True)

    class Meta:
        model = RecipeIngredient
        fields = ('id', 'ingredient', 'ingredient_name', 'amount', 'unit', 'unit_name')


class RecipeStepSerializer(serializers.ModelSerializer):
    class Meta:
        model = RecipeStep
        fields = ('step_number', 'description', 'image_url')


class RecipeSerializer(serializers.ModelSerializer):
    ingredients = RecipeIngredientSerializer(source='recipe_ingredients', many=True, read_only=True)
    steps = RecipeStepSerializer(many=True, read_only=True)

    # Все КБЖУ-поля — это @property на модели, DRF достаёт их через getattr.
    # Указываем явно, чтобы контролировать точность вывода.
    total_calories = serializers.DecimalField(max_digits=8, decimal_places=1, read_only=True)
    total_proteins = serializers.DecimalField(max_digits=8, decimal_places=1, read_only=True)
    total_fats = serializers.DecimalField(max_digits=8, decimal_places=1, read_only=True)
    total_carbs = serializers.DecimalField(max_digits=8, decimal_places=1, read_only=True)
    total_weight_g = serializers.DecimalField(max_digits=8, decimal_places=1, read_only=True)
    per_serving_calories = serializers.DecimalField(max_digits=8, decimal_places=1, read_only=True)
    per_serving_proteins = serializers.DecimalField(max_digits=8, decimal_places=1, read_only=True)
    per_serving_fats = serializers.DecimalField(max_digits=8, decimal_places=1, read_only=True)
    per_serving_carbs = serializers.DecimalField(max_digits=8, decimal_places=1, read_only=True)

    class Meta:
        model = Recipe
        fields = (
            'id', 'title', 'image_url', 'cook_time', 'servings',
            'ingredients', 'steps',
            'total_calories', 'total_proteins', 'total_fats', 'total_carbs', 'total_weight_g',
            'per_serving_calories', 'per_serving_proteins', 'per_serving_fats', 'per_serving_carbs',
        )
