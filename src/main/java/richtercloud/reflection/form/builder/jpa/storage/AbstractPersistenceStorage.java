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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Metamodel;
import javax.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.FieldInfo;
import richtercloud.reflection.form.builder.storage.AbstractStorage;
import richtercloud.reflection.form.builder.storage.StorageCallback;
import richtercloud.reflection.form.builder.storage.StorageConfValidationException;
import richtercloud.reflection.form.builder.storage.StorageCreationException;
import richtercloud.reflection.form.builder.storage.StorageException;
import richtercloud.validation.tools.FieldRetriever;
import richtercloud.validation.tools.ValidationTools;

/**
 * {@link Storage} which uses any kind of JPA with any underlying database.
 * @author richter
 */
/*
internal implementation notes:
- JPA implementations should be able to create a lot of EntityManagers and let
the user run queries on each of them. That works, but consumes insane amount of
memory in conjuction with Apache Derby and PostgreSQL 9.5, but MySQL works ->
Since this is a severe issue, the locking mechanism for queries is removed from
AbstractPersistenceStorage and can be re-implemented using dbf6110 as a template
(using PrioritizableReentrantLock in order to allow EDT queries for
auto-completion and else to not freeze the GUI is the main idea)
- caching works fine with MySQL (might not with other databases as soon as
memory issues are fixed or worked around)
- memory leaks in PostgreSQL, MySQL and Apache Derby aren't fixed by excluding
byte[].class in initialize
- flushing, clearing and closing EntityManager doesn't help against memory leak
- memory leaks in PostgreSQL with Hibernate (and probably all other combinations
of DB and JPA provider) even occur when running only one query at the same time
when having one 7-page document (with ca. 100 MB binary data) in the database
- need to limit parallel querying in order to avoid memory leak no matter
whether large binary data is fetched lazily or not
*/
public abstract class AbstractPersistenceStorage<C extends AbstractPersistenceStorageConf> extends AbstractStorage<Object, AbstractPersistenceStorageConf> implements PersistenceStorage<Long> {
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractPersistenceStorage.class);
    private EntityManagerFactory entityManagerFactory;
    private final C storageConf;
    private final String persistenceUnitName;
    /**
     * Determines how many queries can run in parallel.
     */
    /*
    internal implementation notes:
    - providing a default value doesn't make sense because callers need to
    determine which value makes sense based on expected memory consumption
    */
    private final Semaphore querySemaphore;
    private final FieldRetriever fieldRetriever;

    public AbstractPersistenceStorage(C storageConf,
            String persistenceUnitName,
            int parallelQueryCount,
            FieldRetriever fieldRetriever) throws StorageConfValidationException {
        this.storageConf = storageConf;
        this.fieldRetriever = fieldRetriever;
        this.persistenceUnitName = persistenceUnitName;
        if(parallelQueryCount <= 0) {
            throw new IllegalArgumentException("parallelQueryCount has to be > 0");
        }
        this.querySemaphore = new Semaphore(parallelQueryCount);
        storageConf.validate();
    }

    @Override
    public void start() throws StorageCreationException {
        super.start();
        //storageConf already validated in constructor (and immutable)
        init();
        recreateEntityManager();
    }

    protected abstract void init() throws StorageCreationException;

    @Override
    public void delete(Object object) throws StorageException {
        EntityManager entityManager = this.retrieveEntityManager();
        try {
            Object toRemove = entityManager.merge(object);
                //avoids `Exception in thread "AWT-EventQueue-0" java.lang.IllegalArgumentException: Entity must be managed to call remove: Test 1, try merging the detached and try the remove again`
                //(was not an issue when using the same EntityManager for all
                //actions)
                //EntityManager.refresh fails due to `java.lang.IllegalArgumentException: Cannot refresh unmanaged object`
                //need to pass the return value to EntityManager.remove because
                //the argument isn't attached to the persistence context
                //<ref>http://stackoverflow.com/questions/9338999/entity-must-be-managed-to-call-remove</ref>
            entityManager.getTransaction().begin();
            entityManager.remove(toRemove);
            entityManager.getTransaction().commit();

        }catch(EntityExistsException ex) {
            entityManager.getTransaction().rollback();
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
        EntityManager entityManager = this.retrieveEntityManager();
        try {
            List<StorageCallback> preStoreCallbacks = getPreStoreCallbacks(object);
            if(preStoreCallbacks != null) {
                for(StorageCallback preStoreCallback : preStoreCallbacks) {
                    preStoreCallback.callback(object);
                }
            }
            //- Skipping of values in collections of mapped fields in many-to-many
            //relationships have nothing to do with EntityManager.merge because
            //they're persisted only on one side -> use StroageCallbacks to
            //acchieve their storage
            //- unclear why entityManger.merge(object) and working with return
            //value was here before (helps in delete, but seems to be
            //unnecessary here and causes id to be not set on object, i.e.
            //omitting EntityManager.merge avoids to return the instance with
            //its id set from store)
            entityManager.getTransaction().begin();
            entityManager.persist(object);
            entityManager.getTransaction().commit();
            List<StorageCallback> postStoreCallbacks = getPostStoreCallbacks(object);
            if(postStoreCallbacks != null) {
                for(StorageCallback postStoreCallback : postStoreCallbacks) {
                    postStoreCallback.callback(object);
                }
            }
            entityManager.detach(object); //detaching necessary in
                //order to be able to change one single value and save again
        }catch(EntityExistsException ex) {
            entityManager.getTransaction().rollback();
            throw new StorageException(ex);
        }catch(RollbackException ex) {
             //cannot call entityManager.getTransaction().rollback() here because transaction isn' active
            throw new StorageException(ex);
        }
    }

    @Override
    public void refresh(Object object) throws StorageException {
        EntityManager entityManager = this.retrieveEntityManager();
        entityManager.getTransaction().begin();
        entityManager.refresh(object);
        entityManager.getTransaction().commit();
    }

    /**
     * a wrapper around {@link EntityManager#persist(java.lang.Object) }
     * @param object
     */
    @Override
    public void update(Object object) throws StorageException {
        EntityManager entityManager = this.retrieveEntityManager();
        try {
            entityManager.getTransaction().begin();
            entityManager.merge(object);
            entityManager.flush();
            entityManager.getTransaction().commit();
            entityManager.detach(object); //detaching necessary in
                //order to be able to change one single value and save again
        }catch(ConstraintViolationException ex) {
            //needs to be caught here because ConstraintViolationException is
            //so smart to not contain the violation text in its message
            String message = ValidationTools.buildConstraintVioloationMessage(ex.getConstraintViolations(),
                    object,
                    fieldRetriever,
                violationField -> {
                    FieldInfo violationFieldInfo = violationField.getAnnotation(FieldInfo.class);
                    if(violationFieldInfo != null) {
                        return violationFieldInfo.name();
                    }
                    return null;
                }
            ); //@TODO: fix checkstyle indentation failure, see https://github.com/checkstyle/checkstyle/issues/3342
                //for issue report
            throw new StorageException(message,
                    ex);
        }catch(EntityExistsException ex) {
            entityManager.getTransaction().rollback();
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
    public <T> T retrieve(Object id, Class<T> clazz) {
        EntityManager entityManager = this.retrieveEntityManager();
        T retValue = entityManager.find(clazz, id);
        return retValue;
    }

    @Override
    public boolean isClassSupported(Class<?> clazz) {
        EntityManager entityManager = this.retrieveEntityManager();
        Metamodel meta = entityManager.getMetamodel();
        try {
            meta.entity(clazz);
            return true;
        }catch(IllegalArgumentException ex) {
            return false;
        }
    }

    private <E> TypedQuery<E> createQuery(String queryText, Class<E> entityClass) throws StorageException {
        EntityManager entityManager = this.retrieveEntityManager();
        try {
            TypedQuery<E> query = entityManager.createQuery(queryText, entityClass);
            return query;
        }catch(Exception ex) {
            throw new StorageException(ex);
        }
    }

    @Override
    public <T> List<T> runQuery(String queryString,
            Class<T> clazz,
            int queryLimit) throws StorageException {
        try {
            LOGGER.trace(String.format("waiting for semaphore (with approx. %d "
                    + "other threads)",
                    querySemaphore.getQueueLength()));
            querySemaphore.acquire();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        try {
            LOGGER.trace(String.format("semaphore aquired (%d remaining permits)", querySemaphore.availablePermits()));
            LOGGER.debug(String.format("running query '%s'", queryString));
            List<T> retValue;
            TypedQuery<T> query = createQuery(queryString,
                    clazz);
            retValue = query.setMaxResults(queryLimit).getResultList();
            return retValue;
        }finally {
            querySemaphore.release();
            LOGGER.trace(String.format("semaphore released (%d remaining permits)", querySemaphore.availablePermits()));
        }
    }

    @Override
    public <T> List<T> runQuery(String attribueName,
            String attributeValue,
            Class<T> clazz) {
        try {
            LOGGER.trace(String.format("waiting for semaphore (with approx. %d "
                    + "other threads)",
                    querySemaphore.getQueueLength()));
            querySemaphore.acquire();
        }catch(InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        try {
            LOGGER.trace(String.format("semaphore aquired (%d remaining permits, approx. %d threads waiting)",
                    querySemaphore.availablePermits(),
                    querySemaphore.getQueueLength()));
            EntityManager entityManager = this.retrieveEntityManager();
            CriteriaQuery<T> criteria = entityManager.getCriteriaBuilder().createQuery(clazz);
            Root<T> personRoot = criteria.from(clazz);
            criteria.select( personRoot );
            criteria.where( entityManager.getCriteriaBuilder().equal( personRoot.get(attribueName),
                    attributeValue));
                //attributeName Company.name was used before, unclear why (causes
                //` java.lang.IllegalArgumentException: The attribute [Company.name] is not present in the managed type [EntityTypeImpl@553585467:Company [ javaType: class richtercloud.document.scanner.model.Company descriptor: RelationalDescriptor(richtercloud.document.scanner.model.Company --> [DatabaseTable(COMPANY)]), mappings: 8]].`)
            List<T> results = entityManager.createQuery( criteria ).getResultList();
            return results;
        }finally {
            querySemaphore.release();
            LOGGER.trace(String.format("semaphore released (%d remaining permits)", querySemaphore.availablePermits()));
        }
    }

    @Override
    public <T> List<T> runQueryAll(Class<T> clazz) {
        try {
            LOGGER.trace(String.format("waiting for semaphore (with approx. %d "
                    + "other threads)",
                    querySemaphore.getQueueLength()));
            querySemaphore.acquire();
        }catch(InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        try {
            LOGGER.trace(String.format("semaphore aquired (%d remaining permits)", querySemaphore.availablePermits()));
            EntityManager entityManager = this.retrieveEntityManager();
            CriteriaQuery<T> criteriaQuery = entityManager.getCriteriaBuilder().createQuery(clazz);
            Root<T> queryRoot = criteriaQuery.from(clazz);
            criteriaQuery.select(queryRoot);
            List<T> retValue = entityManager.createQuery(criteriaQuery).getResultList();
            return retValue;
        }finally{
            querySemaphore.release();
            LOGGER.trace(String.format("semaphore released (%d remaining permits)", querySemaphore.availablePermits()));
        }
    }

    @Override
    public boolean isManaged(Object object) {
        boolean retValue = this.retrieveEntityManager().contains(object);
        return retValue;
    }

    /**
     * Get the {@link EntityManager} used for persistent storage.
     * @return
     */
    @Override
    public EntityManager retrieveEntityManager() {
        if(!isStarted()) {
            throw new IllegalStateException("persistence storage hasn't been "
                    + "started");
        }
        return this.entityManagerFactory.createEntityManager();
    }

    @Override
    public void shutdown() {
        if(this.entityManagerFactory != null && this.entityManagerFactory.isOpen()) {
            //might be null if an exception occured in Derby
            this.entityManagerFactory.close();
        }
    }

    protected Map<String, String> getEntityManagerProperties() {
        Map<String, String> properties = new HashMap<>(4);
        properties.put("javax.persistence.jdbc.url", storageConf.getConnectionURL());
        properties.put("javax.persistence.jdbc.user", storageConf.getUsername());
        properties.put("javax.persistence.jdbc.password", storageConf.getPassword());
        properties.put("javax.persistence.jdbc.driver", storageConf.getDatabaseDriver());
        //see comments in persistence.xml as well
        return properties;
    }

    public void recreateEntityManager() throws StorageCreationException {
        Map<String, String> properties = getEntityManagerProperties();
        if(this.entityManagerFactory != null && this.entityManagerFactory.isOpen()) {
            this.entityManagerFactory.close();
        }
        //seems like properties need to be specified on EntityManagerFactory
        //difference between setting on EMF and EntityManager unclear
        this.entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName,
                properties //additional properties
        );
    }

    @Override
    public C getStorageConf() {
        return storageConf;
    }
}
