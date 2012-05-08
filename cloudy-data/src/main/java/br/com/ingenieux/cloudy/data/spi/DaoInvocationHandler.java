package br.com.ingenieux.cloudy.data.spi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javassist.Modifier;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.sf.cglib.proxy.InvocationHandler;

import br.com.ingenieux.cloudy.data.EntityClass;

import static org.apache.commons.lang.StringUtils.*;

public class DaoInvocationHandler implements InvocationHandler {
    private final EntityManager entityManager;

    private final Class<?> entityType;

    private final Map<String, Method> methodMap = new TreeMap<String, Method>();

    public DaoInvocationHandler(Class<?> daoClazz, EntityManager entityManager) {
        this.entityType = findEntityType(daoClazz);
        this.entityManager = entityManager;

        populateMethodMap();
    }

    private void populateMethodMap() {
        try {
            for (Method m : this.getClass().getDeclaredMethods()) {
                String name = m.getName();

                if ("invoke".equals(name))
                    continue;

                if (!Modifier.isPublic(m.getModifiers()))
                    continue;

                methodMap.put(name, m);
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private Class<?> findEntityType(Class<?> daoClazz) {
        return daoClazz.getAnnotation(EntityClass.class).value();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();

        if ("persist".equals(name)) {
            entityManager.persist(args[0]);

            return null;
        } else if ("remove".equals(name)) {
            entityManager.remove(args[0]);

            return null;
        } else if ("refresh".equals(name)) {
            entityManager.refresh(args[0]);
        } else if ("merge".equals(name)) {
            return entityManager.merge(args[0]);
        } else if ("findByItemName".equals(name)) {
            return entityManager.find(entityType, args[0]);
        } else if (methodMap.containsKey(name)) {
            try {
                return methodMap.get(name).invoke(this, args);
            } catch (InvocationTargetException exc) {
                throw exc.getCause();
            }
        } else if (name.matches("^findAllBy.+")) {
            return findAllBy(method.getName(), args);
        } else if (name.matches("^findBy.+")) {
            return findBy(method.getName(), args);
        } else if (name.matches("^findAll")) {
        	return findAll();
        }

        // TODO Auto-generated method stub
        return null;
    }
    
    private Object findAll() {
        StringBuilder queryText = new StringBuilder();

        queryText.append(String.format("SELECT o FROM %s o", entityType.getSimpleName()));

        Query query = entityManager.createQuery(queryText.toString());

    	return query.getResultList();
    }

    private Object findBy(String name, Object[] args) {
        Query query = getFindQuery(name, args);

        return query.getSingleResult();
    }

    private Query getFindQuery(String name, Object[] args) {
        String predicate = name.substring(2 + name.indexOf("By"));
        String orderBy = "";

        if (predicate.contains("OrderBy")) {
            int index = predicate.indexOf("OrderBy");

            orderBy = predicate.substring(index + "OrderBy".length());

            predicate = predicate.substring(0, index);

            {
                String[] terms = orderBy.split("And");

                List<String> termList = new ArrayList<String>();

                for (String term : terms) {
                    String property = term;

                    if (term.matches("^.*Desc$")) {
                        property = term.substring(0, -4 + term.length());
                        termList.add("o." + uncapitalize(property) + " DESC");
                    } else {
                        termList.add("o." + uncapitalize(property));
                    }

                }

                orderBy = " ORDER BY " + join(termList, ", ");
            }
        }

        String[] terms = predicate.split("And");

        StringBuilder queryText = new StringBuilder();

        queryText.append(String.format("SELECT o FROM %s o WHERE ", entityType.getSimpleName()));

        List<String> termList = new ArrayList<String>();

        for (String term : terms) {
            termList.add(String.format("o.%s = :%s", uncapitalize(term), uncapitalize(term)));
        }

        queryText.append(join(termList, " AND "));

        queryText.append(orderBy);

        Query query = entityManager.createQuery(queryText.toString());

        for (int i = 0; i < terms.length; i++)
            query.setParameter(uncapitalize(terms[i]), args[i]);

        return query;
    }

    @SuppressWarnings("rawtypes")
    private Collection findAllBy(String name, Object[] args) {
        Query query = getFindQuery(name, args);

        return query.getResultList();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Collection findAllByItemNames(String... itemNames) {
        List result = new ArrayList();

        for (String k : itemNames)
            result.add(entityManager.find(entityType, k));

        return result;
    }

    public Long getCount() {
        Query q = entityManager.createQuery(String.format("SELECT count(o) FROM %s o", entityType.getSimpleName()));

        return (Long) q.getSingleResult();
    }

    @SuppressWarnings("rawtypes")
    public Collection findAllSinceItemName(String itemName) {
        String queryText = null;
        boolean hasItemName = isNotBlank(itemName);

        if (!hasItemName) {
            queryText = String.format("SELECT o FROM %s o", entityType.getSimpleName());
        } else {
            queryText = String.format("SELECT o FROM %s o WHERE o.itemName > :itemName", entityType.getSimpleName());
        }

        Query q = entityManager.createQuery(queryText);

        if (hasItemName)
            q.setParameter("itemName", itemName);

        return q.getResultList();
    }
}
