from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('recipes', '0007_alter_recipeingredient_unique_together_and_more'),
    ]

    operations = [
        # Добавляем флаг is_base в Unit для замены хардкодных строк ('г', 'g', 'грамм' ...).
        # Базовые единицы (г, мл) можно пометить прямо в админке или через фикстуру.
        migrations.AddField(
            model_name='unit',
            name='is_base',
            field=models.BooleanField(default=False),
        ),
        # Исправляем опечатку в имени таблицы: unit_convertion → unit_conversion.
        migrations.AlterModelTable(
            name='unitconversion',
            table='unit_conversion',
        ),
        # Упрощаем ограничение: один ингредиент — одна запись на рецепт.
        # Старый вариант допускал дубли одного ингредиента в разных единицах.
        migrations.RemoveConstraint(
            model_name='recipeingredient',
            name='unique_recipe_ingredient_unit',
        ),
        migrations.AddConstraint(
            model_name='recipeingredient',
            constraint=models.UniqueConstraint(
                fields=['recipe', 'ingredient'],
                name='unique_recipe_ingredient',
            ),
        ),
    ]
