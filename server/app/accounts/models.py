from django.db import models
from django.contrib.auth.models import AbstractUser

class Allergy(models.Model):
    name = models.CharField(max_length=255)

    def __str__(self):
        return self.name

class DietType(models.Model):
    name = models.CharField(max_length=255)

    def __str__(self):
        return self.name

class User(AbstractUser):
    email = models.EmailField(unique=True, max_length=255)
    portion_size = models.IntegerField(null=True, blank=True)
    
    allergies = models.ManyToManyField(Allergy, blank=True, related_name='users')
    diet_types = models.ManyToManyField(DietType, blank=True, related_name='users')

    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = ['username']

    def __str__(self):
        return self.email

class UserFavorite(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='favorites')
    recipe = models.ForeignKey('recipes.Recipe', on_delete=models.CASCADE, related_name='favorited_by')
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.user.email} - {self.recipe.title}"
