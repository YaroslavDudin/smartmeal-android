from rest_framework import serializers
from app.cart.models import CartItem


class CartItemSerializer(serializers.ModelSerializer):
    ingredient_name = serializers.CharField(source='ingredient.name', read_only=True)
    unit_name = serializers.CharField(source='unit.name', read_only=True)
    category_name = serializers.CharField(source='ingredient.category.name', read_only=True)

    class Meta:
        model = CartItem
        fields = ('id', 'ingredient', 'ingredient_name', 'category_name', 'total_amount', 'unit', 'unit_name')
    
    def validate(self, data):
        request = self.context.get('request')
        user = request.user
        ingredient = data.get('ingredient')
        
        if not self.instance: # проверка только при создании
            if not ingredient.can_be_added_to_cart:
                raise serializers.ValidationError({'can_be_added_to_cart': 'Этот ингредиент отмечен как не для корзины'})
            if CartItem.objects.filter(user=user, ingredient=ingredient).exists():
                raise serializers.ValidationError('Этот ингредиент уже есть в корзине')
        return data


class RecalculateCartSerializer(serializers.Serializer):
    # ID меню, ингредиенты которого перенести в корзину
    # Если не указано, ищется активное меню (в период которого входит текущая дата)
    menu_id = serializers.IntegerField(required=False, allow_null=True)
    start_date = serializers.DateField(required=False, allow_null=True)
    end_date = serializers.DateField(required=False, allow_null=True)
    # Мапа { "menu_item_id": servings }
    item_servings = serializers.DictField(
        child=serializers.IntegerField(min_value=1),
        required=False,
        default=dict
    )
    # Общее кол-во порций (если не указано в item_servings)
    global_servings = serializers.IntegerField(required=False, min_value=1)


class ExportCartSerializer(serializers.Serializer):
    # список с ID cart items
    cart_items_ids = serializers.ListField(
        child=serializers.IntegerField(),
        default=[],
    )
