# Common Gurobi routines

import os
import re
import subprocess
import base64
import datetime
import urllib
import urllib2
import argparse
import uuid

### Definitions
ERROR = {
	'CONFIRM_TRANSFER': 501,
	'CONFIRM_ROOT': 502,
	'INVALID_CODE': 1001,
	'CONFIRM_OVERWRITE': 1002,
}
CONFIRMATIONS = [v for k,v in ERROR.items() if k.startswith('CONFIRM_')]
DEFAULTS = {
	'server': 'apps.gurobi.com',
	'port': 80,
}

### Functions

# Based on http://stackoverflow.com/questions/5226958/which-equivalent-function-in-python
def which(filename):
    """which command"""
    locations = os.environ.get("PATH").split(os.pathsep)
    candidates = []
    for location in locations:
        candidate = os.path.join(location, filename)
        if os.path.isfile(candidate):
            candidates.append(candidate)
    return candidates

def twhich(filename):
	"""which target file"""
	return [os.path.realpath(f) for f in which(filename)]

def randpass(length=12):
	"""Random password"""
	return base64.b64encode(os.urandom(length),"%$")

def strtolines(s):
	"""Split string into lines"""
	return re.split('[\n\r\f\v]', s)

def normalizefile(data, strip="$"):
	"""Normalize file CR/LF, optionally stripping out data"""
	filter = re.compile(strip, re.IGNORECASE)
	lines = [i for i in strtolines(data) if i and not filter.match(i)]
	lines.append('')
	return os.linesep.join(lines)

def grbinfo():
	"""Return Gurobi software info"""
	info = {'version': '', 'platform': ''}
	try:
		output = subprocess.check_output(['gurobi_cl','--version'])
		result = re.search('(\d+\.\d+\.\d+) .*\((\w+)\)', output)
		info['version'] = result.group(1)
		info['platform'] = result.group(2)
	except (AttributeError, subprocess.CalledProcessError):
		pass
	return info

def has_license():
	"""Determine if system has a valid license"""
	try:
		from subprocess import DEVNULL # py3k
	except ImportError:
		import os
		DEVNULL = open(os.devnull, 'wb')
	try:
		subprocess.check_call('gurobi_cl', stdout=DEVNULL)
		return True
	except subprocess.CalledProcessError:
		pass
	return False

def getkey(keyid, path=None, port=DEFAULTS['port'],
		   server=DEFAULTS['server'], errnums=[]):
	"""Retrieve a license key

	keyid: ID for license key
	path: path, if not using default
	port: alternate port for server
	server: alternate server
	errnums: list of error numbers that will cause an exception
	"""
	try:
		keyid = str(uuid.UUID(keyid))
	except ValueError:
		raise IOError(
			ERROR['INVALID_CODE'],
			"%s is not a valid key code" % keyid)
	probe = grbprobe()
	data = [(k.lower(),v) for k,v in probe.tokenize(True)]
	data.append(('id', keyid))
	data.append(('os', grbinfo()['platform']))
	data.append(('localdate', str(datetime.date.today())))
	data.append(('confirm', int(ERROR['CONFIRM_TRANSFER'] not in errnums)))
	url = "http://%s:%i/keyserver.php?" % (server, port)
	url += urllib.urlencode(data)
	key = grb_licfile(path=path)
	if key.data and ERROR['CONFIRM_OVERWRITE'] in errnums:
		raise IOError(ERROR['CONFIRM_OVERWRITE'],
			"A license key file already exists in %s" % key.filename())
	key.data = urllib2.urlopen(url, timeout=4).read()
	result = int(key.get('result'))
	if result != 0:
		if result == ERROR['CONFIRM_TRANSFER']:
			username = key.get('username')
			raise IOError(
				result,
				"License ID %s was found, but is currently tied to\n%shost '%s'" %
				(key.get('licenseid'),
				"user '%s' on " % username if username else "",
				key.get('hostname')))
		if result == ERROR['CONFIRM_ROOT']:
			raise IOError(
				result,
				"Cannot assign this named user license to user '%s'" % userName)
		raise IOError(result,key.get('errormsg'))
	if key.get('username') and not path:
		key.keypath = os.path.expanduser('~')
		key.can_write()
	key.rm('result')
	key.rm('licenseid')
	key.write()
	output = "License %s written to key file %s" % (key.get('licenseid'), key.filename())
	if path:
		output += "\nPlease ensure that you set the GRB_LICENSE_FILE environment variable"
	return output

