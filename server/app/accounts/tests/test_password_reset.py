import pytest
from django.contrib.auth import get_user_model
from django.core import mail
from django.utils.http import urlsafe_base64_encode
from django.utils.encoding import force_bytes
from django.contrib.auth.tokens import default_token_generator

User = get_user_model()
pytestmark = pytest.mark.django_db

class TestPasswordReset:
    reset_url = '/api/accounts/password-reset/'
    confirm_url = '/api/accounts/password-reset-confirm/'

    def test_password_reset_request_success(self, api_client, test_user):
        data = {'email': test_user.email}
        response = api_client.post(self.reset_url, data)
        
        assert response.status_code == 200
        assert len(mail.outbox) == 1
        assert test_user.email in mail.outbox[0].to
        assert 'reset-password' in mail.outbox[0].body

    def test_password_reset_request_non_existent_email(self, api_client):
        data = {'email': 'nonexistent@example.com'}
        response = api_client.post(self.reset_url, data)
        
        # Should still return 200 to avoid email harvesting
        assert response.status_code == 200
        assert len(mail.outbox) == 0

    def test_password_reset_confirm_success(self, api_client, test_user):
        uid = urlsafe_base64_encode(force_bytes(test_user.pk))
        token = default_token_generator.make_token(test_user)
        
        data = {
            'uid': uid,
            'token': token,
            'new_password': 'NewSecurePassword123!',
            'new_password_confirm': 'NewSecurePassword123!'
        }
        response = api_client.post(self.confirm_url, data)
        
        assert response.status_code == 200
        test_user.refresh_from_db()
        assert test_user.check_password('NewSecurePassword123!')

    def test_password_reset_confirm_invalid_token(self, api_client, test_user):
        uid = urlsafe_base64_encode(force_bytes(test_user.pk))
        token = 'invalid-token'
        
        data = {
            'uid': uid,
            'token': token,
            'new_password': 'NewSecurePassword123!',
            'new_password_confirm': 'NewSecurePassword123!'
        }
        response = api_client.post(self.confirm_url, data)
        
        assert response.status_code == 400
        assert 'token' in response.data
