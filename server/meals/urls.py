from django.urls import path
from .views import IngredientListView

urlpatterns = [
    path('ingredients/', IngredientListView.as_view(), name='ingredient-list'),
]
