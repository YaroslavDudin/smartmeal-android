from rest_framework import serializers

from app.accounts.models import User, DietType, Allergy
from app.recipes.models import (
    Recipe, RecipeIngredient, RecipeStep,
    Ingredient, IngredientNutrition, IngredientCategory,
    Unit, UnitConversion,
)
from app.menus.models import Menu, MenuItem


# ---------------------------------------------------------------------------
# Accounts
# ---------------------------------------------------------------------------

class AdminDietTypeSerializer(serializers.ModelSerializer):
    class Meta:
        model = DietType
        fields = ['id', 'name']


class AdminAllergySerializer(serializers.ModelSerializer):
    class Meta:
        model = Allergy
        fields = ['id', 'name']


class AdminUserSerializer(serializers.ModelSerializer):
    diet_type_name = serializers.SerializerMethodField()
    allergies = AdminAllergySerializer(many=True, read_only=True)

    class Meta:
        model = User
        fields = [
            'id', 'email', 'username', 'first_name', 'last_name',
            'is_active', 'is_staff', 'is_superuser',
            'portion_size', 'created_at', 'date_joined',
            'diet_type', 'diet_type_name',
            'allergies',
        ]
        read_only_fields = ['id', 'created_at', 'date_joined']

    def get_diet_type_name(self, obj):
        if obj.diet_type:
            return obj.diet_type.name
        return None


# ---------------------------------------------------------------------------
# Recipes – units and categories
# ---------------------------------------------------------------------------

class AdminUnitSerializer(serializers.ModelSerializer):
    class Meta:
        model = Unit
        fields = ['id', 'name', 'is_base']


class AdminIngredientCategorySerializer(serializers.ModelSerializer):
    class Meta:
        model = IngredientCategory
        fields = ['id', 'name']


# ---------------------------------------------------------------------------
# Recipes – ingredients
# ---------------------------------------------------------------------------

class AdminIngredientNutritionSerializer(serializers.ModelSerializer):
    calories = serializers.SerializerMethodField()

    class Meta:
        model = IngredientNutrition
        fields = ['id', 'base_weight_g', 'protein', 'fat', 'carbs', 'calories']

    def get_calories(self, obj):
        return float(obj.calories)


class AdminUnitConversionSerializer(serializers.ModelSerializer):
    unit_name = serializers.CharField(source='unit.name', read_only=True)

    class Meta:
        model = UnitConversion
        fields = ['id', 'unit', 'unit_name', 'grams_per_unit']


class AdminIngredientSerializer(serializers.ModelSerializer):
    category_name = serializers.CharField(source='category.name', read_only=True)
    nutrition = AdminIngredientNutritionSerializer(source='ingredient_nutrition', read_only=True)
    unit_conversions = AdminUnitConversionSerializer(many=True, read_only=True)

    class Meta:
        model = Ingredient
        fields = ['id', 'name', 'category', 'category_name', 'nutrition', 'unit_conversions']


class AdminIngredientWriteSerializer(serializers.ModelSerializer):
    class Meta:
        model = Ingredient
        fields = ['id', 'name', 'category']


# ---------------------------------------------------------------------------
# Recipes – recipe itself
# ---------------------------------------------------------------------------

class AdminRecipeIngredientSerializer(serializers.ModelSerializer):
    ingredient_name = serializers.CharField(source='ingredient.name', read_only=True)
    unit_name = serializers.CharField(source='unit.name', read_only=True)

    class Meta:
        model = RecipeIngredient
        fields = ['id', 'recipe', 'ingredient', 'ingredient_name', 'amount', 'unit', 'unit_name']
        read_only_fields = ['id']


class AdminRecipeStepSerializer(serializers.ModelSerializer):
    class Meta:
        model = RecipeStep
        fields = ['id', 'recipe', 'step_number', 'description', 'image_url', 'timer']
        read_only_fields = ['id']


