from django.test import TestCase
from django.contrib.auth import get_user_model

User = get_user_model()

class AccountsModelTests(TestCase):
    def test_create_user(self):
        user = User.objects.create_user(
            username='testuser', 
            email='test@test.com', 
            password='password123',
            portion_size=2
        )
        self.assertEqual(user.email, 'test@test.com')
        self.assertEqual(user.portion_size, 2)
        self.assertTrue(user.check_password('password123'))
