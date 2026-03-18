from decimal import Decimal
from django.core.management.base import BaseCommand
from django.contrib.auth import get_user_model
from app.accounts.models import UserFavorite


User = get_user_model()


class Command(BaseCommand):
    help = 'Удаляет данные о пользователях и их избранных рецептах каскадом'

    def add_arguments(self, parser):
        parser.add_argument(
            '--no-admin',
            action='store_true',
            help='Не удалять записи, связанные с администраторами',
        )
        parser.add_argument(
            '--user-lookup',
            type=str,
            default='',
            help='Фильтрация пользователей: параметр1=значение1,параметр2=значение2'
        )
        parser.add_argument(
            '--favorite-lookup',
            type=str,
            default='',
            help='Фильтрация избранного: параметр1=значение1,параметр2=значение2'
        )
        parser.add_argument(
            '--only-favorites',
            action='store_true',
            help='Удалить только записи избранного (пользователи не удаляются)',
        )

    def handle(self, *args, **options):
        no_admin = options['no_admin']
        only_favorites = options['only_favorites']
        user_lookup = options['user_lookup']
        favorite_lookup = options['favorite_lookup']

        user_lookup_params = self._parse_lookup(user_lookup)
        favorite_lookup_params = self._parse_lookup(favorite_lookup)

        if user_lookup_params is None or favorite_lookup_params is None:
            return

        if no_admin:
            user_lookup_params['is_superuser'] = False

        if only_favorites:
            self._delete_favorites(user_lookup_params, favorite_lookup_params)
        else:
            if not user_lookup_params and not favorite_lookup_params:
                confirm = input(self.style.WARNING('Удалить всех пользователей? [y/n]: '))
                if confirm.lower() != 'y':
                    self.stdout.write('Отменено')
                    return
            self._delete_users(user_lookup_params)

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

    def _delete_favorites(self, user_lookup_params, favorite_lookup_params):
        favorites = UserFavorite.objects.filter(**favorite_lookup_params)
        if user_lookup_params:
            favorites = favorites.filter(user__in=User.objects.filter(**user_lookup_params))

        if not favorites.exists():
            self.stdout.write(self.style.WARNING('Нет записей избранного для удаления'))
            return

        favorites.delete()
        self.stdout.write(self.style.SUCCESS('Записи избранного удалены'))

    def _delete_users(self, user_lookup_params):
        users = User.objects.filter(**user_lookup_params)

        if not users.exists():
            self.stdout.write(self.style.WARNING('Нет пользователей для удаления'))
            return

        users.delete()
        self.stdout.write(self.style.SUCCESS('Пользователи удалены'))
