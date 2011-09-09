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
package org.dspace.foresite.atom;

import org.dspace.foresite.OREParser;
import org.dspace.foresite.ResourceMap;
import org.dspace.foresite.OREParserException;
import org.dspace.foresite.OREFactory;
import org.dspace.foresite.Aggregation;
import org.dspace.foresite.Agent;
import org.dspace.foresite.OREVocabulary;
import org.dspace.foresite.OREException;
import org.dspace.foresite.AggregatedResource;
import org.dspace.foresite.ReMSerialisation;
import org.dspace.foresite.Proxy;
import org.dspace.foresite.Vocab;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;


import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Date;
import java.util.Properties;

import com.sun.syndication.io.XmlReader;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedInput;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.atom.Person;
import com.sun.syndication.feed.atom.Link;
import com.sun.syndication.feed.atom.Category;
import com.sun.syndication.feed.atom.Generator;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Content;

/**
 * @Author: Richard Jones
 */
public class AtomOREParser implements OREParser
{
	public static Namespace rdfNS = Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

	public ResourceMap parse(InputStream is, URI uri) throws OREParserException
	{
		return null;
	}

	public ResourceMap parse(InputStream is)
            throws OREParserException
    {
        try
        {
            // read in the ATOM document
            XmlReader reader = new XmlReader(is);
            WireFeedInput input = new WireFeedInput();
            Feed atom = (Feed) input.build(reader);

            // mine the atom feed
            URI uri_a = new URI(atom.getId());
			List<Link> links = atom.getOtherLinks();
			List<Link> altLinks = atom.getAlternateLinks();
            links.addAll(altLinks); // add all the links together for convenience
			List<Person> authors = atom.getAuthors();
			String title = atom.getTitle();
			List<Category> categories = atom.getCategories();
			List<Person> contributors = atom.getContributors();
			Date updated = atom.getUpdated();
			Generator generator = atom.getGenerator();
			String rights = atom.getRights();
			List<Element> rdf = (List<Element>) atom.getForeignMarkup();
			List<Entry> entries = atom.getEntries();

			// do the initial validation
			this.validate(categories);

			// declare some very specific required variables
			URI uri_r = this.getURIR(links);

			// start reading in the atom feed to the model
			//////////////////////////////////////////////

			// atom:id :: URI-A
			Aggregation agg = OREFactory.createAggregation(uri_a);

			// atom:link@rel='self' :: URI-R
			ResourceMap rem = agg.createResourceMap(uri_r);

			// atom:author :: URI-A dcterms:creator
            for (Person author : authors)
            {
				String auri = author.getUri();
				Agent creator;
				if (auri != null)
				{
					creator = OREFactory.createAgent(new URI(auri));
				}
				else
				{
					creator = OREFactory.createAgent();
				}

				String name = author.getName();
				if (name != null)
				{
					creator.addName(name);
				}

				String mbox = author.getEmail();
				if (mbox != null)
				{
					if (!mbox.startsWith("mailto:"))
					{
						mbox = "mailto:" + mbox;
					}
					creator.addMbox(new URI(mbox));
				}

				agg.addCreator(creator);
			}

			// atom:title :: Aggregation title
			if (title != null)
			{
				agg.addTitle(title);
			}

			// atom:category :: URI-A rdf:type
            for (Category category : categories)
            {
				// exclude the mandatory Aggregation, as this exists by default in the model
				String aggURI = Vocab.ore_Aggregation.uri().toString();
                if (!aggURI.equals(category.getTerm()))
                {
                    // the @term specifies the rdf:type relation
                    String type = category.getTerm();
					URI typeURI = new URI(type);
					agg.addType(typeURI);

					// FIXME: we need to find a nice simple way of doing this
					// FIXME: actually, don't we just want to build this into the model
					// 			in all cases?
					// the @scheme specifies the rdfs:isDefinedBy relation of the term
					// Triple triple = OREFactory.createTriple(typeURI, new URI(""));

					// FIXME: we need to find a nice simple way of doing this
					// the @label specifies the rdfs:label relation
					String label = category.getLabel();
					agg.createTriple(Vocab.rdfs_label, label);
				}
            }

			// deal with the various links
			for (Link link : links)
			{
				// atom:link@rel='related' :: URI-A ore:similarTo
				if ("related".equals(link.getRel()))
				{
					agg.addSimilarTo(new URI(link.getHref()));

					// FIXME: we need to find a nice simple way of doing these

					// @type :: URI-similar dc:format
					// @hreflang :: URI-similar dc:language
					// @title :: URI-similar dc:title
				}

				// atom:link@rel='alternate' :: URI-A ore:isDescribedBy
				if ("alternate".equals(link.getRel()))
				{
					ReMSerialisation serial = new ReMSerialisation();
					serial.setURI(new URI(link.getHref()));

					// FIXME: we need to find a nice simple way of doing these

					// @type :: URI-R-other dc:format
					// @hreflang :: URI-R-other dc:language
					// @title :: URI-R-other dc:title

					// FIXME: consider adding a title to the ReMSerialisation class?

					// add the resource map serialisation
					agg.addReMSerialisation(serial);
				}

				// atom:link@rel='alternate' :: URI-A dcterms:rights URI-rights
				if ("license".equals(link.getRel()))
				{
					// FIXME: the model doesn't currently support Rights as a URI
					// FIXME: we need to find a nice simple way of doing these

					// @type :: URI-R-other dc:format
					// @hreflang :: URI-R-other dc:language
					// @title :: URI-R-other dc:title
				}
			}

			// FIXME: need an easier way to add arbitrary metadata
			// atom:icon :: URI-A foaf:logo URI-icon
			String iconURI = atom.getIcon();

			// FIXME: not yet supported by the model
			// atom:contributor :: URI-A dcterms:contributor URI-contributor
			// use the List<Contributor> defined above

			// FIXME: meed an easier way to add arbitrary metadata
			// atom:subtitle :: URI-A dc:description

			// atom:updated :: URI-R dcterms:modified
			if (updated != null)
            {
                rem.setModified(updated);
            }

			// atom:generator :: URI-R dcterms:creator
			if (generator != null)
            {
				Agent remCreator;
				String genURL = generator.getUrl();
				if (genURL != null)
				{
					remCreator = OREFactory.createAgent(new URI(genURL));
				}
				else
				{
					remCreator = OREFactory.createAgent();
				}

				String name = generator.getValue();
				if (name != null)
				{
					remCreator.addName(generator.getValue());
				}

                rem.addCreator(remCreator);
            }

			// atom:rights :: URI-R dc:rights
			if (rights != null)
			{
				rem.setRights(rights);
			}

			// now process the individual atom entries
			//////////////////////////////////////////

			for (Entry entry : entries)
			{
				this.aggregatedResource(entry, agg);
			}

            // Finally extract the goodies from the embedded RDF
			////////////////////////////////////////////////////

			// rdf:Description :: Add to model directly
			Element root = new Element("RDF", rdfNS);
			for (Element element : rdf)
			{
				root.addContent(element);
			}
			XMLOutputter out = new XMLOutputter();
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			out.output(root, os);
			agg.addRDF(os.toString(), "RDF/XML");

			/* FIXME: I don't think this is relevant any longer, but hold on to it for the moment ...
			AggregationRDF ardf = new AggregationRDF();
            RemRDF rrdf = new RemRDF();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            Namespace oreNs = Namespace.getNamespace("ore", "http://www.openarchives.org/ore/terms/");
            Namespace rdfNs = Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            Namespace dcNs = Namespace.getNamespace("dc", "http://purl.org/dc/terms/");
            for (Element element : rdf)
            {
                String about = element.getAttributeValue("about");
                if (about.equals(uri_a.toString()))
                {
                    ardf.setAbout(about);

                    List<Element> children = element.getChildren("isAggregatedBy", oreNs);
                    List<String> aggregatedBy = new ArrayList<String>();
                    for (Element child : children)
                    {
                        String aggregator = child.getValue().trim();
                        aggregatedBy.add(aggregator);
                    }
                    ardf.setAggregatedBy(aggregatedBy);

                    List<Element> kids = element.getChildren("created", dcNs);
                    List<Date> created = new ArrayList<Date>();
                    for (Element kid : kids)
                    {
                        String create = kid.getValue().trim();
                        Date date = sdf.parse(create);
                        created.add(date);
                    }
                    ardf.setCreated(created);
                }
                else if (about.equals(uri_r.toString()))
                {
                    rrdf.setAbout(about);

                    List<Element> kids = element.getChildren("created", dcNs);
                    List<Date> created = new ArrayList<Date>();
                    for (Element kid : kids)
                    {
                        String create = kid.getValue().trim();
                        Date date = sdf.parse(create);
                        created.add(date);
                    }
                    rrdf.setCreated(created);
                }

				// FIXME: what if the about is about neither?????
			}*/

			// finally, some cleaning up
			////////////////////////////

			// if no date rem created specified, use now
			if (rem.getCreated() == null)
			{
				rem.setCreated(new Date());
			}

			// if no date aggregation created specified, use now
			if (agg.getCreated() == null)
			{
				agg.setCreated(new Date());
			}

			// now we can just return (the model is already assembled)
			//////////////////////////////////////////////////////////
			
			return rem;
        }
        catch (IOException e)
        {
            throw new OREParserException(e);
        }
        catch (FeedException e)
        {
            throw new OREParserException(e);
        }
        catch (URISyntaxException e)
        {
            throw new OREParserException(e);
        }
        catch (OREException e)
        {
            throw new OREParserException(e);
        }
    }

