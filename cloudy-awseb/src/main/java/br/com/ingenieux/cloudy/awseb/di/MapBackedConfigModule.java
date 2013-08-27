package br.com.ingenieux.cloudy.awseb.di;

import br.com.ingenieux.cloudy.awseb.config.ConfigStore;
import br.com.ingenieux.cloudy.awseb.config.MapBackedConfigStore;

import com.google.inject.AbstractModule;

public class MapBackedConfigModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(ConfigStore.class).to(MapBackedConfigStore.class);
	}
}
