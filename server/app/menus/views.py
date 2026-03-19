import random

from django.db import transaction
from rest_framework import viewsets, permissions, status
from rest_framework.decorators import action
from rest_framework.response import Response

from app.menus.models import Menu, MenuItem, Period, MealType
from app.menus.serializers import MenuSerializer, MenuItemSerializer, GenerateMenuSerializer
from app.recipes.models import Recipe


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

        # diet_type: из запроса → из профиля → без фильтра
        diet_type_id = data.get('diet_type') or request.user.diet_type_id
        allergy_ids = data.get('exclude_allergies') or set(request.user.allergies.values_list('id', flat=True))
        max_cook_time = data.get('max_cook_time')

        meal_types = list(MealType.objects.all().order_by('order'))
        if not meal_types:
            return Response(
                {'detail': 'В базе нет типов приемов пищи. Создайте их (MealType).'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # Формируем пул рецептов (ORDER BY id — детерминировано)
        qs = Recipe.objects.all().order_by('id')
        if diet_type_id:
            qs = qs.filter(diet_types__id=diet_type_id)
        if max_cook_time:
            qs = qs.filter(cook_time__lte=max_cook_time)
        if allergy_ids:
            qs = qs.exclude(
                recipe_ingredients__ingredient__allergies__id__in=allergy_ids
            ).distinct()


        if not qs.exists():
            return Response(
                {'detail': 'Нет рецептов для заданных параметров. Попробуйте изменить фильтры.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Детерминированный shuffle: одни и те же параметры → одно и то же меню
        pools = {}
        used_per_day = {day: set() for day in range(days)}
        for mt in meal_types:
            valid_recipes = list(qs.filter(meal_types=mt).values_list('id', flat=True))
            if not valid_recipes:
                return Response(
                    {'detail': f'Нет рецептов с заданными параметрами для приема пищи "{mt.name}". Попробуйте измените фильтры.'},
                    status=status.HTTP_400_BAD_REQUEST
                )

            rng = random.Random(f'{request.user.id}-{start_date}-{period}-{mt.id}')
            rng.shuffle(valid_recipes)

        # Набираем нужное количество блюд для этого приема пищи на все дни
            selected = []
            inx = 0
            for day in range(days):
                # Ищем подходящий рецепт, начиная с текущей позиции
                start_inx = inx
                while True:
                    candidate = valid_recipes[inx % len(valid_recipes)]
                    inx += 1
                    if candidate not in used_per_day[day]:
                        break
                    if inx % len(valid_recipes) == start_inx % len(valid_recipes):
                        # Обошли все рецепты, берем повтор
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
            .prefetch_related('items__recipe')
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
        """
        POST /api/menus/items/{id}/replace/

        Заменяет текущий рецепт в пункте меню на другой случайный.
        Учитывает MealType и DietType пользователя.
        """
        menu_item = self.get_object()
        user = request.user
        allergy_ids = set(user.allergies.values_list('id', flat=True))
        # Рецепты уже использованные в этот день (кроме текущего)
        other_today_recipes_ids = set(
            MenuItem.objects
            .filter(menu=menu_item.menu, day_offset=menu_item.day_offset)
            .exclude(id=menu_item.id)
            .values_list('recipe_id', flat=True)
        )

        # Определяем фильтры для подбора замены
        # 1. Тот же тип приема пищи (MealType)
        # 2. Другой рецепт (не текущий)
        # 3. Соответствие диете и аллергиям пользователя (если установлены)
        
        # На нужный прием пищи исключая заменяемый рецепт
        qs = Recipe.objects.filter(meal_types=menu_item.meal_type) \
            .exclude(id=menu_item.recipe_id)

        if not qs.exists():
            return Response(
                {'detail': 'Рецепты для замены не найдены'},
                status=status.HTTP_404_NOT_FOUND
            )

        # С фильтром по типу питания (диете):
        if user.diet_type_id:
            qs = qs.filter(diet_types__id=user.diet_type_id)

        if not qs.exists():
            return Response(
                {'detail': 'Не удалось найти подходящий рецепт для замены с указанным типом питания.'},
                status=status.HTTP_404_NOT_FOUND
            )

        # Исключая аллергены:
        if allergy_ids:
            qs = qs.exclude(
                recipe_ingredients__ingredient__allergies__id__in=allergy_ids
            ).distinct()

        if not qs.exists():
            return Response(
                {'detail': 'Не удалось найти подходящий рецепт для замены с указанными параметрами.'},
                status=status.HTTP_404_NOT_FOUND
            )
        
        # Пытаемся выбрать без совпадений с другими рецептами этого дня:
        new_recipe = qs.exclude(id__in=other_today_recipes_ids).order_by('?').first()

        # Выбираем случайный из подходящих игнорируя совпадения с рецептами на другие приемы пищи:
        if not new_recipe:
            new_recipe = qs.order_by('?').first()

        menu_item.recipe = new_recipe
        menu_item.save()

        serializer = self.get_serializer(menu_item)
        return Response(serializer.data, status=status.HTTP_200_OK)
