
import re
from ore import *
from foresite import libraryName, libraryUri, libraryVersion
from utils import namespaces, OreException, unconnectedAction, pageSize, gen_uuid, build_html_atom_content
from rdflib import URIRef, BNode, Literal, plugin, syntax
from lxml import etree
from lxml.etree import Element, SubElement

plugin.register('rdfa', syntax.serializers.Serializer, 'foresite.RDFaSerializer', 'RDFaSerializer')

class ORESerializer(object):
    # Take objects and produce data
    mimeType = ""
    format = ""
    public = 1

    def __init__(self, format, public=1):
        mimetypes = {'atom' : 'application/atom+xml', 
                     'rdfa' : 'application/xhtml+xml',
                     'xml' : 'application/rdf+xml',
                     'nt' : 'text/plain',
                     'n3' : 'text/rdf+n3',
                     'turtle' : 'application/turtle',
                     'pretty-xml' : 'application/rdf+xml'
                     }
        self.format = format
        self.public = public
        self.mimeType = mimetypes.get(format, '')

    def merge_graphs(self, rem, page=-1):
        g = Graph()
        # Put in some sort of recognition of library?
        n = now()
        if not rem.created:
            rem._dcterms.created = n
        rem._dcterms.modified = n

        g += rem._graph_
        for at in rem._triples_:
            g += at._graph_
        for c in rem._agents_:
            g += c._graph_
        
        aggr = rem._aggregation_
        g += aggr._graph_
        for at in aggr._triples_:
            g += at._graph_
        for c in aggr._agents_:
            g += c._graph_

        if page != -1:
            # first is 1, 2, 3 ...
            start = (page-1) * pageSize
            tosrlz = aggr._resources_[start:start+pageSize]
        else:
            tosrlz = aggr._resources_
            
        for (res, proxy) in tosrlz:
            g += res._graph_
            if proxy:
                g += proxy._graph_
            for at in res._triples_:
                g += at._graph_
            for c in res._agents_:
                g += c._graph_
            if isinstance(res, Aggregation):
                # don't recurse, remove aggregates                
                for a in res._ore.aggregates:
                    g.remove((res._uri_, namespaces['ore']['aggregates'], a))

        if self.public:
            # Remove internal methods
            for p in internalPredicates:
                for (s,o) in g.subject_objects(p):
                    g.remove((s,p,o))
        if not aggr._resources_:
            raise OreException("Aggregation must aggregate something")
        g = self.connected_graph(g, aggr._uri_)
        return g

    def connected_graph(self, graph, uri):
        if unconnectedAction == 'ignore':
            return graph
        g = Graph()
        all_nodes = list(graph.all_nodes())
        discovered = {}
        visiting = [uri]
        while visiting:
            x = visiting.pop()
            if not discovered.has_key(x):
                discovered[x] = 1
            for (p, new_x) in graph.predicate_objects(subject=x):
                g.add((x,p,new_x))
                if isinstance(new_x, URIRef) and not discovered.has_key(new_x) and not new_x in visiting:
                    visiting.append(new_x)
            for (new_x, p) in graph.subject_predicates(object=x):
                g.add((new_x,p,x))
                if isinstance(new_x, URIRef) and not discovered.has_key(new_x) and not new_x in visiting:
                    visiting.append(new_x)
        if len(discovered) != len(all_nodes):
            if unconnectedAction == 'warn':
                print "Warning: Graph is unconnected, some nodes being dropped"
            elif unconnectedAction == 'raise':
                raise OreException('Graph to be serialized is unconnected')
            elif unconnectedAction != 'drop':
                raise ValueError('Unknown unconnectedAction setting: %s' % unconnectedAction)
        return g


class RdfLibSerializer(ORESerializer):

    def serialize(self, rem, page=-1):
        g = self.merge_graphs(rem, page)
        data = g.serialize(format=self.format)
        uri = str(rem._uri_)
        rd = ReMDocument(uri, data, format=self.format)
        return rd

