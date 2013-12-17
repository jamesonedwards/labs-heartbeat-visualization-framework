# Create your views here.
# from django.shortcuts import render_to_response
# from django.template import RequestContext
# from django.core import serializers
#from django.utils import simplejson
#from time import gmtime, strftime
from django.http import HttpResponse
from django.http import HttpResponseRedirect
from datetime import datetime
from random import randrange
from datetime import timedelta
import time
# import labsheartbeatbackend
from labsheartbeatbackend import settings
from labsheartbeatbackend.mongomodels import *
from util import ApiResponse
from tweepyparser import TweepyRawModelParser
from bson import json_util, MAX_INT32
import json
import sys
from django.views.decorators.csrf import csrf_exempt
import tweepy
import unicodedata
from pprint import pprint
from TwitterSearch import *
from django.core.cache import cache
from labstwittersearchorder import *
from django.utils import http
import os
from flickrapi import *
import xml.etree.ElementTree as ET 
from django.utils.timezone import pytz

def viewtwitterevents(request):
    '''
    Query the Twitter API for a given hashtag and date range, using these Twitter-Python bindings:
    https://github.com/ckoepp/TwitterSearch
    '''
    try:
        # Hashtag is required.
        hashtag = None
        query = None
        if 'hashtag' in request.GET and len(request.GET['hashtag']) > 0:
            hashtag = __sanitize_input(request.GET['hashtag'])
        else:
            raise Exception('Hashtag is required!')
        # If datetime range is supplied, use that, else return all events.
        dtStart = dtEnd = None
        strStart = strEnd = None
        if 'date_start' in request.GET and request.GET['date_start']:
            strStart = request.GET['date_start']
            dtStart = datetime.datetime.strptime(strStart, "%Y-%m-%d").date()
        if 'date_end' in request.GET and request.GET['date_end']:
            strEnd = request.GET['date_end']
            dtEnd = datetime.datetime.strptime(strEnd, "%Y-%m-%d").date()
        # Check for teh "no tweet cap" param.
        noTweetCap = False
        if 'no_tweet_cap' in request.GET and request.GET['no_tweet_cap'] == 'true':
            noTweetCap = True
        # First, check the cache for the Twitter API result.
        cacheKey = hashtag + '_' + strStart + '_' + strEnd
        secondaryCacheFilePath = settings.SECONDARY_CACHE_DIRECTORY + cacheKey + '.json' 
        response = cache.get(cacheKey)
        if response == None:
            if os.path.isfile(secondaryCacheFilePath):
                with open(secondaryCacheFilePath, "r") as textFile:
                    response = json.load(textFile)
        if response == None or len(response) == 0:
            totalEventCnt = 0
            totalEventCntThresh = 0
            events = []
            tsMax = 0
            tsMin = sys.maxint
            # Authenticate with Twitter API.
            tso = TwitterSearchOrder()
            tso.setLanguage('en')
            tso.setCount(100)
            tso.setIncludeEntities(False)
            tso.setResultType('recent')
            # Create a TwitterSearch object with our secret tokens
            twitterSearch = TwitterSearch(
                consumer_key=settings.TWITTER_CONSUMER_KEY,
                consumer_secret=settings.TWITTER_CONSUMER_SECRET,
                access_token=settings.TWITTER_ACCESS_TOKEN,
                access_token_secret=settings.TWITTER_ACCESS_TOKEN_SECRET
             )
            twitterSearch.authenticate()
            # Construct and run the twitter search query.
            if dtStart != None and dtEnd != None:
                query = hashtag
                tso.setUntil(dtEnd)
            else:
                query = hashtag
            tso.setKeywords([query])
            maxId = 0
            tweetCnt = MAX_INT32
            doLoop = True
            # Page through the Twitter search API results until we either get no results or we arrive at the start date.
            while (doLoop):
                # Exit conditions.
                if not doLoop:
                    break;
                if tweetCnt == 0:
                    break
                if maxId > 0:
                    tso.setMaxID(maxId)
                    tso.setKeywords([hashtag])
                # Reset counter.
                tweetCnt = 0
                # Reset last tweet.
                lastTweet = None
                # Create an additional retry loop for when Twitter refuses the next page.
                try:
                    for tweet in twitterSearch.searchTweetsIterable(tso):
                        dt = __getDateFromTweetCreatedAt(tweet['created_at'])
                        if dt.date() < dtStart:
                            doLoop = False
                            break;
                        ts = time.mktime(dt.timetuple())
                        if ts > tsMax:
                            tsMax = ts
                        if ts < tsMin:
                            tsMin = ts
                        lastTweet = tweet
                        # Copy search results to the Event list.
                        events.append(
                                      {
                                       'event_key': hashtag,
                                       'event_datetime': str(tweet['created_at']),
                                       'event_timestamp': ts,
                                       'event_value': tweet['text'],
                                       'event_tags': [hashtag],
                                       'raw_data': tweet
                                       }
                                      )
                        # Increment counter.
                        tweetCnt += 1
                        totalEventCnt += 1
                        totalEventCntThresh += 1
                        if totalEventCntThresh >= 1000:
                            print('Processed ' + str(totalEventCnt) + ' tweets.')
                            totalEventCntThresh = 0
                        # Exit conditions:
                        if not noTweetCap and totalEventCnt >= settings.TWITTER_SEARCH_API_TWEET_CAP:
                            doLoop = False
                            break
                except Exception as ex:
                    # Wait and then try last request again.
                    sleepDurationSeconds = 900  # 15 minutes.
                    print("Got exception when querying Twitter search API: " + ex.message)
                    # Save the portion of the events JSON.
                    #with open('C:/Dev/labs-heartbeat-visualization-framework/json-backup/' + cacheKey + '-part-' + str(totalEventCnt) + '.json', "w") as textFile:
                    with open(settings.SECONDARY_CACHE_DIRECTORY + cacheKey + '-part-' + str(totalEventCnt) + '.json', "w") as textFile:
                        textFile.write(json.dumps(events, default=json_util.default))
                    print("Sleeping for " + str(sleepDurationSeconds) + " seconds.")
                    time.sleep(sleepDurationSeconds)
                    # Reset the tweet counter to make sure we don't artificually trigger the loop exit condition.
                    tweetCnt = -1
                    print("Time to wake up and try again from maxId = " + str(maxId))
                if lastTweet != None:
                    maxId = long(lastTweet['id_str'])
            # Return the file list as JSON.
            response = {
                        'heartbeat_events': events,
                        'timestamp_max': tsMax,
                        'timestamp_min': tsMin,
                        'allowed_event_keys': [hashtag]
                        };
            # Now cache response.
            cache.set(cacheKey, response, 43200)  # 12 hours
            # Finally, store the events in a text file (TODO: I may remove this later).
            with open(secondaryCacheFilePath, "w") as textFile:
                textFile.write(json.dumps(response, default=json_util.default))
        ser = json.dumps(response, default=json_util.default)
        return HttpResponse(ser, mimetype="application/json")
    except Exception as ex:
        # Respond with error as JSON.
        return HttpResponse(ApiResponse.from_exception(ex).to_json(), mimetype="application/json")

