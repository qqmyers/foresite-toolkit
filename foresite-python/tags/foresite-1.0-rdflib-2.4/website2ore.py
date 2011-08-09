#!/home/cheshire/install/bin/python -i

# depth first web crawler

import sys, os, re
import urllib
import urlparse
from lxml import etree
import StringIO
import hashlib

from foresite import *
from foresite import conneg
from rdflib import URIRef, Literal


parser = etree.HTMLParser()
nonHttpRe = re.compile("^(mailto|ftp|telnet):(.*)", re.I)
nonHtmlRe = re.compile("\.(pdf|doc|ppt|odp|jpg|png|gif|zip|gz|tgz|bz2|ps|mpg|java|py|c|h|txt|num)$", re.I)

contentTypes = {}
md5Hash = {}
pageHash = {}
starts = []
webGraphs = [{}]

start = "http://www.openarchives.org/ore/1.0/"
restrictTemplates = [re.compile("http://www\.openarchives\.org/ore/1\.0.*")]
stack = [(start, -1)]


srlz = RdfLibSerializer(format='pretty-xml')
aggr = Aggregation(start + '#aggregation')


def crawl(uri, src):
    if not pageHash.has_key(uri):
        pid = len(pageHash)
        pageHash[uri] = pid
    else:
        pid = pageHash[uri]
    linkHash = webGraphs[-1]
    if not linkHash.has_key(pid):
        linkHash[pid] = []
    else:
        return

    print "processing %s->%s: %s" % (src, pid, uri)

    if src != -1:
        linkHash[src].append(pid)

    #fetch, find links, record, crawl
    try:
        fh = urllib.urlopen(uri)
    except:
        print "... BROKEN"
        return


    ar = AggregatedResource(uri)

    ct = fh.headers['content-type']
    try:
        cl = fh.headers['content-length']
        ar._dc.extent = Literal(cl)
    except:
        pass
    try:
        lm = fh.headers['last-modified']
        ar._dcterms.modified = Literal(lm)
    except:
        pass

    mt = conneg.parse(ct)
    if mt:
        ct = mt[0].mimetype1 + '/' + mt[0].mimetype2
    ar._dc.format = Literal(ct)
    if ct != 'text/html':
        aggr.add_resource(ar)
        try:
            contentTypes[ct] += 1
        except KeyError:
            contentTypes[ct] = 1
        return

    data = fh.read()
    fh.close()

    # hash page for redirects/duplicates etc
    md5 = hashlib.new('md5')
    md5.update(data)
    hd = md5.hexdigest()
    if md5Hash.has_key(hd):
        print "%s == %s" % (pid, md5Hash[hd])
        return
    else:
        md5Hash[hd] = pid
        # only add it here
        aggr.add_resource(ar)

    try:
        dom = etree.parse(StringIO.StringIO(data), parser)
    except:
        print " --- failed to parse"
        return

    title = dom.xpath('//title/text()')
    if title:
        ar._dc.title = Literal(title[0])

    links = dom.xpath('//a/@href')
    frames = dom.xpath('//frame/@src')
    links.extend(frames)

    imgs = dom.xpath('//img/@src')
    links.extend(imgs)
    css = dom.xpath('//link/@href')
    links.extend(css)

    for l in links:

        l = l.strip()
        if l.find('#') > -1:
            l = l[:l.find('#')]
        if not l:
            # was just a hash URL
            continue

        if l[0] == "/":
            l = urlparse.urljoin(uri, l)
        elif l[:7].lower() != "http://" and l[:8].lower() != "https://":
            # check other protocols
            if nonHttpRe.search(l):
                continue
            # put in current directory
            l = urlparse.urljoin(uri, l)

        # check if we really want to crawl...
        if nonHtmlRe.search(l):
            # ignore common stuff
            # print "Skipping: %s" % chk
            pass
        elif pageHash.has_key(l):
            # ignore already done
            # print "Skipping: %s" % chk
            pass
        else:
            match = 1
            for t in restrictTemplates:
                if not t.match(l):
                    match = 0
                    break
            if match:
                stack.append((l, pid))



while stack:
    (l, pid) = stack.pop(0)
    crawl(l, pid)


rem = aggr.register_serialization(srlz, '#rem')
rd = rem.get_serialization()
print rd.data