class AtomSerializer(ORESerializer):

    def __init__(self, format="atom", public=1):
        ORESerializer.__init__(self, format)
        self.spacesub = re.compile('(?<=>)[ ]+(?=<)')
        self.done_triples = []


    def generate_rdf(self, parent, sg):
        # remove already done, then serialize to rdf/xml
        for t in self.done_triples:
            sg.remove(t)
        data = sg.serialize(format='xml')
        root = etree.fromstring(data)
        for child in root:
            parent.append(child)

    def make_agent(self, parent, agent):
        n = SubElement(parent, 'name')
        try:
            name = agent._foaf.name[0]
            n.text = str(name)
            self.done_triples.append((agent._uri_, namespaces['foaf']['name'], name))
        except:
            pass
        if agent._foaf.mbox:
            n = SubElement(parent, 'email')
            mb = agent._foaf.mbox[0]
            self.done_triples.append((agent._uri_, namespaces['foaf']['mbox'], mb))
            mb = str(mb)
            if mb[:7] == "mailto:":
                mb = mb[7:]
            n.text = mb            
        if not isinstance(agent._uri_, BNode):
            n = SubElement(parent, 'uri')
            n.text = str(agent._uri_)
            
        #if agent._foaf.page:
        #    n = SubElement(parent, 'email')
        #    fp = agent._foaf.page[0]
        #    self.done_triples.append((agent._uri_, namespaces['foaf']['mbox'], fp))
        #    n.text = fp


    def make_link(self, parent, rel, t, g):

        iana = str(namespaces['iana'])
        if rel.startswith(iana):
            rel = rel[len(iana):]
        e = SubElement(parent, 'link', rel=rel, href=str(t))
        fmts = list(g.objects(t, namespaces['dc']['format']))
        if fmts:
            f = fmts[0]
            e.set('type', str(f))
            self.done_triples.append((t, namespaces['dc']['format'], f))
        langs = list(g.objects(t, namespaces['dc']['language']))
        if langs:
            l = langs[0]
            e.set('hreflang', str(langs[0]))        
            self.done_triples.append((t, namespaces['dc']['language'], l))
            
        exts = list(g.objects(t, namespaces['dc']['extent']))
        if exts:
            l = exts[0]
            e.set('length', str(l))
            self.done_triples.append((t, namespaces['dc']['extent'], l))
            
        titls = list(g.objects(t, namespaces['dc']['title']))
        if titls:
            l = titls[0]
            e.set('title', str(l))
            self.done_triples.append((t, namespaces['dc']['title'], l))


    def serialize(self, rem, page=-1):
        aggr = rem._aggregation_
        g = self.merge_graphs(rem)
        
        #namespaces[''] = namespaces['atom']
        try:
            del namespaces[u'']
        except: pass
        root = Element("entry", nsmap=namespaces)
        namespaces[''] = myNamespace

        # entry/id == tag for entry == ReM dc:identifier
        # if not exist, generate Yet Another uuid
        e = SubElement(root, 'id')
        if rem._dc.identifier:
            dcid = rem._dc.identifier[0]
            e.text = str(dcid)
            self.done_triples.append((rem._uri_, namespaces['dc']['identifier'], dcid))
        else:
            e.text = "urn:uuid:%s" % gen_uuid()

        # entry/title == Aggr's dc:title 
        if not aggr._dc.title:
            raise OreException("Atom Serialisation requires title on aggregation")
        else:
            e = SubElement(root, 'title')
            dctit = aggr._dc.title[0]
            e.text = str(dctit)
            self.done_triples.append((aggr._uri_, namespaces['dc']['title'], dctit))

        # entry/author == Aggr's dcterms:creator
        for who in aggr._dcterms.creator:
            e = SubElement(root, 'author')
            agent = all_objects[who]
            self.make_agent(e, agent)
            self.done_triples.append((aggr._uri_, namespaces['dcterms']['creator'], agent._uri_))

        # entry/contributor == Aggr's dcterms:contributor
        for bn in aggr._dcterms.contributor:
            e = SubElement(root, 'contributor')
            agent = all_objects[bn]
            self.make_agent(e, agent)
            self.done_triples.append((aggr._uri_, namespaces['dcterms']['contributor'], agent._uri_))


        # entry/category[@scheme="(magic)"][@term="(datetime)"]        
        for t in aggr._dcterms.created:
            e = SubElement(root, 'category', term=str(t),
                           scheme="http://www.openarchives.org/ore/terms/datetime/created")   
        for t in aggr._dcterms.modified:
            e = SubElement(root, 'category', term=str(t),
                           scheme="http://www.openarchives.org/ore/terms/datetime/modified")
        
        # entry/category == Aggr's rdf:type
        for t in aggr._rdf.type:
            e = SubElement(root, 'category', term=str(t))
            try:
                scheme = list(g.objects(t, namespaces['rdfs']['isDefinedBy']))[0]
                e.set('scheme', str(scheme))
                self.done_triples.append((t, namespaces['rdfs']['isDefinedBy'], scheme))
            except:
                pass
            try:
                label = list(g.objects(t, namespaces['rdfs']['label']))[0]
                e.set('label', str(label))
                self.done_triples.append((t, namespaces['rdfs']['label'], label))
            except:
                pass
            self.done_triples.append((aggr._uri_, namespaces['rdf']['type'], t))

        # entry/summary
        desc = ""
        if aggr._dc.description:
            desc = aggr._dc.description[0]
            self.done_triples.append((aggr._uri_, namespaces['dc']['description'], desc))
        elif aggr._dcterms.abstract:
            desc = aggr._dcterm.abstract[0]
            self.done_triples.append((aggr._uri_, namespaces['dcterms']['abstract'], desc))
        if desc:
            e = SubElement(root, 'summary')
            e.text = str(desc)

        # All aggr links:
        done = [namespaces['rdf']['type'],
                namespaces['ore']['aggregates'],
                namespaces['dcterms']['creator'],
                namespaces['dcterms']['contributor'],
                namespaces['dc']['title'],
                namespaces['dc']['description']
                ]
        for (p, o) in g.predicate_objects(aggr.uri):
            if not p in done:
                if isinstance(o, URIRef):
                    self.make_link(root, p, o, g)
                    self.done_triples.append((aggr._uri_, p, o))
        
        # entry/content   //  link[@rel="alternate"]
        # Do we have a splash page?
        altDone = 0
        atypes = aggr._rdf._type
        possAlts = []
        for (r, p) in aggr.resources:
            mytypes = r._rdf.type
            if namespaces['eurepo']['humanStartPage'] in mytypes:
                altDone = 1
                self.make_link(root, 'alternate', r.uri, g)
                break
            # check if share non Aggregation type
            # eg aggr == article and aggres == article, likely
            # to be good alternate
            for m in mytypes:
                if m != namespaces['ore']['Aggregation'] and \
                   m in atypes:
                    possAlt.append(r.uri)

        if not altDone:
            # look through resource maps for HTML/XHTML
            # eg an RDFa enabled splash page
            for orm in aggr._ore.isDescribedBy:
                try:
                    rem2 = all_objects[orm]
                except KeyError:
                    # just a link
                    continue
                found = 0
                for f in rem2._dc.format:
                    if str(f) == "text/html":
                        possAlts.append(orm)
                        found = 1
                if not found:
                    # check orm ends in html
                    if str(orm)[-5:] == ".html":
                        possAlt.append(orm)
                
        if not altDone and atomXsltUri:
            self.make_link(root, 'alternate', atomXsltUri % rem.uri, g)
            altDone = 1

        if not altDone and possAlts:
            # XXX more intelligent algorithm here
            self.make_link(root, 'alternate', possAlts[0], g)
            altDone = 1

        if not altDone and build_html_atom_content:
            e = SubElement(root, 'content')
            e.set('type', 'html')
            # make some representative html
            html = ['<ul>']
            for (r, p) in aggr.resources:
                html.append('<li><a href="%s">%s</a></li>' % (r.uri, r.title[0]))
            html.append('</ul>')
            e.text = '\n'.join(html)

        # entry/link[@rel='self'] == URI-R
        self.make_link(root, 'self', rem._uri_, g)
        # entry/link[@rel='about'] == URI-A
        self.make_link(root, 'about', aggr._uri_, g)
        
        ### These are generated automatically in merge_graphs
        
        # entry/published == ReM's dcterms:created
        if rem._dcterms.created:
            e = SubElement(root, 'published')
            c = rem._dcterms.created[0]
            e.text = str(c)
            self.done_triples.append((rem._uri_, namespaces['dcterms']['created'], c))

        # entry/updated == ReM's dcterms:modified
        e = SubElement(root, 'updated')
        if rem._dcterms.modified:
            c = rem._dcterms.modified[0]
            e.text = str(c)
            self.done_triples.append((rem._uri_, namespaces['dcterms']['modified'], c))
        else:
            e.text = now()

        # entry/rights == ReM's dc:rights
        if rem._dc.rights:
            e = SubElement(root, 'rights')
            r = rem._dc.rights[0]
            e.text = str(r)
            self.done_triples.append((rem._uri_, namespaces['dc']['rights'], r))


        # entry/source/author == ReM's dcterms:creator
        if rem._dcterms.creator:
            # Should at least be our generator! (right?)
            src = SubElement(root, 'source')
            for who in rem._dcterms.creator:
                e = SubElement(src, 'author')
                agent = all_objects[who]
                self.make_agent(e, agent)
                self.done_triples.append((rem._uri_, namespaces['dcterms']['creator'], agent._uri_))
            for who in rem._dcterms.contributor:
                e = SubElement(src, 'contributor')
                agent = all_objects[who]
                self.make_agent(e, agent)
                self.done_triples.append((rem._uri_, namespaces['dcterms']['contributor'], agent._uri_))
            e = SubElement(src, 'generator', uri=str(libraryUri), version=str(libraryVersion))
            e.text = str(libraryName)


        # Remove aggregation, resource map props already done
        # All of agg res needs to be done

        for (r, p) in aggr.resources:
            self.make_link(root, namespaces['ore']['aggregates'], r.uri, g)
            self.done_triples.append((aggr._uri_, namespaces['ore']['aggregates'], r._uri_))

        # Now create ore:triples
        # and populate with rdf/xml

        trips = SubElement(root, '{%s}triples' % namespaces['ore'])
        self.generate_rdf(trips, g)

        data = etree.tostring(root, pretty_print=True)
        #data = data.replace('\n', '')
        #data = self.spacesub.sub('', data)
        uri = str(rem._uri_)

        return ReMDocument(uri, data)