class AdminRecipeShortSerializer(serializers.ModelSerializer):
    diet_type_names = serializers.SerializerMethodField()
    total_calories = serializers.SerializerMethodField()

    class Meta:
        model = Recipe
        fields = ['id', 'title', 'image_url', 'cook_time', 'servings', 'diet_type_names', 'total_calories']

    def get_diet_type_names(self, obj):
        return list(obj.diet_types.values_list('name', flat=True))

    def get_total_calories(self, obj):
        try:
            return float(obj.total_calories)
        except Exception:
            return 0.0


class AdminRecipeSerializer(serializers.ModelSerializer):
    diet_types = serializers.SerializerMethodField()
    diet_type_names = serializers.SerializerMethodField()
    ingredients = AdminRecipeIngredientSerializer(source='recipe_ingredients', many=True, read_only=True)
    steps = AdminRecipeStepSerializer(many=True, read_only=True)
    total_calories = serializers.SerializerMethodField()
    total_proteins = serializers.SerializerMethodField()
    total_fats = serializers.SerializerMethodField()
    total_carbs = serializers.SerializerMethodField()

    class Meta:
        model = Recipe
        fields = [
            'id', 'title', 'image_url', 'cook_time', 'servings',
            'diet_types', 'diet_type_names', 'ingredients', 'steps',
            'total_calories', 'total_proteins', 'total_fats', 'total_carbs',
        ]

    def get_diet_types(self, obj):
        return list(obj.diet_types.values_list('id', flat=True))

    def get_diet_type_names(self, obj):
        return list(obj.diet_types.values_list('name', flat=True))

    def get_total_calories(self, obj):
        try:
            return float(obj.total_calories)
        except Exception:
            return 0.0

    def get_total_proteins(self, obj):
        try:
            return float(obj.total_proteins)
        except Exception:
            return 0.0

    def get_total_fats(self, obj):
        try:
            return float(obj.total_fats)
        except Exception:
            return 0.0

    def get_total_carbs(self, obj):
        try:
            return float(obj.total_carbs)
        except Exception:
            return 0.0


class AdminRecipeWriteSerializer(serializers.ModelSerializer):
    diet_types = serializers.PrimaryKeyRelatedField(
        queryset=DietType.objects.all(),
        many=True,
        required=False,
    )

    class Meta:
        model = Recipe
        fields = ['id', 'title', 'image_url', 'cook_time', 'servings', 'diet_types']

    def create(self, validated_data):
        diet_types = validated_data.pop('diet_types', [])
        recipe = Recipe.objects.create(**validated_data)
        recipe.diet_types.set(diet_types)
        return recipe

    def update(self, instance, validated_data):
        diet_types = validated_data.pop('diet_types', None)
        for attr, value in validated_data.items():
            setattr(instance, attr, value)
        instance.save()
        if diet_types is not None:
            instance.diet_types.set(diet_types)
        return instance


# ---------------------------------------------------------------------------
# Menus
# ---------------------------------------------------------------------------

class AdminMenuItemSerializer(serializers.ModelSerializer):
    recipe_title = serializers.CharField(source='recipe.title', read_only=True)

    class Meta:
        model = MenuItem
        fields = ['id', 'recipe', 'recipe_title', 'day_offset', 'meal_type']


class AdminMenuSerializer(serializers.ModelSerializer):
    user_email = serializers.EmailField(source='user.email', read_only=True)
    items = AdminMenuItemSerializer(many=True, read_only=True)

    class Meta:
        model = Menu
        fields = ['id', 'user', 'user_email', 'period', 'start_date', 'created_at', 'items']
        read_only_fields = ['id', 'created_at']


# ---------------------------------------------------------------------------
# Dashboard stats
# ---------------------------------------------------------------------------

class StatsSerializer(serializers.Serializer):
    recipes_count = serializers.IntegerField()
    ingredients_count = serializers.IntegerField()
    users_count = serializers.IntegerField()
    menus_count = serializers.IntegerField()
    recent_recipes = AdminRecipeShortSerializer(many=True)
    recent_users = AdminUserSerializer(many=True)
