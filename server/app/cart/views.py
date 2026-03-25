from rest_framework import viewsets, permissions
from rest_framework.response import Response
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

    def list(self, request, *args, **kwargs):
        queryset = self.get_queryset()

        show_checked = request.query_params.get('show_checked')
        if show_checked == 'false':
            queryset = queryset.filter(is_checked=False)

        serializer = self.get_serializer(queryset, many=True)
        flat_data = serializer.data

        grouped_data = {}
        for item in flat_data:
            category = item.get('category_name')
            if category not in grouped_data:
                grouped_data[category] = []
            grouped_data[category].append(item)

        return Response(grouped_data)