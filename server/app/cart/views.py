from datetime import date, timedelta
from decimal import Decimal
from django.db import transaction
from django.db.models import Max
from django.http import HttpResponse
from rest_framework import viewsets, permissions, status
from rest_framework.decorators import action
from rest_framework.response import Response
from app.accounts.models import UserStock
from app.cart.models import CartItem
from app.cart.serializers import CartItemSerializer, RecalculateCartSerializer, ExportCartSerializer
from app.menus.models import Menu, MenuItem


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

    def list(self, *args, **kwargs):
        queryset = self.get_queryset()

        serializer = self.get_serializer(queryset, many=True)
        flat_data = serializer.data

        grouped_data = {}
        for item in flat_data:
            category = item.get('category_name')
            if category not in grouped_data:
                grouped_data[category] = []
            grouped_data[category].append(item)

        return Response(grouped_data, status=status.HTTP_200_OK)
    
    # удаляет корзину пользователя и наполняет заново на основе меню
    @action(detail=False, methods=['post'], url_path='recalculate')
    def recalculate(self, request):
        input_serializer = RecalculateCartSerializer(data=request.data)
        input_serializer.is_valid(raise_exception=True)
        data = input_serializer.validated_data

        user = request.user

        # Остатки от прошлых покупок или пользователь указал, что у него есть дома
        user_stock = {
            stock.ingredient_id: stock
            for stock in user.ingredients_in_stock.select_related('unit', 'ingredient__ingredient_nutrition')
            .prefetch_related(
                'ingredient__unit_conversions__from_unit',
                'ingredient__unit_conversions__to_unit',
                'ingredient__ingredient_nutrition__base_unit'
            )
            .all()
        }
        user_menus = Menu.objects.filter(user=user)
        menu_id = data.get('menu_id')
        today = date.today()
        
        if menu_id is not None:
            try:
                menu = user_menus.get(pk=menu_id)
            except Menu.DoesNotExist:
                return Response(
                    {'detail': f'Меню ID {menu_id} не пренадлежит пользователю {user} или не существует'},
                    status=status.HTTP_404_NOT_FOUND,
                )
        else:
            # Ищем меню среди начавшихся раньше или сегодня
            menu = None

            candidate = (
                user_menus
                # сохраняем значение максимального day_offset у соответствующих menu items
                .annotate(max_day_offset=Max('items__day_offset'))
                .filter(start_date__lte=today)
                # Сортируем от самого ближайшего к сегодня
                .order_by('-start_date')
                .first()
            )

            # Если меню найдено и у него есть items
            if candidate and candidate.max_day_offset is not None:
                # Проверяем, что меню кончается не раньше или хотя бы сегодня
                if candidate.start_date + timedelta(days=candidate.max_day_offset) >= today:
                    menu = candidate

            if menu is None:
                # У пользователя нет меню, в которое входит текущая дата
                return Response(
                    {'detail': f'У пользователя {user} нет активного меню'},
                    status=status.HTTP_404_NOT_FOUND,
                )

        # Берем все элементы меню
        menu_items = MenuItem.objects.filter(menu=menu) \
            .select_related('recipe').prefetch_related(
                'recipe__recipe_ingredients__ingredient__unit_conversions',
                'recipe__recipe_ingredients__unit',
                'recipe__recipe_ingredients__ingredient__ingredient_nutrition__base_unit',
            )

        # Получаем все ингредиенты из рецептов
        recipe_ingredients = [
            ri
            for menu_item in menu_items
            for ri in menu_item.recipe.recipe_ingredients.all()
            if ri.ingredient.can_be_added_to_cart # берем только те, которые отмечены для добавления в корзину
        ]

        items_to_create = {}
        
        for ri in recipe_ingredients:
            ingredient = ri.ingredient
            # базовая единица измерения (г или мл или любая c is_base=true)
            unit = ri.base_unit
            try:
                # могут быть не только граммы, но и мл (и любым другим unit c is_base=true)
                amount_to_add = ri.amount_in_base_units
            
            except ValueError as e:
                return Response(
                    {'detail': str(e)},
                    status=status.HTTP_400_BAD_REQUEST
                )

            # Если ингредиент уже добавлен / обновлен в предыдущих итерациях цикла
            in_progress = items_to_create.get(ingredient.pk)
            # не был в предыдущих итерациях цикла
            if in_progress is None:
                # Добавляем в объект для создания
                items_to_create[ingredient.pk] = CartItem(
                    user=user,
                    ingredient=ingredient,
                    total_amount=amount_to_add,
                    unit=unit,
                )
            else:
                cart_item = in_progress
                cart_item.total_amount += amount_to_add
        
        for ingredient_id in list(items_to_create.keys()):
            item = items_to_create[ingredient_id]
            stock = user_stock.get(ingredient_id, None)
            if stock is not None:
                new_amount = item.total_amount - stock.amount_in_base_units
                if new_amount <= 0:
                    del items_to_create[ingredient_id]
                else:
                    item.total_amount = new_amount

        # Создаем и обновляем все одной транзакцией
        with transaction.atomic():
            self.get_queryset().delete()
            if items_to_create:
                CartItem.objects.bulk_create(items_to_create.values())

        return Response(status=status.HTTP_204_NO_CONTENT)
    
    @action(detail=False, methods=['post'], url_path='export')
    def export(self, request):
        input_serializer = ExportCartSerializer(data=request.data)
        input_serializer.is_valid(raise_exception=True)
        data = input_serializer.validated_data

        cart_items_ids = data.get('cart_items_ids') or []
        export_all = request.query_params.get('all', 'false').lower() == 'true'
        
        # Должен быть либо запрос на api/cart/export/?all=true, либо передан непустой список ID
        if not export_all and not cart_items_ids:
            return Response(
                {'detail': 'Не выбрано ни одного товара для экспорта. Укажите cart_items_ids или добавьте параметр ?all=true'},
                status=status.HTTP_400_BAD_REQUEST
            )
        
        queryset = self.get_queryset()
        if not export_all:
            queryset = queryset.filter(id__in=cart_items_ids)

            # Если не все указанные id нашлись
            found_ids = set(queryset.values_list('id', flat=True))
            missing_ids = set(cart_items_ids) - found_ids
            if missing_ids:
                return Response(
                    {'detail': f'Указанные ID продуктов корзины ({", ".join(map(str, missing_ids))}) не принадлежит текущему пользователю'},
                    status=status.HTTP_400_BAD_REQUEST
                )

        # Группировка по категориям
        grouped = {}
        for item in queryset.select_related('ingredient__category', 'unit'):
            category_name = item.ingredient.category.name
            grouped.setdefault(category_name, []).append(item)

        # Формирование текстового представления
        lines = []
        for category, items in grouped.items():
            lines.append(f'{category}:')
            for item in items:
                lines.append(f'\t- {item.ingredient.name}: {item.total_amount} {item.unit.name}')
            lines.append('')  # пустая строка между категориями

        text_output = '\n'.join(lines).strip()
        return HttpResponse(
            text_output,
            content_type='text/plain',
            status=status.HTTP_200_OK,
            headers={'Content-Disposition': 'attachment; filename="shopping_list.txt"'} # для скачивания как файла .txt
        )
