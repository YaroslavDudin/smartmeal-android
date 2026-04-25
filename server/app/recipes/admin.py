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


from django.utils.html import format_html


class RecipeStepInline(admin.StackedInline):
    model = RecipeStep
    extra = 1
    # Каждая группа полей будет в своем блоке
    fieldsets = (
        (None, {
            'fields': (('step_number', 'timer'), 'description')
        }),
        ('Медиа-контент (выберите что-то одно)', {
            'fields': (('image_url', 'photo_preview'), ('video_url', 'video_preview')),
            'classes': ('collapse',), # Можно скрыть по умолчанию, если шагов много
        }),
    )
    readonly_fields = ('photo_preview', 'video_preview')
    
    def photo_preview(self, obj):
        if obj.image_url:
            return format_html('<img src="{}" style="max-height: 150px; border-radius: 8px; border: 1px solid #ccc;" />', obj.image_url.url)
        return "Нет изображения"
    photo_preview.short_description = "Превью фото"

    def video_preview(self, obj):
        if obj.video_url:
            return format_html(
                '<video src="{}" style="max-height: 150px; border-radius: 8px; background: black;" controls />',
                obj.video_url.url
            )
        return "Нет видео"
    video_preview.short_description = "Превью видео"


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
        super().save_related(request, form, formsets, change)
        # Всегда пересчитываем КБЖУ для текущего рецепта после сохранения всех связей (ингредиентов)
        form.instance.update_nutrition_cache()


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
