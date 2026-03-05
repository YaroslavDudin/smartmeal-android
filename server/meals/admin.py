from django.contrib import admin
from .models import Ingredient, Recipe, RecipeIngredient, Menu, MenuItem, Favorite

class RecipeIngredientInline(admin.TabularInline):
    model = RecipeIngredient
    extra = 1

@admin.register(Recipe)
class RecipeAdmin(admin.ModelAdmin):
    inlines = [RecipeIngredientInline]
    list_display = ['title', 'diet_type', 'calories', 'cook_time']
    search_fields = ['title']

class MenuItemInline(admin.TabularInline):
    model = MenuItem
    extra = 3

@admin.register(Menu)
class MenuAdmin(admin.ModelAdmin):
    inlines = [MenuItemInline]
    list_display = ['user', 'period', 'start_date']

admin.site.register(Ingredient)
admin.site.register(Favorite)
