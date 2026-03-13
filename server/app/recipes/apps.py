from django.apps import AppConfig


class RecipesConfig(AppConfig):
    name = 'app.recipes'

    def ready(self):
        import app.recipes.signals