def __getDateFromTweetCreatedAt(createdAt):
    return datetime.datetime.strptime(createdAt, '%a %b %d %H:%M:%S +0000 %Y')

def _get_flickr_token(request):
    flickr = FlickrAPI(settings.FLICKR_KEY, settings.FLICKR_SECRET)
    fUrl = flickr.web_login_url("write")
    return HttpResponseRedirect(fUrl)

def _parse_flickr_token(request):
    flickr = FlickrAPI(settings.FLICKR_KEY, settings.FLICKR_SECRET)
    frob = request.GET['frob']
    api_token = flickr.get_token(frob)
    # Now test the token to make sure it's valid.
    flickr = FlickrAPI(settings.FLICKR_KEY, settings.FLICKR_SECRET, token=api_token)
    fGroups = flickr.groups_pools_getGroups()
    return HttpResponse(ApiResponse(success=True, message='token: ' + api_token).to_json(), mimetype="application/json")

def __parse_flickr_title(fileName):
    '''
    Turn the file name of the format "#labsmb_2013-07-09_2013-07-10_tan.png" into a string of the format "#labsmb 07.09.13".
    '''
    parts = fileName.split('_')
    dateParts = parts[1].split('-')
    return parts[0] + ' ' + dateParts[1] + '.' + dateParts[2] + '.' + dateParts[0][2:]

