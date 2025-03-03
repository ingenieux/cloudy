package br.com.ingenieux.cloudy.awseb.util;

import io.github.pixee.security.HostValidator;
import io.github.pixee.security.Urls;
import java.net.URL;

import org.apache.commons.io.IOUtils;

public class EC2MetadataUtil {
	public static String fetchMetadata(String metaPath, String defaultResult) {
		try {
			URL metaUrl = Urls.create("http://169.254.169.254/latest/meta-data/"
					+ metaPath, Urls.HTTP_PROTOCOLS, HostValidator.DENY_COMMON_INFRASTRUCTURE_TARGETS);
			return IOUtils.toString(metaUrl.openStream());
		} catch (Exception exc) {
			return defaultResult;
		}
	}
}
