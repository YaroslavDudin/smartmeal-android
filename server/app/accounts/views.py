from django.db import IntegrityError, transaction
from django.contrib.auth import get_user_model
from rest_framework import generics, permissions, status
from rest_framework.response import Response
from app.accounts.serializers import UserRegistrationSerializer


User = get_user_model()


class RegisterView(generics.CreateAPIView):
    serializer_class = UserRegistrationSerializer
    permission_classes = [permissions.AllowAny]
    
    def post(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        try:
            with transaction.atomic():
                user = serializer.save()
        except IntegrityError as e:
            if 'username' in str(e).lower():
                return Response(
                    {'username': ['Пользователь с таким именем уже был создан']},
                    status=status.HTTP_400_BAD_REQUEST
                )
            if 'email' in str(e).lower():
                return Response(
                    {'email': ['Пользователь с таким email уже был создан']},
                    status=status.HTTP_400_BAD_REQUEST
                )
            raise e

        return Response({
            'user': {
                'username': user.username,
                'email': user.email,
            },
            'message': 'Пользователь был успешно создан',
        }, status=status.HTTP_201_CREATED)
