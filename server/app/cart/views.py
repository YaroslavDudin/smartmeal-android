from datetime import date, timedelta
from decimal import Decimal
from django.db import transaction
from django.db.models import Max
from rest_framework import viewsets, permissions, status
from rest_framework.decorators import action
from rest_framework.response import Response
from app.cart.models import CartItem
from app.cart.serializers import CartItemSerializer, RecalculateCartSerializer
from app.menus.models import Menu, MenuItem
from app.recipes.models import UnitConversion


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
        if show_checked != 'true':
            queryset = queryset.filter(is_checked=False)

        serializer = self.get_serializer(queryset, many=True)
        flat_data = serializer.data

        grouped_data = {}
        for item in flat_data:
            category = item.get('category_name')
            if category not in grouped_data:
                grouped_data[category] = []
            grouped_data[category].append(item)

        return Response(grouped_data, status=status.HTTP_200_OK)
    
    @action(detail=False, methods=['post'], url_path='recalculate')
    def recalculate(self, request):
        input_serializer = RecalculateCartSerializer(data=request.data)
        input_serializer.is_valid(raise_exception=True)
        data = input_serializer.validated_data

        user = request.user

        CartItem.objects.filter(user=user, is_checked=False).delete()

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
            menu = None

            # Ищем меню среди начавшихся раньше или сегодня
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
                # Иначе ищем любое ближайшее будущее меню
                menu = user_menus.filter(start_date__gt=today).order_by('start_date').first()

            if menu is None:
                # Если все меню в прошлом или их вообще нет
                return Response(
                    {'detail': f'У пользователя {user} нет активного меню'},
                    status=status.HTTP_404_NOT_FOUND,
                )

        # Если меню начинается раньше текущей даты, разница между датами положительная, иначе отрицательная и берем 0
        day_offset = max((today - menu.start_date).days, 0)
        # Ищем элементы меню только для сегодня и далее, не берем элементы для прошедших дней
        menu_items = MenuItem.objects.filter(menu=menu, day_offset__gte=day_offset) \
            .select_related('recipe').prefetch_related(
                'recipe__recipe_ingredients__ingredient__unit_conversions',
                'recipe__recipe_ingredients__unit',
            )
        # Получаем все ингредиенты из рецептов
        recipe_ingredients = [
            ri
            for menu_item in menu_items
            for ri in menu_item.recipe.recipe_ingredients.all()
            if ri.ingredient.can_be_added_to_cart # берем только те, которые отмечены для добавления в корзину
        ]
        ingredient_ids = [ri.ingredient.pk for ri in recipe_ingredients]
        # Получаем продукты из корзины, которые будем обновлять и кладем в объект ID ингредиента: CartItem
        existing_cart_items = {
            cart_item.ingredient.pk: cart_item
            for cart_item in CartItem.objects.filter(
                user=user,
                ingredient_id__in=ingredient_ids,
            ).select_related('unit')
        }
        # Делаем сет с парами (ID единицы измерения добавляемого ингредиента, ID единицы измерения из корзины)
        unit_pairs = set()
        for ri in recipe_ingredients:
            ingredient_id = ri.ingredient.pk
            existing_item = existing_cart_items.get(ingredient_id)
            # Добавляем пару только если единицы измерения не совпадают и существующий ингредиент не отмечен
            if existing_item and not existing_item.is_checked and existing_item.unit.pk != ri.base_unit.pk:
                unit_pairs.add((ri.base_unit.pk, existing_item.unit.pk))
        
        # Создаем объект с конвертациями (unit_id, base_unit_id): UnitConversion
        unit_conversion_map = {}
        if unit_pairs:
            # Получаем все уникальные IDs единиц измерения, которые есть в парах
            all_unit_ids = {unit_id for pair in unit_pairs for unit_id in pair}
            # Ищем все конвертации для этих единиц измерения
            conversions = UnitConversion.objects.filter(
                unit_id__in=all_unit_ids,
                base_unit_id__in=all_unit_ids,
            )
            for conversion in conversions:
                unit_conversion_map[(conversion.unit.pk, conversion.base_unit.pk)] = conversion
        
        items_to_create = {}
        items_to_update = {}
        
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
            existing_item = existing_cart_items.get(ingredient.pk)
            # Если ингредиент уже добавлен / обновлен в предыдущих итерациях цикла
            in_progress = items_to_create.get(ingredient.pk) or items_to_update.get(ingredient.pk)
            
            # Не существует в корзине и не был в предыдущих итерациях цикла
            if existing_item is None and in_progress is None:
                # Добавляем в объект для создания
                items_to_create[ingredient.pk] = CartItem(
                    user=user,
                    ingredient=ingredient,
                    total_amount=amount_to_add,
                    unit=unit,
                    is_checked=False,
                )
            else:
                cart_item = in_progress or existing_item

                # Ингредиент есть в корзине, но пользователь когда-то отмечал, что у него есть дома
                # Обновляем запись с новыми данными и снимаем отметку
                if cart_item.is_checked:
                    # Обнуляем количество, позже прибавим из добавляемого ингредиента
                    cart_item.total_amount = Decimal(0)
                    cart_item.unit = unit
                    cart_item.is_checked = False
                # если ингредиента нет дома, но единицы измерения в корзине и в добавляемом не совпадают
                elif cart_item.unit.pk != unit.pk:
                    # Ищем конвертацию в ранее сделанном unit_conversion_map
                    unit_convertion = (
                        unit_conversion_map.get((unit.pk, cart_item.unit.pk))
                        or unit_conversion_map.get((cart_item.unit.pk, unit.pk))
                    )
                    if not unit_convertion:
                        # если конвертации нет, невозможно добавить сложить количества ингредиентов, возвращаем ответ
                        return Response(
                            {'detail': f'Невозможно увеличить количество существующего ингредиента ID {cart_item.pk},'
                                f'разные единицы измерения: в корзине {cart_item.unit}, у ингредиента {unit}'},
                            status=status.HTTP_400_BAD_REQUEST,
                        )
                    if unit_convertion.base_unit.pk == unit.pk:
                        # пересчитываем существующее количество в новых единицах измерения unit
                        cart_item.total_amount = cart_item.total_amount * unit_convertion.amount_per_unit
                        cart_item.unit = unit
                    else:
                        # пересчитываем добавляемое количество в существующих единицах измерения cart_item.unit
                        amount_to_add = amount_to_add * unit_convertion.amount_per_unit

                cart_item.total_amount += amount_to_add

                # Добавляем в объект только если не обрабатывали до этого такой ингредиент
                if existing_item and in_progress is None:
                    items_to_update[ingredient.pk] = cart_item
        
        # Создаем и обновляем все одной транзакцией
        with transaction.atomic():
            if items_to_create:
                CartItem.objects.bulk_create(items_to_create.values())
            if items_to_update:
                CartItem.objects.bulk_update(
                    items_to_update.values(),
                    fields=['total_amount', 'unit', 'is_checked'],
                )

        return Response({'detail': 'Корзина обновлена'}, status=status.HTTP_204_NO_CONTENT)
