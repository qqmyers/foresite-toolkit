package org.dspace.foresite;

import java.net.URI;

/**
 * Created by IntelliJ IDEA.
 * User: richard
 * Date: Jun 20, 2008
 * Time: 3:19:50 PM
 * To change this template use File | Settings | File Templates.
 */
public enum OREModel
{
	describes(Vocab.ore_ResourceMap, Vocab.ore_describes, Vocab.ore_Aggregation),
	isDescribedBy(Vocab.ore_Aggregation, Vocab.ore_isDescribedBy, Vocab.ore_ResourceMap),
	aggregates(Vocab.ore_Aggregation, Vocab.ore_aggregates, Vocab.ore_AggregatedResource),
	isAggregatedBy(Vocab.ore_AggregatedResource, Vocab.ore_isAggregatedBy, Vocab.ore_Aggregation),
	proxyIn(Vocab.ore_Proxy, Vocab.ore_proxyIn, Vocab.ore_Aggregation),
	proxyFor(Vocab.ore_Proxy, Vocab.ore_proxyFor, Vocab.ore_AggregatedResource),
	lineage(Vocab.ore_Proxy, Vocab.ore_lineage, Vocab.ore_Proxy);

	Vocab subject;

	Vocab predicate;

	Vocab object;

	OREModel(Vocab subject, Vocab predicate, Vocab object)
	{
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
	}

	public Vocab getSubject()
	{
		return subject;
	}

	public Vocab getPredicate()
	{
		return predicate;
	}

	public Vocab getObject()
	{
		return object;
	}

	public static Vocab getSubjectOf(Vocab predicate)
	{
		OREModel[] models = OREModel.values();
		for (int i = 0; i < models.length; i++)
		{
			if (models[i].getPredicate().equals(predicate))
			{
				return models[i].getSubject();
			}
		}
		return null;
	}

	public static Vocab getObjectOf(Vocab predicate)
	{
		OREModel[] models = OREModel.values();
		for (int i = 0; i < models.length; i++)
		{
			if (models[i].getPredicate().equals(predicate))
			{
				return models[i].getObject();
			}
		}
		return null;
	}
}
