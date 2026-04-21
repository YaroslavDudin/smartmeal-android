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
        return Ingredient.objects.select_related(
            'category', 
            'ingredient_nutrition__base_unit'
        ).all()


class RecipeViewSet(viewsets.ReadOnlyModelViewSet):
    serializer_class = RecipeSerializer
    permission_classes = [permissions.IsAuthenticated]
    filter_backends = [filters.SearchFilter, filters.OrderingFilter]
    search_fields = ['title']
    ordering_fields = ['per_serving_calories', 'cook_time', 'title']
    ordering = ['title']
        
    # добавить количество порций в контекст для перерасчета кбжу и общего веса ингредиентов
    def get_serializer_context(self):
        context = super().get_serializer_context()
        servings = self.request.query_params.get('servings')
        if servings and servings.isdigit():
            context['target_servings'] = int(servings)
        return context

    def get_queryset(self):
        queryset = (
            Recipe.objects
            .with_prefetched_ingredients()
            .prefetch_related('steps', 'diet_types')
        )
        
        # Фильтрация по калориям
        max_calories = self.request.query_params.get('max_calories')
        if max_calories and max_calories.isdigit():
            queryset = queryset.filter(per_serving_calories__lte=int(max_calories))
            
        min_calories = self.request.query_params.get('min_calories')
        if min_calories and min_calories.isdigit():
            queryset = queryset.filter(per_serving_calories__gte=int(min_calories))

        # Фильтрация по диетам (уже была, но проверим)
        diet_type = self.request.query_params.get('diet_type')
        if diet_type and diet_type.isdigit():
            queryset = queryset.filter(diet_types__id=diet_type)

        return queryset
