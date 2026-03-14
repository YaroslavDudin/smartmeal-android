from django.db import migrations, models
import django.utils.timezone


class Migration(migrations.Migration):

    dependencies = [
        ('menus', '0004_alter_menu_period_alter_menuitem_meal_type'),
    ]

    operations = [
        # Добавляем created_at в Menu (поле из ER-диаграммы, ранее отсутствовало).
        # Существующие строки получат текущую дату (django.utils.timezone.now).
        migrations.AddField(
            model_name='menu',
            name='created_at',
            field=models.DateTimeField(auto_now_add=True, default=django.utils.timezone.now),
            preserve_default=False,
        ),
        # Добавляем сортировку по умолчанию.
        migrations.AlterModelOptions(
            name='menu',
            options={'ordering': ['-created_at']},
        ),
        # Уникальное ограничение: в одном меню не может быть двух одинаковых
        # слотов (день + тип приёма пищи).
        migrations.AddConstraint(
            model_name='menuitem',
            constraint=models.UniqueConstraint(
                fields=['menu', 'day_offset', 'meal_type'],
                name='unique_menu_day_meal_slot',
            ),
        ),
    ]
