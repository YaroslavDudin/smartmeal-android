import pytest
from rest_framework import status

pytestmark = pytest.mark.django_db

class TestLogout:
    login_url = '/api/accounts/login/'
    logout_url = '/api/accounts/logout/'
    me_url = '/api/accounts/me/'

    def test_logout_success(self, api_client, test_user):
        # 1. Login to get tokens
        login_data = {
            'email': test_user.email,
            'password': 'StrongPassword123!'
        }
        login_response = api_client.post(self.login_url, login_data)
        assert login_response.status_code == status.HTTP_200_OK
        
        access_token = login_response.data['access']
        refresh_token = login_response.data['refresh']

        # 2. Logout using refresh token
        api_client.credentials(HTTP_AUTHORIZATION=f'Bearer {access_token}')
        logout_response = api_client.post(self.logout_url, {'refresh': refresh_token})
        
        assert logout_response.status_code == status.HTTP_200_OK
        assert logout_response.data['detail'] == "Successfully logged out."

        # 3. Try to use the same refresh token to get a new access token (should fail)
        refresh_url = '/api/accounts/token/refresh/'
        refresh_response = api_client.post(refresh_url, {'refresh': refresh_token})
        assert refresh_response.status_code == status.HTTP_401_UNAUTHORIZED

    def test_logout_without_token(self, api_client):
        response = api_client.post(self.logout_url, {})
        assert response.status_code == status.HTTP_400_BAD_REQUEST

    def test_logout_invalid_token(self, api_client, test_user):
        # Authenticate first
        login_data = {
            'email': test_user.email,
            'password': 'StrongPassword123!'
        }
        login_response = api_client.post(self.login_url, login_data)
        access_token = login_response.data['access']
        
        api_client.credentials(HTTP_AUTHORIZATION=f'Bearer {access_token}')
        
        # Post invalid refresh token
        response = api_client.post(self.logout_url, {'refresh': 'invalid-token'})
        assert response.status_code == status.HTTP_200_OK
