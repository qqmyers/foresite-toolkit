/*
 * OREResourceJena.java
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
package org.dspace.foresite.jena;

import org.dspace.foresite.OREResource;
import org.dspace.foresite.OREException;
import org.dspace.foresite.Triple;
import org.dspace.foresite.TripleSelector;
import org.dspace.foresite.Predicate;
import org.dspace.foresite.OREFactory;
import org.dspace.foresite.Vocab;
import org.dspace.foresite.OREModel;
import org.dspace.foresite.Agent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.ByteArrayInputStream;

import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.DC;

/**
 * @Author Richard Jones
 */
public abstract class OREResourceJena implements OREResource, GraphResource
{
    protected Model model;

    protected Resource res;

    protected OREResourceJena()
    {
        model = ModelFactory.createDefaultModel();
    }

    ///////////////////////////////////////////////////////////////////
    // Methods from OREResource
    ///////////////////////////////////////////////////////////////////

    public URI getURI()
            throws OREException
    {
        try
        {
			String uri = res.getURI();
			if (uri != null)
			{
				return new URI(uri);
			}
			return null;
		}
        catch (URISyntaxException e)
        {
            throw new OREException(e);
        }
    }

    public List<Triple> listTriples()
            throws OREException
    {
        StmtIterator itr = res.listProperties();
		List<Triple> triples = new ArrayList<Triple>();
        while (itr.hasNext())
        {
            Statement statement = itr.nextStatement();
            Triple triple = JenaOREFactory.createTriple(statement);
			triples.add(triple);
		}
        return triples;
    }

	public List<Triple> listAllTriples()
			throws OREException
	{
		StmtIterator itr = model.listStatements();
		List<Triple> triples = new ArrayList<Triple>();
        while (itr.hasNext())
        {
            Statement statement = itr.nextStatement();
            Triple triple = JenaOREFactory.createTriple(statement);
			triples.add(triple);
		}
        return triples;
	}

	public List<Triple> listTriples(TripleSelector selector)
            throws OREException
    {
		// force the source to be the current resource
		selector.setSubjectURI(this.getURI());
		return this.listAllTriples(selector);
    }

	public List<Triple> listAllTriples(TripleSelector selector)
			throws OREException
	{
		// get the possible selection values
		URI subjectURI = selector.getSubjectURI();
		Predicate predInit = selector.getPredicate();
		URI objectURI = selector.getObjectURI();
		Object objectLiteral = selector.getLiteral();

		// delegate to a general handler
		return listTriples(subjectURI, predInit, objectURI, objectLiteral);
	}

	public void addTriples(List<Triple> triples)
            throws OREException
    {
        for (Triple triple : triples)
        {
            this.addTriple(triple);
        }
    }

    public void addTriple(Triple triple)
            throws OREException
    {
		// for the graph to be connected, we need to ensure that any
		// given triple refers to an existing part of the model

		Statement statement = JenaOREFactory.createStatement(triple);

		boolean connected = false;

		Resource subject = statement.getSubject();
		Selector selector1 = new SimpleSelector(subject, null, (RDFNode) null);
		Selector selector2 = new SimpleSelector(null, null, subject);
		StmtIterator itr1 = model.listStatements(selector1);
		StmtIterator itr2 = model.listStatements(selector2);
		if (itr1.hasNext() || itr2.hasNext())
		{
			connected = true;
		}

		RDFNode object = statement.getObject();
		Resource oResource = null;
		if (object instanceof Resource)
		{
			oResource = (Resource) object;
		}
		if (oResource != null && !connected)
		{
			Selector selector3 = new SimpleSelector(oResource, null, (RDFNode) null);
			Selector selector4 = new SimpleSelector(null, null, oResource);
			StmtIterator itr3 = model.listStatements(selector3);
			StmtIterator itr4 = model.listStatements(selector4);
			if (itr3.hasNext() || itr4.hasNext())
			{
				connected = true;
			}
		}

		if (!connected)
		{
			throw new OREException("Illegal Triple; graph must be connected");
		}

		// FIXME: consider rejecting any statements which have ORE semantics in them
		// and throw an error telling the developer to use the damn API, that's what
		// it's there for!

		// if we get this far, then it's fine to add the statement
		model.add(statement);
    }

    public void removeTriple(Triple triple)
            throws OREException
    {
        Statement statement = JenaOREFactory.createStatement(triple);
        model.remove(statement);
    }

