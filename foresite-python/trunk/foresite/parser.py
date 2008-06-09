
from ore import *
from utils import namespaces
from lxml import etree
from rdflib import StringInputSource, URIRef


class OREParser(object):
    # Take some data and produce objects/graph
    pass

class RdfLibParser(OREParser):

    def set_fields(self, what, graph):
        for (pred, obj) in graph.predicate_objects(what.uri):
            # assert to what's graph
            what.graph.add((what.uri, pred, obj))

    
    def parse(self, doc):
        # parse to find graph
        graph =  Graph()
        data = StringInputSource(doc.data)
        if doc.format:
            graph.parse(data, format=doc.format)
        else:
            graph.parse(data)
        
        # take graph and find objects, split up stuff into graph

        # Find ReM/Aggr        
        lres = list(graph.query("PREFIX ore: <%s> SELECT ?a ?b WHERE {?a ore:describes ?b .}" % namespaces['ore']))
        uri_r = lres[0][0]
        uri_a = lres[0][1]

        rem = ResourceMap(uri_r)
        aggr = Aggregation(uri_a)
        rem.set_aggregation(aggr)
        self.set_fields(rem, graph)
        self.set_fields(aggr, graph)

        things = {uri_r : rem, uri_a : aggr}

        res2 = graph.query("PREFIX ore: <http://www.openarchives.org/ore/terms/> SELECT ?b WHERE {<%s> ore:aggregates ?b .}" % uri_a )
        for uri_ar in res2:
            uri_ar = uri_ar[0]
            res = AggregatedResource(uri_ar)
            things[uri_ar] = res
            proxy = list(graph.query("PREFIX ore: <http://www.openarchives.org/ore/terms/> SELECT ?a WHERE {?a ore:proxyFor <%s> .}" % uri_ar ))
            uri_p = proxy[0][0]
            p = Proxy(uri_p)
            p.set_forIn(res, aggr)
            things[uri_p] = p
            aggr.add_resource(res, p)
            self.set_fields(res, graph)
            self.set_fields(p, graph)

        allThings = things.copy()

        agents = list(graph.query("PREFIX foaf: <%s> PREFIX dcterms: <%s> SELECT ?a WHERE { { ?a foaf:name ?b } UNION { ?a foaf:mbox ?b } UNION { ?b dcterms:creator ?a } UNION { ?b dcterms:contributor ?a } }" % (namespaces['foaf'], namespaces['dcterms'])))
        for a_uri in agents:
            a_uri = a_uri[0]
            a = Agent(a_uri)
            allThings[a_uri] = a
            self.set_fields(a, graph)
            for (subj, pred) in graph.subject_predicates(URIRef(a_uri)):
                if things.has_key(subj):
                    # direct manipulation, as will have already added predicate in set_fields
                    things[subj]._agents_.append(a)

        # rem and aggr will have default rdf:type triples already
        for at in rem.triples:
            allThings[at.uri] = at
        for at in aggr.triples:
            allThings[at.uri] = at            

        for subj in graph.subjects():
            if not allThings.has_key(subj):
                # triple needed
                ar = ArbitraryResource(subj)
                allThings[subj] = ar
                # find our graph
                for (pred, obj) in graph.predicate_objects(subj):
                    ar.graph.add((subj, pred, obj))

                # find shortest distance to main object to link to main graph
                # Breadth First Search
                found = 0
                checked = {}
                tocheck = list(graph.subject_predicates(subj))
                while tocheck:
                    subsubj = tocheck.pop(0)[0]
                    if things.has_key(subsubj):
                        things[subsubj]._triples_.append(ar)
                        found = 1
                        break
                    else:
                        tocheck.extend(graph.subject_predicates(subsubj))
                if not found:
                    # Input graph is not connected!
                    rem._triples_.append(ar)

        return rem
        

