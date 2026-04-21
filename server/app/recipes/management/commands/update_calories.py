from django.core.management.base import BaseCommand
from app.recipes.models import Recipe

class Command(BaseCommand):
    help = 'Recalculates calories for all recipes'

    def handle(self, *args, **options):
        recipes = Recipe.objects.all()
        self.stdout.write(f"Updating {recipes.count()} recipes...")
        
        for r in recipes:
            try:
                r.update_nutrition_cache()
                self.stdout.write(self.style.SUCCESS(f"Updated: {r.title} (Cals: {r.per_serving_calories})"))
            except Exception as e:
                self.stdout.write(self.style.ERROR(f"Error {r.title}: {e}"))