def grbgetkey():
	"""Python version of grbgetkey"""
	parser = argparse.ArgumentParser(prog='grbgetkey',
		description='Install a Gurobi license key')
	parser.add_argument('id',
		nargs="?",
		default=None,
		help='License code')
	parser.add_argument('--server',
		default=DEFAULTS['server'],
		help='Alternate server address')
	parser.add_argument('--port',
		type=int,
		default=DEFAULTS['port'],
		help='Alternate port for key server')
	parser.add_argument('--path',
		default=None,
		help='Path for the license key file')
	parser.add_argument('--quiet',
		action='store_true',
		help='Install the key quietly without prompts')
	args = parser.parse_args()
	if not args.quiet:
		print("\nGurobi license key client (version %s)\n" % grbinfo()['version'])
	id = args.id
	while not id:
		print("Enter the Key Code for the license you want to install")
		print("(format is xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx):")
		id = raw_input()
	unconfirmed = [] if args.quiet else list(CONFIRMATIONS)
	while True:
		try:
			result = getkey(
				keyid=id,
				server=args.server,
				port=args.port,
				path=args.path,
				errnums=unconfirmed)
			if not args.quiet:
				print(result)
			break
		except urllib2.URLError as e:
			print "Communication error: %s" % str(e.reason)
			exit(e.errno)
		except IOError as e:
			print(e.strerror)
			if e.errno not in CONFIRMATIONS:
				exit(e.errno)
			while True:
				response = raw_input("Confirm installation? (Y/N) ")
				if re.match("[YyNn]", response):
					break
			if response[0].upper() == 'N':
				return
			unconfirmed.remove(e.errno)

### Classes

class tokendata(object):
	"""Base tokenization class"""
	def __init__(self):
		self.data = ""

	def find(self, field):
		f = field.upper()
		tokens = self.tokenize()
		for i in range(len(tokens)):
			v = tokens[i]
			try:
				if v[0].upper() == f:
					return v[1], i
			except IndexError:
				pass
		return "", None

	def get(self, field):
		return self.find(field)[0]

	def rm(self, field):
		k = self.find(field)[1]
		if k != None:
			self.rmln(k)

	def rmln(self, k):
		lines = self.lines()
		lines.pop(k)
		self.data = os.linesep.join(lines)

	def set(self, field, value):
		self.rm(field)
		self.data += os.linesep + field + "=" + value

	def lines(self):
		return [i for i in strtolines(self.data) if i]

	def tokenize(self, strict=False):
		if strict:
			return [i for i in self.tokenize() if len(i) > 1]
		else:
			return [[i] if i.startswith('#') else re.split('=', i, 1)
					for i in self.lines()]

class grbprobe(tokendata):
	"""Tokenized grbprobe data"""
	def __init__(self):
		try:
			self.data = subprocess.check_output('grbprobe', stdin=subprocess.PIPE)
		except OSError:
			self.data = ""

class conffile(tokendata):
	"""Base configuration file"""
	def __init__(self, read=True, raiseError=False):
		super(conffile, self).__init__()
		if read:
			try:
				self.read()
			except IOError as e:
				if raiseError:
					raise e

	def read(self):
		with open(self.filename()) as f:
			self.data = f.read()

	def write(self):
		with open(self.filename(), "w") as f:
			for v in self.tokenize():
				if len(v) > 1:
					v[0] = v[0].upper()
				f.write("=".join(v) + os.linesep)

class grb_licfile(conffile):
	"""Gurobi license key file"""
	def __init__(self, read=True, raiseError=False, path=None, file="gurobi.lic"):
		self.file = file
		if path:
			self.keypath = path
		else:
			# Set default key path
			info = grbinfo()
			if info['platform'] in ('win32','win64'):
				path1 = ['C:']
			elif info['platform'] == 'mac64':
				path1 = ['', 'Library']
			else:
				path1 = ['', 'opt']
			path2 = list(path1)
			path1.append('gurobi')
			path2.append('gurobi%s' % info['version'].replace('.', ''))
			pathlist = [os.sep.join(path1), os.sep.join(path2), os.path.expanduser('~')]
			for p in pathlist: # First, try to read an existing file
				try:
					self.__init__(True, True, p, file)
					return
				except IOError:
					pass
			for p in pathlist: # Otherwise, search for a path to write
				self.keypath = p
				try:
					self.can_write()
					self.rm_keyfile() # remove temp file
					break
				except:
					pass
		super(grb_licfile, self).__init__(read, raiseError)
	def filename(self):
		return self.keypath+os.sep+self.file
	def rm_keyfile(self):
		os.unlink(self.filename())
	def can_write(self):
		# Create path, if needed
		if not os.path.exists(self.keypath):
			try:
				os.makedirs(self.keypath)
			except OSError:
				raise IOError("Cannot create license directory %s" % self.keypath)
		# Check permissions on key file
		try:
			with open(self.filename(), 'a'):
				os.utime(self.filename(), None)
		except IOError:
			raise IOError("Cannot update license file %s" % self.filename())