class AtomParser(OREParser):
    remMap = {}
    aggrMap = {}
    entryMap = {}
    aggrRels = {}
    entryRels = {}

    def __init__(self):
        self.remMap = {'/atom:feed/atom:updated/text()' : 
                           {'p' : 'modified',
                            'ns'  : 'dcterms'},
                       '/atom:feed/atom:rights/text()' : 
                           {'p' : 'rights',
                            'ns' : 'dc'},
                       "atom:link[@rel='self']/@type" :
                             {'p' : 'format'},
                       "atom:link[@rel='self']/@hreflang" :
                             {'p' : 'language'},
                       "atom:link[@rel='self']/@title" :
                             {'p' : 'title'},
                       "atom:link[@rel='self']/@length" :
                             {'p' : 'extent'}
                      }

        self.aggrMap = {'/atom:feed/atom:title/text()' :
                            {'p' : 'title'},
                        '/atom:feed/atom:icon/text()' :
                            {'p' : 'logo', 'type' : URIRef},
                        '/atom:feed/atom:logo/text()' :
                            {'p' : 'logo', 'type' : URIRef},
                        '/atom:feed/atom:subtitle/text()' :
                            {'p' : 'description'}
                        }

        # about aggregated resource
        self.entryMap = {'atom:title/text()' : {'p' : 'title'},
                         'atom:summary/text()' : {'p' : 'abstract', 'ns' : 'dcterms'},
                         "atom:link[@rel='alternate']/@type" :
                             {'p' : 'format'},
                         "atom:link[@rel='alternate']/@hreflang" :
                             {'p' : 'language'},
                         "atom:link[@rel='alternate']/@title" :
                             {'p' : 'title'},
                         "atom:link[@rel='alternate']/@length" :
                             {'p' : 'extent'}
                         }

        self.aggrRels = {'related' : {'p' : 'similarTo'},
                         'alternate' : {'p' : 'isDescribedBy'},
                         'license' : {'p' : 'rights', 'ns' : 'dcterms'}
                         }

        # self = no map, alternate = URI-AR, via = Proxy
        self.entryRels = {'related' : {'p' : 'isAggregatedBy'},
                          'license' : {'p' : 'rights', 'ns' : 'dcterms'}
                          }
                          

    def handle_person(self, elem, what, type):
        name = elem.xpath('atom:name/text()', namespaces=namespaces)
        mbox = elem.xpath('atom:email/text()', namespaces=namespaces)
        uri = elem.xpath('atom:uri/text()', namespaces=namespaces)
        if not uri:
            uri = ["urn:uuid:%s" % gen_uuid()]
        agent = Agent(uri[0])
        if name:
            agent.name = name[0]
        if mbox:
            mb = mbox[0]
            if mb[:7] != "mailto:":
                mb = "mailto:%s" % mb
            agent.mbox = mb
        what.add_agent(agent, type)

    def handle_category(self, elem, what):
        uri = elem.attrib['term']
        scheme = elem.attrib.get('scheme', '')
        label = elem.attrib.get('label', '')
        what._rdf.type = URIRef(uri)
        if scheme or label:
            t = ArbitraryResource(uri)
            if label:
                t._rdfs.label = label
            if scheme:
                t._rdfs.isDefinedBy = scheme
        what.add_triple(t)
        
    def handle_link(self, elem, what):
        uri = elem.attrib['href']
        type = elem.attrib['rel']
        format = elem.attrib.get('type', '')
        lang = elem.attrib.get('hreflang', '')
        title = elem.attrib.get('title', '')
        extent = elem.attrib.get('length', '')

        # These don't map 'self', 'next', 'archive' etc
        if isinstance(what, Aggregation):
            pred = self.aggrRels.get(type, '')
        else:
            pred = self.entryRels.get(type, '')

        if pred:
            if pred.has_key('ns'):
                getattr(what, "_%s" % pred['ns'])
            setattr(what, pred['p'], URIRef(uri))            
            if format or lang or title or extent:
                t = ArbitraryResource(uri)
                if format:
                    t._dc.format = format
                if lang:
                    t._dc.language = lang
                if title:
                    t._dc.title = title
                if extent:
                    t._dc.extent = extent
                what.add_triple(t)

    def handle_rdf(self, elem, what):
        # Create AT for @about
        uri_at = elem.attrib['{%s}about' % namespaces['rdf']]
        if uri_at == str(what.uri):
            at = what
        else:
            at = ArbitraryResource(uri_at)
            what.add_triple(at)
        for kid in elem:
            # set attribute on at from kid
            full = kid.tag  # {ns}elem
            match = namespaceElemRe.search(full)
            if match:
                name = match.groups()[1]
            else:
                name = full
            val = kid.text
            if not val:
                # look in @rdf:resource
                try:
                    val = kid.attrib['{%s}resource' % namespaces['rdf']]
                    val = URIRef(val)
                except:
                    print "NO MATCH FOR %s" % etree.tostring(kid)
                    continue                
            setattr(at, name, val)

    def parse(self, doc):
        root = etree.fromstring(doc.data)
        self.curr_root = root
        graph = Graph()
        # first construct aggr and rem

        try:
            del namespaces['']
        except:
            pass
        uri_a = root.xpath('/atom:feed/atom:id/text()', namespaces=namespaces)
        uri_r = root.xpath("/atom:feed/atom:link[@rel='self']/@href", namespaces=namespaces)

        rem = ResourceMap(uri_r[0])
        aggr = Aggregation(uri_a[0])
        rem.set_aggregation(aggr)

        for (xp, pred) in self.remMap.iteritems():
            val = root.xpath(xp, namespaces=namespaces)
            for v in val:
                if pred.has_key('ns'):
                    getattr(rem, "_%s" % pred['ns'])
                if pred.has_key('type'):
                    v = pred['type'](v)
                setattr(rem, pred['p'], v)

        # Handle generator
        gen = root.xpath('/atom:feed/atom:generator', namespaces=namespaces)
        if gen:
            gen = gen[0]
            try:
                uri = gen.attrib['uri']
            except:
                uri = "urn:uuid:%s" % gen_uuid()
            name = gen.text
            agent = Agent(uri)
            agent.name = name
            rem.add_agent(agent, 'creator')

        for (xp, pred) in self.aggrMap.iteritems():
            val = root.xpath(xp, namespaces=namespaces)
            for v in val:
                if pred.has_key('ns'):
                    getattr(aggr, "_%s" % pred['ns'])
                if pred.has_key('type'):
                    v = pred['type'](v)
                setattr(aggr, pred['p'], v)
        
        # Now handle types, agents, links
        for auth in root.xpath('/atom:feed/atom:author', namespaces=namespaces):
            self.handle_person(auth, aggr, 'creator')
        for auth in root.xpath('/atom:feed/atom:contributor', namespaces=namespaces):
            self.handle_person(auth, aggr, 'contributor')
        for cat in root.xpath('/atom:feed/atom:category', namespaces=namespaces):
            self.handle_category(cat, aggr)
        for link in root.xpath('/atom:feed/atom:link', namespaces=namespaces):
            self.handle_link(link, aggr)

        # RDF blocks. Put everything on aggregation
        # XXX:  If link out of ReM to something, then something goes onto Aggr
        #       But then in new ReM for same Aggr, will have unconnected graph
        #       DISCUSS:  throw out unconnected at serialisation time

        for rdf in root.xpath('/atom:feed/rdf:Description', namespaces=namespaces):
            if rdf.attrib['{%s}about' % namespaces['rdf']] == uri_r[0]:
                self.handle_rdf(rdf, rem)
            else:
                self.handle_rdf(rdf, aggr)


        for entry in root.xpath('/atom:feed/atom:entry', namespaces=namespaces):
            uri_p = entry.xpath('atom:id/text()', namespaces=namespaces)
            uri_ar = entry.xpath("atom:link[@rel='alternate']/@href", namespaces=namespaces)
            
            res = AggregatedResource(uri_ar[0])
            proxy = Proxy(uri_p[0])
            proxy.set_forIn(res, aggr)
            aggr.add_resource(res, proxy)

            # look for via
            via = entry.xpath("atom:link[@rel='via']/@href", namespaces=namespaces)
            if via:
                proxy._ore.lineage = URIRef(via[0])

            for (xp, pred) in self.entryMap.iteritems():
                val = entry.xpath(xp, namespaces=namespaces)
                for v in val:
                    if pred.has_key('ns'):
                        getattr(res, "_%s" % pred['ns'])
                    if pred.has_key('type'):
                        v = pred['type'](v)
                    setattr(res, pred['p'], v)

            for auth in entry.xpath('atom:author', namespaces=namespaces):
                self.handle_person(auth, res, 'creator')
            for auth in entry.xpath('atom:contributor', namespaces=namespaces):
                self.handle_person(auth, res, 'contributor')
            for cat in entry.xpath('atom:category', namespaces=namespaces):
                self.handle_category(cat, res)
            for link in entry.xpath('atom:link', namespaces=namespaces):
                self.handle_link(link, res)

            # RDF blocks. Put everything on aggregation
            for rdf in entry.xpath('rdf:Description', namespaces=namespaces):
                self.handle_rdf(rdf, res)
        return rem
