from django.contrib import admin
from .models import Menu, MenuItem

class MenuItemInline(admin.TabularInline):
    model = MenuItem
    extra = 3

@admin.register(Menu)
class MenuAdmin(admin.ModelAdmin):
    list_display = ('user', 'period', 'start_date')
    list_filter = ('start_date', 'period')
    search_fields = ('user__email',)
    inlines = [MenuItemInline]

@admin.register(MenuItem)
class MenuItemAdmin(admin.ModelAdmin):
    list_display = ('menu', 'day', 'meal_type', 'recipe')
    list_filter = ('day', 'meal_type')