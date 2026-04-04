from django.contrib.auth import get_user_model
from rest_framework import generics, permissions, status, viewsets
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework_simplejwt.views import TokenObtainPairView, TokenRefreshView
from app.accounts.models import DietType, Allergy, UserFavorite, UserStock
from app.accounts.serializers import (
    UserRegistrationSerializer, CustomTokenObtainPairSerializer,
    CustomTokenRefreshSerializer,
    UserSerializer, DietTypeSerializer, AllergySerializer,
    UserFavoriteSerializer, UserStockSerializer
)


User = get_user_model()


class CustomTokenObtainPairView(TokenObtainPairView):
    serializer_class = CustomTokenObtainPairSerializer


class CustomTokenRefreshView(TokenRefreshView):
    serializer_class = CustomTokenRefreshSerializer


class UserViewSet(viewsets.ModelViewSet):
    """
    Пример ViewSet с оптимизацией N+1.
    """
    serializer_class = UserSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        # Используем select_related для ForeignKey (один запрос через JOIN)
        # и prefetch_related для ManyToMany (один дополнительный запрос для всех связей)
        return User.objects.all().select_related('diet_type').prefetch_related('allergies')


class CurrentUserView(generics.RetrieveUpdateAPIView):
    """
    Получение или обновление данных текущего пользователя.
    """
    serializer_class = UserSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_object(self):
        # Оптимизируем получение данных для конкретного пользователя
        return User.objects.select_related('diet_type').prefetch_related('allergies').get(pk=self.request.user.pk)


class DietTypeListView(generics.ListAPIView):
    serializer_class = DietTypeSerializer
    permission_classes = [permissions.IsAuthenticated]
    queryset = DietType.objects.all()


class AllergyListView(generics.ListAPIView):
    serializer_class = AllergySerializer
    permission_classes = [permissions.IsAuthenticated]
    queryset = Allergy.objects.all()


class RegisterView(generics.CreateAPIView):
    serializer_class = UserRegistrationSerializer
    permission_classes = [permissions.AllowAny]
    
    def post(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        user = serializer.save()
        refresh = RefreshToken.for_user(user)

        return Response({
            'user': UserSerializer(user).data,
            'access': str(refresh.access_token),
            'refresh': str(refresh),
            'message': 'Пользователь был успешно создан',
        }, status=status.HTTP_201_CREATED)


class LogoutView(generics.GenericAPIView):
    """
    Выход из системы (инвалидация refresh токена).
    """
    permission_classes = [permissions.AllowAny]

    def post(self, request):
        try:
            refresh_token = request.data.get("refresh")
            if not refresh_token:
                return Response(
                    {"detail": "Refresh token is required."},
                    status=status.HTTP_400_BAD_REQUEST
                )
            
            try:
                token = RefreshToken(refresh_token)
                token.blacklist()
            except Exception:
                # Если токен уже недействителен или в черном списке, 
                # считаем, что выход успешно завершен.
                pass

            return Response(
                {"detail": "Successfully logged out."},
                status=status.HTTP_200_OK
            )
        except Exception as e:
            return Response(
                {"detail": str(e)},
                status=status.HTTP_400_BAD_REQUEST
            )


class UserFavoriteViewSet(viewsets.ModelViewSet):
    serializer_class = UserFavoriteSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        return UserFavorite.objects.filter(user=self.request.user).select_related('recipe')
    
    def perform_create(self, serializer):
        serializer.save(user=self.request.user)

    @action(detail=False, methods=['post'])
    def toggle(self, request):
        recipe_id = request.data.get('recipe')
        if not recipe_id:
            return Response({"error": "Recipe ID is required"}, status=status.HTTP_400_BAD_REQUEST)
        
        favorite = UserFavorite.objects.filter(user=request.user, recipe_id=recipe_id).first()
        if favorite:
            favorite.delete()
            return Response({"is_favorite": False}, status=status.HTTP_200_OK)
        else:
            UserFavorite.objects.create(user=request.user, recipe_id=recipe_id)
            return Response({"is_favorite": True}, status=status.HTTP_201_CREATED)


class UserStockViewSet(viewsets.ModelViewSet):
    serializer_class = UserStockSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        return UserStock.objects.filter(user=self.request.user).select_related('ingredient', 'unit')
    
    def perform_create(self, serializer):
        serializer.save(user=self.request.user)

