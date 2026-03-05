from django.contrib.auth.models import AbstractUser
from django.db import models

class Allergy(models.Model):
    name = models.CharField(max_length=100, unique=True)

    def __str__(self):
        return self.name

    class Meta:
        verbose_name_plural = "Allergies"

class User(AbstractUser):
    DIET_CHOICES = [
        ('classic', 'Classic'),
        ('vegetarian', 'Vegetarian'),
        ('vegan', 'Vegan'),
        ('keto', 'Keto'),
        ('paleo', 'Paleo'),
    ]
    
    email = models.EmailField(unique=True)
    diet_type = models.CharField(max_length=20, choices=DIET_CHOICES, default='classic')
    portion_size = models.CharField(max_length=20, default='medium')
    allergies = models.ManyToManyField(Allergy, blank=True, related_name='users')
    created_at = models.DateTimeField(auto_now_add=True)

    # Используем email как логин
    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = ['username']

    def __str__(self):
        return self.email