@csrf_exempt
def _upload_flickr_image(request):
    # Grab image from POST and save image to local file system.
    img = request.FILES.values()[0]
    fileName = img.name.encode('utf8')
    filePath = settings.SAVED_IMAGE_DIRECTORY + fileName
    fTitle = __parse_flickr_title(fileName)
    with open(filePath, 'wb+') as destination:
        for chunk in img.chunks():
            destination.write(chunk)
    # Get the hastag from the file name.
    hashtag = fileName.split('_')[0]
    # Make sure to use the right tag so that these images can be grouped properly.
    fTags = 'labsmb mcgarrybowen heartbeat datavisualization ' + hashtag 
    # The image description.
    fDesc = "A data visualization of tweets for the " + hashtag + " hashtag over time." 
    # Upload image to Flickr.
    flickr = FlickrAPI(settings.FLICKR_KEY, settings.FLICKR_SECRET, token=settings.FLICKR_TOKEN)
    resp = flickr.upload(
                         filename=filePath,
                         callback=__flickr_upload_callback,
                         title=fTitle,
                         tags=fTags,
                         description=fDesc,
                         is_public=1,
                         format='rest'
                         )
    # Parse out image ID for uploaded image.
    xmlRoot = ET.XML(resp)
    photoid = xmlRoot.find('photoid').text
    # Now add the photo to a photoset.
    resp2 = flickr.photosets_addPhoto(
                                      photoset_id=settings.FLICKR_PHOTOSET_ID,
                                      photo_id=photoid,
                                      format='rest'
                                      )
    # Parse out the response. FOr sucessful requests, the response should be blank.
    xmlRoot2 = ET.XML(resp2)
    success = xmlRoot.attrib['stat']
    if success != 'ok':
        raise Exception('There was an unspecified error moving photo ' + photoid + ' to photoset ' + settings.FLICKR_PHOTOSET_ID)
    # Finally, return URL to flickr image.
    imageUrl = 'http://www.flickr.com/photos/mcgarrybowenlabs/' + photoid
    ser = json.dumps({ 'image_url': imageUrl }, default=json_util.default)
    return HttpResponse(ser, mimetype="application/json")

# FIXME: MOVE THIS TO ITS OWN SERVER: 
''' HACK FOR PHOTOBOOTH '''
@csrf_exempt
def upload_photobooth_image(request):
    # Grab image from POST and save image to local file system.
    img = request.FILES.values()[0]
    fileName = img.name.encode('utf8')
    filePath = '/home/bitnami/htdocs/img/labs-photobooth-images/' + fileName
    with open(filePath, 'wb+') as destination:
        for chunk in img.chunks():
            destination.write(chunk)
    # Finally, return URL to flickr image.
    imageUrl = 'http://heartbeatvisualization.labsmb.com/img/labs-photobooth-images/' + fileName
    ser = json.dumps({ 'image_url': imageUrl }, default=json_util.default)
    return HttpResponse(ser, mimetype="application/json")
''' END HACK FOR PHOTOBOOTH '''


def __flickr_upload_callback(progress, done):
    if done:
        print "Done uploading"
    else:
        print "At %s%%" % progress

def viewevents(request):
    try:
        events = []
        allowedKeys = []
        tsMax = 0
        tsMin = sys.maxint
        hasFilter = False
        queryFilter = {}
        # If datetime range is supplied, use that, else return all events.
        timestampStart = timestampEnd = None
        if 'timestamp_start' in request.GET and request.GET['timestamp_start']:
            timestampStart = datetime.datetime.fromtimestamp(float(request.GET['timestamp_start']))
        if 'timestamp_end' in request.GET and request.GET['timestamp_end']:
            timestampEnd = datetime.datetime.fromtimestamp(float(request.GET['timestamp_end']))
        # If tag is supplied, use that, else return all events.
        eventTag = None
        if 'event_tag' in request.GET and request.GET['event_tag']:
            eventTag = __sanitize_input(request.GET['event_tag'])
            queryFilter['event_tags'] = eventTag
            hasFilter = True
        # Get heartbeat events.
        if timestampStart != None and timestampEnd != None:
            # eventQuery = Event.objects(__raw__={'event_timestamp': {"$gte": timestampStart, "$lt": timestampEnd}})
            queryFilter['event_timestamp'] = {"$gte": timestampStart, "$lt": timestampEnd}
            hasFilter = True
        # If we have a filter, use a raw query.
        if hasFilter:
            eventQuery = Event.objects(__raw__=queryFilter)
        else:
            eventQuery = Event.objects()
        for event in eventQuery.order_by('+event_timestamp'):
        # for event in Event.objects.order_by('+event_timestamp'):
            eventDict = event.to_dict()
            if eventDict['event_timestamp'] > tsMax:
                tsMax = eventDict['event_timestamp']
            if eventDict['event_timestamp'] < tsMin:
                tsMin = eventDict['event_timestamp']
            events.append(eventDict)
        # Get allowed keys.
        for allowedKey in Allowed_event_key.objects.order_by('+key'):
            allowedKeys.append(allowedKey.key)
        # Return the file list as JSON.
        response = {
                    'heartbeat_events': events,
                    'timestamp_max': tsMax,
                    'timestamp_min': tsMin,
                    'allowed_event_keys': allowedKeys
                    };
        ser = json.dumps(response, default=json_util.default)
        return HttpResponse(ser, mimetype="application/json")
    except Exception as ex:
        # Respond with error as JSON.
        return HttpResponse(ApiResponse.from_exception(ex).to_json(), mimetype="application/json")

