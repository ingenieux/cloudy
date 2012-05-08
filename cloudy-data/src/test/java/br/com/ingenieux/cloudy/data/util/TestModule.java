package br.com.ingenieux.cloudy.data.util;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;

import br.com.ingenieux.cloudy.data.test.Customer;
import br.com.ingenieux.cloudy.data.test.TestDao;
import br.com.ingenieux.cloudy.data.util.DaoProvider;
import br.com.ingenieux.cloudy.data.util.SDBDomainService;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.spaceprogram.simplejpa.EntityManagerFactoryImpl;

public class TestModule extends AbstractModule implements Module {
    private String unitName;

    public TestModule(String unitName) {
        this.unitName = unitName;
    }

    @Override
    protected void configure() {
        bind(SDBDomainService.class).in(Singleton.class);

        bind(TestDao.class).toProvider(new DaoProvider<TestDao>(TestDao.class, getEntityManager()));
    }

    private EntityManager getEntityManager() {
        EntityManagerFactoryImpl emfi = EntityManagerFactoryImpl.newInstanceWithClassNames(unitName, null, Customer.class.getName());

        return emfi.createEntityManager();
    }

    @Inject
    @Provides
    public AmazonSimpleDB getAmazonSimpleDb(AWSCredentials awsCredentials) {
        return new AmazonSimpleDBClient(awsCredentials);
    }

    @Provides
    public AWSCredentials getAWSCredentials(@Named("accessKey") String accessKey, @Named("secretKey") String secretKey) {
        return new BasicAWSCredentials(accessKey, secretKey);
    }
}