class OldAtomSerializer(ORESerializer):

    def __init__(self, format="atom0.9", public=1):
        ORESerializer.__init__(self, format)
        self.spacesub = re.compile('(?<=>)[ ]+(?=<)')
        self.done_triples = []

    def remove_link_attrs(self, sg, a):
        # only remove first from each list
        for ns in (namespaces['dc']['format'], namespaces['dc']['title'], namespaces['dc']['language'], namespaces['dc']['extent']):
            objs = list(sg.objects(a, ns))
            if objs:
                sg.remove((a, ns, objs[0]))
        
    def generate_rdf(self, parent, what):
        # extract not processed parts of graph
        # serialise with rdflib
        # parse with lxml and add to parent element

        sg = Graph()
        sg += what.graph
        for at in what.triples:
            sg += at.graph
        for a in what.agents:
            sg += a.graph

        for a in what.type:                
            for b in sg.objects(a, namespaces['rdfs']['isDefinedBy']):
                sg.remove((a, namespaces['rdfs']['isDefinedBy'], b))
            for b in sg.objects(a, namespaces['rdfs']['label']):
                sg.remove((a, namespaces['rdfs']['label'], b))
            sg.remove((what.uri, namespaces['rdf']['type'], a))

        for t in self.done_triples:
            sg.remove(t)

        if isinstance(what, Aggregation) or isinstance(what, AggregatedResource):
            # remove atom srlzd bits
            self.remove_link_attrs(sg, what.uri)
            try:
                sg.remove((what.uri, namespaces['dc']['description'], what.description[0]))
            except:
                pass
            for a in what.creator:
                sg.remove((what.uri, namespaces['dcterms']['creator'], a))
                                
            for a in what.contributor:
                sg.remove((what.uri, namespaces['dcterms']['contributor'], a))

            for a in what._ore.similarTo:
                self.remove_link_attrs(sg, a)
                sg.remove((what.uri, namespaces['ore']['similarTo'], a))
            for a in what._ore.aggregates:
                sg.remove((what.uri, namespaces['ore']['aggregates'], a))
            try:
                # aggregation uses dcterms rights, as it's a URI
                for a in what._dcterms.rights:
                    self.remove_link_attrs(sg, a)
                    sg.remove((what.uri, namespaces['dcterms']['rights'], a))
            except:
                pass
            try:
                sg.remove((what.uri, namespaces['foaf']['logo'], what._foaf.logo))
            except:
                pass
            if isinstance(what, Aggregation):
                for a in sg.objects(what.uri, namespaces['ore']['isDescribedBy']):
                    self.remove_link_attrs(sg, a)
                    sg.remove((what.uri, namespaces['ore']['isDescribedBy'], a))
                self.done_triples.extend(list(sg))
            else:
                # remove isAggregatedBy == rel=related
                for a in what._ore.isAggregatedBy:
                    sg.remove((what.uri, namespaces['ore']['isAggregatedBy'], a))
                self.done_triples = []
                # and add in proxy info
                proxy = what._currProxy_
                sg += proxy.graph
                for a in proxy._agents_:
                    sg += a.graph
                # remove proxyFor, proxyIn
                for a in proxy._ore.proxyFor:
                    sg.remove((proxy.uri, namespaces['ore']['proxyFor'], a))
                for a in proxy._ore.proxyIn:
                    sg.remove((proxy.uri, namespaces['ore']['proxyIn'], a))
                for a in proxy.type:                
                    for b in sg.objects(a, namespaces['rdfs']['isDefinedBy']):
                        sg.remove((a, namespaces['rdfs']['isDefinedBy'], b))
                    for b in sg.objects(a, namespaces['rdfs']['label']):
                        sg.remove((a, namespaces['rdfs']['label'], b))
                    sg.remove((proxy.uri, namespaces['rdf']['type'], a))

        elif isinstance(what, ResourceMap):

            self.remove_link_attrs(sg, what.uri)
            for a in what.describes:
                sg.remove((what.uri, namespaces['ore']['describes'], a))
            for a in what.creator:
                sg.remove((what.uri, namespaces['dcterms']['creator'], a))
            try:
                # ReM uses dc rights, as it's a string
                sg.remove((what.uri, namespaces['dc']['rights'], what._dc.rights[0]))
            except:
                pass
            try:
                sg.remove((what.uri, namespaces['dcterms']['modified'], what._dcterms.modified[0]))
            except:
                pass
            try:
                sg.remove((what.uri, namespaces['foaf']['logo'], what._foaf.logo[0]))
            except:
                pass
            try:
                sg.remove((what.uri, namespaces['ore']['describes'], what._ore.describes[0]))
            except:
                pass
            self.done_triples = []

        data = sg.serialize(format='xml')
        root = etree.fromstring(data)
        for child in root:
            parent.append(child)


    def make_agent(self, parent, agent):
        n = SubElement(parent, 'name')
        try:
            name = agent._foaf.name[0]
            n.text = str(name)
            self.done_triples.append((agent._uri_, namespaces['foaf']['name'], name))
        except:
            # allow blank names where unknown
            pass

        if agent._foaf.mbox:
            n = SubElement(parent, 'email')
            mb = agent._foaf.mbox[0]
            self.done_triples.append((agent._uri_, namespaces['foaf']['mbox'], mb))
            mb = str(mb)
            # Strip mailto: (eg not a URI any more)
            if mb[:7] == "mailto:":
                mb = mb[7:]
            n.text = mb            
        if not isinstance(agent._uri_, BNode):
            n = SubElement(parent, 'uri')
            n.text = str(agent._uri_)


    def make_link(self, parent, rel, t, g):
        e = SubElement(parent, 'link', rel=rel, href=str(t))
        # look for format, language, extent  of t
        fmts = list(g.objects(t, namespaces['dc']['format']))
        if fmts:
            e.set('type', str(fmts[0]))
        langs = list(g.objects(t, namespaces['dc']['language']))
        if langs:
            e.set('hreflang', str(langs[0]))        
        exts = list(g.objects(t, namespaces['dc']['extent']))
        if exts:
            e.set('length', str(exts[0]))
        titls = list(g.objects(t, namespaces['dc']['title']))
        if titls:
            e.set('title', str(titls[0]))

    def serialize(self, rem, page=-1):
        aggr = rem._aggregation_
        # Check entire graph is connected
        g = self.merge_graphs(rem)
        
        namespaces[''] = namespaces['atom']
        root = Element("feed", nsmap=namespaces)
        namespaces[''] = myNamespace

        ## Aggregation Info
        e = SubElement(root, 'id')
        e.text = str(aggr.uri)
        if not aggr._dc.title:
            raise OreException("Atom Serialisation requires title on aggregation")
        else:
            e = SubElement(root, 'title')
            e.text = str(aggr._dc.title[0])
        if aggr._dc.description:
            e = SubElement(root, 'subtitle')
            e.text = str(aggr._dc.description[0])

        for who in aggr._dcterms.creator:
            e = SubElement(root, 'author')
            agent = all_objects[who]
            self.make_agent(e, agent)

        for bn in aggr._dcterms.contributor:
            e = SubElement(root, 'contributor')
            agent = all_objects[bn]
            self.make_agent(e, agent)
            
        for t in aggr._ore.similarTo:
            self.make_link(root, 'related', t, g)

        for t in aggr._dcterms.rights:
            self.make_link(root, 'license', t, g)

        for t in aggr._rdf.type:
            e = SubElement(root, 'category', term=str(t))
            try:
                scheme = list(g.objects(t, namespaces['rdfs']['isDefinedBy']))[0]
                e.set('scheme', str(scheme))
            except:
                pass
            try:
                label = list(g.objects(t, namespaces['rdfs']['label']))[0]
                e.set('label', str(label))
            except:
                pass

        orms = []
        for orm in aggr._resourceMaps_:
            if orm != rem:
                self.make_link(root, 'alternate', orm.uri, g)
                orms.append(orm.uri)
        for t in aggr._ore.isDescribedBy:
            # check not in orms
            if not t in orms:
                self.make_link(root, 'alternate', t, g)

        self.generate_rdf(root, aggr)

        ## ReM Info
        self.make_link(root, 'self', rem.uri, g)

        e = SubElement(root, 'updated')
        e.text = now()

        # ReM Author
        if rem._dcterms.creator:
            uri = rem._dcterms.creator[0]
            e = SubElement(root, 'generator', uri=str(uri))
            agent = all_objects[uri]
            n = agent._foaf.name[0]
            e.text = str(n)
            self.done_triples.append((uri, namespaces['foaf']['name'], n))

        # if no logo, put in nice ORE icon
        e = SubElement(root, 'icon')
        if aggr._foaf.logo:
            e.text = str(aggr._foaf.logo[0])
        elif rem._foaf.logo:
            e.text = str(rem._foaf.logo[0])
        else:
            e.text = "http://www.openarchives.org/ore/logos/ore_icon.png"
        
        if rem._dc.rights:
            e = SubElement(root, 'rights')
            e.text = rem._dc.rights[0]

        self.generate_rdf(root, rem)

        ## Process Entries
        for (res, proxy) in aggr._resources_:
            entry = SubElement(root, 'entry')
            
            e = SubElement(entry, 'id')
            e.text = str(proxy.uri)
            e = SubElement(entry, 'link', rel="alternate", href=str(res.uri))
            # type = dc:format
            fmt = list(g.objects(res.uri, namespaces['dc']['format']))
            if fmt:
                e.set('type', str(fmt[0]))
            
            if not res._dc.title:
                raise ValueError("All entries must have a title for ATOM serialisation")
            else:
                e = SubElement(entry, 'title')
                e.text = str(res._dc.title[0])
            for t in res._rdf.type:
                e = SubElement(entry, 'category', term=str(t))
                try:
                    scheme = list(g.objects(t, namespaces['rdfs']['isDefinedBy']))[0]
                    e.set('scheme', str(scheme))
                except:
                    pass
                try:
                    label = list(g.objects(t, namespaces['rdfs']['label']))[0]
                    e.set('label', str(label))
                except:
                    pass
            for a in res._dcterms.creator:
                e = SubElement(entry, 'author')
                agent = all_objects[a]
                self.make_agent(e, agent)
            for a in res._dcterms.contributor:
                e = SubElement(entry, 'contributor')
                agent = all_objects[a]
                self.make_agent(e, agent)
            if res._dcterms.abstract:
                e = SubElement(entry, 'summary')
                e.text = str(res._dcterms.abstract[0])

            # Not sure about this at object level?
            for oa in res._ore.isAggregatedBy:
                if oa != aggr._uri_:
                    e = SubElement(entry, 'link', rel="related", href=str(oa))

            e = SubElement(entry, 'updated')
            e.text = now()

            # Put in Proxy info
            # ORE DISCUSS:  Is updated == proxy's dcterms:modified?
            #               Is published == proxy's dcterms:created?

            if proxy._ore.lineage:
                e = SubElement(entry, 'link', rel="via", href=str(proxy._ore.lineage[0]))
            res._currProxy_ = proxy
            self.generate_rdf(entry, res)
            res._currProxy_ = None

        data = etree.tostring(root)
        data = data.replace('\n', '')
        data = self.spacesub.sub('', data)
        uri = str(rem._uri_)

        return ReMDocument(uri, data)
