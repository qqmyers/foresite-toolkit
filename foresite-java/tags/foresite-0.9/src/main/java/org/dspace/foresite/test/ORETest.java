/*
 * ORETest.java
 *
 * Copyright (c) 2008, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.foresite.test;

import org.dspace.foresite.Agent;
import org.dspace.foresite.OREFactory;
import org.dspace.foresite.ResourceMap;
import org.dspace.foresite.ORESerialiser;
import org.dspace.foresite.ORESerialiserFactory;
import org.dspace.foresite.Aggregation;
import org.dspace.foresite.AggregatedResource;
import org.dspace.foresite.OREParser;
import org.dspace.foresite.ReMSerialisation;
import org.dspace.foresite.ResourceMapDocument;
import org.dspace.foresite.Proxy;
import org.dspace.foresite.OREParserFactory;
import org.dspace.foresite.Predicate;
import org.dspace.foresite.Triple;
import org.dspace.foresite.TripleSelector;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.io.InputStream;
import java.io.FileInputStream;

import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.DC;

/**
 * @Author Richard Jones
 */
public class ORETest
{
    public static void main(String[] args)
            throws Exception
    {
        ORETest test = new ORETest();
        // test.rdfxmlWriting();
        // test.nTripleWriting();
        // test.n3Writing();
        // test.rdfxmlAbbrevWriting();
        // test.rdfaWriting();
        // test.basicReading();
        // test.atomParsing();
        // test.clearingStuff();
        // test.rdfxmlParsing();
        // test.querying();
		test.atomWriting();
		// test.turtleWriting();
	}

    public void atomParsing()
            throws Exception
    {
        InputStream is = new FileInputStream("/home/richard/workspace/dspace-trunk/ore4j/test_article.xml");
        OREParser parser = OREParserFactory.getInstance("ATOM-1.0");
        ResourceMap rem = parser.parse(is);

        ORESerialiser s = ORESerialiserFactory.getInstance("RDF/XML");
        ResourceMapDocument rmd = s.serialise(rem);
        System.out.println(rmd.toString());
    }

    public void rdfxmlParsing()
            throws Exception
    {
        InputStream is = new FileInputStream("/home/richard/workspace/dspace-trunk/ore4j/test_rdfxml.xml");
        OREParser parser = OREParserFactory.getInstance("RDF/XML");
        ResourceMap rem = parser.parse(is);

        ORESerialiser s = ORESerialiserFactory.getInstance("RDF/XML-ABBREV");
        ResourceMapDocument rmd = s.serialise(rem);
		System.out.println(rmd.toString());
	}

	public void atomWriting()
            throws Exception
    {
        System.out.println("-- ATOM-1.0 --");
        ResourceMap rem = this.testResourceMap();
        ORESerialiser s = ORESerialiserFactory.getInstance("ATOM-1.0");
        ResourceMapDocument rmd = s.serialise(rem);
        System.out.println(rmd.toString());
    }

	public void rdfxmlWriting()
            throws Exception
    {
        System.out.println("-- RDF/XML --");
        ResourceMap rem = this.testResourceMap();
        ORESerialiser s = ORESerialiserFactory.getInstance("RDF/XML");
        ResourceMapDocument rmd = s.serialise(rem);
		ResourceMapDocument rmdRaw = s.serialiseRaw(rem);

		System.out.println("--- Valid ---");
		System.out.println(rmd.toString());

		System.out.println("--- Raw ---");
		System.out.println(rmdRaw.toString());
	}

	public void turtleWriting()
            throws Exception
    {
        System.out.println("-- Turtle --");
        ResourceMap rem = this.testResourceMap();
        ORESerialiser s = ORESerialiserFactory.getInstance("TURTLE");
        ResourceMapDocument rmd = s.serialise(rem);
		ResourceMapDocument rmdRaw = s.serialiseRaw(rem);
		System.out.println(rmd.toString());
		System.out.println(rmdRaw.toString());
	}

