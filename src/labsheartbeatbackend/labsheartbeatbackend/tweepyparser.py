'''
Source:
https://gist.github.com/inactivist/5263501
'''
import simplejson as json
import tweepy

class TweepyRawModelParser(tweepy.parsers.ModelParser):
    def parse(self, method, payload):
        result = super(TweepyRawModelParser, self).parse(method, payload)
        result.raw_payload = json.loads(payload)
        return result
