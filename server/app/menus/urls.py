from django.urls import path, include
from rest_framework.routers import DefaultRouter
from app.menus.views import MenuViewSet, MenuItemViewSet

router = DefaultRouter()
router.register(r'items', MenuItemViewSet, basename='menu-item')
router.register(r'', MenuViewSet, basename='menu')

urlpatterns = [
    path('', include(router.urls)),
]
