from django.contrib import admin
from .models import CartItem

@admin.register(CartItem)
class CartItemAdmin(admin.ModelAdmin):
    list_display = ('user', 'ingredient', 'total_amount', 'unit', 'is_checked')
    list_filter = ('is_checked', 'user')
    search_fields = ('user__email', 'ingredient__name')