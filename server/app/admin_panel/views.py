import os
import uuid

from django.contrib.auth import authenticate
from django.conf import settings
from django.db.models import Q

from rest_framework import status, generics
from rest_framework.generics import RetrieveAPIView
from rest_framework.pagination import PageNumberPagination
from rest_framework.parsers import MultiPartParser, FormParser
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView

from rest_framework_simplejwt.tokens import RefreshToken

from app.accounts.models import User, DietType, Allergy
from app.recipes.models import (
    Recipe, RecipeIngredient, RecipeStep,
    Ingredient, IngredientCategory, Unit,
)
from app.menus.models import Menu

from .permissions import IsSuperuser
from .serializers import (
    AdminUserSerializer,
    AdminRecipeShortSerializer,
    AdminRecipeSerializer,
    AdminRecipeWriteSerializer,
    AdminRecipeIngredientSerializer,
    AdminRecipeStepSerializer,
    AdminIngredientSerializer,
    AdminIngredientWriteSerializer,
    AdminIngredientCategorySerializer,
    AdminUnitSerializer,
    AdminDietTypeSerializer,
    AdminAllergySerializer,
    AdminMenuSerializer,
    StatsSerializer,
)


# ---------------------------------------------------------------------------
# Pagination
# ---------------------------------------------------------------------------

class StandardPagination(PageNumberPagination):
    page_size = 20
    page_size_query_param = 'page_size'
    max_page_size = 100


# ---------------------------------------------------------------------------
# Auth views
# ---------------------------------------------------------------------------

