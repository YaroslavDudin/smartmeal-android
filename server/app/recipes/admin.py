from django.contrib import admin
from django.core.management import call_command
from .models import (
    IngredientCategory, Unit, UnitConversion,
    Ingredient, IngredientNutrition,
    Recipe, RecipeIngredient, RecipeStep
)


class RecipeIngredientInline(admin.TabularInline):
    model = RecipeIngredient
    extra = 1
    autocomplete_fields = ['ingredient', 'unit']


class RecipeStepInline(admin.TabularInline):
    model = RecipeStep
    extra = 1


class UnitConversionInline(admin.TabularInline):
    model = UnitConversion
    extra = 1
    autocomplete_fields = ['from_unit', 'to_unit']


class IngredientNutritionInline(admin.StackedInline):
    model = IngredientNutrition
    can_delete = False
    max_num = 1


@admin.register(Recipe)
class RecipeAdmin(admin.ModelAdmin):
    list_display = ('title', 'cook_time', 'per_serving_calories', 'total_calories', 'servings')
    list_filter = ('diet_types', 'cook_time')
    search_fields = ('title',)
    inlines = [RecipeIngredientInline, RecipeStepInline]
    filter_horizontal = ('diet_types', 'meal_types')
    actions = ['recalculate_nutrition']

    @admin.action(description='Пересчитать КБЖУ для выбранных рецептов')
    def recalculate_nutrition(self, request, queryset):
        for recipe in queryset:
            recipe.update_nutrition_cache()
        self.message_user(request, f"КБЖУ пересчитаны для {queryset.count()} рецептов.")

    def save_related(self, request, form, formsets, change):
        is_new = not change
        super().save_related(request, form, formsets, change)
        if is_new:
            call_command('update_calories')


@admin.register(Ingredient)
class IngredientAdmin(admin.ModelAdmin):
    list_display = ('name', 'category')
    list_filter = ('category', 'allergies', 'can_be_added_to_cart')
    search_fields = ('name',)
    inlines = [UnitConversionInline, IngredientNutritionInline]


@admin.register(IngredientCategory)
class IngredientCategoryAdmin(admin.ModelAdmin):
    list_display = ('id', 'name')
    search_fields = ('name',)


@admin.register(Unit)
class UnitAdmin(admin.ModelAdmin):
    list_display = ('id', 'name', 'is_base')
    search_fields = ('name',)


@admin.register(UnitConversion)
class UnitConversionAdmin(admin.ModelAdmin):
    list_display = ('ingredient', 'from_unit', 'to_unit', 'amount_per_unit')
    list_filter = ('from_unit', 'to_unit')
    search_fields = ('ingredient__name', 'from_unit__name', 'to_unit__name')
    autocomplete_fields = ['ingredient', 'from_unit']


@admin.register(IngredientNutrition)
class IngredientNutritionAdmin(admin.ModelAdmin):
    list_display = ('ingredient', 'protein', 'fat', 'carbs', 'calories')
    search_fields = ('ingredient__name',)
    autocomplete_fields = ['ingredient']
