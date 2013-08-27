package br.com.ingenieux.cloudy.awseb.di;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;

public class TwelveFactorConfigModule extends AbstractModule {
	/*
	@Provides
	@Named("config")
	@Inject
	@Singleton
	public Map<String, String> getConfig(@Named("beanstalk.app") String app, @Named("aws.region") String region, @Named("beanstalk.environment") String environment, AmazonSimpleDB simpleDb) {
		return null;
	} */

	@Override
	protected void configure() {
	}
}
