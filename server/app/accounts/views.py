from django.contrib.auth import get_user_model
from rest_framework import generics, permissions, status, viewsets
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework_simplejwt.tokens import RefreshToken
from rest_framework_simplejwt.views import TokenObtainPairView, TokenRefreshView
from app.accounts.models import DietType, Allergy, UserFavorite, UserStock
from django.core.mail import send_mail
from django.conf import settings
from django.utils.http import urlsafe_base64_encode
from django.utils.encoding import force_bytes
from django.contrib.auth.tokens import default_token_generator
from app.accounts.serializers import (
    UserRegistrationSerializer, CustomTokenObtainPairSerializer,
    CustomTokenRefreshSerializer,
    UserSerializer, DietTypeSerializer, AllergySerializer,
    UserFavoriteSerializer, UserStockSerializer,
    PasswordResetRequestSerializer, PasswordResetConfirmSerializer
)


User = get_user_model()


class PasswordResetRequestView(generics.GenericAPIView):
    serializer_class = PasswordResetRequestSerializer
    permission_classes = [permissions.AllowAny]

    def post(self, request):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        email = serializer.validated_data['email']
        user = User.objects.filter(email=email).first()
        
        if user:
            token = default_token_generator.make_token(user)
            uid = urlsafe_base64_encode(force_bytes(user.pk))
            # Формируем прямую ссылку (без лишних слешей)
            reset_url = f"smartmeal://reset-password?uid={uid}&token={token}"
            
            # For development: print to console for easy access
            print(f"\n--- DEBUG PARAMS ---")
            print(f"UID: {uid}")
            print(f"TOKEN: {token}")
            print(f"FULL URL: {reset_url}")
            print(f"--------------------\n")

            send_mail(
                'Сброс пароля - SmartMeal',
                f'Используйте эту ссылку для сброса пароля: {reset_url}\n\n'
                f'Если вы не запрашивали сброс, просто проигнорируйте это письмо.',
                settings.DEFAULT_FROM_EMAIL,
                [email],
                fail_silently=False,
            )
            
        return Response(
            {"detail": "Если аккаунт с таким email существует, письмо для сброса пароля было отправлено."},
            status=status.HTTP_200_OK
        )


class PasswordResetConfirmView(generics.GenericAPIView):
    serializer_class = PasswordResetConfirmSerializer
    permission_classes = [permissions.AllowAny]

    def post(self, request):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(
            {"detail": "Пароль успешно изменен."},
            status=status.HTTP_200_OK
        )


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

        # Send welcome email
        try:
            send_mail(
                'Добро пожаловать в SmartMeal!',
                f'Здравствуйте, {user.username}!\n\n'
                f'Спасибо за регистрацию в SmartMeal. Теперь вы можете планировать свой рацион и следить за статистикой.',
                settings.DEFAULT_FROM_EMAIL,
                [user.email],
                fail_silently=True,
            )
        except Exception:
            pass

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
        return (
            UserFavorite.objects
            .filter(user=self.request.user)
            .select_related('recipe', 'meal_type')
        )
    
    def perform_create(self, serializer):
        serializer.save(user=self.request.user)

    @action(detail=False, methods=['post'])
    def toggle(self, request):
        recipe_id = request.data.get('recipe')
        meal_type_raw = request.data.get('meal_type')
        if not recipe_id:
            return Response({"error": "Recipe ID is required"}, status=status.HTTP_400_BAD_REQUEST)
        
        meal_type_id = None
        if meal_type_raw:
            try:
                # Пытаемся распарсить как ID
                meal_type_id = int(meal_type_raw)
            except (ValueError, TypeError):
                # Если не число, ищем по имени
                from app.menus.models import MealType
                mt = MealType.objects.filter(name__iexact=meal_type_raw).first()
                if mt:
                    meal_type_id = mt.id
        
        # Ищем по паре пользователь+рецепт+тип_приема
        # Если meal_type_id не передан, ищем любую запись этого рецепта в избранном
        filters = {"user": request.user, "recipe_id": recipe_id}
        if meal_type_id:
            filters["meal_type_id"] = meal_type_id
            
        favorites = UserFavorite.objects.filter(**filters)
        
        if favorites.exists():
            favorites.delete()
            return Response({"is_favorite": False}, status=status.HTTP_200_OK)
        else:
            UserFavorite.objects.create(
                user=request.user, 
                recipe_id=recipe_id, 
                meal_type_id=meal_type_id
            )
            return Response({"is_favorite": True}, status=status.HTTP_201_CREATED)


class UserStockViewSet(viewsets.ModelViewSet):
    serializer_class = UserStockSerializer
    permission_classes = [permissions.IsAuthenticated]
    
    def get_queryset(self):
        return UserStock.objects.filter(user=self.request.user).select_related('ingredient', 'unit')
    
    def perform_create(self, serializer):
        serializer.save(user=self.request.user)