class AdminTokenView(APIView):
    """
    POST /api/admin/auth/token/
    Accepts { email, password }, validates that user is a superuser,
    and returns a JWT access + refresh pair together with basic user data.
    """
    permission_classes = []
    authentication_classes = []

    def post(self, request):
        email = request.data.get('email', '').strip()
        password = request.data.get('password', '')

        if not email or not password:
            return Response(
                {'detail': 'email and password are required.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # authenticate() uses USERNAME_FIELD which is 'email' for this project
        user = authenticate(request, username=email, password=password)

        if user is None:
            return Response(
                {'detail': 'Invalid credentials.'},
                status=status.HTTP_401_UNAUTHORIZED,
            )

        if not user.is_superuser:
            return Response(
                {'detail': 'Only superusers can access the admin panel.'},
                status=status.HTTP_403_FORBIDDEN,
            )

        if not user.is_active:
            return Response(
                {'detail': 'User account is disabled.'},
                status=status.HTTP_403_FORBIDDEN,
            )

        refresh = RefreshToken.for_user(user)

        return Response({
            'access': str(refresh.access_token),
            'refresh': str(refresh),
            'user': {
                'id': user.id,
                'email': user.email,
                'username': user.username,
                'is_superuser': user.is_superuser,
            },
        })


class AdminMeView(RetrieveAPIView):
    """
    GET /api/admin/auth/me/
    Returns the currently authenticated superuser's profile.
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    serializer_class = AdminUserSerializer

    def get_object(self):
        return self.request.user


# ---------------------------------------------------------------------------
# Image upload
# ---------------------------------------------------------------------------

class ImageUploadView(APIView):
    """
    POST /api/admin/upload/image/
    Accepts multipart/form-data with an 'image' field.
    Saves to MEDIA_ROOT/recipes/ and returns the relative URL.
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    parser_classes = [MultiPartParser, FormParser]

    def post(self, request):
        image_file = request.FILES.get('image')
        if not image_file:
            return Response(
                {'detail': 'No image file provided.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        # Build a unique filename to avoid collisions
        ext = os.path.splitext(image_file.name)[1].lower()
        filename = f'{uuid.uuid4().hex}{ext}'
        save_dir = os.path.join(settings.MEDIA_ROOT, 'recipes')
        os.makedirs(save_dir, exist_ok=True)
        save_path = os.path.join(save_dir, filename)

        with open(save_path, 'wb+') as destination:
            for chunk in image_file.chunks():
                destination.write(chunk)

        relative_url = f'{settings.MEDIA_URL}recipes/{filename}'
        return Response({'url': relative_url}, status=status.HTTP_201_CREATED)


# ---------------------------------------------------------------------------
# Stats / dashboard
# ---------------------------------------------------------------------------

class StatsView(APIView):
    """
    GET /api/admin/stats/
    Returns aggregate counts and the last 5 recipes / users.
    """
    permission_classes = [IsAuthenticated, IsSuperuser]

    def get(self, request):
        recent_recipes = (
            Recipe.objects
            .prefetch_related('diet_types', 'recipe_ingredients__ingredient__ingredient_nutrition',
                              'recipe_ingredients__ingredient__unit_conversions',
                              'recipe_ingredients__unit')
            .order_by('-id')[:5]
        )
        recent_users = User.objects.select_related('diet_type').prefetch_related('allergies').order_by('-created_at')[:5]

        data = {
            'recipes_count': Recipe.objects.count(),
            'ingredients_count': Ingredient.objects.count(),
            'users_count': User.objects.count(),
            'menus_count': Menu.objects.count(),
            'recent_recipes': recent_recipes,
            'recent_users': recent_users,
        }
        serializer = StatsSerializer(data)
        return Response(serializer.data)


# ---------------------------------------------------------------------------
# Recipe views
# ---------------------------------------------------------------------------

class AdminRecipeListCreateView(generics.ListCreateAPIView):
    """
    GET  /api/admin/recipes/  – paginated list; supports ?search= and ?diet_type=<id>
    POST /api/admin/recipes/  – create new recipe
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    pagination_class = StandardPagination

    def get_queryset(self):
        qs = Recipe.objects.prefetch_related(
            'diet_types',
            'recipe_ingredients__ingredient__ingredient_nutrition',
            'recipe_ingredients__ingredient__unit_conversions',
            'recipe_ingredients__unit',
        )
        search = self.request.query_params.get('search')
        diet_type = self.request.query_params.get('diet_type')
        if search:
            qs = qs.filter(title__icontains=search)
        if diet_type:
            qs = qs.filter(diet_types__id=diet_type)
        return qs

    def get_serializer_class(self):
        if self.request.method == 'POST':
            return AdminRecipeWriteSerializer
        return AdminRecipeShortSerializer

    def create(self, request, *args, **kwargs):
        serializer = AdminRecipeWriteSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        recipe = serializer.save()
        return Response(
            AdminRecipeSerializer(recipe).data,
            status=status.HTTP_201_CREATED,
        )


class AdminRecipeDetailView(generics.RetrieveUpdateDestroyAPIView):
    """
    GET    /api/admin/recipes/<pk>/
    PATCH  /api/admin/recipes/<pk>/
    DELETE /api/admin/recipes/<pk>/
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    queryset = Recipe.objects.prefetch_related(
        'diet_types',
        'recipe_ingredients__ingredient__ingredient_nutrition',
        'recipe_ingredients__ingredient__unit_conversions',
        'recipe_ingredients__unit',
        'steps',
    )

    def get_serializer_class(self):
        if self.request.method in ('PATCH', 'PUT'):
            return AdminRecipeWriteSerializer
        return AdminRecipeSerializer

    def update(self, request, *args, **kwargs):
        partial = kwargs.pop('partial', False)
        instance = self.get_object()
        serializer = AdminRecipeWriteSerializer(instance, data=request.data, partial=partial)
        serializer.is_valid(raise_exception=True)
        recipe = serializer.save()
        return Response(AdminRecipeSerializer(recipe).data)

    def partial_update(self, request, *args, **kwargs):
        kwargs['partial'] = True
        return self.update(request, *args, **kwargs)


# ---------------------------------------------------------------------------
# Recipe Ingredient views
# ---------------------------------------------------------------------------

class AdminRecipeIngredientListCreateView(generics.ListCreateAPIView):
    """
    GET  /api/admin/recipes/<recipe_pk>/ingredients/
    POST /api/admin/recipes/<recipe_pk>/ingredients/
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    serializer_class = AdminRecipeIngredientSerializer

    def get_queryset(self):
        return RecipeIngredient.objects.filter(
            recipe_id=self.kwargs['recipe_pk']
        ).select_related('ingredient', 'unit')

    def perform_create(self, serializer):
        serializer.save(recipe_id=self.kwargs['recipe_pk'])


class AdminRecipeIngredientDetailView(generics.RetrieveUpdateDestroyAPIView):
    """
    GET    /api/admin/recipes/<recipe_pk>/ingredients/<pk>/
    PATCH  /api/admin/recipes/<recipe_pk>/ingredients/<pk>/
    DELETE /api/admin/recipes/<recipe_pk>/ingredients/<pk>/
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    serializer_class = AdminRecipeIngredientSerializer

    def get_queryset(self):
        return RecipeIngredient.objects.filter(
            recipe_id=self.kwargs['recipe_pk']
        ).select_related('ingredient', 'unit')


# ---------------------------------------------------------------------------
# Recipe Step views
# ---------------------------------------------------------------------------

class AdminRecipeStepListCreateView(generics.ListCreateAPIView):
    """
    GET  /api/admin/recipes/<recipe_pk>/steps/
    POST /api/admin/recipes/<recipe_pk>/steps/
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    serializer_class = AdminRecipeStepSerializer

    def get_queryset(self):
        return RecipeStep.objects.filter(recipe_id=self.kwargs['recipe_pk'])

    def perform_create(self, serializer):
        serializer.save(recipe_id=self.kwargs['recipe_pk'])


class AdminRecipeStepDetailView(generics.RetrieveUpdateDestroyAPIView):
    """
    GET    /api/admin/recipes/<recipe_pk>/steps/<pk>/
    PATCH  /api/admin/recipes/<recipe_pk>/steps/<pk>/
    DELETE /api/admin/recipes/<recipe_pk>/steps/<pk>/
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    serializer_class = AdminRecipeStepSerializer

    def get_queryset(self):
        return RecipeStep.objects.filter(recipe_id=self.kwargs['recipe_pk'])


# ---------------------------------------------------------------------------
# Ingredient views
# ---------------------------------------------------------------------------

class AdminIngredientListCreateView(generics.ListCreateAPIView):
    """
    GET  /api/admin/ingredients/  – paginated; supports ?search= and ?category=<id>
    POST /api/admin/ingredients/
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    pagination_class = StandardPagination

    def get_queryset(self):
        qs = Ingredient.objects.select_related(
            'category', 'ingredient_nutrition'
        ).prefetch_related('unit_conversions__unit')
        search = self.request.query_params.get('search')
        category = self.request.query_params.get('category')
        if search:
            qs = qs.filter(name__icontains=search)
        if category:
            qs = qs.filter(category_id=category)
        return qs

    def get_serializer_class(self):
        if self.request.method == 'POST':
            return AdminIngredientWriteSerializer
        return AdminIngredientSerializer

    def create(self, request, *args, **kwargs):
        serializer = AdminIngredientWriteSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        ingredient = serializer.save()
        return Response(
            AdminIngredientSerializer(ingredient).data,
            status=status.HTTP_201_CREATED,
        )


class AdminIngredientDetailView(generics.RetrieveUpdateDestroyAPIView):
    """
    GET    /api/admin/ingredients/<pk>/
    PATCH  /api/admin/ingredients/<pk>/
    DELETE /api/admin/ingredients/<pk>/
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    queryset = Ingredient.objects.select_related(
        'category', 'ingredient_nutrition'
    ).prefetch_related('unit_conversions__unit')

    def get_serializer_class(self):
        if self.request.method in ('PATCH', 'PUT'):
            return AdminIngredientWriteSerializer
        return AdminIngredientSerializer

    def update(self, request, *args, **kwargs):
        partial = kwargs.pop('partial', False)
        instance = self.get_object()
        serializer = AdminIngredientWriteSerializer(instance, data=request.data, partial=partial)
        serializer.is_valid(raise_exception=True)
        ingredient = serializer.save()
        return Response(AdminIngredientSerializer(ingredient).data)

    def partial_update(self, request, *args, **kwargs):
        kwargs['partial'] = True
        return self.update(request, *args, **kwargs)


# ---------------------------------------------------------------------------
# Ingredient Category views
# ---------------------------------------------------------------------------

class AdminCategoryListCreateView(generics.ListCreateAPIView):
    """
    GET  /api/admin/categories/
    POST /api/admin/categories/
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    queryset = IngredientCategory.objects.all()
    serializer_class = AdminIngredientCategorySerializer


class AdminCategoryDetailView(generics.RetrieveUpdateDestroyAPIView):
    """
    GET    /api/admin/categories/<pk>/
    PATCH  /api/admin/categories/<pk>/
    DELETE /api/admin/categories/<pk>/
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    queryset = IngredientCategory.objects.all()
    serializer_class = AdminIngredientCategorySerializer


# ---------------------------------------------------------------------------
# Unit views
# ---------------------------------------------------------------------------

class AdminUnitListCreateView(generics.ListCreateAPIView):
    """
    GET  /api/admin/units/
    POST /api/admin/units/
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    queryset = Unit.objects.all()
    serializer_class = AdminUnitSerializer


class AdminUnitDetailView(generics.RetrieveUpdateDestroyAPIView):
    """
    GET    /api/admin/units/<pk>/
    PATCH  /api/admin/units/<pk>/
    DELETE /api/admin/units/<pk>/
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    queryset = Unit.objects.all()
    serializer_class = AdminUnitSerializer


# ---------------------------------------------------------------------------
# User views
# ---------------------------------------------------------------------------

class AdminUserListView(generics.ListAPIView):
    """
    GET /api/admin/users/  – paginated; supports ?search= (email or username)
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    pagination_class = StandardPagination
    serializer_class = AdminUserSerializer

    def get_queryset(self):
        qs = User.objects.select_related('diet_type').prefetch_related('allergies')
        search = self.request.query_params.get('search')
        if search:
            qs = qs.filter(
                Q(email__icontains=search) | Q(username__icontains=search)
            )
        return qs


class AdminUserDetailView(generics.RetrieveUpdateAPIView):
    """
    GET   /api/admin/users/<pk>/
    PATCH /api/admin/users/<pk>/
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    serializer_class = AdminUserSerializer
    queryset = User.objects.select_related('diet_type').prefetch_related('allergies')


# ---------------------------------------------------------------------------
# DietType views
# ---------------------------------------------------------------------------

class AdminDietTypeListCreateView(generics.ListCreateAPIView):
    """
    GET  /api/admin/diet-types/
    POST /api/admin/diet-types/
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    queryset = DietType.objects.all()
    serializer_class = AdminDietTypeSerializer


class AdminDietTypeDetailView(generics.RetrieveUpdateDestroyAPIView):
    """
    GET    /api/admin/diet-types/<pk>/
    PATCH  /api/admin/diet-types/<pk>/
    DELETE /api/admin/diet-types/<pk>/
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    queryset = DietType.objects.all()
    serializer_class = AdminDietTypeSerializer


# ---------------------------------------------------------------------------
# Allergy views
# ---------------------------------------------------------------------------

class AdminAllergyListCreateView(generics.ListCreateAPIView):
    """
    GET  /api/admin/allergies/
    POST /api/admin/allergies/
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    queryset = Allergy.objects.all()
    serializer_class = AdminAllergySerializer


class AdminAllergyDetailView(generics.RetrieveUpdateDestroyAPIView):
    """
    GET    /api/admin/allergies/<pk>/
    PATCH  /api/admin/allergies/<pk>/
    DELETE /api/admin/allergies/<pk>/
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    queryset = Allergy.objects.all()
    serializer_class = AdminAllergySerializer


# ---------------------------------------------------------------------------
# Menu views
# ---------------------------------------------------------------------------

class AdminMenuListView(generics.ListAPIView):
    """
    GET /api/admin/menus/  – paginated
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    pagination_class = StandardPagination
    serializer_class = AdminMenuSerializer

    def get_queryset(self):
        return Menu.objects.select_related('user').prefetch_related(
            'items__recipe'
        )


class AdminMenuDetailView(generics.RetrieveDestroyAPIView):
    """
    GET    /api/admin/menus/<pk>/
    DELETE /api/admin/menus/<pk>/
    """
    permission_classes = [IsAuthenticated, IsSuperuser]
    serializer_class = AdminMenuSerializer
    queryset = Menu.objects.select_related('user').prefetch_related('items__recipe')
