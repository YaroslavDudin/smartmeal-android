from django.urls import path

from .views import (
    # Auth
    AdminTokenView,
    AdminMeView,
    # Upload
    ImageUploadView,
    # Image search
    ImageSearchView,
    # Stats
    StatsView,
    # Recipes
    AdminRecipeListCreateView,
    AdminRecipeDetailView,
    AdminRecipeIngredientListCreateView,
    AdminRecipeIngredientDetailView,
    AdminRecipeStepListCreateView,
    AdminRecipeStepDetailView,
    # Ingredients
    AdminIngredientListCreateView,
    AdminIngredientDetailView,
    # Categories
    AdminCategoryListCreateView,
    AdminCategoryDetailView,
    # Units
    AdminUnitListCreateView,
    AdminUnitDetailView,
    # Users
    AdminUserListView,
    AdminUserDetailView,
    # Diet types
    AdminDietTypeListCreateView,
    AdminDietTypeDetailView,
    # Allergies
    AdminAllergyListCreateView,
    AdminAllergyDetailView,
    # Menus
    AdminMenuListView,
    AdminMenuDetailView,
)

urlpatterns = [
    # Auth
    path('auth/token/', AdminTokenView.as_view(), name='admin-token'),
    path('auth/me/', AdminMeView.as_view(), name='admin-me'),

    # Upload
    path('upload/image/', ImageUploadView.as_view(), name='admin-upload-image'),
    # Image search
    path('search/images/', ImageSearchView.as_view(), name='admin-image-search'),

    # Stats
    path('stats/', StatsView.as_view(), name='admin-stats'),

    # Recipes
    path('recipes/', AdminRecipeListCreateView.as_view(), name='admin-recipe-list'),
    path('recipes/<int:pk>/', AdminRecipeDetailView.as_view(), name='admin-recipe-detail'),
    path('recipes/<int:recipe_pk>/ingredients/', AdminRecipeIngredientListCreateView.as_view(), name='admin-recipe-ingredient-list'),
    path('recipes/<int:recipe_pk>/ingredients/<int:pk>/', AdminRecipeIngredientDetailView.as_view(), name='admin-recipe-ingredient-detail'),
    path('recipes/<int:recipe_pk>/steps/', AdminRecipeStepListCreateView.as_view(), name='admin-recipe-step-list'),
    path('recipes/<int:recipe_pk>/steps/<int:pk>/', AdminRecipeStepDetailView.as_view(), name='admin-recipe-step-detail'),

    # Ingredients
    path('ingredients/', AdminIngredientListCreateView.as_view(), name='admin-ingredient-list'),
    path('ingredients/<int:pk>/', AdminIngredientDetailView.as_view(), name='admin-ingredient-detail'),

    # Ingredient categories
    path('categories/', AdminCategoryListCreateView.as_view(), name='admin-category-list'),
    path('categories/<int:pk>/', AdminCategoryDetailView.as_view(), name='admin-category-detail'),

    # Units
    path('units/', AdminUnitListCreateView.as_view(), name='admin-unit-list'),
    path('units/<int:pk>/', AdminUnitDetailView.as_view(), name='admin-unit-detail'),

    # Users
    path('users/', AdminUserListView.as_view(), name='admin-user-list'),
    path('users/<int:pk>/', AdminUserDetailView.as_view(), name='admin-user-detail'),

    # Diet types
    path('diet-types/', AdminDietTypeListCreateView.as_view(), name='admin-diet-type-list'),
    path('diet-types/<int:pk>/', AdminDietTypeDetailView.as_view(), name='admin-diet-type-detail'),

    # Allergies
    path('allergies/', AdminAllergyListCreateView.as_view(), name='admin-allergy-list'),
    path('allergies/<int:pk>/', AdminAllergyDetailView.as_view(), name='admin-allergy-detail'),

    # Menus
    path('menus/', AdminMenuListView.as_view(), name='admin-menu-list'),
    path('menus/<int:pk>/', AdminMenuDetailView.as_view(), name='admin-menu-detail'),
]
