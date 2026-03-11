import re
from rest_framework import serializers
from django.contrib.auth import get_user_model
from django.contrib.auth.password_validation import validate_password


User = get_user_model()


from rest_framework_simplejwt.serializers import TokenObtainPairSerializer
from django.contrib.auth import authenticate

class CustomTokenObtainPairSerializer(TokenObtainPairSerializer):
    def validate(self, attrs):
        email = attrs.get(self.username_field)
        password = attrs.get("password")

        user = authenticate(request=self.context.get("request"), email=email, password=password)
        
        if not user:
            # For security, we don't tell if email exists or not, we use a generic error
            # or specifically handled ones if business logic requires it.
            # But here we follow the prompt's advice to simplify.
            raise serializers.ValidationError({"detail": "no_active_account_found"})

        return super().validate(attrs)

class UserSerializer(serializers.ModelSerializer):
    diet_type_name = serializers.CharField(source='diet_type.name', read_only=True)
    allergies_names = serializers.SlugRelatedField(
        many=True,
        read_only=True,
        slug_field='name',
        source='allergies'
    )

    class Meta:
        model = User
        fields = ('id', 'username', 'email', 'portion_size', 'diet_type', 'diet_type_name', 'allergies', 'allergies_names')

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
        user = User.objects.create_user(**validated_data)
        return user
