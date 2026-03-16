from django.contrib import admin
from django.urls import path, include
from django.conf import settings
from django.conf.urls.static import static

urlpatterns = [
    path('admin/', admin.site.urls),
    path('api/accounts/', include('app.accounts.urls')),
    path('api/recipes/', include('app.recipes.urls')),
    path('api/menus/', include('app.menus.urls')),
    path('api/cart/', include('app.cart.urls')),
    path('api/admin/', include('app.admin_panel.urls')),
]

if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
