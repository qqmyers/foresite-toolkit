#!/home/cheshire/install/bin/python -i

from foresite import *
import urllib2
import os, sys
import getopt

# given an initial starting point, crawl nested and linked ORE aggregations
# download aggregated resources
# content negotiation for prefered ReM format

def usage():
    print """Usage:
%s [-r] [-d DEPTH] [-f ReM-Format] [-remDir ReM-Directory]
%s [-resDir Resource-Directory] URI
  ReM-Format is one of: xml, atom, rdfa, nt, n3, turtle""" % (sys.argv[0], ' ' * len(sys.argv[0]))
    sys.exit(0)

optstr = "rd:f:"
longs = ['remDir=', 'arDir=']
mimeHash = {'atom' : 'application/atom+xml',
             'rdfa' : 'application/xhtml+xml',
             'xml' : 'application/rdf+xml',
             'nt' : 'text/plain',
             'n3' : 'text/rdf+n3',
             'turtle' : 'application/x-turtle'}

optlist, args = getopt.getopt(sys.argv[1:], optstr, longs)

if len(args) != 1:
    usage()
else:
    uri = args[0]

maxDepth = -1
fetchAR = 0
remDirectory = 'rems'
arDirectory = 'resources'
accept_header = ''


for o in optlist:
    if o[0] == '-d':
        try:
            maxDepth = int(o[1])
        except:
            print "DEPTH must be an integer"
            usage()
    elif o[0] == '-r':
        fetchAR = 1
    elif o[0] == '--remDir':
        remDirectory = o[1]
    elif o[0] == '--resDir':
        arDirectory = o[1]
    elif o[0] == '-f':
        if not mimeHash.has_key(o[1]):
            print "Unknown format '%s'" % o[1]
            usage()
        else:
            # pass through accept_header
            accept_header = '%s;q=1.0' % mimeHash[o[1]]
    else:
        print "Unknown option: %s" % o[0]
        usage()

done = {}
doneAr = {}
stack = {}

p = RdfLibParser()
ap = AtomParser()
rdfap = RdfAParser()

if not os.path.exists(remDirectory):
    os.mkdir(remDirectory)
if not os.path.exists(arDirectory):
    os.mkdir(arDirectory)

stack[uri] = 0

while stack:
    # NB unordered pop
    (next, depth) = stack.popitem()
    done[next] = 1
    if maxDepth > -1 and depth > maxDepth:
        continue

    print "Fetching %s..." % next
    rd = ReMDocument(next, accept=accept_header)

    fn = rd.uri.replace('http://', '')
    fn = fn.replace('/', '_')
    fn = fn.replace('\\', '_')
    fn = os.path.join(remDirectory, fn)
    fh = open(fn, 'w')
    fh.write(rd.data)
    fh.close()

    try:
        if rd.format == 'atom':
            rem = ap.parse(rd)
        elif rd.format == 'rdfa':
            rem = rdfap.parse(rd)
        else:
            rem = p.parse(rd)
    except:
        # unparsable
        
        print 'URI %s is unparsable' % next
        raise


    # XXX Maybe write in alternative formats?

    # find refs to all other aggregations
    oas = rem.aggregation.do_sparql('SELECT ?a WHERE {?a a ore:Aggregation }')
    for oa in oas:
        oa = str(oa[0])
        if not done.has_key(oa) and not stack.has_key(oa):
            stack[oa] = depth + 1

    oas = rem.aggregation.do_sparql('SELECT ?a WHERE {?b ore:isAggregatedBy ?a }')
    for oa in oas:
        oa = str(oa[0])
        if not done.has_key(oa) and not stack.has_key(oa):
            stack[oa] = depth + 1
    
    if fetchAR:
        # find aggregated resources
        ars = rem.aggregation.do_sparql('SELECT ?a WHERE {?b ore:aggregates ?a }')
        for ar in ars:
            ar = str(ar[0])
            if not done.has_key(ar) and not stack.has_key(ar) and not doneAr.has_key(ar):
                print "Fetching Aggregated Resource: %s..." % ar
                req = urllib2.Request(ar)
                fh = urllib2.urlopen(req)
                data = fh.read()
                fh.close()
                fn = ar.replace('http://', '')
                fn = fn.replace('/', '_')
                fn = fn.replace('\\', '_')
                fn = os.path.join(arDirectory, fn)
                fh = open(fn, 'w')
                fh.write(data)
                fh.close()
                doneAr[ar] = 1
            
