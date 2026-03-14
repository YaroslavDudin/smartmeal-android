from rest_framework import viewsets, permissions
from app.cart.models import CartItem
from app.cart.serializers import CartItemSerializer


class CartViewSet(viewsets.ModelViewSet):
    serializer_class = CartItemSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        return (
            CartItem.objects
            .filter(user=self.request.user)
            .select_related('ingredient', 'unit')
        )

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)
