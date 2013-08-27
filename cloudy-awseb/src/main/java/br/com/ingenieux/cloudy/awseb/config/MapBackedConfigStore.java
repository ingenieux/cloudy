package br.com.ingenieux.cloudy.awseb.config;

import java.util.HashMap;
import java.util.Map;

public class MapBackedConfigStore implements ConfigStore {
	Map<String, String> backingStore = new HashMap<String, String>();

	@Override
	public void set(String key, String value) {
		backingStore.put(key, value);
	}

	@Override
	public String get(String key) {
		return get(key, null);
	}

	@Override
	public String get(String key, String defaultValue) {
		if (! backingStore.containsKey(key))
			return defaultValue;
		
		return backingStore.get(key);
	}
	
	

}
