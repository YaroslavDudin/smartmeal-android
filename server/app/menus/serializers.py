from rest_framework import serializers
from app.menus.models import Menu, MenuItem


class MenuItemSerializer(serializers.ModelSerializer):
    actual_date = serializers.DateField(read_only=True)
    recipe_title = serializers.CharField(source='recipe.title', read_only=True)

    class Meta:
        model = MenuItem
        fields = ('id', 'recipe', 'recipe_title', 'day_offset', 'meal_type', 'actual_date')


class MenuSerializer(serializers.ModelSerializer):
    items = MenuItemSerializer(many=True, read_only=True)

    class Meta:
        model = Menu
        fields = ('id', 'period', 'start_date', 'created_at', 'items')
        read_only_fields = ('created_at',)
