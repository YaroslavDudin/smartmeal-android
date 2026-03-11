from django.contrib.auth import get_user_model
from rest_framework import generics, permissions, status, viewsets
from rest_framework.response import Response
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework_simplejwt.views import TokenObtainPairView
from app.accounts.serializers import UserRegistrationSerializer, CustomTokenObtainPairSerializer, UserSerializer


User = get_user_model()


class CustomTokenObtainPairView(TokenObtainPairView):
    serializer_class = CustomTokenObtainPairSerializer


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


class RegisterView(generics.CreateAPIView):
    serializer_class = UserRegistrationSerializer
    permission_classes = [permissions.AllowAny]
    
    def post(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        user = serializer.save()
        refresh = RefreshToken.for_user(user)

        return Response({
            'user': {
                'username': user.username,
                'email': user.email,
            },
            'access': str(refresh.access_token),
            'refresh': str(refresh),
            'message': 'Пользователь был успешно создан',
        }, status=status.HTTP_201_CREATED)
