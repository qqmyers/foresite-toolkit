/*
 * Vocab.java
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
import java.net.URISyntaxException;

/**
 * @author Richard Jones
 */
public enum Vocab
{
	///////////////////////////////////////////////////////////////////
	// Vocabulary Definitions
	//////////////////////////////////////////////////////////////////

	// ORE Vocabulary: Predicates
	ore_aggregates		("ore", "aggregates", 		"http://www.openarchives.org/ore/terms/Aggregates", 	"http://www.openarchives.org/ore/terms/"),
	ore_isAggregatedBy	("ore", "isAggregatedBy", 	"http://www.openarchives.org/ore/terms/isAggregatedBy", "http://www.openarchives.org/ore/terms/"),
	ore_describes		("ore", "describes", 		"http://www.openarchives.org/ore/terms/describes", 		"http://www.openarchives.org/ore/terms/"),
	ore_isDescribedBy	("ore", "isDescribedBy", 	"http://www.openarchives.org/ore/terms/isDescribedBy", 	"http://www.openarchives.org/ore/terms/"),
	ore_similarTo		("ore", "similarTo", 		"http://www.openarchives.org/ore/terms/similarTo", 		"http://www.openarchives.org/ore/terms/"),
	ore_proxyFor		("ore", "proxyFor", 		"http://www.openarchives.org/ore/terms/proxyFor", 		"http://www.openarchives.org/ore/terms/"),
	ore_proxyIn			("ore", "proxyIn", 			"http://www.openarchives.org/ore/terms/proxyIn", 		"http://www.openarchives.org/ore/terms/"),
	ore_lineage			("ore", "lineage", 			"http://www.openarchives.org/ore/terms/lineage", 		"http://www.openarchives.org/ore/terms/"),

	// ORE Vocabulary: Resource Types
	ore_ResourceMap			("ore", "ResourceMap", 			"http://www.openarchives.org/ore/terms/ResourceMap", 		"http://www.openarchives.org/ore/terms/"),
	ore_Aggregation			("ore", "Aggregation", 			"http://www.openarchives.org/ore/terms/Aggregation", 		"http://www.openarchives.org/ore/terms/"),
	ore_AggregatedResource	("ore", "AggregatedResource", 	"http://www.openarchives.org/ore/terms/AggregatedResource", "http://www.openarchives.org/ore/terms/"),
	ore_Proxy				("ore", "Proxy", 				"http://www.openarchives.org/ore/terms/Proxy", 				"http://www.openarchives.org/ore/terms/"),

	// Dublin Core
	dc_title("dc", "title", "http://purl.org/dc/elements/1.1/title", "http://purl.org/dc/elements/1.1/"),

	// DC Terms
	dcterms_Agent("dcterms", "Agent", "http://purl.org/dc/terms/Agent", "http://purl.org/dc/terms/"),

	// RDFS: predicates
	rdfs_label("rdfs", "label", "http://www.w3.org/2000/01/rdf-schema#label", "http://www.w3.org/2000/01/rdf-schema#");

	///////////////////////////////////////////////////////////////////
	// Enum implementation
	///////////////////////////////////////////////////////////////////

	private String nsName;
	private String elementName;
	private URI uri;
	private URI namespace;

	Vocab(String nsName, String elementName, String uri, String namespace)
	{
		try
		{
			this.nsName = nsName;
			this.elementName = elementName;
			this.uri = new URI(uri);
			this.namespace = new URI(namespace);
		}
		catch (URISyntaxException e)
		{
			// do nothing, swallow the errors, there shouldn't be any anyway
		}
	}

	// getters

	public String schema()
	{
		return this.nsName;
	}

	public String element()
	{
		return this.elementName;
	}

	public URI uri()
	{
		return this.uri;
	}

	public URI ns()
	{
		return this.namespace;
	}

	// query methods

	public boolean inNamespace(String ns)
	{
		if (this.nsName.equals(ns))
		{
			return true;
		}
		return false;
	}

	// static discovery methods

	public static Vocab getByURI(URI uri)
	{
		Vocab[] elements = Vocab.values();
		for (int i = 0; i < elements.length; i++)
		{
			if (uri.equals(elements[i].uri()))
			{
				return elements[i];
			}
		}
		return null;
	}
}