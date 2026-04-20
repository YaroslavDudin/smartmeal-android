from django.apps import AppConfig


class RecipesConfig(AppConfig):
    name = 'app.recipes'
    verbose_name = 'Рецепты'

    def ready(self):
        import app.recipes.signals
