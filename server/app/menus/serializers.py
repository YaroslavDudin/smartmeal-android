from rest_framework import serializers
from app.menus.models import Menu, MenuItem, Period, MealType


class MenuItemSerializer(serializers.ModelSerializer):
    recipe_title = serializers.CharField(source='recipe.title', read_only=True)
    cook_time = serializers.IntegerField(source='recipe.cook_time', read_only=True)
    image_url = serializers.ImageField(source='recipe.image_url', read_only=True)
    meal_type = serializers.SlugRelatedField(slug_field='name', queryset=MealType.objects.all())
    is_favorite = serializers.SerializerMethodField()

    class Meta:
        model = MenuItem
        fields = ('id', 'recipe', 'recipe_title', 'cook_time', 'image_url', 'day_offset', 'meal_type', 'actual_date', 'is_favorite')

    def get_is_favorite(self, obj):
        user = self.context.get('request').user
        if user.is_authenticated:
            return obj.recipe.favorited_by.filter(user=user).exists()
        return False


class MenuSerializer(serializers.ModelSerializer):
    items = MenuItemSerializer(many=True, read_only=True)

    class Meta:
        model = Menu
        fields = ('id', 'period', 'start_date', 'created_at', 'items')
        read_only_fields = ('created_at',)


class GenerateMenuSerializer(serializers.Serializer):
    """Входные параметры для POST /api/menus/generate/"""
    period = serializers.CharField() # Используем строку, так как может быть "custom"
    days = serializers.IntegerField(required=False, min_value=1, max_value=256)
    start_date = serializers.DateField()
    # Если не передан — берётся из профиля пользователя (diet_type)
    diet_type = serializers.IntegerField(required=False, allow_null=True, min_value=1)
    # Диапазон времени приготовления ('short', 'medium', 'long', 'any')
    cook_time_range = serializers.CharField(required=False, allow_null=True)
    # Детальная настройка по приемам пищи
    cook_times = serializers.DictField(
        child=serializers.CharField(),
        required=False,
        allow_null=True,
        help_text='Словарь: {"breakfast": "short", "lunch": "medium", "dinner": "long"}'
    )
    # Максимальное время приготовления в минутах (30 = «до 30 минут», null = без ограничения)
    max_cook_time = serializers.IntegerField(required=False, allow_null=True, min_value=1)
    # Зарезервировано на будущее — исключение аллергенов
    exclude_allergies = serializers.ListField(
        child=serializers.IntegerField(min_value=1),
        required=False,
        default=list,
    )
