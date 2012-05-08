package br.com.ingenieux.cloudy.data;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import br.com.ingenieux.cloudy.data.test.Customer;
import br.com.ingenieux.cloudy.data.test.TestDao;
import br.com.ingenieux.cloudy.data.util.SDBTestRunner;

import static org.junit.Assert.*;

@RunWith(SDBTestRunner.class)
public class SimpleDBDaoTest {
    @Inject
    TestDao testDao;
    
    @Test
    public void testSave() throws Exception {
        Customer c = new Customer();
        
        c.setItemName("initrode");
        c.setName("Initrode, LLC");
        c.setDomain("initrode.com");
        c.setEmail("admin@initrode.com");
        
        testDao.persist(c);
    }
    
    @Test
    public void testUpdate() throws Exception {
        Customer c = testDao.findByItemName("initrode");
        
        assertNotNull(c);
        
        c.setName("IniTech, LLC");
        
        testDao.persist(c);
        
        assertEquals(Integer.valueOf(2), c.getRevision());
    }
}
