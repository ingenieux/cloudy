package br.com.ingenieux.cloudy.awseb.config;

public interface ConfigStore {

	void set(String key, String value);

	String get(String key);

	String get(String key, String defaultValue);

}