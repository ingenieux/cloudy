package br.com.ingenieux.cloudy.data.util;

import javax.persistence.EntityManager;

import net.sf.cglib.proxy.Proxy;

import br.com.ingenieux.cloudy.data.spi.DaoInvocationHandler;

import com.google.inject.Provider;

public class DaoProvider<K> implements Provider<K> {
    private final Class<K> daoClazz;
    
    private EntityManager entityManager;

    public DaoProvider(final Class<K> daoClazz, EntityManager entityManager) {
        this(daoClazz);
        this.entityManager = entityManager;
    }
    
    public DaoProvider(final Class<K> daoClazz) {
        this.daoClazz = daoClazz;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public K get() {
        return (K) Proxy.newProxyInstance(daoClazz.getClassLoader(), new Class<?>[] { daoClazz }, new DaoInvocationHandler(daoClazz, entityManager));
    }
}
