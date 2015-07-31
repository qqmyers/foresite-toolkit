Documentation for the Python library.

## Introduction ##

The library requires two prerequisites rdflib and lxml.  These are both available in pypi, and hence the setup.py should install them if you don't have them already.  To install, the regular mantra should work:

`python ./setup.py install`

The main goal of the library is to provide an intuitive object oriented layer over top of the graph storage system (rdflib) to make ORE 'objects' more easy to work with at a programmatic level.


## Usage ##

---


Import everything
```
    >>> from foresite import *
    >>> from rdflib import URIRef, Namespace
```

Create an aggregation
```
    >>> a = Aggregation('my-aggregation-uri')
```

Set properties on the aggregation.
The first defaults to dc:title, the second explicitly sets it as
dcterms:created.
```
    >>> a.title = "My Aggregation"
    >>> a._dcterms.created = "2008-07-10T12:00:00"
```

And retrieve properties:
```
    >>> a._dc.title
    [rdflib.Literal('My Aggregation', ...
    >>> a.created
    [rdflib.Literal('2008-07-10T12:00:00', ...
```
Note that they become lists as any property can be added multiple times.

Create and Aggregate two resources
```
    >>> res = AggregatedResource('my-photo-1-uri')
    >>> res.title = "My first photo"
    >>> res2 = AggregatedResource('my-photo-2-uri')
    >>> res2.title = "My second photo"
    >>> a.add_resource(res)
    >>> a.add_resource(res2)
```

Create and associate an agent (without a URI) with the aggregation

```
    >>> me = Agent()
    >>> me.name = "Rob Sanderson"
    >>> a.add_agent(me, 'creator')
```
If no URI assigned, then it will be a blank node:
```
    >>> me.uri
    rdflib.BNode(...
```
Create an agent with a URI:
```
    >>> you = Agent('uri-someone-else')
```

Register an Atom serializer with the aggregation.
The registration creates a new ResourceMap, which needs a URI.

```
    >>> serializer = AtomSerializer()
    >>> rem = a.register_serialization(serializer, 'my-atom-rem-uri')
```

And fetch the serialisation.
```
    >>> remdoc = a.get_serialization()
    >>> print remdoc.data
    <feed ...
```

Or, equivalently:
```
    >>> remdoc = rem.get_serialization()
    >>> print remdoc.data
    <feed ...
```

Resource Maps can be created by hand:
```
    >>> rem2 = ResourceMap('my-rdfa-rem-uri')
    >>> rem2.set_aggregation(a)
```
And have their own serializers:
```
    >>> rdfa = RdfLibSerializer('rdfa')
    >>> rem2.register_serialization(rdfa)
    >>> remdoc2 = rem2.get_serialization()
    >>> print remdoc2.data
    <div id="ore:ResourceMap" xmlns...
```
Possible values for RdfLibSerializer:  rdf (rdf/xml), pretty-xml (pretty rdf/xml), nt (n triples), turtle, n3, rdfa (Invisible RDFa XHTML snippet)

Parsing existing Resource Maps.
The argument to ReMDocument can be a filename or a URL.
```
    >>> remdoc = ReMDocument("http://www.openarchives.org/ore/0.9/atom-examples/atom_dlib_maxi.atom")
    >>> ap = AtomParser()
    >>> rem = ap.parse(remdoc)
    >>> aggr = rem.aggregation
```

Or an RDF Parser, which requires format to be set on the rem document:
```
    >>> rdfp = RdfLibParser()
    >>> remdoc2.format = 'rdfa'     # done by the serializer by default
    >>> rdfp.parse(remdoc2)
    <foresite.ore.ResourceMap object ...
```
Possible values for format:  xml, trix, n3, nt, rdfa

And then re-serialise in a different form:
```
    >>> rdfxml = RdfLibSerializer('xml')
    >>> rem2 = aggr.register_serialization(rdfxml, 'my-rdf-rem-uri')
    >>> remdoc3 = rem2.get_serialization()
```

Creating arbitrary triples:
```
    >>> something = ArbitraryResource('uri-random')
    >>> a.add_triple(something)
```
And then treat them like any object
```
    >>> something.title = "Random Title"
    >>> something._rdf.type = URIRef('http://somewhere.org/class/something')
```

To add in additional namespaces:

```
    >>> utils.namespaces['nss'] = Namespace('http://nss.com/namespace/ns')
    >>> utils.namespaceSearchOrder.append('nss')
    >>> utils.elements['nss'] = ['element1', 'element2', 'element3']
```

And finally, some options that can be set to change the behaviour of the library:

```
     >>> utils.assignAgentUri = True   # instead of blank node, assign UUID URI
     >>> utils.proxyType = 'UUID'  # instead of oreproxy.org, assign UUID proxy
```

If you try to serialize an unconnected graph, there are several possibilities:
```
     >>> utils.unconnectedAction = 'ignore' # serialize anyway
     >>> utils.unconnectedAction = 'drop' # drop unconnected parts of graph
     >>> utils.unconnectedAction = 'warn' # print a warning to stdout
     >>> utils.unconnectedAction = 'raise' # raise exception
```


---

## Gotchas to Avoid ##

  * Remember that all properties are lists, even if you only set one value for it.
  * It doesn't automatically detect URIs (there are so many types!) and turn them into URIRefs.  If they're not URIRefs, then the graph will treat them as literals.
  * The serialisers will ensure connectedness of the graphs... this is important in combination with the above point ... if it's not a URIRef, then any triples that have it as a subject will be dropped.