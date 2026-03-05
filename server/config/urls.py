from django.contrib import admin
from django.urls import path , include
from django.conf  import settings
from django.conf.urls.static import static
from accounts.views import CustomAuthToken

urlpatterns = [
    path('admin/', admin.site.urls),
    path('api/auth/login/', CustomAuthToken.as_view(), name='api_token_auth'),
    path('api/meals/', include('meals.urls')),
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL , document_root = settings.MEDIA_ROOT)