	public Triple createTriple(Predicate pred, URI uri)
			throws OREException
    {
        Triple triple = OREFactory.createTriple(this, pred, uri);
        this.addTriple(triple);
        return triple;
    }

    public Triple createTriple(Predicate pred, Object literal)
			throws OREException
    {
        Triple triple = OREFactory.createTriple(this, pred, literal);
        this.addTriple(triple);
        return triple;
    }

	public Triple createTriple(URI pred, URI uri) throws OREException
	{
		Predicate predicate = new Predicate(pred);
		return this.createTriple(predicate, uri);
	}

	public Triple createTriple(URI pred, Object literal) throws OREException
	{
		Predicate predicate = new Predicate(pred);
		return this.createTriple(predicate, literal);
	}

	public Triple createTriple(Vocab pred, URI uri) throws OREException
	{
		Predicate predicate = new Predicate(pred.uri());
		return this.createTriple(predicate, uri);
	}

	public Triple createTriple(Vocab pred, Object literal) throws OREException
	{
		Predicate predicate = new Predicate(pred.uri());
		return this.createTriple(predicate, literal);
	}

	public List<Agent> getCreators()
	{
		List<Agent> agents = new ArrayList<Agent>();
		StmtIterator itr = res.listProperties(DC.creator);
		while (itr.hasNext())
		{
			Statement statement = itr.nextStatement();
			Agent agent = JenaOREFactory.createAgent((Resource) statement.getObject());
			agents.add(agent);
		}

		return agents;
	}

	public void setCreators(List<Agent> creators)
	{
		this.clearCreators();
		for (Agent creator : creators)
		{
			this.addCreator(creator);
		}
	}

	public void addCreator(Agent creator)
	{
		Resource resource = ((AgentJena) creator).getResource();
		res.addProperty(DC.creator, resource);
		this.addResourceToModel(resource);
	}

	// FIXME: if creators are shared, then there will be problems with the
	// resulting REM.  Therefore need to check for shared creators
	public void clearCreators()
	{
		List<Agent> creators = this.getCreators();
		for (Agent creator : creators)
		{
			Model cModel = ((AgentJena) creator).getModel();
			StmtIterator itr = cModel.listStatements();
			model.remove(itr);
		}
	}

	public List<Agent> getAgents(URI relationship) throws OREException
	{
		List<Agent> agents = new ArrayList<Agent>();
		StmtIterator itr = res.listProperties(model.createProperty(relationship.toString()));
		while (itr.hasNext())
		{
			Statement statement = itr.nextStatement();
			Agent agent = JenaOREFactory.createAgent((Resource) statement.getObject());
			agents.add(agent);
		}

		return agents;
	}

	public void setAgents(List<URI> relationship, Agent agent) throws OREException
	{
		this.clearCreators();
		for (URI relation : relationship)
		{
			this.addAgent(relation, agent);
		}
	}

	public void addAgent(URI relationship, Agent agent) throws OREException
	{
		Resource resource = ((AgentJena) agent).getResource();
		res.addProperty(model.createProperty(relationship.toString()), resource);
		this.addResourceToModel(resource);
	}

	public void clearAgents(URI relationship) throws OREException
	{
		List<Agent> agents = this.getAgents(relationship);
		for (Agent agent : agents)
		{
			Model cModel = ((AgentJena) agent).getModel();
			StmtIterator itr = cModel.listStatements();
			model.remove(itr);
		}
	}

	public List<URI> getTypes()
			throws OREException
	{
		try
		{
			List<URI> types = new ArrayList<URI>();
			StmtIterator itr = res.listProperties(RDF.type);
			while (itr.hasNext())
			{
				Statement statement = itr.nextStatement();
				RDFNode node = statement.getObject();
				if (node instanceof Resource)
				{
					types.add(new URI(((Resource) node).getURI()));
				}
				else if (node instanceof Literal)
				{
					throw new OREException("Type MAY NOT be Literal; error in graph");
				}
			}
			return types;
		}
		catch (URISyntaxException e)
		{
			throw new OREException(e);
		}
	}

    public void setTypes(List<URI> types)
    {
        this.clearTypes();
        for (URI type : types)
        {
            this.addType(type);
        }
	}

    public void addType(URI type)
    {
        res.addProperty(RDF.type, model.createResource(type.toString()));
    }

    public void clearTypes()
    {
        StmtIterator itr = res.listProperties(RDF.type);
        model.remove(itr);
	}

