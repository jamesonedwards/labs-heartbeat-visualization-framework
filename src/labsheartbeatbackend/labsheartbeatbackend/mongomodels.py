# from django.db import models
from mongoengine import *
from django.conf import settings
import datetime, time

# Create your models here.
 
# Connect to a db (no need to create this - it will be created automagically)
connect(settings.DATABASES['default']['NAME'], host=settings.DATABASES['default']['HOST'])

class Event(Document):
    event_key = StringField(max_length=200, required=True)
    event_timestamp = DateTimeField(required=True)
    event_value = StringField(max_length=1000, required=True)
    event_tags = ListField(StringField(max_length=200), required=False)

    def __unicode__(self):
        return self.parsed_text
    
    def to_dict(self):
        return {
                'event_key': self.event_key,
                'event_datetime': str(self.event_timestamp),
                'event_timestamp': time.mktime(self.event_timestamp.timetuple()),
                'event_value': self.event_value,
                'event_tags': self.event_tags
                }

class Allowed_event_key(Document):
    key = StringField(max_length=200)

    def __unicode__(self):
        return self.key
