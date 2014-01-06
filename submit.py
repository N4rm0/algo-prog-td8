#!/usr/bin/env python
# vim: set fileencoding=utf8 :


### The only things you'll have to edit (unless you're porting this script over
### to a different language) are at the bottom of this file.

import urllib
import urllib2
import hashlib
import random
import email
import email.message
import email.encoders
import StringIO
import sys
import subprocess

import os
from optparse import OptionParser
from ConfigParser import ConfigParser

""""""""""""""""""""
""""""""""""""""""""

class NullDevice:
  def write(self, s):
    pass

def error_exit(s):
  print s + "\n"
  raw_input ("Appuyez sur entrée pour terminer ce script...")
  exit ()

def checkBinExists():
  return os.path.exists("bin") and os.path.isdir("bin")

def checkClassFilesExist():
  found = False
  for f in os.listdir("bin"):
    if f.endswith(".class"):
      found = True
  return found

def checkJavaInPath():
  DEVNULL = open(os.devnull, 'wb')
  err = subprocess.call("java -h", shell=True, stdout=DEVNULL, stderr=DEVNULL)
  return (err == 0)

def submit():
  global partIds

  # On commence par vérifier certaines propriétés de base.
  print '==\n== Vérifications \n=='
  if not (checkBinExists()):
    error_exit("Je ne trouve pas le dossier bin/ supposé contenir "
        "les fichiers .class.")
  if not (checkClassFilesExist()):
    error_exit("Le dossier bin/ ne contient aucun fichier .class, "
        "je n'arriverai pas à lancer votre programme.")
  if not (checkJavaInPath()):
    error_exit("La commande java n'est pas disponible, vérifiez votre "
        "variable d'environnement PATH")
  print "... vérifications ok\n"

  print '==\n== [sandbox] Submitting Solutions \n=='

  login = None
  password = None

  # First method of accessing the user credentials: the config.ini file.
  if os.access("config.ini", os.R_OK):
    config = ConfigParser()
    config.read("config.ini")
    try:
      login = config.get("coursera", "login")
      password = config.get("coursera", "password")
    except:
      error_exit("Fichier de configuration mal formé")
  else:
    print "Pas de fichier config.ini dans le dossier %s" % os.getcwd()

  # Parsing command-line options so that I don't have to type in my password all
  # the time.
  parser = OptionParser()
  parser.add_option("-l", "--login",
      help="Votre nom d'utilisateur (adresse mail)",
      default=login)
  parser.add_option("-p", "--password",
      help="Votre mot de passe (à usage unique sur la page des exercices)",
      default=password)
  parser.add_option("-d", "--dev", action="store_true",
      help="(for staff)")

  (options, args) = parser.parse_args()
  if options.login:
    login = options.login
  if options.password:
    password = options.password
  if options.dev:
    # See below for the definition of partIds
    partIds = map(lambda x: x + "-dev", partIds)

  # Fallback to interactive prompt, if needed.
  if login == None or password == None:
    (login, password) = loginPrompt()

  if not login:
    print '!! Submission Cancelled'
    return

  print '\n== Connecting to Coursera ... '

  # Part Identifier
  (partIdx, sid) = partPrompt()

  # Get Challenge
  (login, ch, state, ch_aux) = getChallenge(login, sid) #sid is the "part identifier"
  if((not login) or (not ch) or (not state)):
    # Some error occured, error string in first return element.
    error_exit ('\n!! Erreur: %s\n' % login)

  print '\n== Attempting challenge'

  # Attempt Submission with Challenge
  ch_resp = challengeResponse(login, password, ch)

  out = output(partIdx)

  # print '\n== Attempting submission \n' + out
  (result, string) = submitSolution(login, ch_resp, sid, out, \
                                  source(partIdx), state, ch_aux)

  print ("== J'ai envoyé à Coursera la sortie suivante "
    "(1000 derniers caractères) :\n%s\n" % out[-1000:])

  print '== %s' % string.strip()

  raw_input ("Appuyez sur entrée pour terminer ce script...")


# =========================== LOGIN HELPERS - NO NEED TO CONFIGURE THIS =======================================

def loginPrompt():
  """Prompt the user for login credentials. Returns a tuple (login, password)."""
  (login, password) = basicPrompt()
  return login, password


def basicPrompt():
  """Prompt the user for login credentials. Returns a tuple (login, password)."""
  login = raw_input('Login (Email address): ')
  password = raw_input('One-time Password (from the assignment page. This is NOT your own account\'s password): ')
  return login, password

