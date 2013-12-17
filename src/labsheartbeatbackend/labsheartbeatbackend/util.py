from django.utils import simplejson

class ApiResponse(object):
    def __init__(self, success=None, message=None):
        self.success = success
        self.message = message
    success = None
    message = None
    def to_json(self):
        return simplejson.dumps({'success': self.success, 'message': self.message})
    @staticmethod
    def from_exception(ex=None):
        if ex is None:
            return ApiResponse(success=True)
        else:
            return ApiResponse(success=False, message=ExceptionHelper.parse_exception(ex))

class ExceptionHelper(object):
    @staticmethod
    def parse_exception(exception):
        if len(exception.message) == 0 and hasattr(exception, 'args') and len(exception.args) > 0:
            return str(exception.args)
        else:
            return exception.message

if __name__ == "__main__":
    pass