	public void addRDF(String rdf, String format)
			throws OREException
	{
		ByteArrayInputStream is = new ByteArrayInputStream(rdf.getBytes());
		Model model = ModelFactory.createDefaultModel();
        model = model.read(is, null, format);
		this.addModelToModel(model);
	}

	///////////////////////////////////////////////////////////////////
	// methods from OREResource which remain Abstract (maybe)
	///////////////////////////////////////////////////////////////////

	public abstract void empty() throws OREException;

	/* possible implementation
	public void empty()
			throws OREException
	{
		this.recursiveRemove(res);
		this.prune();
	}*/

	public abstract void detach() throws OREException;

	///////////////////////////////////////////////////////////////////
    // Methods from GraphResource
    ///////////////////////////////////////////////////////////////////

    public Resource getResource()
    {
        return res;
    }

	/* old version
	public void setResource(Resource resource)
    {
        StmtIterator itr = resource.listProperties();
        model.removeAll();
        model.add(itr);

        res = model.getResource(resource.getURI());
    }*/

	public void setResource(Resource resource)
    {
        StmtIterator itr = resource.listProperties();
        model.removeAll();
        model.add(itr);

        res = (Resource) resource.inModel(model);
    }

	public Model getModel()
    {
        return model;
    }

    public void setModel(Model model, URI resourceURI)
			throws OREException
	{
		// FIXME: are we 100% sure that this is a valid way of determining
		// protocol-based-ness.  See RFC3986 for reference.
		//
		// we need to ensure that the URI is protocol based
		String ident = resourceURI.toString();
		String rx = ".+://.+";
		Pattern p = Pattern.compile(rx);
		Matcher m = p.matcher(ident);
		if (!m.matches())
		{
			throw new OREException("Illegal URI: " + resourceURI.toString() + "; GraphResource implementer requires a protocol-based URI");
		}

		this.model = model;
        this.res = model.createResource(resourceURI.toString());
    }

	public void setModel(Model model, AnonId blankID)
			throws OREException
	{
		this.model = model;
        this.res = model.createResource(blankID);
    }

	///////////////////////////////////////////////////////////////////
	// Protected utility methods
	///////////////////////////////////////////////////////////////////


	protected List<Triple> listTriples(URI subjectURI, Predicate predInit, URI objectURI, Object objectLiteral)
			throws OREException
	{
		try
		{
			// prepare null or content for the Jena selector
			Resource subject = null;
			if (subjectURI != null)
			{
				subject = model.createResource(subjectURI.toString());
			}

			Property predicate = null;
			if (predInit != null)
			{
				predicate = model.createProperty(predInit.getURI().toString());
			}

			RDFNode object = null;
			if (objectLiteral != null)
			{
				object = model.createTypedLiteral(objectLiteral);
			}
			else if (objectURI != null)
			{
				object = model.createResource(objectURI.toString());
			}

			// construct the selector
			Selector sel = new SimpleSelector(subject, predicate, object);

			// pull the statements out and translate into Triples
			List<Triple> triples = new ArrayList<Triple>();
			StmtIterator itr = model.listStatements(sel);
			while (itr.hasNext())
			{
				Statement statement = itr.nextStatement();
				Resource resource = statement.getSubject();
				Property property = statement.getPredicate();
				Predicate pred = new Predicate();
				pred.setNamespace(property.getNameSpace());
				pred.setName(property.getLocalName());
				pred.setURI(new URI(property.getURI()));

				Triple triple = new TripleJena();
				triple.initialise(new URI(resource.getURI()));

				RDFNode node = statement.getObject();
				if (node instanceof Literal)
				{
					String literal = ((Literal) node).getLexicalForm();
					triple.relate(pred, literal);
				}
				else
				{
					URI obj = new URI(((Resource) node).getURI());
					triple.relate(pred, obj);
				}

				triples.add(triple);
			}

			return triples;
		}
		catch (URISyntaxException e)
		{
			throw new OREException(e);
		}
	}

    protected void addResourceToModel(Resource resource)
    {
        StmtIterator itr = resource.listProperties();
        model.add(itr);
    }

    protected void addModelToModel(Model externalModel)
    {
        StmtIterator itr = externalModel.listStatements();
        model.add(itr);
    }

