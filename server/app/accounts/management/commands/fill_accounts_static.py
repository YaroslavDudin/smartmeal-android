from django.core.management.base import BaseCommand
from app.accounts.models import Allergy, DietType


class Command(BaseCommand):
    help = 'Заполняет базу данных статичными данными (тип питания, аллергии), от которых зависят другие таблицы приложения accounts'
    
    def handle(self, *args, **options):
        allergy_data = ['Лактоза', 'Яйца', 'Рыба', 'Морепродукты', 'Арахис', 'Лесные орехи', 'Соя', 'Глютен', 'Цитрусовые']
        diet_type_data = ['Веганство', 'Вегетерианство', 'Кето', 'Палео', 'Халяль']
        
        for allergy_name in allergy_data:
            Allergy.objects.get_or_create(name=allergy_name)
        
        for diet_type_name in diet_type_data:
            DietType.objects.get_or_create(name=diet_type_name)
    