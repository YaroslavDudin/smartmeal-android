from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView
from app.accounts.views import RegisterView, CustomTokenObtainPairView


urlpatterns = [
    path('register/', RegisterView.as_view(), name='register'),
    path('token/', CustomTokenObtainPairView.as_view(), name='token_pair_obtain'),
    path('token/refresh/', TokenRefreshView.as_view(), name='token_refresh'),
]
