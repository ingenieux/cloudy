package br.com.ingenieux.cloudy.data;

import java.util.Collection;


public interface Dao<T extends SDBEntity> {
	public Collection<T> findAll();
	
    public void persist(T entity);
    
    public T merge(T entity);
    
    public T findByItemName(String name);
    
    public void refresh(T entity);
    
    public void remove(T entity);
    
    public Long getCount();
    
    public Collection<T> findAllSinceItemName(String itemName);
    
    public Collection<T> findAllByItemNames(String... names);
    
    public T findByItemNameAndRevision(String itemName, Integer revision);
}
