from django.contrib import admin
from .models import CartItem

@admin.register(CartItem)
class CartItemAdmin(admin.ModelAdmin):
    list_display = ('user', 'ingredient', 'total_amount', 'unit')
    list_filter = ('user',)
    search_fields = ('user__email', 'ingredient__name')