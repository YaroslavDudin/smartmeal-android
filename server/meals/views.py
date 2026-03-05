from rest_framework import generics, permissions
from .models import Ingredient
from .serializers import IngredientSerializer

class IngredientListView(generics.ListAPIView):
    queryset = Ingredient.objects.all()[:20] # Лимит 20 ингредиентов
    serializer_class = IngredientSerializer
    permission_classes = [permissions.AllowAny] # Разрешим просмотр всем для теста или IsAuthenticated если нужно