	private void aggregatedResource(Entry entry, Aggregation agg)
			throws URISyntaxException, OREException, OREParserException, IOException
	{
		// mine the entry
		URI uri_p = null;
		String proxyURI = entry.getId();
		if (proxyURI != null)
		{
			uri_p = new URI(proxyURI);
		}

		List<Link> links = entry.getOtherLinks();
		List<Link> altLinks = entry.getAlternateLinks();
		links.addAll(altLinks); // add all the links together for convenience

		List<Person> authors = entry.getAuthors();
		String title = entry.getTitle();
		List<Category> categories = entry.getCategories();
		List<Person> contributors = entry.getContributors();
		Content summary = entry.getSummary();
		List<Element> rdf = (List<Element>) entry.getForeignMarkup();

		URI uri_ar = this.getURIAR(links);

		AggregatedResource ar = agg.createAggregatedResource(uri_ar);
		Proxy proxy = null;
		if (uri_p != null)
		{
			proxy = ar.createProxy(uri_p);
		}

		for (Link link : links)
		{
			// atom:link@rel='alternate' :: URI-AR
			if ("alternate".equals(link.getRel()))
			{
				// FIXME: need an easy way to add this information
				// @type :: URI-AR dc:format
				// @hreflang :: URI-AR dc:language
				// @title :: URI-AR dc:title
				// @length :: URI-AR dcterms:extent
			}

			// atom:link@rel='related' :: URI-A-other
			if ("related".equals(link.getRel()))
			{
				ar.addAggregation(new URI(link.getHref()));

				// FIXME: need an easy way to add this information
				// @hreflang :: URI-AR dc:language
				// @title :: URI-AR dc:title
			}

			// atom:link@rel='via' :: URI-P-other
			if ("via".equals(link.getRel()))
			{
				if (proxy != null)
				{
					proxy.setLineage(new URI(link.getRel()));

					// FIXME: need an easy way to add this information
					// @type :: URI-P-other dc:format
					// @hreflang :: URI-P-other dc:language
					// @title :: URI-P-other dc:title
				}
			}
		}

		// atom:author :: URI-AR dcterms:creator
		if (authors != null)
		{
			for (Person author : authors)
			{
				Agent creator;
				String authorURI = author.getUri();
				if (authorURI != null)
				{
					creator = OREFactory.createAgent(new URI(authorURI));
				}
				else
				{
					creator = OREFactory.createAgent();
				}

				String name = author.getName();
				if (name != null)
				{
					creator.addName(name);
				}

				String mbox = author.getEmail();
				if (mbox != null)
				{
					if (!mbox.startsWith("mailto:"))
					{
						mbox = "mailto:" + mbox;
					}
					creator.addMbox(new URI(mbox));
				}

				ar.addCreator(creator);
			}
		}

		// FIXME: need easier way of adding this information
		// atom:title :: URI-AR dc:title

		// atom:category :: URI-AR rdf:type
		for (Category category : categories)
		{
			ar.addType(new URI(category.getTerm()));

			// @scheme :: URI-term rdfs:isDefinedBy
			// @label :: URI-term rdfs:label
		}

		// FIXME: the model does not yet support this
		// atom:contributors :: URI-AR dcterms:contributor

		// FIXME: need easier way of adding this information
		// atom:summary :: URI-AR dcterms:abstract

		// rdf:Description :: Add to model directly
		Element root = new Element("RDF", rdfNS);
		for (Element element : rdf)
		{
			root.addContent(element);
		}
		XMLOutputter out = new XMLOutputter();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		out.output(root, os);
		ar.addRDF(os.toString(), "RDF/XML");
	}