	public void nTripleWriting()
            throws Exception
    {
        System.out.println("-- N-Triples --");
        ResourceMap rem = this.testResourceMap();
        ORESerialiser s = ORESerialiserFactory.getInstance("N-TRIPLE");
        ResourceMapDocument rmd = s.serialise(rem);
        System.out.println(rmd.toString());
    }

    public void n3Writing()
            throws Exception
    {
        System.out.println("-- N3 --");
        ResourceMap rem = this.testResourceMap();
        ORESerialiser s = ORESerialiserFactory.getInstance("N3");
        ResourceMapDocument rmd = s.serialise(rem);
        System.out.println(rmd.toString());
    }

    public void rdfxmlAbbrevWriting()
            throws Exception
    {
        System.out.println("-- RDF/XML Abbreviated --");
        ResourceMap rem = this.testResourceMap();
        ORESerialiser s = ORESerialiserFactory.getInstance("RDF/XML-ABBREV");
        ResourceMapDocument rmd = s.serialise(rem);
        System.out.println(rmd.toString());
    }

    public void rdfaWriting()
            throws Exception
    {
        System.out.println("-- RDFa --");
        ResourceMap rem = this.testResourceMap();
        ORESerialiser s = ORESerialiserFactory.getInstance("RDFa");
        ResourceMapDocument rmd = s.serialise(rem);
        System.out.println(rmd.toString());
    }

    public void basicReading()
            throws Exception
    {
        // get the usual test map
        ResourceMap rem = this.testResourceMap();
        this.screenPrint(rem);
    }

    public void clearingStuff()
            throws Exception
    {
        ResourceMap rem = this.testResourceMap();
        this.screenPrint(rem);

        rem.clearCreators();

        this.screenPrint(rem);
    }

    public void querying()
            throws Exception
    {
        ResourceMap rem = this.testResourceMap();
        this.screenPrint(rem);

        TripleSelector selector = new TripleSelector();
        selector.setSubjectURI(new URI("http://some.uri.com/my/resource/map"));
        Predicate predicate = new Predicate();
        predicate.setURI(new URI("http://www.my.predicate/namespace/Relationship"));
        selector.setPredicate(predicate);

        List<Triple> triples = rem.listTriples(selector);
        System.out.println("-- Found Triples -------");
        for (Triple triple : triples)
        {
            String object = (triple.isLiteral() ? triple.getObjectLiteral().toString() : triple.getObjectURI().toString());
            System.out.println(triple.getSubjectURI() + " : " + triple.getPredicate().getURI() + " : " + object);
        }
    }

    ///////////////////////////////////////////////////////////////////
    // private methods for constructing test datasets
    ///////////////////////////////////////////////////////////////////

