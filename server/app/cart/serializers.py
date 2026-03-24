from rest_framework import serializers
from app.cart.models import CartItem


class CartItemSerializer(serializers.ModelSerializer):
    ingredient_name = serializers.CharField(source='ingredient.name', read_only=True)
    unit_name = serializers.CharField(source='unit.name', read_only=True)

    class Meta:
        model = CartItem
        fields = ('id', 'ingredient', 'ingredient_name', 'total_amount', 'unit', 'unit_name', 'is_checked')
    
    def validate(self, data):
        request = self.context.get('request')
        user = request.user
        ingredient = data.get('ingredient')

        if not self.instance: # проверка только при создании
            if CartItem.objects.filter(user=user, ingredient=ingredient).exists():
                raise serializers.ValidationError('Этот ингредиент уже есть в корзине')
        return data
