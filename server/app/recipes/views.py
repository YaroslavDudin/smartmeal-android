from rest_framework import viewsets, permissions, filters
from app.recipes.models import IngredientCategory, Unit, Ingredient, Recipe
from app.recipes.serializers import (
    IngredientCategorySerializer, UnitSerializer,
    IngredientSerializer, RecipeSerializer,
)


class IngredientCategoryViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = IngredientCategory.objects.all()
    serializer_class = IngredientCategorySerializer
    permission_classes = [permissions.IsAuthenticated]


class UnitViewSet(viewsets.ReadOnlyModelViewSet):
    queryset = Unit.objects.all()
    serializer_class = UnitSerializer
    permission_classes = [permissions.IsAuthenticated]


class IngredientViewSet(viewsets.ReadOnlyModelViewSet):
    serializer_class = IngredientSerializer
    permission_classes = [permissions.IsAuthenticated]
    filter_backends = [filters.SearchFilter]
    search_fields = ['name']

    def get_queryset(self):
        return Ingredient.objects.select_related('category', 'ingredient_nutrition').all()


class RecipeViewSet(viewsets.ReadOnlyModelViewSet):
    serializer_class = RecipeSerializer
    permission_classes = [permissions.IsAuthenticated]
    filter_backends = [filters.SearchFilter]
    search_fields = ['title']
        
    # добавить количество порций в контекст для перерасчета кбжу и общего веса ингредиентов
    def get_serializer_context(self):
        context = super().get_serializer_context()
        context['target_servings'] = self.request.user.portion_size
        return context

    def get_queryset(self):
        return (
            Recipe.objects
            .with_prefetched_ingredients()
            .prefetch_related('steps', 'diet_types')
        )
