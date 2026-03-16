from rest_framework.permissions import BasePermission


class IsSuperuser(BasePermission):
    """Allow access only to superusers."""
    message = 'Only superusers can access the admin panel.'

    def has_permission(self, request, view):
        return bool(
            request.user and
            request.user.is_authenticated and
            request.user.is_superuser
        )
