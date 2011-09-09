/*
 * AtomOREParser.java
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
package org.dspace.foresite;

import java.net.URI;
import java.util.List;

/**
 * Interface for every ORE Resource that will be implemented.  This
 * is, in turn, extended by every explicit resource type interface.
 *
 * This interface defines general RDF graph methods that we will want
 * to perform on individual resources and the graphs of which they are
 * members, as well as common ORE operations which apply to each resource
 *
 * @see org.dspace.foresite.Agent
 * @see org.dspace.foresite.AggregatedResource
 * @see org.dspace.foresite.Aggregation
 * @see org.dspace.foresite.Proxy
 * @see org.dspace.foresite.ResourceMap
 *
 * @author Richard Jones
 */
public interface OREResource
{
	/**
	 * Get the URI representing the resource
	 * 
	 * @return	the URI representing the resource, if it has one, <code>null</code> if not.
	 * @throws OREException
	 */
	URI getURI() throws OREException;

	//////////////////////////////////////////////////////////////////////////
	// methods to deal with arbitrary relationships associated with resources
	//////////////////////////////////////////////////////////////////////////

	/**
	 * List all of the triples directly descended from the resource type.
	 * That is: all the triples whose Subject is the resource
	 *
	 * @return	all the triples descended directly from the resource
	 * @throws OREException
	 */
	List<Triple> listTriples() throws OREException;

	/**
	 * List all the triples directly descended from the resource type which match
	 * the selection criteria.  This means that the Subject of the TripleSelector
	 * will be set to the ORE Resource, irrespective of what is specified there
	 *
	 * @param selector
	 * @return	all the selected triples descended from the resource
	 * @throws OREException
	 */
	List<Triple> listTriples(TripleSelector selector) throws OREException;

	/**
	 * List all triples associated with the whole ORE graph as known about by
	 * the resource in its current environment.  For a complete Resource Map, for
	 * example, this will list every single triple in the graph, which is sufficient
	 * information to re-construct a fresh Resource Map.
	 *
	 * @return	all the triples associated with the graph the object is aware of
	 * @throws OREException
	 */
	List<Triple> listAllTriples() throws OREException;

	/**
	 * List all triples associated with the whole ORE graph as known about by
	 * the resource in its current environment, which match the selection
	 * criteria
	 * 
	 * @param selector
	 * @return all the selected triples associated with the graph that the object is aware of
	 * @throws OREException
	 */
	List<Triple> listAllTriples(TripleSelector selector) throws OREException;

	/**
	 * Add the list of triples to the current graph.  The list should be structured such
	 * that once all the relationships have been added the underlying graph is still
	 * connected.  Failure to do so will result in an exception being thrown.
	 *
	 * @param relationships
	 * @throws OREException
	 */
	void addTriples(List<Triple> relationships) throws OREException;

	/**
	 * Add the given triple to the current graph.  The triple must be connected to
	 * the graph in its current state, otherwise an exception will be thrown.
	 *
	 * @param relationship
	 * @throws OREException
	 */
	void addTriple(Triple relationship) throws OREException;

	/**
	 * Remove the given triple from the current graph.  Removal of the triple should
	 * not result in the graph becoming unconnected.  If it does, all relationships
	 * to the left of the triple (i.e. those referring to the object of this triple) will
	 * be removed (recursively).
	 *
	 * @param triple
	 * @throws OREException
	 */
	void removeTriple(Triple triple) throws OREException;

	///////////////////////////////////////////////////////////////////////////////
	// methods to deal with arbitrary relationships associated with /this/ resource
	///////////////////////////////////////////////////////////////////////////////

	/**
	 * Create a triple with the given predicate on the given URI
	 *
	 * @param pred
	 * @param uri
	 * @return The newly created triple
	 * @throws OREException
	 */
	Triple createTriple(Predicate pred, URI uri) throws OREException;

	/**
	 * Create a triple with the given predicate on the given literal.  The literal
	 * in this case can be any java object.  It will be converted to its literal
	 * form either through it's <code>.toString()</code> method or another method
	 * as per the implementation.
	 *
	 * @param pred
	 * @param literal
	 * @return the newly created triple
	 * @throws OREException
	 */
	Triple createTriple(Predicate pred, Object literal) throws OREException;

	/**
	 * Create a triple using the given URI for the predicate and the given
	 * URI for the subject.
	 *
	 * This is intended to be a quick wrapper for creating relationships in the graph.
	 * 
	 * @param pred
	 * @param uri
	 * @return	the newly created triple
	 * @throws OREException
	 */
	Triple createTriple(URI pred, URI uri) throws OREException;