    private ResourceMap testResourceMap()
            throws Exception
    {
		// create some agents
		/////////////////////

		Agent agent = OREFactory.createAgent();  // blank node
        agent.addName("Richard");
        agent.addMbox(new URI("mailto:richard.d.jones@hp.com"));

		Agent agent2 = OREFactory.createAgent(new URI("http://www.rich.com/"));
        agent2.addName("Rich");
        agent2.addMbox(new URI("mailto:rich.d.jones@gmail.com"));

		Agent agent3 = OREFactory.createAgent(new URI("http://www.ed.com/"));
        agent3.addName("Ed");
        agent3.addMbox(new URI("mailto:ed@ed.ed"));

		Agent agent4 = OREFactory.createAgent(new URI("mailto:agent@notblanknode.com"));
		agent4.addMbox(new URI("mailto:agent@notblanknode.com"));
		agent4.addName("No Blank Node Here");

		// create some triples that we will want to use later
		/////////////////////////////////////////////////////
		Triple monkeyTypeDef = OREFactory.createTriple(new URI("http://purl.org/dc/terms/Monkey"), new URI(RDFS.isDefinedBy.getURI()), new URI("http://purl.org/dc/terms/"));
		Triple monkeyLabel = OREFactory.createTriple(new URI("http://purl.org/dc/terms/Monkey"), new URI(RDFS.label.getURI()), "Monkey");
		Triple similarType = OREFactory.createTriple(new URI("http://uri.that.im/similar/to"), new URI(DC.format.getURI()), "appliation/octet-stream");
		Triple similarLang = OREFactory.createTriple(new URI("http://uri.that.im/similar/to"), new URI(DC.language.getURI()), "en");
		Triple similarTitle = OREFactory.createTriple(new URI("http://uri.that.im/similar/to"), new URI(DC.title.getURI()), "Similar Item");
		Triple remsLang = OREFactory.createTriple(new URI("http://atom.serialisation/rem"), new URI(DC.language.getURI()), "fr");
		Triple remsTitle = OREFactory.createTriple(new URI("http://atom.serialisation/rem"), new URI(DC.title.getURI()), "Other Resource Map");
		Triple rightsType = OREFactory.createTriple(new URI("http://my.rights.com/"), new URI(DC.format.getURI()), "appliation/octet-stream");
		Triple rightsLang = OREFactory.createTriple(new URI("http://my.rights.com/"), new URI(DC.language.getURI()), "no");
		Triple rightsTitle = OREFactory.createTriple(new URI("http://my.rights.com/"), new URI(DC.title.getURI()), "Aggregation Rights");

		// create some resource map serialisations
		//////////////////////////////////////////

		ReMSerialisation serialisation = new ReMSerialisation("application/atom+xml", new URI("http://atom.serialisation/rem"));
        ReMSerialisation serialisation2 = new ReMSerialisation("text/plain+n-triple", new URI("http://n.triple.serial/ntriple"));

		// create our aggregation and resource map
		//////////////////////////////////////////
		Aggregation aggregation = OREFactory.createAggregation(new URI("http://some.uri.com/my/aggregation"));
		ResourceMap rem = aggregation.createResourceMap(new URI("http://some.uri.com/my/resource/map"));

		// add properties of the aggregation
		////////////////////////////////////

		aggregation.addCreator(agent3);
		aggregation.addCreator(agent);
		aggregation.addTitle("This is the aggregation of Richard");
		aggregation.addType(new URI("http://purl.org/dc/terms/Monkey"));
		aggregation.addTriple(monkeyTypeDef);
		aggregation.addTriple(monkeyLabel);
		aggregation.addSimilarTo(new URI("http://uri.that.im/similar/to"));
		aggregation.addTriple(similarType);
		aggregation.addTriple(similarLang);
		aggregation.addTriple(similarTitle);
		aggregation.addSimilarTo(new URI("http://uri.that.im/similar/to2"));
		aggregation.addReMSerialisation(serialisation);
		aggregation.addTriple(remsLang);
		aggregation.addTriple(remsTitle);
		aggregation.addReMSerialisation(serialisation2);
		aggregation.addRights(new URI("http://my.rights.com/"));
		aggregation.addTriple(rightsType);
		aggregation.addTriple(rightsLang);
		aggregation.addTriple(rightsTitle);
		aggregation.addAgent(new URI(DC.contributor.getURI()), agent2);
		aggregation.addAgent(new URI(DC.contributor.getURI()), agent);
		aggregation.createTriple(new URI(DC.description.getURI()), "This is a subtitle");
		aggregation.setModified(new Date());
        aggregation.setCreated(new Date());

		// add properties to the resource map
		/////////////////////////////////////

		rem.setModified(new Date());
		rem.setRights("(c) 2008 Richard Jones");

		// where in ATOM serialisation?
		rem.setCreated(new Date());
        rem.addCreator(agent);
        rem.addCreator(agent2);
		rem.addCreator(agent4);
		rem.setAuthoritative(true);

		// not finished yet...
		
		Predicate pred2 = new Predicate();
        pred2.setURI(new URI("http://agent.triple.1/"));

        Predicate pred3 = new Predicate();
        pred3.setURI(new URI("http://agent.triple.2/"));

		AggregatedResource ar = OREFactory.createAggregatedResource(new URI("http://my.aggregated.res/pdf.pdf"));
        ar.addAggregation(new URI("http://some.other.agg/here"));
        Predicate pred4 = new Predicate();
        pred4.setURI(new URI("http://aggregated.resource.triple/A"));
        ar.createTriple(pred4, new URI("http://aggreated.resource.relation/1"));

        AggregatedResource ar2 = OREFactory.createAggregatedResource(new URI("http://my.other.agg/resource.pdf"));
        ar2.addAggregation(new URI("http://yet.another.agg/there"));
        ar2.createTriple(pred4, new URI("http://aggregated.resource.relation/2"));

        aggregation.addAggregatedResource(ar);
        aggregation.addAggregatedResource(ar2);
        aggregation.createProxy(new URI("http://my.proxy/"), ar.getURI());

        Proxy proxy = OREFactory.createProxy(new URI("http://other.proxy/"));
        proxy.setProxyForURI(ar2.getURI());
        aggregation.addProxy(proxy);

		Predicate pred = new Predicate();
        pred.setURI(new URI("http://www.my.predicate/namespace/Relationship"));
        Triple triple = OREFactory.createTriple(rem, pred, "some value");
        rem.addTriple(triple);

        return rem;
    }

