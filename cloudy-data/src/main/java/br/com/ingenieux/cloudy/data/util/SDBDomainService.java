package br.com.ingenieux.cloudy.data.util;

import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DeleteDomainRequest;

public class SDBDomainService {
    private final AmazonSimpleDB sdbClient;

    @Inject
    public SDBDomainService(final AmazonSimpleDB amazonSimpleDb) {
        this.sdbClient = amazonSimpleDb;
    }
    
    public void createDomain(String domain) {
        sdbClient.createDomain(new CreateDomainRequest(domain));
    }
    
    public void deleteDomain(String domain) {
        sdbClient.deleteDomain(new DeleteDomainRequest(domain));
    }
    
    public Set<String> listDomains() {
        return new TreeSet<String>(sdbClient.listDomains().getDomainNames());
    }
}
