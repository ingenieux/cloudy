package br.com.ingenieux.cloudy.awseb.di;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class BeanstalkEnvironmentModule extends AbstractModule {
	@Override
	protected void configure() {
	}
	
	@Provides
	@Singleton
	@Named("beanstalk.app.name")
	public String getAppName(@Named("beanstalk.env.id") String envId, AWSElasticBeanstalk eb) {
		DescribeEnvironmentsResult environments = eb.describeEnvironments(new DescribeEnvironmentsRequest().withEnvironmentIds(envId));
		
		List<EnvironmentDescription> envs = environments.getEnvironments();
		
		if (envs.isEmpty()) {
			return "n/a";
		}
		
		return envs.get(0).getApplicationName();
	}

	@Provides
	@Singleton
	@Named("beanstalk.env.id")
	public String getEnvId(@Named("aws.instance.id") String instanceId, AmazonEC2 ec2) {
		DescribeInstancesRequest request = new DescribeInstancesRequest()
				.withInstanceIds(instanceId)
				.withFilters(
						new Filter("instance-state-name").withValues("running"));

		for (Reservation r : ec2.describeInstances(request).getReservations()) {
			for (Instance i : r.getInstances()) {
				for (Tag t : i.getTags()) {
					if ("elasticbeanstalk:environment-id".equals(t.getKey())) {
						return t.getValue();
					}
				}
			}
		}
		
		return "n/a";
	}

	@Provides
	@Singleton
	@Named("beanstalk.env.name")
	public String getEnvName(@Named("aws.instance.id") String instanceId, AmazonEC2 ec2) {
		DescribeInstancesRequest request = new DescribeInstancesRequest()
				.withInstanceIds(instanceId)
				.withFilters(
						new Filter("instance-state-name").withValues("running"));

		for (Reservation r : ec2.describeInstances(request).getReservations()) {
			for (Instance i : r.getInstances()) {
				for (Tag t : i.getTags()) {
					if ("elasticbeanstalk:environment-name".equals(t.getKey())) {
						return t.getValue();
					}
				}
			}
		}
		
		return "n/a";
	}

}
