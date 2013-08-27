package br.com.ingenieux.cloudy.awseb.di;

import java.util.concurrent.ExecutorService;

import br.com.ingenieux.cloudy.awseb.util.EC2MetadataUtil;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.AmazonCloudFrontAsync;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Async;
import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheAsync;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkAsync;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingAsync;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceAsync;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementAsync;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSAsync;
import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53Async;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceAsync;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBAsync;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsync;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowAsync;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.name.Names;

/**
 * A Generic AWS Object Factory for Guice
 * 
 * @author aldrin
 * 
 */
public class BaseAWSModule extends AbstractModule implements Module {
	private AWSCredentialsProvider providerChain = new BeanstalkerCredentialsProviderChain();

	private ClientConfiguration clientConfiguration = new ClientConfiguration();

	private ExecutorService executorService = null;

	private Region region;

	public BaseAWSModule withRegion(String region) {
		return withRegion(Region.getRegion(Regions.fromName(region)));
	}

	public BaseAWSModule withRegion(Region region) {
		this.region = region;

		return this;
	}

	public BaseAWSModule withDynamicRegion() {
		return withDynamicRegion("us-east-1");
	}

	public BaseAWSModule withDynamicRegion(String defaultRegion) {
		String newRegion = defaultRegion;

		String availZone = EC2MetadataUtil.fetchMetadata("placement/availability-zone", defaultRegion);

		newRegion = availZone.replaceAll("(\\d)\\p{Lower}$", "$1");

		return withRegion(newRegion);
	}

	/**
	 * Represents a Guice Provider Factory for General Instantiation of AWS
	 * Clients
	 * 
	 * @author aldrin
	 * 
	 * @param <K>
	 *            the Service Interface class of an AWS Service Client
	 */
	public class AWSClientProvider<K extends AmazonWebServiceClient> implements
			Provider<K> {
		private final Class<K> serviceClass;

		public AWSClientProvider(Class<K> serviceClazz) {
			this.serviceClass = serviceClazz;
		}

		@Override
		public K get() {
			AWSCredentialsProvider awsCreds = getProviderChain();
			ClientConfiguration clientConfig = getClientConfiguration();

			try {
				return (K) region.createClient(serviceClass, awsCreds,
						clientConfig);
			} catch (Exception exc) {
				throw new RuntimeException(exc);
			}
		}
	}

	public AWSCredentialsProvider getCredentials() {
		return getProviderChain();
	}

	@Provides
	public AWSCredentialsProvider getProviderChain() {
		return providerChain;
	}

	public ClientConfiguration getClientConfiguration() {
		return clientConfiguration;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public void setClientConfiguration(ClientConfiguration clientConfiguration) {
		this.clientConfiguration = clientConfiguration;
	}

	public void setProviderChain(AWSCredentialsProvider providerChain) {
		this.providerChain = providerChain;
	}

	@SuppressWarnings("unchecked")
	public <K extends AmazonWebServiceClient> ScopedBindingBuilder bindClient(
			Class<K> serviceClazz) {
		try {
			// boolean asyncP = serviceClazz.getSimpleName().endsWith("Async");

			Class<K> clientClazz = (Class<K>) Class.forName(serviceClazz
					.getName() + "Client");

			AWSClientProvider<K> provider = new AWSClientProvider<K>(
					clientClazz);

			return bind(serviceClazz).toProvider(provider);
		} catch (Exception e) {
			if (RuntimeException.class.isAssignableFrom(e.getClass()))
				throw (RuntimeException) e;

			throw new RuntimeException(e);
		}
	}

	public void bindClients(
			Class<? extends AmazonWebServiceClient>... serviceClasses) {
		for (Class<? extends AmazonWebServiceClient> serviceClass : serviceClasses)
			bindClient(serviceClass);
	}

	@Override
	protected void configure() {
		configureClients();

		bind(String.class).annotatedWith(Names.named("aws.region")).toInstance(
				region.getName());
		bind(String.class).annotatedWith(Names.named("aws.availability.zone"))
				.toInstance(
						EC2MetadataUtil.fetchMetadata(
								"placement/availability-zone", "us-east-1z"));
		;
	}

	@SuppressWarnings("unchecked")
	protected void configureClients() {
		Class<?>[] serviceClasses = getServiceClasses();

		if (null == serviceClasses) {
			serviceClasses = new Class<?>[] { AmazonCloudFront.class, //
					AmazonCloudFrontAsync.class, //
					AmazonCloudWatch.class, //
					AmazonCloudWatchAsync.class, //
					AmazonDynamoDB.class, //
					AmazonDynamoDBAsync.class, //
					AmazonEC2.class, //
					AmazonEC2Async.class, //
					AWSElasticBeanstalk.class, //
					AWSElasticBeanstalkAsync.class, //
					AmazonElastiCache.class, //
					AmazonElastiCacheAsync.class, //
					AmazonElasticLoadBalancing.class, //
					AmazonElasticLoadBalancingAsync.class, //
					AmazonElasticMapReduce.class, //
					AmazonElasticMapReduceAsync.class, //
					AmazonIdentityManagement.class, //
					AmazonIdentityManagementAsync.class, //
					AmazonRDS.class, //
					AmazonRDSAsync.class, //
					AmazonRoute53.class, //
					AmazonRoute53Async.class, //
					AmazonS3.class, //
					AWSSecurityTokenService.class, //
					AWSSecurityTokenServiceAsync.class, //
					AmazonSimpleDB.class, //
					AmazonSimpleDBAsync.class, //
					AmazonSimpleEmailService.class, //
					AmazonSimpleEmailServiceAsync.class, //
					AmazonSimpleWorkflow.class, //
					AmazonSimpleWorkflowAsync.class, //
					AmazonSQS.class, //
					AmazonSQSAsync.class, //
					AmazonSNS.class, //
					AmazonSNSAsync.class, //
			};
		}

		bindClients((Class<? extends AmazonWebServiceClient>[]) serviceClasses);
	}

	protected Class<? extends AmazonWebServiceClient>[] getServiceClasses() {
		return null;
	}
}
