import random

from django.db import transaction
from rest_framework import viewsets, permissions, status
from rest_framework.decorators import action
from rest_framework.response import Response

from app.menus.models import Menu, MenuItem, Period, MealType
from app.menus.serializers import MenuSerializer, MenuItemSerializer, GenerateMenuSerializer
from app.recipes.models import Recipe

# Приёмы пищи, которые генерируются по умолчанию
_DEFAULT_MEAL_TYPES = [MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER]


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

    @action(detail=False, methods=['post'], url_path='generate')
    def generate(self, request):
        """
        POST /api/menus/generate/

        Автоматически создаёт меню и заполняет его рецептами.

        Алгоритм:
        1. Фильтрует рецепты по diet_type (из запроса или профиля пользователя)
           и max_cook_time (если передан).
        2. Детерминированный shuffle (seed = user_id + start_date + period).
        3. Заполняет слоты breakfast/lunch/dinner × days без повторов.
           Если рецептов меньше чем слотов — допускает повторы.
        4. Создаёт Menu + MenuItems атомарно.

        Возвращает созданное меню в формате MenuSerializer (включая items).
        """
        input_serializer = GenerateMenuSerializer(data=request.data)
        input_serializer.is_valid(raise_exception=True)
        data = input_serializer.validated_data

        period = data['period']
        start_date = data['start_date']
        days = 7 if period == Period.WEEK else 1
        total_slots = days * len(_DEFAULT_MEAL_TYPES)

        # diet_type: из запроса → из профиля → без фильтра
        diet_type_id = data.get('diet_type') or request.user.diet_type_id
        max_cook_time = data.get('max_cook_time')

        # Формируем пул рецептов (ORDER BY id — детерминировано)
        qs = Recipe.objects.all().order_by('id')
        if diet_type_id:
            qs = qs.filter(diet_types__id=diet_type_id)
        if max_cook_time:
            qs = qs.filter(cook_time__lte=max_cook_time)

        recipe_ids = list(qs.values_list('id', flat=True))

        if not recipe_ids:
            return Response(
                {'detail': 'Нет рецептов для заданных параметров. Попробуйте изменить фильтры.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Детерминированный shuffle: одни и те же параметры → одно и то же меню
        rng = random.Random(f"{request.user.id}-{start_date}-{period}")
        rng.shuffle(recipe_ids)

        # Если рецептов меньше чем слотов — циклически повторяем список
        selected: list[int] = []
        while len(selected) < total_slots:
            selected.extend(recipe_ids)
        selected = selected[:total_slots]

        # Создаём Menu + все MenuItems одной транзакцией
        with transaction.atomic():
            menu = Menu.objects.create(
                user=request.user,
                period=period,
                start_date=start_date,
            )
            items = []
            idx = 0
            for day_offset in range(days):
                for meal_type in _DEFAULT_MEAL_TYPES:
                    items.append(MenuItem(
                        menu=menu,
                        recipe_id=selected[idx],
                        day_offset=day_offset,
                        meal_type=meal_type,
                    ))
                    idx += 1
            MenuItem.objects.bulk_create(items)

        # Возвращаем полное меню с items (prefetch чтобы не делать N+1)
        created_menu = (
            Menu.objects
            .prefetch_related('items__recipe')
            .get(id=menu.id)
        )
        return Response(MenuSerializer(created_menu).data, status=status.HTTP_201_CREATED)


class MenuItemViewSet(viewsets.ModelViewSet):
    serializer_class = MenuItemSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        return (
            MenuItem.objects
            .filter(menu__user=self.request.user)
            .select_related('recipe', 'menu')
        )

    @action(detail=True, methods=['post'])
    def replace(self, request, pk=None):
        """
        POST /api/menus/items/{id}/replace/
        
        Заменяет рецепт в MenuItem на случайный другой, подходящий по диете.
        """
        menu_item = self.get_object()
        user = request.user
        
        # Получаем диету пользователя
        diet_type_id = user.diet_type_id
        
        # Базовый запрос для подходящих рецептов
        qs = Recipe.objects.all()
        if diet_type_id:
            qs = qs.filter(diet_types__id=diet_type_id)
            
        # Исключаем текущий рецепт
        qs = qs.exclude(id=menu_item.recipe_id)
        
        recipe_ids = list(qs.values_list('id', flat=True))
        
        if not recipe_ids:
            return Response(
                {'detail': 'Нет других подходящих рецептов для замены.'},
                status=status.HTTP_400_BAD_REQUEST
            )
            
        # Выбираем случайный рецепт
        new_recipe_id = random.choice(recipe_ids)
        
        menu_item.recipe_id = new_recipe_id
        menu_item.save()
        
        # Возвращаем обновленный объект (MenuItemSerializer подтянет новые данные рецепта)
        serializer = self.get_serializer(menu_item)
        return Response(serializer.data)
