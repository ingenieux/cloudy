package br.com.ingenieux.cloudy.awseb.config;

import java.util.Map;

import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;

public class TwelveFactorConfigStore implements ConfigStore {
	private Map<String, String> backingStore;

	private String domainName;

	private AmazonSimpleDB simpleDb;

	private String envName;

	public TwelveFactorConfigStore(String envName, Map<String, String> maps,
			String domainName, AmazonSimpleDB simpleDb) {
		this.envName = envName;
		this.backingStore = maps;
		this.domainName = domainName;
		this.simpleDb = simpleDb;
	}

	public void set(String key, String value) {
		boolean changed = !value.equals(backingStore.get(key));
		
		if (changed) {
			String itemName = String.format("%s-%s", envName, key);
			
			ReplaceableAttribute newAttrKey = new ReplaceableAttribute().withName("key").withValue(key).withReplace(true);
			ReplaceableAttribute newAttrValue = new ReplaceableAttribute().withName("value").withValue(value).withReplace(true);
			
			simpleDb.putAttributes(new PutAttributesRequest().withDomainName(domainName).withItemName(itemName).withAttributes(newAttrKey, newAttrValue));
			
			backingStore.put(key, value);
		}
	}
	
	public String get(String key) {
		return get(key, null);
	}
	
	public String get(String key, String defaultValue) {
		if (! backingStore.containsKey(key))
			return defaultValue;
		
		return backingStore.get(key);
	}
}