@csrf_exempt
def saveevent(request):
    try:
        if request.method != 'POST':
            raise Exception('This service requires POST requests.')
        __save_event_helper(request)
        # Return the file list as JSON.
        return HttpResponse(ApiResponse(success=True, message='Event saved successfully!').to_json(), mimetype="application/json")
    except Exception as ex:
        # Respond with error as JSON.
        return HttpResponse(ApiResponse.from_exception(ex).to_json(), mimetype="application/json")

def populate_test_data(request):
    if not settings.DEBUG:
        return HttpResponse(ApiResponse(success=False, message='This method can only be used in DEBUG mode!').to_json(), mimetype="application/json")
    else:
        # Populate the database with a bunch of test data.
        # Clear all collections. Note: Do NOT drop the collections or indexes will be lost.
        for item in Allowed_event_key.objects:
            item.delete()
        for item in Event.objects:
            item.delete()
        # Generate allowed keys.
        allowedKeys = ['Twitter', 'Facebook', 'Foursquare', 'MB-Key-Card-Checkins']
        for item in allowedKeys:
            allowedKey = Allowed_event_key(key=item)
            allowedKey.save()
        # Generate events for each allowed key.
        tags = ['labs', 'mcgarrybowen', 'summer', 'innovation', 'mongodb', 'python', 'rest', 'datavis']
        keyCntThreshold = 100
        for item in allowedKeys:
            tagCnt = 0
            keyCnt = 0
            while keyCnt < keyCntThreshold:
                now = datetime.datetime.now()
                event = Event(
                              event_key=item,
                              event_timestamp=__get_random_date(),
                              event_value='This is a string ' + now.strftime('%f')[:-3],  # A random value.
                              event_tags=[tags[tagCnt]]
                              )
                event.save()
                keyCnt += 1
                tagCnt += 1
                if tagCnt >= len(tags):
                    tagCnt = 0

        return HttpResponse(ApiResponse(success=True, message='Test data successfully generated!').to_json(), mimetype="application/json")

def __get_random_date():
    '''
    This function will return a random datetime between two datetime objects.
    Source: http://stackoverflow.com/questions/553303/generate-a-random-date-between-two-other-dates
    '''
    start = datetime.datetime.strptime('1/1/2012 1:30 PM', '%m/%d/%Y %I:%M %p')
    end = datetime.datetime.strptime('7/1/2013 4:50 AM', '%m/%d/%Y %I:%M %p')
    delta = end - start
    int_delta = (delta.days * 24 * 60 * 60) + delta.seconds
    random_second = randrange(int_delta)
    return start + timedelta(seconds=random_second)

def __sanitize_input(inp):
    '''
    Sanitize the user input!
    '''
    return inp.replace('$', '').lower()

def __check_event_key(key):
    # for aek in Allowed_event_key.objects.all():
    for aek in Allowed_event_key.objects:
        if str(key) == str(aek):
            return True
    return False

def __save_event_helper(request):
    # Check for a valid event key.
    eventKey = request.POST['event_key']
    if not __check_event_key(eventKey):
        raise Exception('Cannot save event with invalid event key: ' + eventKey)
    # Save this event row.
    if request.POST['event_timestamp'] == None:
        ts = datetime.now()
    else:
        ts = request.POST['event_timestamp']
    newEvent = Event(
                   event_timestamp=ts,
                   event_key=eventKey
    )
    newEvent.save()
