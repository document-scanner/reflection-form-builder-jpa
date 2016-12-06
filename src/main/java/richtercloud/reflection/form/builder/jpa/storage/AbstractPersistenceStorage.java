/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package richtercloud.reflection.form.builder.jpa.storage;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Metamodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.storage.StorageException;

/**
 * {@link Storage} which uses any kind of JPA with any underlying database.
 * @author richter
 */
/*
internal implementation notes:
- store username and password in abstract subclass DatabasePersistenceStorage
because there might be database connections which don't need it
*/
public abstract class AbstractPersistenceStorage<C extends AbstractPersistenceStorageConf> implements PersistenceStorage {
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractPersistenceStorage.class);
    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private final AbstractPersistenceStorageConf storageConf;

    public AbstractPersistenceStorage(C storageConf) {
        this.storageConf = storageConf;
        recreateEntityManager(); //after this.storageConf has been assigned
    }

    @Override
    public void delete(Object object) throws StorageException {
        try {
            this.retrieveEntityManager().getTransaction().begin();
            this.retrieveEntityManager().remove(object);
            this.retrieveEntityManager().getTransaction().commit();

        }catch(EntityExistsException ex) {
            this.retrieveEntityManager().getTransaction().rollback();
            throw new StorageException(ex);
        }catch(RollbackException ex) {
             //cannot call entityManager.getTransaction().rollback() here because transaction isn' active
            throw new StorageException(ex);
        }
    }

    /**
     * a wrapper around {@link EntityManager#persist(java.lang.Object) }
     * @param object
     */
    @Override
    public void store(Object object) throws StorageException {
        try {
            this.retrieveEntityManager().getTransaction().begin();
            this.retrieveEntityManager().persist(object);
            this.retrieveEntityManager().getTransaction().commit();
            this.retrieveEntityManager().detach(object); //detaching necessary in
                //order to be able to change one single value and save again
        }catch(EntityExistsException ex) {
            this.retrieveEntityManager().getTransaction().rollback();
            throw new StorageException(ex);
        }catch(RollbackException ex) {
             //cannot call entityManager.getTransaction().rollback() here because transaction isn' active
            throw new StorageException(ex);
        }
    }

    /**
     * a wrapper around {@link EntityManager#persist(java.lang.Object) }
     * @param object
     */
    @Override
    public void update(Object object) throws StorageException {
        try {
            this.retrieveEntityManager().getTransaction().begin();
            this.retrieveEntityManager().merge(object);
            this.retrieveEntityManager().getTransaction().commit();
            this.retrieveEntityManager().detach(object); //detaching necessary in
                //order to be able to change one single value and save again
        }catch(EntityExistsException ex) {
            this.retrieveEntityManager().getTransaction().rollback();
            throw new StorageException(ex);
        }catch(RollbackException ex) {
             //cannot call entityManager.getTransaction().rollback() here because transaction isn' active
            throw new StorageException(ex);
        }
    }

    /**
     * a wrapper around {@link EntityManager#find(java.lang.Class, java.lang.Object) }
     * @param id
     * @param clazz
     * @return
     */
    @Override
    public Object retrieve(Object id, Class clazz) {
        Object retValue = this.retrieveEntityManager().find(clazz, id);
        return retValue;
    }

    @Override
    public boolean isClassSupported(Class<?> clazz) {
        Metamodel meta = this.retrieveEntityManager().getMetamodel();
        try {
            meta.entity(clazz);
            return true;
        }catch(IllegalArgumentException ex) {
            return false;
        }
    }

    private <E> TypedQuery<E> createQuery(String queryText, Class<E> entityClass) throws StorageException {
        try {
            TypedQuery<E> query = this.retrieveEntityManager().createQuery(queryText, entityClass);
            return query;
        }catch(Exception ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public <T> List<T> runQuery(String queryString, Class<T> clazz,
            int queryLimit) throws StorageException {
        TypedQuery<T> query = createQuery(queryString,
                clazz);
        List<T> retValue = query.setMaxResults(queryLimit).getResultList();
        return retValue;
    }

    @Override
    public <T> List<T> runQuery(String attribueName,
            String attributeValue,
            Class<T> clazz) {
        CriteriaQuery<T> criteria = entityManager.getCriteriaBuilder().createQuery(clazz);
        Root<T> personRoot = criteria.from(clazz);
        criteria.select( personRoot );
        criteria.where( entityManager.getCriteriaBuilder().equal( personRoot.get(attribueName),
                attributeValue));
            //attributeName Company.name was used before, unclear why (causes
            //` java.lang.IllegalArgumentException: The attribute [Company.name] is not present in the managed type [EntityTypeImpl@553585467:Company [ javaType: class richtercloud.document.scanner.model.Company descriptor: RelationalDescriptor(richtercloud.document.scanner.model.Company --> [DatabaseTable(COMPANY)]), mappings: 8]].`)
        List<T> results = entityManager.createQuery( criteria ).getResultList();
        return results;
    }

    @Override
    public <T> List<T> runQueryAll(Class<T> clazz) {
        CriteriaQuery<T> criteriaQuery = this.entityManager.getCriteriaBuilder().createQuery(clazz);
        Root<T> queryRoot = criteriaQuery.from(clazz);
        criteriaQuery.select(queryRoot);
        List<T> retValue = this.entityManager.createQuery(criteriaQuery).getResultList();
        return retValue;
    }

    @Override
    public boolean isManaged(Object object) {
        boolean retValue = this.retrieveEntityManager().contains(object);
        return retValue;
    }

    @Override
    public void initialize(Object entity,
            FieldRetriever fieldRetriever) throws IllegalArgumentException, IllegalAccessException {
        for(Field field : fieldRetriever.retrieveRelevantFields(entity.getClass())) {
            field.get(entity);
            if(Collection.class.isAssignableFrom(field.getType())) {
                ((Collection)field.get(entity)).size();
                    //need to explicitly call Collection.size on the field value
                    //in order to get it initialized
            }
        }
    }

    /**
     * Get the {@link EntityManager} used for persistent storage.
     * @return
     */
    protected EntityManager retrieveEntityManager() {
        if(this.entityManager == null) {
            recreateEntityManager();
        }
        return this.entityManager;
    }

    @Override
    public void shutdown() {
        if(this.entityManager != null && this.entityManager.isOpen()) {
            //might be null if an exception occured in Derby
            this.entityManager.close();
        }
        if(this.entityManagerFactory != null && this.entityManagerFactory.isOpen()) {
            //might be null if an exception occured in Derby
            this.entityManagerFactory.close();
        }
    }

    public void recreateEntityManager() {
        Map<String, String> properties = new HashMap<>(4);
        properties.put("javax.persistence.jdbc.url", storageConf.getConnectionURL());
        properties.put("javax.persistence.jdbc.user", storageConf.getUsername());
        properties.put("javax.persistence.jdbc.password", storageConf.getPassword());
        properties.put("javax.persistence.jdbc.driver", storageConf.getDatabaseDriver());
        if(this.entityManagerFactory != null && this.entityManagerFactory.isOpen()) {
            this.entityManagerFactory.close();
        }
        //seems like properties need to be specified on EntityManagerFactory
        //difference between setting on EMF and EntityManager unclear
        this.entityManagerFactory = Persistence.createEntityManagerFactory("richtercloud_document-scanner_jar_1.0-SNAPSHOTPU",
                properties
        );
        this.entityManager = entityManagerFactory.createEntityManager();
    }

    @Override
    public AbstractPersistenceStorageConf getStorageConf() {
        return storageConf;
    }
}
