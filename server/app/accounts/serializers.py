from rest_framework import serializers
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer, TokenRefreshSerializer
from rest_framework_simplejwt.exceptions import AuthenticationFailed, InvalidToken
from django.contrib.auth import get_user_model
from django.contrib.auth.password_validation import validate_password
from app.accounts.models import DietType, Allergy, UserFavorite, UserStock


from django.contrib.auth.tokens import default_token_generator
from django.utils.http import urlsafe_base64_decode


User = get_user_model()


class PasswordResetRequestSerializer(serializers.Serializer):
    email = serializers.EmailField()

    def validate_email(self, value):
        if not User.objects.filter(email=value).exists():
            # We don't want to leak if an email exists, 
            # but usually for password reset it's okay or handled by view.
            pass
        return value


class PasswordResetConfirmSerializer(serializers.Serializer):
    uid = serializers.CharField()
    token = serializers.CharField()
    new_password = serializers.CharField(write_only=True, validators=[validate_password])
    new_password_confirm = serializers.CharField(write_only=True)

    def validate(self, data):
        if data['new_password'] != data['new_password_confirm']:
            raise serializers.ValidationError({"new_password_confirm": "Пароли не совпадают"})
        
        try:
            uid = urlsafe_base64_decode(data['uid']).decode()
            self.user = User.objects.get(pk=uid)
        except (TypeError, ValueError, OverflowError, User.DoesNotExist):
            raise serializers.ValidationError({"uid": "Invalid user"})

        if not default_token_generator.check_token(self.user, data['token']):
            raise serializers.ValidationError({"token": "Неверный или просроченный токен"})

        return data

    def save(self):
        self.user.set_password(self.validated_data['new_password'])
        self.user.save()
        return self.user


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
            'id', 'username', 'email', 'avatar', 'portion_size', 'diet_type', 'diet_type_name', 
            'preferred_cook_time', 'preferred_cook_time_display', 'allergies', 'allergies_names',
            'birth_date', 'gender'
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
