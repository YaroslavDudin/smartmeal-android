import pytest
from django.contrib.auth import get_user_model

from app.accounts.serializers import UserSerializer


User = get_user_model()


@pytest.mark.django_db
def test_user_serializer_accepts_valid_nutrition_target():
    user = User.objects.create_user(
        username='target-user',
        email='target-user@example.com',
        password='password123',
    )

    serializer = UserSerializer(
        user,
        data={
            'username': user.username,
            'email': user.email,
            'portion_size': 1,
            'allergies': [],
            'calories_enabled': True,
            'target_calories': 2000,
            'calorie_margin': 100,
            'protein_percent': 20,
            'fat_percent': 30,
            'carbs_percent': 50,
        },
        partial=True,
    )

    assert serializer.is_valid(), serializer.errors


@pytest.mark.django_db
def test_user_serializer_rejects_invalid_macro_sum():
    user = User.objects.create_user(
        username='bad-target-user',
        email='bad-target-user@example.com',
        password='password123',
    )

    serializer = UserSerializer(
        user,
        data={
            'protein_percent': 25,
            'fat_percent': 25,
            'carbs_percent': 40,
        },
        partial=True,
    )

    assert serializer.is_valid() is False
    assert 'carbs_percent' in serializer.errors
