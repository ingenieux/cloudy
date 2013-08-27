package br.com.ingenieux.cloudy.awseb.di;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import br.com.ingenieux.cloudy.awseb.config.ConfigStore;
import br.com.ingenieux.cloudy.awseb.config.TwelveFactorConfigStore;

import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ListDomainsResult;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;

public class TwelveFactorConfigModule extends AbstractModule {
	@Provides
	@Named("config.domain.name")
	@Inject
	@Singleton
	public String getDomainName(
			@Named("beanstalk.env") EnvironmentDescription env) {
		return String.format("cloudy-config-%s", env.getApplicationName());
	}

	@Provides
	@Inject
	@Singleton
	public ConfigStore getTwelveFactorConfig(
			@Named("beanstalk.env.name") String envName,
			@Named("cloudy.twelvefactor.config") Map<String, String> maps,
			@Named("config.domain.name") String domainName,
			AmazonSimpleDB simpleDb) {
		return new TwelveFactorConfigStore(envName, maps, domainName, simpleDb);
	}

	@Provides
	@Named("cloudy.twelvefactor.config")
	@Inject
	@Singleton
	public Map<String, String> getConfig(
			@Named("beanstalk.env") EnvironmentDescription env,
			@Named("config.domain.name") String domainName, AmazonSimpleDB sdb) {
		Map<String, String> result = new LinkedHashMap<String, String>();

		ListDomainsResult domains = sdb.listDomains();

		if (!domains.getDomainNames().contains(domainName)) {
			sdb.createDomain(new CreateDomainRequest(domainName));
		}

		SelectResult selectResults = sdb.select(new SelectRequest()
				.withSelectExpression(String.format(
						"SELECT key, value FROM `%s` WHERE envName='%s'",
						domainName, env.getEnvironmentName())));

		for (Item i : selectResults.getItems()) {
			List<Attribute> attrs = i.getAttributes();

			String key = null;
			String value = null;
			for (Attribute a : attrs) {
				if ("key".equals(a.getName())) {
					key = a.getValue();
				} else if ("value".equals(a.getName())) {
					value = a.getValue();
				}
			}

			if (null != key && null != value)
				result.put(key, value);
		}

		return result;
	}

	@Override
	protected void configure() {
	}
}
