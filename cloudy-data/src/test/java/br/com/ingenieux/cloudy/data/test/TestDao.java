package br.com.ingenieux.cloudy.data.test;

import br.com.ingenieux.cloudy.data.Dao;
import br.com.ingenieux.cloudy.data.EntityClass;

@EntityClass(Customer.class)
public interface TestDao extends Dao<Customer> {
    public Customer findByName(String name);
}
