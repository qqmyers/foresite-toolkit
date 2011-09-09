/*
 * Agent.java
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
 * Interface representing an Agent which can be attached to any ORE resource to
 * represent a person or other entity which has acted upon the resource.
 *
 * Agents can be either blank nodes or bound to a URI, depending on how they
 * are initialised.
 *
 * This class extends {@link OREResource}, so inherits all the interface
 * definitions from there also.
 *
 * @see OREResource
 *
 * @author Richard Jones
 */
public interface Agent extends OREResource
{
	///////////////////////////////////////////////////////////////////
	// methods for initialising the Agent
    ///////////////////////////////////////////////////////////////////

	/**
	 * Initialise an Agent as a blank node
	 */
	void initialise();

	/**
	 * Initialise an Agent with the given URI
	 *
	 * @param uri
	 */
	void initialise(URI uri);

	///////////////////////////////////////////////////////////////////
	// methods for setting specific Agent properties
	///////////////////////////////////////////////////////////////////

	/**
	 * Get all the names associated with this Agent
	 *
	 * @return	the list of names associated with this Agent
	 * @throws OREException
	 */
	List<String> getNames() throws OREException;

	/**
	 * Set the list of names associated with this Agent.  Should override
	 * any existing names
	 *
	 * @param names
	 */
	void setNames(List<String> names);

	/**
	 * Add the given name to the current list of names associated with this Agent
	 * @param name
	 */
	void addName(String name);

	/**
	 * Get all the mboxes (i.e. email address URIs) associated with this Agent
	 *
	 * @return the list of mboxes associated with this agent
	 * @throws OREException
	 */
	List<URI> getMboxes() throws OREException;

	/**
	 * Set the list of mboxes (i.e. email address URIs) asscoiated with this Agent.
	 * Should override any existing mboxes
	 *
	 * @param mboxes
	 */
	void setMboxes(List<URI> mboxes);

	/**
	 * Add the given mbox (i.e. email address URI) to the Agent
	 * 
	 * @param mbox
	 */
	void addMbox(URI mbox);
}