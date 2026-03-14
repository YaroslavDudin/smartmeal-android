from django.urls import path, include
from rest_framework.routers import DefaultRouter
from rest_framework_simplejwt.views import TokenRefreshView
from app.accounts.views import RegisterView, CustomTokenObtainPairView, CurrentUserView, UserViewSet, DietTypeListView, AllergyListView, LogoutView


router = DefaultRouter()
router.register(r'users', UserViewSet, basename='user')

urlpatterns = [
    path('register/', RegisterView.as_view(), name='register'),
    path('login/', CustomTokenObtainPairView.as_view(), name='login'), # Added alias for clarity
    path('token/', CustomTokenObtainPairView.as_view(), name='token_pair_obtain'),
    path('token/refresh/', TokenRefreshView.as_view(), name='token_refresh'),
    path('logout/', LogoutView.as_view(), name='logout'),
    path('me/', CurrentUserView.as_view(), name='current-user'),
    path('diet-types/', DietTypeListView.as_view(), name='diet-types'),
    path('allergies/', AllergyListView.as_view(), name='allergies'),
    path('', include(router.urls)),
]