	/**
	 * Create a triple using the given URI for the predicate and the given
	 * literal for the subject.  The literal can by any java object, and
	 * will be converted to its literal form by the implementation.
	 *
	 * This is intended to be a quick wrapper for creating relationships in the graph.
	 *
	 * @param pred
	 * @param literal
	 * @return	the newly created triple
	 * @throws OREException
	 */
	Triple createTriple(URI pred, Object literal) throws OREException;

	/**
	 * Create a triple using the given Vocab enum element as the source of the
	 * predicate, and the given URI for the subject.  This is intended to be a quick
	 * wrapper for creating relationships in the graph
	 *
	 * @param pred
	 * @param uri
	 * @return the newly created triple
	 * @throws OREException
	 */
	Triple createTriple(Vocab pred, URI uri) throws OREException;

	/**
	 * Create a triple using the given Vocab enum element as the source of the
	 * prdicate, and the given Object as a literal.  The literal will be converted to
	 * its literal form through whatever method the implementation enforces.
	 * 
	 * @param pred
	 * @param literal
	 * @return
	 * @throws OREException
	 */
	Triple createTriple(Vocab pred, Object literal) throws OREException;

	/**
	 * Implementation specific createTriple method, allowing it to do whatever
	 * it needs in order to create a triple
	 * 
	 * @param subject
	 * @param predicate
	 * @param object
	 * @return
	 * @throws OREException
	 */
	// Triple createTriple(Object subject, Object predicate, Object object) throws OREException;

	////////////////////////////////////////////////////////////
	// methods to deal with OREResource type information
	////////////////////////////////////////////////////////////

	/**
	 * Get the Vocab element which refers to the ORE type that this object
	 * is.  The ORE type can be one of the following:
	 *
	 * <ul>
	 * <li>ore:ResourceMap</li>
	 * <li>ore:Aggregation</li>
	 * <li>ore:AggregatedResource</li>
	 * <li>ore:Proxy</li>
	 * <li>dcterms:Agent</li>
	 * </ul>
	 *
	 * @return
	 * @throws OREException
	 */
	Vocab getOREType() throws OREException;

	/**
	 * Get a list of the types associated with this resource.  This will
	 * include minimally the URI of the resource type itself, which in
	 * this case will be an ore:AggregatedResource
	 *
	 * @return
	 * @throws OREException
	 */
	List<URI> getTypes() throws OREException;

	/**
	 * Set the list of types associated with this resource.  This will overwrite
	 * any existing type list.  Nonetheless, whether present in the passed list
	 * or not, this object will continue to declare its type as minimally
	 * that of an ore:AggregatedResource
	 *
	 * @param types
	 * @throws OREException
	 */
	void setTypes(List<URI> types) throws OREException;

	/**
	 * Add the given type URI to the resource
	 *
	 * @param type
	 * @throws OREException
	 */
	void addType(URI type) throws OREException;

	/**
	 * Remove all the types associated with this resource.  This will not
	 * remove the minimally required type ore:AggregatedResource
	 *
	 * @throws OREException
	 */
	void clearTypes() throws OREException;

	///////////////////////////////////////////////////////////////////
	// methods to deal with graph manipulation
	///////////////////////////////////////////////////////////////////

	/**
	 * Remove everyting inside the object, but leave the object itself
	 * in existence.  This removes everything as follows:
	 *
	 * <ul>
	 * <li>All triples with the ORE resource as the subject</li>
	 * <li>All triples which are descended from the ORE resource by hierarchy</li>
	 * <li>All resources which are no longer connected in the graph once the two above removals have completed</li>
	 * </ul>
	 *
	 * @throws OREException
	 */
	void empty() throws OREException;

	/**
	 * Detach the current object from the current model.  This will not affect
	 * the object in the model, but will create a new copy which contains only
	 * the following:
	 *
	 * <ul>
	 * <li>All triples with the ORE resource as the subject</li>
	 * <li>All triples which are descended from the ORE resource by hierarchy (except as described by the list below)</li>
	 * </ul>
	 *
	 * It WILL NOT contain the following:
	 *
	 * <ul>
	 * <li>Other ORE Resource descriptions</li>
	 * </ul>
	 *
	 * @throws OREException
	 */
	void detach() throws OREException;

	///////////////////////////////////////////////////////////////////
	// methods for dealing with person/Agent constructs
	///////////////////////////////////////////////////////////////////

	List<Agent> getCreators();

    void setCreators(List<Agent> creators);

    void addCreator(Agent creator);

    void clearCreators();

	List<Agent> getAgents(URI relationship) throws OREException;

	void setAgents(List<URI> relationship, Agent agent) throws OREException;

	void addAgent(URI relationship, Agent agent) throws OREException;

	void clearAgents(URI relationship) throws OREException;

	//////////////////////////////////////////////////////////////////////////////
	// utilities for dealing with chunks of external rdf being added to the graph
	//////////////////////////////////////////////////////////////////////////////

	void addRDF(String rdf, String format) throws OREException;
}
