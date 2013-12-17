from django.conf.urls import patterns, include, url
from django.conf import settings

# Uncomment the next two lines to enable the admin:
# from django.contrib import admin
# admin.autodiscover()

urlpatterns = patterns('labsheartbeatbackend.views',
    url(r'^viewevents/$', 'viewevents', name='viewevents'),
    url(r'^viewtwitterevents/$', 'viewtwitterevents', name='viewtwitterevents'),
    url(r'^saveevent/$', 'saveevent', name='saveevent'),
    url(r'^populate_test_data/$', 'populate_test_data', name='populate_test_data'),
    url(r'^_get_flickr_token/$', '_get_flickr_token', name='_get_flickr_token'),
    url(r'^_parse_flickr_token/$', '_parse_flickr_token', name='_parse_flickr_token'),
    url(r'^_upload_flickr_image/$', '_upload_flickr_image', name='_upload_flickr_image'),
    url(r'^upload_photobooth_image/$', 'upload_photobooth_image', name='upload_photobooth_image'),
)

if settings.DEBUG:
    # static files (images, css, javascript, etc.)
    urlpatterns += patterns('',
        (r'^media/(?P<path>.*)$', 'django.views.static.serve', {
        'document_root': settings.MEDIA_ROOT}))