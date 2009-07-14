from __future__ import generators

from rdflib.syntax.serializers import Serializer

from rdflib.URIRef import URIRef
from rdflib.Literal import Literal
from rdflib.BNode import BNode

from rdflib.util import uniq
from rdflib.exceptions import Error
from rdflib.syntax.xml_names import split_uri

from xml.sax.saxutils import quoteattr, escape

try:
    import json
except ImportError:
    import simplejson as json

class JsonSerializer(Serializer):

    def __init__(self, store):
        super(JsonSerializer, self).__init__(store)

    def __bindings(self):
        store = self.store
        nm = store.namespace_manager
        bindings = {}
        for predicate in uniq(store.predicates()):
            prefix, namespace, name = nm.compute_qname(predicate)
            bindings[prefix] = URIRef(namespace)
        RDFNS = URIRef("http://www.w3.org/1999/02/22-rdf-syntax-ns#")
        if "rdf" in bindings:
            assert bindings["rdf"]==RDFNS
        else:
            bindings["rdf"] = RDFNS
        for prefix, namespace in bindings.iteritems():
            yield prefix, namespace


    def serialize(self, stream, base=None, encoding=None, **args):
        self.base = base
        self.__stream = stream
        self.__serialized = {}
        encoding = self.encoding
        self.write = lambda uni: stream.write(uni.encode(encoding, 'replace'))

        jsonObj = {}
        for b in self.__bindings():            
            jsonObj['xmlns$%s' % b[0]] = '%s' % b[1]

        self.jsonObj = jsonObj
        for subject in self.store.subjects():
            self.subject(subject)

        srlzd = json.dumps(self.jsonObj, indent=2)
        self.write(srlzd)
        del self.__serialized


    def subject(self, subject):
        if not subject in self.__serialized:
            self.__serialized[subject] = 1

            if isinstance(subject, URIRef): 
                uri = self.relativize(subject)
            else:
                # Blank Node
                uri = '%s' % subject.n3()                
                uri = uri.replace(':', '$')
            data = {}
            for predicate, objt in self.store.predicate_objects(subject):
                predname = self.store.namespace_manager.qname(predicate)
                predname = predname.replace(':', '$')
                value = self.value(objt)
                if data.has_key(predname):
                    data[predname].append(value)
                else:
                    data[predname] = [value]
            self.jsonObj[uri] = data

    def value(self, objt):
        data = {}
        if isinstance(objt, Literal):
            data['type'] = 'literal'
            if objt.language:
                data['lang'] = objt.language
            if objt.datatype:
                data['datatype'] = objt.datatype
            data['value'] = objt
        else:
            if isinstance(objt, URIRef):
                href = self.relativize(objt)
            else:
                # BNode
                href= '%s' % objt.n3()                

            data['type'] = 'uri'
            data['value'] = href

        return data
