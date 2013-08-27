package br.com.ingenieux.cloudy.awseb.util;

import java.net.URL;

import org.apache.commons.io.IOUtils;

public class EC2MetadataUtil {
	public static String fetchMetadata(String metaPath, String defaultResult) {
		try {
			URL metaUrl = new URL("http://169.254.169.254/latest/meta-data/"
					+ metaPath);
			return IOUtils.toString(metaUrl.openStream());
		} catch (Exception exc) {
			return defaultResult;
		}
	}
}
