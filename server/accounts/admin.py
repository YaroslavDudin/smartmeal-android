from django.contrib import admin
from django.contrib.auth.admin import UserAdmin
from .models import User, Allergy

class CustomUserAdmin(UserAdmin):
    model = User
    list_display = ['email', 'username', 'diet_type', 'is_staff']
    fieldsets = UserAdmin.fieldsets + (
        (None, {'fields': ('diet_type', 'portion_size', 'allergies')}),
    )
    add_fieldsets = UserAdmin.add_fieldsets + (
        (None, {'fields': ('diet_type', 'portion_size', 'allergies')}),
    )

admin.site.register(User, CustomUserAdmin)
admin.site.register(Allergy)
