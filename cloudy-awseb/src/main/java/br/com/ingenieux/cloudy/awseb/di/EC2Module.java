package br.com.ingenieux.cloudy.awseb.di;

import javax.inject.Inject;

import br.com.ingenieux.cloudy.awseb.util.EC2MetadataUtil;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class EC2Module extends AbstractModule {
	@Override
	protected void configure() {
	}

	@Provides
	@Named("aws.instance.id")
	@Inject
	@Singleton
	public String getInstanceId() {
		return EC2MetadataUtil.fetchMetadata("instance-id", "i-ffffffff");
	}

	@Provides
	@Named("aws.instance.type")
	@Inject
	@Singleton
	public String getInstanceType() {
		return EC2MetadataUtil.fetchMetadata("instance-type", "m0.verylarge");
	}
}