	// FIXME: this is a graph environment, so there's plenty of room for circularities
	// we need to add in some sanity checks
	// FIXME: we also need to be careful that it doesn't remove other core elements of
	// the resource map, like, say, the resource map itself (unless that's what's required)
	protected void recursiveRemove(Resource resource)
			throws OREException
	{
		// set up a general selector for getting things which the resource is the subject of
		Selector self = new SimpleSelector(resource, null, (RDFNode) null);

		// first thing to do is drill all the way to the bottom of the hierarchy using
		// this first iterator
		StmtIterator itr1 = model.listStatements(self);
		while (itr1.hasNext())
		{
			Statement statement = itr1.nextStatement();
			RDFNode node = statement.getObject();
			if (node instanceof Resource)
			{
				if (this.recursivelyRemovable(statement))
				{
					this.recursiveRemove((Resource) node);
				}
			}
		}

		// finally, we can remove all these statements using a second iterator
		StmtIterator itr2 = model.listStatements(self);
		model.remove(itr2);
	}

	protected boolean recursivelyRemovable(Statement stmt)
			throws OREException
	{
		try
		{
			// there is an operational hierarchy that we have to respect, thus:
			//
			// - Aggregation
			//   - general triples
			//   - Resource Map
			//     - general triples
			//   - Aggregated Resource
			//     - general triples
			//     - Proxy (see next entry)
			//     - Aggregation (external)
			//     - Resource Map (external)
			//   - Proxy
			//     - general triples
			//
			// to determine whether to follow a link (i.e. to remove this resource)
			// we have to determine where in this tree we are, and only select things
			// beneath it
			//
			// the process is as follows:
			//
			// 1 - look at the resource: does it have an ore type associated with it?
			// 2 - does it fulfil the requirements of its type, or is it an external reference


			boolean removable = false;

			// The first critical thing to do is identify the type of the resource which
			// is the object of the passed statement.
			//
			// This is done in 2 ways:
			// 1) see if the statement we have been passed is an ORE predicated triple, and if so what
			//    the type of the target resource is supposed to be
			// 2) look up all the ORE predicated triples with the target resource as subject or object
			//    and see if we can infer a definitive type.
			//
			// failure to identify a type will result in an exception due to ambiguous type definition

			Resource resource = (Resource) stmt.getObject();

			Property predicate = stmt.getPredicate();
			Vocab pred = Vocab.getByURI(new URI(predicate.getURI()));
			Vocab type = OREModel.getObjectOf(pred);

			if (type == null)
			{
				// see if we can identify the type of the resource by its outgoing relationships
				// if there's more than one relationship in the result, the graph is malformed
				List<Vocab> predSubject = this.getORESubjectOfPredicates(resource);
				if (predSubject.size() > 1)
				{
					throw new OREException("Malformed ORE Resource: ambiguous type");
				}
				if (predSubject.size() == 1)
				{
					type = OREModel.getSubjectOf(predSubject.get(0));
				}

				if (type == null)
				{
					// see if we can identify the type of the resource by its other incoming relationships
					List<Vocab> predObject = this.getOREObjectOfPredicates(resource);

					if (predObject.size() > 1)
					{
						throw new OREException("Malformed ORE Resource: ambiguous type");
					}
					if (predObject.size() == 1)
					{
						type = OREModel.getSubjectOf(predSubject.get(0));
					}
				}
			}

			// FIXME: there's perhaps a more general way of using the above to do type identification
			// without involving being passed the statement.  Probably with just the Resource URI
			// we can do ObjectOfPredicates and SubjectOfPredicates on it, and divine the type from that

			// once we have the type we can go ahead and do our analysis of it

			// first, are we above it in the hierarchy
			if (!this.aboveInHierarchy(type))
			{
				return false;
			}

			// second, is this being used elsewhere
			if (this.isUsedElsewhere(resource))
			{
				return false;
			}

			// if we make it to here, then the node can be removed
			return true;
		}
		catch (URISyntaxException e)
		{
			throw new OREException(e);
		}
	}

	protected Vocab getOREType(Resource resource)
			throws OREException
	{
		try
		{
			Vocab type = Vocab.getByURI(new URI(resource.getURI()));
			if (type.inNamespace("ore"))
			{
				return type;
			}
			return null;
		}
		catch (URISyntaxException e)
		{
			throw new OREException(e);
		}
	}

	protected boolean aboveInHierarchy(Vocab type)
			throws OREException
	{
		// we are above all null types :)
		if (type == null)
		{
			return true;
		}

		Vocab rtype = this.getOREType();
		
		return false;
	}