	/**
     * is this a valid resource map (i.e. is there a category saying so)?
     * 
     * @param categories
     * @throws OREParserException
     */
    private void validate(List<Category> categories)
            throws OREParserException
    {
        for (Category category : categories)
        {
            String aggURI = OREVocabulary.aggregation.getUri().toString();
            if (aggURI.equals(category.getTerm()))
            {
                return;
            }
        }

        throw new OREParserException("Passed ATOM document is not an ORE Resource Map; it is missing a valid atom:category statement");
    }

    private URI getURIR(List<Link> links)
            throws OREParserException
    {
        try
        {
            for (Link link : links)
            {
                String rel = link.getRel();
                if ("self".equals(rel))
                {
                    return new URI(link.getHref());
                }
            }
            throw new OREParserException("Passed ATOM document does not contain a URI for the Resource Map; atom:link[@rel='self']");
        }
        catch (URISyntaxException e)
        {
            throw new OREParserException("unable to parse link in atom:link[@rel='self']", e);
        }
    }

	private URI getURIAR(List<Link> links)
            throws OREParserException
    {
        try
        {
            for (Link link : links)
            {
                String rel = link.getRel();
                if ("alternate".equals(rel))
                {
                    return new URI(link.getHref());
                }
            }
            throw new OREParserException("Passed ATOM document does not contain a URI for the Aggregated Resource");
        }
        catch (URISyntaxException e)
        {
            throw new OREParserException("unable to parse link in atom:link[@rel='alternate']", e);
        }
    }

