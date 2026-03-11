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
    help = f'Генерирует тестовые данные таблиц (пользователь, любимые рецепты пользователя), создает админа (по умолчанию username {admin_name}, email {admin_email}, пароль {admin_password})'
    
    def add_arguments(self, parser):
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
    
    def handle(self, *args, **options):
        allergies_list = list(Allergy.objects.all())
        diet_types = DietType.objects.all()
        recipes_list = list(Recipe.objects.all())

        if not diet_types:
            self.stdout.write(self.style.ERROR(
                'Нет типов питания (DietType). Создайте их перед генерацией пользователей.'
            ))
            return
        if not recipes_list:
            self.stdout.write(self.style.WARNING(
                'Нет рецептов (Recipe). Пользователи будут созданы без избранных.'
            ))

        admin_name = options.get('admin_name')
        admin_email = options.get('admin_email')
        admin_password = options.get('admin_pswd')
        user_count = options.get('users')
        
        admin, created = User.objects.get_or_create(username=admin_name)
        admin.email = admin_email
        admin.set_password(admin_password)
        admin.is_superuser = True
        admin.is_staff = True
        admin.save()
        if created:
            self.stdout.write(self.style.SUCCESS(
                f'Суперпользователь создан: username={admin_name}, email={admin_email}'
            ))
        else:
            self.stdout.write(self.style.SUCCESS(
                f'Суперпользователь обновлён: username={admin_name}, email={admin_email}'
            ))

        for _ in range(user_count):
            username = fake.first_name()
            email = fake.email()
            while User.objects.filter(email=email).exists():
                email = fake.email()
            while User.objects.filter(username=username).exists():
                username = fake.first_name()
            
            user = User.objects.create_user(
                username=username,
                email=email,
                password=str(random.randint(100, 999)),
                portion_size=random.randint(1, 6),
                diet_type=random.choice(diet_types)
            )

            allergies_count = random.randint(0, len(allergies_list))
            user_allergies = random.sample(allergies_list, k=allergies_count)
            user.allergies.set(user_allergies)

            self.stdout.write(self.style.SUCCESS(f'Пользователь {user} создан'))
            
            favorites_count = random.randint(0, 5)
            random_recipes = random.sample(recipes_list, k=favorites_count)
            user_favorites = [UserFavorite(user=user, recipe=rand_recipe) for rand_recipe in random_recipes]
            UserFavorite.objects.bulk_create(user_favorites)
            self.stdout.write(f'В сумме пользователю {user.username} были добавлены {favorites_count} любимых блюд')
