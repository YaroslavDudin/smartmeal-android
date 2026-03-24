from rest_framework import serializers
from app.cart.models import CartItem


class CartItemSerializer(serializers.ModelSerializer):
    ingredient_name = serializers.CharField(source='ingredient.name', read_only=True)
    unit_name = serializers.CharField(source='unit.name', read_only=True)

    class Meta:
        model = CartItem
        fields = ('id', 'ingredient', 'ingredient_name', 'total_amount', 'unit', 'unit_name', 'is_checked')

    def create(self, validated_data):
        user = validated_data.get('user')
        ingredient = validated_data.get('ingredient')
        amount_to_add = validated_data.get('total_amount')

        exist = CartItem.objects.filter(user=user, ingredient=ingredient).first()
        if exist:
            exist.total_amount += amount_to_add
            exist.save()
            return exist
        else:
            return super().create(validated_data)
