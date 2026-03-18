import pytest
from django.contrib.auth import get_user_model

User = get_user_model()
pytestmark = pytest.mark.django_db


class TestRegistration:
    url = '/api/accounts/register/'

    def test_register_success(self, api_client):
        data = {
            'username': 'newuser',
            'email': 'new@example.com',
            'password': 'SecurePassword123!',
            'password_confirm': 'SecurePassword123!',
        }
        response = api_client.post(self.url, data)
        assert response.status_code == 201
        assert User.objects.filter(email='new@example.com').exists()

    def test_register_weak_password(self, api_client):
        data = {
            'username': 'weakling',
            'email': 'new@example.com',
            'password': '123',
            'password_confirm': '123',
        }
        response = api_client.post(self.url, data)
        assert response.status_code == 400

    def test_register_existing_email(self, api_client, test_user):
        data = {
            'username': 'anotheruser',
            'email': test_user.email,
            'password': 'SecurePassword123!',
            'password_confirm': 'SecurePassword123!',
        }
        response = api_client.post(self.url, data)
        assert response.status_code == 400