	protected boolean isUsedElsewhere(Resource resource)
			throws OREException
	{
		return false;
	}

	// for removing unconnected portions of graph
	// the Aggregation is considered to be the "centre" of the graph, so it
	// defines the one true graph, and all other things must be connected to it
	protected void prune()
	{

	}

	// FIXME: willfully ignores ore:similarTo
	protected List<Vocab> getOREObjectOfPredicates(Resource resource)
	{
		List<Vocab> predicates = new ArrayList<Vocab>();

		Selector s1 = new SimpleSelector(null, ORE.aggregates, resource);
		Selector s2 = new SimpleSelector(null, ORE.describes, resource);
		Selector s3 = new SimpleSelector(null, ORE.isAggregatedBy, resource);
		Selector s4 = new SimpleSelector(null, ORE.isDescribedBy, resource);
		Selector s5 = new SimpleSelector(null, ORE.lineage, resource);
		Selector s6 = new SimpleSelector(null, ORE.proxyFor, resource);
		Selector s7 = new SimpleSelector(null, ORE.proxyIn, resource);

		StmtIterator itr1 = model.listStatements(s1);
		if (itr1.hasNext())
		{
			predicates.add(Vocab.ore_aggregates);
		}

		StmtIterator itr2 = model.listStatements(s2);
		if (itr2.hasNext())
		{
			predicates.add(Vocab.ore_describes);
		}

		StmtIterator itr3 = model.listStatements(s3);
		if (itr3.hasNext())
		{
			predicates.add(Vocab.ore_isAggregatedBy);
		}

		StmtIterator itr4 = model.listStatements(s4);
		if (itr4.hasNext())
		{
			predicates.add(Vocab.ore_isDescribedBy);
		}

		StmtIterator itr5 = model.listStatements(s5);
		if (itr5.hasNext())
		{
			predicates.add(Vocab.ore_lineage);
		}

		StmtIterator itr6 = model.listStatements(s6);
		if (itr6.hasNext())
		{
			predicates.add(Vocab.ore_proxyFor);
		}

		StmtIterator itr7 = model.listStatements(s7);
		if (itr7.hasNext())
		{
			predicates.add(Vocab.ore_proxyIn);
		}

		return predicates;
	}

		// FIXME: willfully ignores ore:similarTo
	protected List<Vocab> getORESubjectOfPredicates(Resource resource)
	{
		List<Vocab> predicates = new ArrayList<Vocab>();

		Selector s1 = new SimpleSelector(resource, ORE.aggregates, (RDFNode) null);
		Selector s2 = new SimpleSelector(resource, ORE.describes, (RDFNode) null);
		Selector s3 = new SimpleSelector(resource, ORE.isAggregatedBy, (RDFNode) null);
		Selector s4 = new SimpleSelector(resource, ORE.isDescribedBy, (RDFNode) null);
		Selector s5 = new SimpleSelector(resource, ORE.lineage, (RDFNode) null);
		Selector s6 = new SimpleSelector(resource, ORE.proxyFor, (RDFNode) null);
		Selector s7 = new SimpleSelector(resource, ORE.proxyIn, (RDFNode) null);

		StmtIterator itr1 = model.listStatements(s1);
		if (itr1.hasNext())
		{
			predicates.add(Vocab.ore_aggregates);
		}

		StmtIterator itr2 = model.listStatements(s2);
		if (itr2.hasNext())
		{
			predicates.add(Vocab.ore_describes);
		}

		StmtIterator itr3 = model.listStatements(s3);
		if (itr3.hasNext())
		{
			predicates.add(Vocab.ore_isAggregatedBy);
		}

		StmtIterator itr4 = model.listStatements(s4);
		if (itr4.hasNext())
		{
			predicates.add(Vocab.ore_isDescribedBy);
		}

		StmtIterator itr5 = model.listStatements(s5);
		if (itr5.hasNext())
		{
			predicates.add(Vocab.ore_lineage);
		}

		StmtIterator itr6 = model.listStatements(s6);
		if (itr6.hasNext())
		{
			predicates.add(Vocab.ore_proxyFor);
		}

		StmtIterator itr7 = model.listStatements(s7);
		if (itr7.hasNext())
		{
			predicates.add(Vocab.ore_proxyIn);
		}

		return predicates;
	}
}
