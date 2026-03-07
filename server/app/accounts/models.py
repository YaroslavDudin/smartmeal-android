from django.db import models
from django.contrib.auth.models import AbstractUser

class Allergy(models.Model):
    name = models.CharField(max_length=255, unique=True)

    def __str__(self):
        return self.name
    
    class Meta:
        db_table = 'allergy'

class DietType(models.Model):
    name = models.CharField(max_length=255, unique=True)

    def __str__(self):
        return self.name
    
    class Meta:
        db_table = 'diet_type'

class User(AbstractUser):
    email = models.EmailField(unique=True, max_length=255)
    portion_size = models.IntegerField(default=1)
    created_at = models.DateTimeField(auto_now_add=True)
    
    allergies = models.ManyToManyField(Allergy, blank=True, related_name='users')
    diet_types = models.ManyToManyField(DietType, blank=True, related_name='users')

    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = ['username']

    def __str__(self):
        return self.email
    
    class Meta:
        db_table = 'user'

class UserFavorite(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='favorites')
    recipe = models.ForeignKey('recipes.Recipe', on_delete=models.CASCADE, related_name='favorited_by')
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.user.email} - {self.recipe.title}"
    
    class Meta:
        db_table = 'user_favorite'
        unique_togather = [['user'], ['recipe']]