def partPrompt():
  print 'Hello! These are the assignment parts that you can submit:'
  counter = 0
  for part in partFriendlyNames:
    counter += 1
    print str(counter) + ') ' + partFriendlyNames[counter - 1]
  partIdx = int(raw_input('Please enter which part you want to submit (1-' + str(counter) + '): ')) - 1
  return (partIdx, partIds[partIdx])

def getChallenge(email, sid):
  """Gets the challenge salt from the server. Returns (email,ch,state,ch_aux)."""
  url = challenge_url()
  values = {'email_address' : email, 'assignment_part_sid' : sid, 'response_encoding' : 'delim'}
  data = urllib.urlencode(values)
  req = urllib2.Request(url, data)
  response = urllib2.urlopen(req)
  text = response.read().strip()

  # text is of the form email|ch|signature
  splits = text.split('|')
  if(len(splits) != 9):
    print 'Badly formatted challenge response: %s' % text
    return None
  return (splits[2], splits[4], splits[6], splits[8])

def challengeResponse(email, passwd, challenge):
  sha1 = hashlib.sha1()
  sha1.update("".join([challenge, passwd])) # hash the first elements
  digest = sha1.hexdigest()
  strAnswer = ''
  for i in range(0, len(digest)):
    strAnswer = strAnswer + digest[i]
  return strAnswer

def challenge_url():
  """Returns the challenge url."""
  return "https://class.coursera.org/" + URL + "/assignment/challenge"

def submit_url():
  """Returns the submission url."""
  return "https://class.coursera.org/" + URL + "/assignment/submit"

def submitSolution(email_address, ch_resp, sid, output, source, state, ch_aux):
  """Submits a solution to the server. Returns (result, string)."""
  source_64_msg = email.message.Message()
  source_64_msg.set_payload(source)
  email.encoders.encode_base64(source_64_msg)

  output_64_msg = email.message.Message()
  output_64_msg.set_payload(output)
  email.encoders.encode_base64(output_64_msg)
  values = { 'assignment_part_sid' : sid, \
             'email_address' : email_address, \
             'submission' : output_64_msg.get_payload(), \
             'submission_aux' : source_64_msg.get_payload(), \
             'challenge_response' : ch_resp, \
             'state' : state \
           }
  url = submit_url()
  data = urllib.urlencode(values)
  req = urllib2.Request(url, data)
  response = urllib2.urlopen(req)
  string = response.read().strip()
  result = 0
  return result, string

## This collects the source code (just for logging purposes)
def source(partIdx):
  f = sourceFiles[partIdx]
  if not (os.access(f, os.R_OK)):
    error_exit ("Le fichier source "+f+" n'existe pas")
  # open the file, get all lines
  f = open(f)
  src = f.read()
  f.close()
  return src


def run_and_read(cmd):
  p = subprocess.Popen(
    cmd,
    shell=True,
    stdout=subprocess.PIPE,
    stderr=subprocess.PIPE
  )
  return p.communicate ()


############ BEGIN ASSIGNMENT SPECIFIC CODE - YOU'LL HAVE TO EDIT THIS ##############

# Make sure you change this string to the last segment of your class URL.
# For example, if your URL is https://class.coursera.org/pgm-2012-001-staging, set it to "pgm-2012-001-staging".
URL = 'algoprog-001'

# the "Identifier" you used when creating the part
partIds = [
    'dijkstra-1',
    'dijkstra-2',
    'dijkstra-3',
    'dijkstra-4',
    'dijkstra-5',
    'dijkstra-6',
    'dijkstra-7',
    'dijkstra-8',
    'dijkstra-9',
]

# used to generate readable run-time information for students
partFriendlyNames = [
  "mayotte-1850",
  "mayotte-2000",
  "mayotte-3000",
  "mayotte-3400",
  "mayotte-4000",
  "france-2000",
  "france-5000",
  "france-7000",
  "france-10000",
]

# source files to collect (just for our records)
sourceFiles = [
  "src/Test.java",
  "src/Test.java",
  "src/Test.java",
  "src/Test.java",
  "src/Test.java",
  "src/Test.java",
  "src/Test.java",
  "src/Test.java",
  "src/Test.java",
]

def output(partIdx):
  """Uses the student code to compute the output for test cases."""
  outputString = ''

  cmd = "java -ea -cp bin/ Test"
  (out, err) = run_and_read (cmd)
  if err != "":
    print "J'ai essayé de lancer la commande ci-dessous:"
    print cmd + "\n"
    print "java a retourné l'erreur ci-dessous:"
    print err + "\n"
    error_exit("Échec de la soumission")
  outputString += out

  return outputString.strip()

submit()
