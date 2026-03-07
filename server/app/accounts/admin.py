from django.contrib import admin
from django.contrib.auth.admin import UserAdmin
from .models import User, Allergy, DietType, UserFavorite

@admin.register(User)
class CustomUserAdmin(UserAdmin):
    list_display = ('email', 'username', 'portion_size', 'is_staff')
    search_fields = ('email', 'username')
    fieldsets = UserAdmin.fieldsets + (
        ('Дополнительная информация', {'fields': ('portion_size', 'allergies', 'diet_types')}),
    )

@admin.register(Allergy)
class AllergyAdmin(admin.ModelAdmin):
    list_display = ('id', 'name')
    search_fields = ('name',)

@admin.register(DietType)
class DietTypeAdmin(admin.ModelAdmin):
    list_display = ('id', 'name')
    search_fields = ('name',)

@admin.register(UserFavorite)
class UserFavoriteAdmin(admin.ModelAdmin):
    list_display = ('user', 'recipe', 'created_at')
    list_filter = ('created_at',)
    search_fields = ('user__email', 'recipe__title')
