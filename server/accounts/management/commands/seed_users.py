import random
from django.core.management.base import BaseCommand
from django.contrib.auth import get_user_model
from accounts.models import Allergy

User = get_user_model()

class Command(BaseCommand):
    help = 'Seeds the database with 5 users in the accounts app'

    def handle(self, *args, **kwargs):
        self.stdout.write('Seeding users...')

        # Ensure allergies exist first
        allergies_data = ['Nuts', 'Dairy', 'Gluten', 'Seafood', 'Soy']
        allergies = []
        for name in allergies_data:
            allergy, _ = Allergy.objects.get_or_create(name=name)
            allergies.append(allergy)

        diet_types = ['classic', 'vegetarian', 'vegan', 'keto', 'paleo']
        portion_sizes = ['small', 'medium', 'large']
        names = ['Alex', 'Maria', 'Ivan', 'Sophia', 'Dmitry']

        for i in range(1, 6):
            email = f'user{i}@example.com'
            username = f'user{i}'
            # Using get_or_create to avoid duplicates if run multiple times
            user, created = User.objects.get_or_create(
                email=email,
                defaults={
                    'username': username,
                    'diet_type': random.choice(diet_types),
                    'portion_size': random.choice(portion_sizes),
                    'first_name': names[i-1],
                }
            )

            if created:
                user.set_password('password123')
                # Assign 0-2 random allergies to the user
                user_allergies = random.sample(allergies, random.randint(0, 2))
                user.allergies.set(user_allergies)
                user.save()
                self.stdout.write(f'Created user: {email} (pw: password123)')
            else:
                self.stdout.write(f'User {email} already exists.')

        self.stdout.write(self.style.SUCCESS('Successfully seeded 5 users!'))
