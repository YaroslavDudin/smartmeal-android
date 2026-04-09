from django.urls import path, include
from rest_framework.routers import DefaultRouter
from app.accounts.views import (
    RegisterView, CustomTokenObtainPairView, CustomTokenRefreshView,
    CurrentUserView, UserViewSet, DietTypeListView, AllergyListView, LogoutView,
    UserFavoriteViewSet, UserStockViewSet,
    PasswordResetRequestView, PasswordResetConfirmView
)


router = DefaultRouter()
router.register(r'users', UserViewSet, basename='user')
router.register(r'favorites', UserFavoriteViewSet, basename='favorite')
router.register(r'stock', UserStockViewSet, basename='stock')

urlpatterns = [
    path('register/', RegisterView.as_view(), name='register'),
    path('login/', CustomTokenObtainPairView.as_view(), name='login'), # Added alias for clarity
    path('token/', CustomTokenObtainPairView.as_view(), name='token_pair_obtain'),
    path('token/refresh/', CustomTokenRefreshView.as_view(), name='token_refresh'),
    path('logout/', LogoutView.as_view(), name='logout'),
    path('password-reset/', PasswordResetRequestView.as_view(), name='password_reset'),
    path('password-reset-confirm/', PasswordResetConfirmView.as_view(), name='password_reset_confirm'),
    path('me/', CurrentUserView.as_view(), name='current-user'),
    path('diet-types/', DietTypeListView.as_view(), name='diet-types'),
    path('allergies/', AllergyListView.as_view(), name='allergies'),
    path('', include(router.urls)),
]
