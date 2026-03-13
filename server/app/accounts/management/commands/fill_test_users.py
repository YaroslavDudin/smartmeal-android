import datetime
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
    help = 'Генерирует тестовые данные таблиц (пользователь, любимые рецепты пользователя) приложения accounts, создает админа (по умолчанию username: {admin_name}, email: {admin_email}, пароль: {admin_password})'
    
    def add_arguments(self, parser):
        parser.add_argument(
            '--admin-name',
            type=str,
            default=admin_name,
            help='Имя суперпользователя (по умолчанию {admin_name})'
        )
        parser.add_argument(
            '--admin-email',
            type=str,
            default=admin_email,
            help='Email суперпользователя (по умолчанию {admin_email})'
        )
        parser.add_argument(
            '--admin-pswd',
            type=str,
            default=admin_password,
            help='Пароль суперпользователя (по умолчанию {admin_password})'
        )
        parser.add_argument(
            '--users',
            type=int,
            default=5,
            help='Количество обычных пользователей для создания (по умолчанию 5)'
        )
    
    def handle(self, *args, **options):
        allergies_list = list(Allergy.objects.all())
        diet_types_list = list(DietType.objects.all())
        recipes = list(Recipe.objects.all())

        admin_name = options.get('admin_name')
        admin_email = options.get('admin_email')
        admin_password = options.get('admin_pswd')
        user_count = options.get('users')
        
        admin, is_created = User.objects.get_or_create(username=admin_name)
        if is_created:
            admin.set_password(admin_password)
            admin.email = admin_email
            admin.is_superuser = True
            admin.is_staff = True
            admin.save()
            self.stdout.write(self.style.SUCCESS('Суперпользователь username: {admin_name}, email: {admin_email}, пароль: {admin_password} создан'))
        else:
            admin.email = admin_email
            admin.set_password(admin_password)
            admin.save()
            self.stdout.write(self.style.SUCCESS('Суперпользователь admin обновлён'))

        for _ in range(user_count):
            username = fake.first_name()
            email = fake.email()
            while User.objects.filter(email=email).exists():
                email = fake.email()
            while User.objects.filter(username=username).exists():
                username = fake.first_name()
            password = f'password{random.randint(100, 999)}'
            portion_size = random.randint(1, 6)
            user_allergies = random.sample(allergies_list, k=random.randint(0, len(allergies_list)))
            
            user = User.objects.create_user(
                username=username,
                email=email,
                password=password,
                portion_size=portion_size,
                diet_type=random.choice(diet_types_list),
            )
            user.allergies.set(user_allergies)
            self.stdout.write(f'Пользователь username {username} email {email} password {password} создан')
            
            favorites_count = random.randint(0, 5)
            for _ in range(favorites_count):
                UserFavorite.objects.get_or_create(
                    user=user,
                    recipe=random.choice(recipes)
                )
