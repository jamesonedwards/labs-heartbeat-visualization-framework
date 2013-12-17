'''
Created on Jun 10, 2013

@author: jameson.edwards

This class exists because the TwitterSeach library doesn't expose the "since" search parameter.
'''
import datetime
from TwitterSearch import TwitterSearchOrder, TwitterSearchException

class LabsTwitterSearchOrder(TwitterSearchOrder):
    '''
    classdocs
    '''
    def __init__(self):
        super(LabsTwitterSearchOrder, self).__init__()

    def setSince(self, date):
        if isinstance(date, datetime.date):
            self.arguments.update({ 'since' : '%s' % date.strftime('%Y-%m-%d') }) 
        else:
            raise TwitterSearchException('Not a date object')
