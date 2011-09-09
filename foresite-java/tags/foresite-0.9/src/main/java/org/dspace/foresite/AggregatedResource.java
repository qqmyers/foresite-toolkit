/*
 * AggregatedResource.java
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

import java.util.List;
import java.net.URI;

/**
 * Interface to represent an ORE Aggregated Resource.  An Aggregated Resource
 * is used to represent any web resource which is part of an ORE Aggregation.
 *
 * It extends the {@link org.dspace.foresite.OREResource} interface, so also
 * includes all general ORE graph functionality.
 *
 * @see org.dspace.foresite.OREResource
 *
 * @Author Richard Jones
 */
public interface AggregatedResource extends OREResource
{
	///////////////////////////////////////////////////////////////////
	// methods to initialise the resource
	///////////////////////////////////////////////////////////////////

	/**
	 * Initialise an Aggregated Resource with the given URI.  The URI
	 * MUST be protocol based, such as <code>http://</code>, <code>ftp://</code>
	 * and so on.
	 *
	 * @param uri the URI to initialise on; must be protocol-based
	 * @throws OREException
	 */
	void initialise(URI uri) throws OREException;

	//////////////////////////////////////////////////////////////////////////////
	// methods to work with the Aggregations pertaining to this AggregatedResource
	//////////////////////////////////////////////////////////////////////////////

	/**
	 * List all of the known aggregations that this aggregated resource is a part of.
	 *
	 * An aggregated resource can know that it is part of a number of aggregations.  If
	 * you wish to retrieve an instance of {@link org.dspace.foresite.Aggregation} to
	 * which this resource belongs in the current model, use {@link AggregatedResource#getAggregation()}
	 *
	 * @return
	 * @throws OREException
	 */
	List<URI> getAggregations() throws OREException;

	/**
	 * Set the list of aggregations that this resource is a part of, overwriting existing
	 * data.  If this resource belongs to an Aggregation in the current model, a reference
	 * to it will nonetheless be maintained, even if it is not included in the passed list
	 * and will be retrievable using {@link AggregatedResource#getAggregation()} and included
	 * in the list of responses to {@link AggregatedResource#getAggregations()}.
	 *
	 * @param aggregations
	 */
	void setAggregations(List<URI> aggregations) throws OREException;

	/**
	 * Add an aggregation to the list of aggregations that this resource knows about
	 *
	 * @param aggregation
	 */
	void addAggregation(URI aggregation) throws OREException;

	/**
	 * Remove all the aggregations that this resource knows about.  If this resource
	 * is part of an aggregation in the current model, a reference to it will nonetheless
	 * be maintained.  This will be retrievable using {@link AggregatedResource#getAggregation()} and included
	 * in the list of responses to {@link AggregatedResource#getAggregations()}.
	 */
	void clearAggregations() throws OREException;

	/**
	 * Get the parent aggregation of this aggregated resource in the current model.
	 *
	 * The parent is defined as the single aggregation in the model which is described
	 * by the resource map, and which asserts an ore:aggregates relationship over
	 * this resource.  The resource can be added to the aggregation in two ways:
	 *
	 * By creating them separately
	 *
	 * <code>
	 * AggregatedResource ar = OREFactory.createAggregatedResource(uri_ar);
	 * Aggregation agg = OREFactory.createAggregation(uri_a);
	 * agg.addAggregatedResource(ar);
	 * </code>
	 *
	 * Or by creating the resource directly from the parent:
	 *
	 * <code>
	 * Aggregation agg = OREFactory.createAggregation(uri_a);
	 * AggregatedResource ar = agg.createAggregatedResource(uri_ar);
	 * </code>
	 *
	 * The resource is not limited to knowing about only one aggregation that it is
	 * a member of.  To see other aggregations that the resource knows about, use
	 * {@link AggregatedResource#getAggregations()}
	 *
	 * @return
	 * @throws OREException
	 */
	Aggregation getAggregation() throws OREException;

	///////////////////////////////////////////////////////////////////////
	// methods to deal with AggregatedResources which are also Aggregations
	///////////////////////////////////////////////////////////////////////

	/**
	 * For AggregatedResources which are also Aggregations in another sense,
	 * then we must be able to get hold of the resource map(s) describing it.
	 *
	 * This method returns the URIs of all the resource maps which are associated
	 * with this Aggregation/AggregatedResource
	 *
	 * @return
	 * @throws OREException
	 */
	List<URI> getResourceMaps() throws OREException;

	/**
	 * Specify the URIs of the resource maps whcih describe this Aggregated
	 * Resource if it is also to be treated as an Aggregation
	 *
	 * @param rems
	 * @throws OREException
	 */
	void setResourceMaps(List<URI> rems) throws OREException;

	/**
	 * Add a URI to a resource map which describes this Aggregated Resource if it
	 * is also to be treated as an Aggregation
	 *
	 * @param rem
	 * @throws OREException
	 */
	void addResourceMap(URI rem) throws OREException;

	/**
	 * Remove all references to any resource maps associated with this Aggregated
	 * Resource
	 *
	 * @throws OREException
	 */
	void clearResourceMaps() throws OREException;

	///////////////////////////////////////////////////////////////////
	// methods to deal with Proxies
	///////////////////////////////////////////////////////////////////

	/**
	 * Does this AggregatedResource have a Proxy currently associated with
	 * it in the graph?
	 *
	 * @return
	 * @throws OREException
	 */
	boolean hasProxy() throws OREException;

	/**
	 * Get a Proxy object representing the AggregatedResource in the context of
	 * the Aggregation it is part of.  This will return null if there is no
	 * Proxy (this can be tested with {@link AggregatedResource#hasProxy()}).
	 *
	 * If the AggregatedResource has not yet been added to an Aggregation, it
	 * cannot have a proxy.
	 *
	 * @return
	 * @throws OREException
	 */
	Proxy getProxy() throws OREException;

	/**
	 * Create a proxy representing this AggregatedResource in the context of the
	 * Aggregation it is currently part of.  This operation will throw an exception
	 * if the AggregatedResource is not currently part of an Aggregation, as it
	 * is a meaningless operation in that context
	 * 
	 * @param proxyURI
	 * @return
	 * @throws OREException
	 */
	Proxy createProxy(URI proxyURI) throws OREException;
}
