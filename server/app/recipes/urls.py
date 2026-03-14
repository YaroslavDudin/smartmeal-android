from django.urls import path, include
from rest_framework.routers import DefaultRouter
from app.recipes.views import (
    IngredientCategoryViewSet, UnitViewSet,
    IngredientViewSet, RecipeViewSet,
)

router = DefaultRouter()
router.register(r'categories', IngredientCategoryViewSet, basename='ingredient-category')
router.register(r'units', UnitViewSet, basename='unit')
router.register(r'ingredients', IngredientViewSet, basename='ingredient')
router.register(r'', RecipeViewSet, basename='recipe')

urlpatterns = [
    path('', include(router.urls)),
]
