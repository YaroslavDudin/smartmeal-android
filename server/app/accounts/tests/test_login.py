import pytest

pytestmark = pytest.mark.django_db

class TestLogin:
    url = '/auth/login/'

    def test_login_success(self, api_client, test_user):
        data = {
            'email': test_user.email,
            'password': 'StrongPassword123!'
        }
        response = api_client.post(self.url, data)
        assert response.status_code == 200
        assert 'access' in response.data
        assert 'refresh' in response.data

    def test_login_wrong_password(self, api_client, test_user):
        data = {
            'email': test_user.email,
            'password': 'WrongPassword999'
        }
        response = api_client.post(self.url, data)
        assert response.status_code == 401

    def test_protected_route_without_token(self, api_client):
        protected_url = '/auth/profile/'
        response = api_client.get(protected_url)
        assert response.status_code == 401
        