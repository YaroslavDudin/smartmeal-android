from decimal import Decimal
import random

from django.core.management.base import BaseCommand
from django.contrib.auth import get_user_model
from faker import Faker

from app.accounts.models import Allergy, DietType, UserFavorite
from app.recipes.models import Recipe


User = get_user_model()
fake = Faker('ru_RU')

admin_name = 'admin'
admin_email = 'test@admin.com'
admin_password = 'test'


class Command(BaseCommand):
    help = f'Генерирует тестовые данные таблиц (пользователь, любимые рецепты пользователя) приложения accounts, создает админа (по умолчанию username: {admin_name}, email: {admin_email}, пароль: {admin_password})'
    
    def add_arguments(self, parser):
        parser.add_argument(
            '--no-admin',
            action='store_true',
            help='Добавлять/создавать ли администратора, добавлять ли ему избранные рецепты'
        )
        parser.add_argument(
            '--admin-name',
            type=str,
            default=admin_name,
            help=f'Имя суперпользователя (по умолчанию {admin_name})'
        )
        parser.add_argument(
            '--admin-email',
            type=str,
            default=admin_email,
            help=f'Email суперпользователя (по умолчанию {admin_email})'
        )
        parser.add_argument(
            '--admin-pswd',
            type=str,
            default=admin_password,
            help=f'Пароль суперпользователя (по умолчанию {admin_password})'
        )
        parser.add_argument(
            '--users',
            type=int,
            default=5,
            help='Количество обычных пользователей для создания (по умолчанию 5)'
        )
        parser.add_argument(
            '--user-lookup',
            type=str,
            default='',
            help='Параметры фильтрации пользователей в формате параметр1=значение1,параметр2=значение2'
        )
        parser.add_argument(
            '--only-users',
            action='store_true',
            help='Создавать только пользователей без записей об избранных рецептах'
        )
        parser.add_argument(
            '--recipe-lookup',
            type=str,
            default='',
            help='Параметры фильтрации рецептов в формате параметр1=значение1,параметр2=значение2'
        )
        parser.add_argument(
            '--favorites',
            type=int,
            default=5,
            help='Количество избранных рецептов для добавления пользователю (по умолчанию 5)'
        )
        parser.add_argument(
            '--only-favorites',
            action='store_true',
            help='Создавать только записи об избранных рецептах, не создавать пользователей'
        )
    
    def handle(self, *args, **options):
        no_admin = options.get('no_admin')
        admin_name = options.get('admin_name')
        admin_email = options.get('admin_email')
        admin_password = options.get('admin_pswd')
        user_count = options.get('users')
        user_lookup = options.get('user_lookup')
        only_users = options.get('only_users')
        recipe_lookup = options.get('recipe_lookup')
        favorites_count = options.get('favorites')
        only_favorites = options.get('only_favorites')
        
        user_lookup_params = self._parse_lookup(user_lookup)
        
        if only_favorites:
            if no_admin:
                user_lookup_params['is_superuser'] = False
            
            users = User.objects.filter(**user_lookup_params)
        
            if not users.exists():
                self.stdout.write(self.style.ERROR(f'Пользователей с такими параметрами не существует: {user_lookup}'))
                return
        else:
            users = self._create_users(user_count)
            
            if not no_admin:
                users.append(self._create_or_update_admin(admin_name, admin_email, admin_password))
                
            if only_users:
                return
        
        recipe_lookup_params = self._parse_lookup(recipe_lookup)
        recipes = Recipe.objects.filter(**recipe_lookup_params)
        self._create_favorites(users, recipes, favorites_count)
    
    def _parse_lookup(self, lookup_str):
        params = {}
        if not lookup_str:
            return params
        for item in lookup_str.split(','):
            if '=' not in item:
                self.stdout.write(self.style.ERROR(f'Неверный формат параметра: {item}'))
                return None
            param, value = item.split('=', 1)
            params[param.strip()] = self._parse_value(value.strip())
        return params

    def _parse_value(self, value):
        if value.lower() == 'true':
            return True
        if value.lower() == 'false':
            return False
        if value.isdigit():
            return int(value)
        if value.isdecimal():
            return Decimal(value)
        return value

    def _create_or_update_admin(self, admin_name, admin_email, admin_password):
        admin, is_created = User.objects.get_or_create(username=admin_name)
        if is_created:
            admin.set_password(admin_password)
            admin.email = admin_email
            admin.is_superuser = True
            admin.is_staff = True
            admin.save()
            self.stdout.write(self.style.SUCCESS(f'Суперпользователь username: {admin_name}, email: {admin_email}, пароль: {admin_password} создан'))
        else:
            admin.email = admin_email
            admin.set_password(admin_password)
            admin.save()
            self.stdout.write(self.style.SUCCESS('Суперпользователь admin обновлён'))
        return admin
    
    def _create_users(self, user_count):
        allergies_list = list(Allergy.objects.all())
        diet_types_list = list(DietType.objects.all())
        existing_emails = set(User.objects.values_list('email', flat=True))
        existing_usernames = set(User.objects.values_list('username', flat=True))
        new_users = []
        
        users_allergies = []
        for _ in range(user_count):
            username = fake.first_name()
            email = fake.email()
            while email in existing_emails:
                email = fake.email()
            while username in existing_usernames:
                username = fake.first_name()
            password = f'password{random.randint(100, 999)}'
            portion_size = random.randint(1, 6)
            
            user = User(
                username=username,
                email=email,
                portion_size=portion_size,
                diet_type=random.choice(diet_types_list),
            )
            user.set_password(password)

            users_allergies.append(random.sample(allergies_list, k=random.randint(0, len(allergies_list))))
            new_users.append(user)
            self.stdout.write(f'Будет создан пользователь username {username} email {email} password {password}')
        
        User.objects.bulk_create(new_users)
        self.stdout.write(self.style.SUCCESS('Пользователи успешно созданы'))

        for user, allergies in zip(new_users, users_allergies):
            user.allergies.set(allergies)
            allergies_str = ', '.join(a.name for a in allergies)
            self.stdout.write(self.style.SUCCESS(f'Пользователю {user} добавлены аллергии на {allergies_str}'))

        return new_users
    
    def _create_favorites(self, users, recipes, favorites_count):
        recipes_list = list(recipes)
        k = min(favorites_count, len(recipes_list))
        user_favorites = []
        for user in users:
            recipes_for_user = random.sample(recipes_list, k=k)
            for recipe in recipes_for_user:
                user_favorites.append(UserFavorite(user=user, recipe=recipe))
                self.stdout.write(f'Будет добавлен рецепт {recipe} в избранное пользователя {user}')
        UserFavorite.objects.bulk_create(user_favorites, ignore_conflicts=True)
        self.stdout.write(self.style.SUCCESS('Избранные успешно добавлены пользователям'))
