from django.contrib.auth import get_user_model
from rest_framework import generics, permissions, status, viewsets
from rest_framework.response import Response
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework_simplejwt.views import TokenObtainPairView, TokenRefreshView
from app.accounts.models import DietType, Allergy
from app.accounts.serializers import (
    UserRegistrationSerializer, CustomTokenObtainPairSerializer,
    CustomTokenRefreshSerializer,
    UserSerializer, DietTypeSerializer, AllergySerializer,
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
