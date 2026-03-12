from django.urls import path, include
from rest_framework.routers import DefaultRouter
from app.cart.views import CartViewSet

router = DefaultRouter()
router.register(r'', CartViewSet, basename='cart-item')

urlpatterns = [
    path('', include(router.urls)),
]