    private void screenPrint(ResourceMap rem)
            throws Exception
    {
        System.out.println("Resource Map -------");
        System.out.println("URI: " + rem.getURI().toString());
        System.out.println("Created: " + rem.getCreated());
        System.out.println("Modified: " + rem.getModified());
        System.out.println("Rights: " + rem.getRights());

        List<Agent> creators = rem.getCreators();
        for (Agent creator : creators)
        {
            System.out.println("-- Created By -------");
            List<String> names = creator.getNames();
            for (String name : names)
            {
                System.out.println("Name: " + name);
            }

            List<URI> mboxes = creator.getMboxes();
            for (URI mbox : mboxes)
            {
                System.out.println("MBox: " + mbox);
            }
        }

        Aggregation aggregation = rem.getAggregation();
        List<URI> rights = aggregation.getRights();
        List<String> titles = aggregation.getTitles();
        List<URI> types = aggregation.getTypes();
        List<Agent> aCreators = aggregation.getCreators();
        List<AggregatedResource> ars = aggregation.getAggregatedResources();
        List<ReMSerialisation> serialisations = aggregation.getReMSerialisations();
        List<Proxy> proxies = aggregation.getProxies();

        System.out.println("-- Aggregation -------");
        System.out.println("URI: " + aggregation.getURI().toString());
        System.out.println("Created: " + aggregation.getCreated());
        System.out.println("Modified: " + aggregation.getModified());
        for (URI right : rights)
        {
            System.out.println("Rights: " + right.toString());
        }
        for (String title : titles)
        {
            System.out.println("Title: " + title);
        }
        for (URI type : types)
        {
            System.out.println("Type: " + type);
        }

        for (Agent creator : aCreators)
        {
            System.out.println("-- -- Created By -------");
            List<String> names = creator.getNames();
            for (String name : names)
            {
                System.out.println("Name: " + name);
            }

            List<URI> mboxes = creator.getMboxes();
            for (URI mbox : mboxes)
            {
                System.out.println("MBox: " + mbox);
            }
        }

        for (AggregatedResource ar : ars)
        {
            System.out.println("-- -- Aggregated Resources -------");
            System.out.println("URI: " + ar.getURI().toString());
        }

        System.out.println("-- -- Serialisations -------");
        for (ReMSerialisation serialisation : serialisations)
        {
            System.out.println("Serialisation: " + serialisation.getMimeType() + " : " + serialisation.getURI());
        }

        for (Proxy proxy : proxies)
        {
            System.out.println("-- -- Proxies -------");
            System.out.println("URI: " + proxy.getURI());
            System.out.println("Proxy For: " + proxy.getProxyFor().getURI());
            System.out.println("Proxy In: " + proxy.getProxyIn().getURI());
        }
    }
}