	public void configure(Properties properties)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

	/* backup copy; has been re-written above
	public ResourceMap parse(InputStream is)
				throws OREParserException
		{
			try
			{
				// read in the ATOM document
				XmlReader reader = new XmlReader(is);
				WireFeedInput input = new WireFeedInput();
				Feed atom = (Feed) input.build(reader);

				// mine the atom feed
				URI uri_a = new URI(atom.getId());
				String title = atom.getTitle();
				List<Person> authors = atom.getAuthors();
				List<Link> links = atom.getOtherLinks();
				List<Link> altLinks = atom.getAlternateLinks();
				links.addAll(altLinks); // add all the links together
				List<Category> categories = atom.getCategories();
				List<Element> rdf = (List<Element>) atom.getForeignMarkup();
				Date updated = atom.getUpdated();
				Generator generator = atom.getGenerator();
				String rights = atom.getRights();
				List<Entry> entries = atom.getEntries();

				this.validate(categories);
				URI uri_r = this.getURIR(links);

				// extract the goodies from the embedded RDF
				AggregationRDF ardf = new AggregationRDF();
				RemRDF rrdf = new RemRDF();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
				Namespace oreNs = Namespace.getNamespace("ore", "http://www.openarchives.org/ore/terms/");
				Namespace rdfNs = Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
				Namespace dcNs = Namespace.getNamespace("dc", "http://purl.org/dc/terms/");
				for (Element element : rdf)
				{
					String about = element.getAttributeValue("about");
					if (about.equals(uri_a.toString()))
					{
						ardf.setAbout(about);

						List<Element> children = element.getChildren("isAggregatedBy", oreNs);
						List<String> aggregatedBy = new ArrayList<String>();
						for (Element child : children)
						{
							String aggregator = child.getValue().trim();
							aggregatedBy.add(aggregator);
						}
						ardf.setAggregatedBy(aggregatedBy);

						List<Element> kids = element.getChildren("created", dcNs);
						List<Date> created = new ArrayList<Date>();
						for (Element kid : kids)
						{
							String create = kid.getValue().trim();
							Date date = sdf.parse(create);
							created.add(date);
						}
						ardf.setCreated(created);
					}
					else if (about.equals(uri_r.toString()))
					{
						rrdf.setAbout(about);

						List<Element> kids = element.getChildren("created", dcNs);
						List<Date> created = new ArrayList<Date>();
						for (Element kid : kids)
						{
							String create = kid.getValue().trim();
							Date date = sdf.parse(create);
							created.add(date);
						}
						rrdf.setCreated(created);
					}
				}

				// construct our resource map
				ResourceMap rem = OREFactory.createResourceMap(uri_r);

				if (updated != null)
				{
					rem.setModified(updated);
				}
				if (rights != null)
				{
					rem.setRights(rights);
				}
				rem.setCreated(rrdf.getCreated().get(0));

				if (generator != null)
				{
					Agent remCreator = OREFactory.createAgent();
					remCreator.addName(generator.getValue());
					String genURL = generator.getUrl();
					if (genURL != null)
					{
						// remCreator.addSeeAlso(new URI(genURL));
					}
					rem.addCreator(remCreator);
				}

				// construct the aggregation
				Aggregation aggregation = OREFactory.createAggregation(uri_a);
				aggregation.addTitle(title);
				aggregation.setCreated(ardf.getCreated().get(0));

				List<ReMSerialisation> rems = new ArrayList<ReMSerialisation>();
				for (String aggregator : ardf.getAggregatedBy())
				{
					// FIXME: can we get the mimetype from anywhere?
					ReMSerialisation serial = new ReMSerialisation();
					serial.setURI(new URI(aggregator));
					rems.add(serial);
				}
				aggregation.setReMSerialisations(rems);

				List<String> types = new ArrayList<String>();
				for (Category category : categories)
				{
					String aggURI = OREVocabulary.aggregation.getUri().toString();
					if (!aggURI.equals(category.getTerm()))
					{
						// these specify the aggregation type
						String type = category.getTerm();
						types.add(type);
					}
				}
				// aggregation.setTypes(types);

				List<Agent> creators = new ArrayList<Agent>();
				for (Person author : authors)
				{
					Agent creator = OREFactory.createAgent();
					creator.addName(author.getName());
					creators.add(creator);
				}
				aggregation.setCreators(creators);

				List<URI> similars = new ArrayList<URI>();
				for (Link link : links)
				{
					String rel = link.getRel();
					if ("related".equals(rel))
					{
						similars.add(new URI(link.getHref()));
					}
				}
				aggregation.setSimilarTo(similars);

				// process the entries, each of which is an AggregatedResource
				List<AggregatedResource> ars = new ArrayList<AggregatedResource>();
				for (Entry entry : entries)
				{
					// mine the entry
					URI uri_ar = new URI(entry.getId());
					String arTitle = entry.getTitle();
					Date arUpdated = entry.getUpdated();
					List<Link> arAlternates = entry.getAlternateLinks();
					List<Category> arCategories = entry.getCategories();

					AggregatedResource ar = OREFactory.createAggregatedResource(uri_ar);
					ars.add(ar);
				}

				// construct the model
				aggregation.setAggregatedResources(ars);
				rem.setAggregation(aggregation);

				return rem;
			}
			catch (IOException e)
			{
				throw new OREParserException(e);
			}
			catch (FeedException e)
			{
				throw new OREParserException(e);
			}
			catch (URISyntaxException e)
			{
				throw new OREParserException(e);
			}
			catch (OREException e)
			{
				throw new OREParserException(e);
			}
			catch (ParseException e)
			{
				throw new OREParserException(e);
			}
		}*/

	///////////////////////////////////////////////////////////////////
	// Privately used classes
	///////////////////////////////////////////////////////////////////

	// FIXME: these should no longer be necessary

	private class AggregationRDF
    {
        private String about;

        private List<String> isAggregatedBy;

        private List<Date> created;

        public String getAbout()
        {
            return about;
        }

        public void setAbout(String about)
        {
            this.about = about;
        }

        public List<String> getAggregatedBy()
        {
            return isAggregatedBy;
        }

        public void setAggregatedBy(List<String> aggregatedBy)
        {
            isAggregatedBy = aggregatedBy;
        }

        public List<Date> getCreated()
        {
            return created;
        }

        public void setCreated(List<Date> created)
        {
            this.created = created;
        }
    }

    private class RemRDF
    {
        private String about;

        private List<Date> created;

        public String getAbout()
        {
            return about;
        }

        public void setAbout(String about)
        {
            this.about = about;
        }

        public List<Date> getCreated()
        {
            return created;
        }

        public void setCreated(List<Date> created)
        {
            this.created = created;
        }
    }
}
