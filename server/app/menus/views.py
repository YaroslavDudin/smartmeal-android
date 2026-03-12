from rest_framework import viewsets, permissions
from app.menus.models import Menu, MenuItem
from app.menus.serializers import MenuSerializer, MenuItemSerializer


class MenuViewSet(viewsets.ModelViewSet):
    serializer_class = MenuSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        return (
            Menu.objects
            .filter(user=self.request.user)
            .prefetch_related('items__recipe')
        )

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)


class MenuItemViewSet(viewsets.ModelViewSet):
    serializer_class = MenuItemSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        return (
            MenuItem.objects
            .filter(menu__user=self.request.user)
            .select_related('recipe', 'menu')
        )
