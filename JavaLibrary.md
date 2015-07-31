Documentation for the Java Library

## Building ##

Foresite Java uses Maven 2.  You can build the JAR using

```
mvn clean package
```

in the root of the foresite source directory.

## Command Line ##

You can perform basic resource map transformations via the command line:

```
java org.dspace.foresite.cli.ForesiteCLI -t -i /path/to/file.xml -f ATOM-1.0 -r RDF/XML
```

This will take the ATOM-1.0 format document at /path/to/file.xml and convert it into an RDF/XML document and print it to the standard output.  Add a -o option to write to a file.

## Constructing objects by hand ##

```
Aggregation agg = OREFactory.createAggregation(new URI('my-aggregation-uri'));
ResourceMap rem = agg.createResourceMap(new URI('my-rem-uri'));
AggregatedResource ar = agg.createAggregatedResource(new URI('my-resource-uri'));

Agent creator = OREFactory.createAgent();
creator.setName("My Creator");

rem.setCreator(creator);

agg.setCreator(creator);
agg.setTitle("My Aggregation");
```

## Parsing Objects ##

```
InputStream input = new FileInputStream("file:///my/resource/map.rdf.xml");
OREParser parser = OREParserFactory.getInstance("RDF/XML");
ResourceMap rem = parser.parse(input);
```

## Serialising Objects ##

```
ResourceMap rem = OREFactory.createResourceMap(new URI("my-rem-uri"));
ORESerialiser serial = ORESerialiserFactory.getInstance("N3");
ResourceMapDocument doc = serial.serialise(rem);
String serialisation = doc.toString();
```

## Adding Arbitrary Triples ##

Any triple can be added to any object in the model provided that the overall graph remains connected.  This is tested by whether either end of the triple (the subject uri or the object uri if it exists) is referenced elsewhere in the graph.

You can create a triple directly onto an object, thus:

```
ResourceMap rem = ...;
URI atomUpdated = new URI("http://whatever.atom.uri/terms/Updated");
Date updated = new Date();
rem.createTriple(atomUpdated, updated);
```

When you serialise, you should get a triple like:

```
URI-REM atom:updated "01-01-1970"^^http://www.w3.org/rdf-date-type-uri-whatever-that-is
```

You can also create a stand-alond triple and add it to an object, thus:

```
ResourceMap rem = ...;
Predicate pred = new Predicate();
pred.setURI(new URI("http://whatever.atom.uri/terms/Updated"));
Triple triple = new Triple();
triple.setSubjectURI("subject-uri");
triple.setPredicate(pred);
triple.setObjectURI("object-uri");
rem.addTriple(triple);
```

if "subject-uri" and "object-uri" are not present anywhere else in the graph at the time the triple is added, then an OREException will be thrown warning you that the graph is not connected.

## Supported Formats ##

These are the natively supported serialisation formats that can be written and read.  Use these exactly as written to instantiate parsers and serialisers, thus:

```
OREParser parser = OREParserFactory.getInstance("ATOM-1.0");
ORESerialiser serial = ORESerialiserFactory.getInstance("N3");
```

  * ATOM-1.0
  * RDF/XML
  * RDF/XML-ABBREV
  * N3
  * N-TRIPLE
  * TURTLE
  * RDFa