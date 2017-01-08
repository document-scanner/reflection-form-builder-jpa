/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package richtercloud.reflection.form.builder.jpa.panels;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import richtercloud.message.handler.MessageHandler;

/**
 *
 * @author richter
 * @param <S> the type of storage to create
 */
public abstract class AbstractQueryHistoryEntryStorageFactory<S extends QueryHistoryEntryStorage> implements QueryHistoryEntryStorageFactory<S>{
    private final Set<Class<?>> entityClasses;
    private final boolean forbidSubtypes;
    private final MessageHandler messageHandler;

    public AbstractQueryHistoryEntryStorageFactory(Set<Class<?>> entityClasses,
            boolean forbidSubtypes,
            MessageHandler messageHandler) {
        this.entityClasses = entityClasses;
        this.forbidSubtypes = forbidSubtypes;
        this.messageHandler = messageHandler;
    }

    public boolean isForbidSubtypes() {
        return forbidSubtypes;
    }

    public Set<Class<?>> getEntityClasses() {
        return entityClasses;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    /**
     * This doesn't work with Hibernate 5.1.0 as JPA provider due to bug
     * https://hibernate.atlassian.net/browse/HHH-10653!
     *
     * Since it's possible to use {@link Class#getSimpleName() } to identify
     * classes it's not necessary to use parameters which provides queries which
     * are much more readable if plain text and simple names are used. Note that
     * JPA 2.1 query API and CriteriaBuilder API are seriously incapable of
     * retrieving the text of the query (both Query and TypedQuery) after it has
     * been created with parameters so that it'd be necessary to store
     * parameters like {@code entityClass} in {@link HistoryEntry}s which is
     * quite unelegant or keep the parameter escape string (e.g.
     * {@code :entityClass} in the query).
     *
     * @param entityClass
     * @param forbidSubtypes
     * @return
     */
    protected List<String> generateInitialQueryTexts(Class<?> entityClass,
            boolean forbidSubtypes) {
        //Criteria API doesn't allow retrieval of string/text from objects
        //created with CriteriaBuilder, but text should be the first entry in
        //the query combobox -> construct String instead of using
        //CriteriaBuilder
        String entityClassQueryIdentifier = QueryHistoryEntryStorageFactory.generateEntityClassQueryIdentifier(entityClass);
        String queryTextAllowSubclasses = String.format("SELECT %s FROM %s %s",
                entityClassQueryIdentifier,
                entityClass.getSimpleName(),
                entityClassQueryIdentifier);
        String queryTextForbidSubclasses = String.format("%s WHERE TYPE(%s) = %s",
                queryTextAllowSubclasses,
                entityClassQueryIdentifier,
                entityClass.getSimpleName());
        List<String> retValue = new LinkedList<>();
        if(forbidSubtypes) {
            retValue.add(queryTextForbidSubclasses);
            retValue.add(queryTextAllowSubclasses);
        }else {
            retValue.add(queryTextAllowSubclasses);
            retValue.add(queryTextForbidSubclasses);
        }
        return retValue;
    }

    @Override
    public final S create() throws QueryHistoryEntryStorageCreationException {
        S entryStorage = create0();
        for(Class<?> entityClass : getEntityClasses()) {
            List<String> queryTexts = generateInitialQueryTexts(entityClass,
                    isForbidSubtypes());
            for(String queryText : queryTexts) {
                try {
                    entryStorage.store(entityClass,
                            new QueryHistoryEntry(queryText, //queryText
                                    1, //usageCount
                                    new Date() //lastUsage
                            ));
                } catch (QueryHistoryEntryStorageException ex) {
                    throw new QueryHistoryEntryStorageCreationException(ex);
                }
            }
        }
        return entryStorage;
    }

    protected abstract S create0() throws QueryHistoryEntryStorageCreationException;
}
