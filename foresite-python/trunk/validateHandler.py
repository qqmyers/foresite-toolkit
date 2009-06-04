
#
# Simple Mod_Python handler for validating and transforming
#   ORE Resource Maps
# 
# apache config:
# <Directory /home/cheshire/install/htdocs/txr>
#     SetHandler mod_python
#     PythonDebug On
#     PythonPath "['/path/to/validateHandler.py/']+sys.path"
#     PythonHandler validateHandler
# </Directory>

import cgitb
from mod_python import apache
from mod_python.util import FieldStorage

import re
import minilex
from foresite import *
from foresite.utils import namespaces, OreException
from foresite.serializer import OldAtomSerializer
from xml.sax._exceptions import SAXParseException

srlzHash = {'rdf.xml' : RdfLibSerializer('xml'),
            'pretty.xml' : RdfLibSerializer('pretty-xml'),
            'rem.nt' : RdfLibSerializer('nt'),
            'rem.n3' : RdfLibSerializer('n3'),
            'rem.turtle' : RdfLibSerializer('turtle'),
            'rdfa.html' : RdfLibSerializer('rdfa'),
            'atom.xml' : AtomSerializer(),
            'old-atom.xml' : OldAtomSerializer()}

srlzHash['old-atom.xml'].mimeType = "application/atom+xml;version=0.9"
srlzHash['pretty.xml'].mimeType += ";format=pretty"

p = RdfLibParser()
p.strict = True
ap = AtomParser()
p.strict = True
rdfap = RdfAParser()
p.strict = True

mimeHash = {}
for (k,v) in srlzHash.items():
    mimeHash[v.mimeType] = k
mimestr = ', '.join(mimeHash.keys())
mimeList = minilex.parse(mimestr)

protoUriRe = re.compile("^([s]?http[s]?://|[t]?ftp:/|z39.50r:|gopher:|imap://|news:|nfs:|nntp:|rtsp:)")

class validateHandler:
    def send(self, text, req, code=200, ct="text/xml"):
        req.content_type = ct
        req.content_length = len(text)
        req.send_http_header()
        if type(text) == unicode:
            req.write(text.encode('utf-8'))
        else:
            req.write(text)

    def error(self, msg, req):
        text = "<html><body><h3>Error</h3><p>%s</p></body></html>" % msg
        req.content_type = "text/html"
        req.content_length = len(text)
        req.send_http_header()
        req.write(text)
        

    def handle(self, req):
        path = req.uri[5:]
        form = FieldStorage(req)

        strict = form.get('strict', True)
        if strict in ['false', 'False', '0', None, '']:
            strict = False

        mt = form.get('mimeType', '')
        mt = mt.replace(' ', '+')

        if not mt:
            xtn = form.get('extension', '')
            if xtn:
                if not srlzHash.has_key(xtn):
                    # can't continue
                    raise ValueError(xtn)
                else:
                    mt = srlzHash[xtn].mimeType
        
        if not mt:
            try:
                wanted = req.headers_in['Accept']
                mts = minilex.parse(wanted)
                mt = minilex.best(mts, mimeList)
            except:
                mt = ''

        if mt:
            xtn = mimeHash[str(mt)]
        else:
            # default to rdf/xml
            xtn = "rdf.xml"

        srlz = srlzHash[xtn]

        if form.has_key('aggregation'):
            uri = form.get('aggregation')
        else:
            uri = path

        if not uri:
            data = '<html><body>Instructions etc. goes here</body></html>'
            self.send(data, req, ct="text/html");
            return
        elif not protoUriRe.match(uri):
            self.error("Resource Map URI must be a protocol based URI", req)
            return

        try:
            # fetch
            
            rd = ReMDocument(uri)
        except Exception, e:
            self.error("Could not retrieve Resource Map from '%s': %s" % (uri, e.message), req)
            return

        try:
            # parse
            if rd.format == 'atom':
                parser = ap
            elif rd.format == 'rdfa':
                parser = rdfap
            else:
                parser = p
            if not strict:
                parser.strict = False
            try:
                rem = parser.parse(rd)
                parser.strict = True
            except:                
                parser.strict = True
                raise

        except OreException, e:
            # get exception message
            self.error("Resource Map Invalid: %s" % e.message, req)
            return
        except SAXParseException, e:
            self.error("Could not parse XML: %s (line %s, column %s)" % (e.getMessage(), e.getLineNumber(), e.getColumnNumber()), req)
            return
        except:
            raise

        try:
            # serialize
            rem2 = rem._aggregation_.register_serialization(srlz, 'http://foresite.cheshire3.org/%s#rem' % req.uri)
            rd = rem2.get_serialization()
            data = rd.data
            if srlz == srlzHash['rdfa.html']:
                data = '<xhtml xmlns="http://www.w3.org/1999/xhtml"><body><i>Invisible RDFa resource map follows, it must have validated okay. [view source] :)</i>' + data + "</body></xhtml>"

        except Exception, e:
            self.error("Could not serialize Aggregation to Resource Map: %s" % e.message, req)
            return
        
        self.send(data, req, ct=srlz.mimeType)


def handler(req):
    # do stuff
    myhandler = validateHandler()
    try:
        myhandler.handle(req)
    except:
        req.content_type = "text/html"
        cgitb.Hook(file = req).handle()
    return apache.OK

