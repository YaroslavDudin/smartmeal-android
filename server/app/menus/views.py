import random

from django.db import transaction
from django.db.models import Prefetch
from rest_framework import viewsets, permissions, status
from rest_framework.decorators import action
from rest_framework.response import Response

from app.menus.models import Menu, MenuItem, Period, MealType
from app.menus.serializers import MenuSerializer, MenuItemSerializer, GenerateMenuSerializer
from app.recipes.models import Recipe


def filter_recipes_by_cook_time(qs, cook_time_range):
    """
    Фильтрует QuerySet рецептов по выбранному диапазону времени.
    Если выбрано 'any' или значение не указано, фильтрация не применяется.
    """
    if not cook_time_range or cook_time_range == 'any':
        return qs
        
    if cook_time_range == 'short':
        return qs.filter(cook_time__lte=30)
    elif cook_time_range == 'medium':
        # От 30 до 60 (исключая границы 30 и 60, так как они в short и long)
        return qs.filter(cook_time__gt=30, cook_time__lt=60)
    elif cook_time_range == 'long':
        # 60 минут и более
        return qs.filter(cook_time__gte=60)
    return qs


class MenuViewSet(viewsets.ModelViewSet):
    serializer_class = MenuSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        return (
            Menu.objects
            .filter(user=self.request.user)
            .prefetch_related(
                Prefetch(
                    'items',
                    queryset=MenuItem.objects.select_related('recipe', 'meal_type'),
                )
            )
        )

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)

    @action(detail=False, methods=['post'], url_path='generate')
    def generate(self, request):
        input_serializer = GenerateMenuSerializer(data=request.data)
        input_serializer.is_valid(raise_exception=True)
        data = input_serializer.validated_data

        period = data['period']
        start_date = data['start_date']
        
        if 'days' in data:
            days = data['days']
        else:
            days = 7 if period == 'week' else 1

        diet_type_id = data.get('diet_type') or request.user.diet_type_id
        allergy_ids = data.get('exclude_allergies') or set(request.user.allergies.values_list('id', flat=True))
        
        # cook_time_range: берем строго из запроса или профиля
        cook_time_range = data.get('cook_time_range') or request.user.preferred_cook_time
        max_cook_time = data.get('max_cook_time')

        meal_types = list(MealType.objects.all().order_by('order'))
        if not meal_types:
            return Response(
                {'detail': 'В базе нет типов приемов пищи.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        qs = Recipe.objects.all().order_by('id')
        if diet_type_id:
            qs = qs.filter(diet_types__id=diet_type_id)
        
        # Применяем фильтр по времени строго
        if max_cook_time:
            qs = qs.filter(cook_time__lte=max_cook_time)
        
        # Если есть cook_time_range, применяем его дополнительно или вместо max_cook_time
        if cook_time_range and cook_time_range != 'any':
            qs = filter_recipes_by_cook_time(qs, cook_time_range)

        if allergy_ids:
            qs = qs.exclude(
                recipe_ingredients__ingredient__allergies__id__in=allergy_ids
            ).distinct()

        # Проверка пула для КАЖДОГО приема пищи
        pools = {}
        used_per_day = {day: set() for day in range(days)}
        for mt in meal_types:
            valid_recipes = list(qs.filter(meal_types=mt).values_list('id', flat=True))
            if not valid_recipes:
                return Response(
                    {'detail': f'Для приема пищи "{mt.name}" нет рецептов, подходящих под ваши фильтры времени или диеты.'},
                    status=status.HTTP_400_BAD_REQUEST
                )

            rng = random.Random(f'{request.user.id}-{start_date}-{period}-{mt.id}')
            rng.shuffle(valid_recipes)
            
            selected = []
            inx = 0
            for day in range(days):
                start_inx = inx
                candidate = None
                while True:
                    candidate = valid_recipes[inx % len(valid_recipes)]
                    inx += 1
                    if candidate not in used_per_day[day]:
                        break
                    if inx % len(valid_recipes) == start_inx % len(valid_recipes):
                        # Если все рецепты перебрали и они все уже есть в этот день — берем первый попавшийся из подходящих под фильтр
                        candidate = valid_recipes[start_inx % len(valid_recipes)]
                        break
                selected.append(candidate)
                used_per_day[day].add(candidate)
            pools[mt.id] = selected


        # Создаём Menu + все MenuItems одной транзакцией
        with transaction.atomic():
            menu = Menu.objects.create(
                user=request.user,
                period=period,
                start_date=start_date,
            )
            items = []
            for day_offset in range(days):
                for mt in meal_types:
                    recipe_id = pools[mt.id][day_offset]
                    items.append(MenuItem(
                        menu=menu,
                        recipe_id=recipe_id,
                        day_offset=day_offset,
                        meal_type=mt,
                    ))
            MenuItem.objects.bulk_create(items)
                
        # Возвращаем полное меню с items (prefetch чтобы не делать N+1)
        created_menu = (
            Menu.objects
            .prefetch_related(
                Prefetch(
                    'items',
                    queryset=MenuItem.objects.select_related('recipe', 'meal_type'),
                )
            )
            .get(id=menu.id)
        )
        serializer = self.get_serializer(created_menu)
        return Response(serializer.data, status=status.HTTP_201_CREATED)


class MenuItemViewSet(viewsets.ModelViewSet):
    serializer_class = MenuItemSerializer
    permission_classes = [permissions.IsAuthenticated]

    def get_queryset(self):
        return (
            MenuItem.objects
            .filter(menu__user=self.request.user)
            .select_related('recipe', 'menu', 'meal_type')
        )

    @action(detail=True, methods=['post'], url_path='replace')
    def replace(self, request, pk=None):
        menu_item = self.get_object()
        user = request.user
        allergy_ids = set(user.allergies.values_list('id', flat=True))
        
        other_today_recipes_ids = set(
            MenuItem.objects
            .filter(menu=menu_item.menu, day_offset=menu_item.day_offset)
            .exclude(id=menu_item.id)
            .values_list('recipe_id', flat=True)
        )

        # Базовый набор рецептов для этого приема пищи
        qs = Recipe.objects.filter(meal_types=menu_item.meal_type).exclude(id=menu_item.recipe_id)
        if not qs.exists():
            return Response({'detail': 'В базе вообще нет других рецептов для этого приема пищи.'}, status=status.HTTP_404_NOT_FOUND)

        # 1. Фильтр по диете
        if user.diet_type_id:
            qs = qs.filter(diet_types__id=user.diet_type_id)
            if not qs.exists():
                return Response({'detail': 'Невозможно обновить блюдо, так как нет рецептов, подходящих под ваш тип питания.'}, status=status.HTTP_404_NOT_FOUND)

        # 2. Фильтр по аллергиям
        if allergy_ids:
            qs_before_allergy = qs
            qs = qs.exclude(recipe_ingredients__ingredient__allergies__id__in=allergy_ids).distinct()
            if not qs.exists():
                return Response({'detail': 'Невозможно обновить блюдо, так как нет рецептов, подходящих под ваши ограничения по аллергии.'}, status=status.HTTP_404_NOT_FOUND)

        # 3. Фильтр по времени готовки
        if user.preferred_cook_time and user.preferred_cook_time != 'any':
            qs_before_time = qs
            qs = filter_recipes_by_cook_time(qs, user.preferred_cook_time)
            if not qs.exists():
                return Response({'detail': f'Невозможно обновить блюдо, так как нет рецептов, подходящих под ваше время приготовления ({user.get_preferred_cook_time_display()}).'}, status=status.HTTP_404_NOT_FOUND)
        
        # Пытаемся выбрать без совпадений с другими рецептами этого дня
        new_recipe = qs.exclude(id__in=other_today_recipes_ids).order_by('?').first()

        if not new_recipe:
            new_recipe = qs.order_by('?').first()

        menu_item.recipe = new_recipe
        menu_item.save()

        serializer = self.get_serializer(menu_item)
        return Response(serializer.data, status=status.HTTP_200_OK)
