from rest_framework import serializers
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer, TokenRefreshSerializer
from rest_framework_simplejwt.exceptions import AuthenticationFailed, InvalidToken
from django.contrib.auth import get_user_model
from django.contrib.auth.password_validation import validate_password
from app.accounts.models import DietType, Allergy, UserFavorite, UserStock


User = get_user_model()


class DietTypeSerializer(serializers.ModelSerializer):
    class Meta:
        model = DietType
        fields = ('id', 'name')


class AllergySerializer(serializers.ModelSerializer):
    class Meta:
        model = Allergy
        fields = ('id', 'name')


class CustomTokenObtainPairSerializer(TokenObtainPairSerializer):
    def validate(self, attrs):
        try:
            return super().validate(attrs)
        except AuthenticationFailed:
            # Перехватываем ошибку родителя и возвращаем единый ключ ошибки,
            # не раскрывая, существует ли такой email в системе.
            raise AuthenticationFailed({'detail': 'no_active_account_found'})


class CustomTokenRefreshSerializer(TokenRefreshSerializer):
    """
    Кастомный сериализатор для обновления токена.
    Предотвращает 500 ошибку, если пользователь не найден.
    """
    def validate(self, attrs):
        try:
            return super().validate(attrs)
        except User.DoesNotExist:
            raise AuthenticationFailed({'detail': 'user_not_found'}, code='user_not_found')
        except Exception as e:
            # Логируем другие ошибки токена как 401
            raise InvalidToken({'detail': str(e)})


class UserSerializer(serializers.ModelSerializer):
    diet_type_name = serializers.CharField(source='diet_type.name', read_only=True)
    preferred_cook_time_display = serializers.CharField(source='get_preferred_cook_time_display', read_only=True)
    allergies_names = serializers.SlugRelatedField(
        many=True,
        read_only=True,
        slug_field='name',
        source='allergies'
    )

    class Meta:
        model = User
        fields = (
            'id', 'username', 'email', 'portion_size', 'diet_type', 'diet_type_name', 
            'preferred_cook_time', 'preferred_cook_time_display', 'allergies', 'allergies_names'
        )


class UserRegistrationSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, required=True, validators=[validate_password])
    password_confirm = serializers.CharField(write_only=True, required=True)

    class Meta:
        model = User
        fields = ('username', 'email', 'password', 'password_confirm')

    def validate_username(self, value):
        if User.objects.filter(username=value).exists():
            raise serializers.ValidationError('Пользователь с таким именем уже существует')
        return value

    def validate_email(self, value):
        if User.objects.filter(email=value).exists():
            raise serializers.ValidationError('Пользователь с таким email уже существует')
        return value

    def validate(self, data):
        if data['password'] != data['password_confirm']:
            raise serializers.ValidationError({'password_confirm': 'Пароли не совпадают'})
        return data

    def create(self, validated_data):
        validated_data.pop('password_confirm')
        return User.objects.create_user(**validated_data)


class UserFavoriteSerializer(serializers.ModelSerializer):
    recipe_title = serializers.CharField(source='recipe.title', read_only=True)
    recipe_image_url = serializers.ImageField(source='recipe.image_url', read_only=True)
    recipe_cook_time = serializers.IntegerField(source='recipe.cook_time', read_only=True)
    meal_types = serializers.SlugRelatedField(
        many=True,
        read_only=True,
        slug_field='name',
        source='recipe.meal_types'
    )
    
    class Meta:
        model = UserFavorite
        fields = ('id', 'recipe', 'recipe_title', 'recipe_image_url', 'recipe_cook_time', 'meal_types')
        read_only_fields = ('created_at',)


class UserStockSerializer(serializers.ModelSerializer):
    ingredient_name = serializers.CharField(source='ingredient.name', read_only=True)
    unit_name = serializers.CharField(source='unit.name', read_only=True)
    
    class Meta:
        model = UserStock
        fields = ('id', 'ingredient', 'ingredient_name', 'amount', 'unit', 'unit_name')
