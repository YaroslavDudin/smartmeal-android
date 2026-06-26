from django.db import migrations, models
import django.core.validators


class Migration(migrations.Migration):

    dependencies = [
        ('accounts', '0016_remove_userfavorite_unique_user_favorite_recipe_and_more'),
    ]

    operations = [
        migrations.AddField(
            model_name='user',
            name='calories_enabled',
            field=models.BooleanField(default=False, verbose_name='Планировать по калориям'),
        ),
        migrations.AddField(
            model_name='user',
            name='target_calories',
            field=models.PositiveSmallIntegerField(
                default=2000,
                validators=[
                    django.core.validators.MinValueValidator(1200),
                    django.core.validators.MaxValueValidator(3000),
                ],
                verbose_name='Целевая калорийность',
            ),
        ),
        migrations.AddField(
            model_name='user',
            name='calorie_margin',
            field=models.PositiveSmallIntegerField(
                default=100,
                validators=[
                    django.core.validators.MinValueValidator(50),
                    django.core.validators.MaxValueValidator(500),
                ],
                verbose_name='Допустимый разброс калорий',
            ),
        ),
        migrations.AddField(
            model_name='user',
            name='protein_percent',
            field=models.PositiveSmallIntegerField(
                default=20,
                validators=[
                    django.core.validators.MinValueValidator(10),
                    django.core.validators.MaxValueValidator(80),
                ],
                verbose_name='Белки, %',
            ),
        ),
        migrations.AddField(
            model_name='user',
            name='fat_percent',
            field=models.PositiveSmallIntegerField(
                default=30,
                validators=[
                    django.core.validators.MinValueValidator(10),
                    django.core.validators.MaxValueValidator(80),
                ],
                verbose_name='Жиры, %',
            ),
        ),
        migrations.AddField(
            model_name='user',
            name='carbs_percent',
            field=models.PositiveSmallIntegerField(
                default=50,
                validators=[
                    django.core.validators.MinValueValidator(10),
                    django.core.validators.MaxValueValidator(80),
                ],
                verbose_name='Углеводы, %',
            ),
        ),
    ]
