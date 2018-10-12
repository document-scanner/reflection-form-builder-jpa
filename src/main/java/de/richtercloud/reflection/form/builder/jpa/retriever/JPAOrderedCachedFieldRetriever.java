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
package de.richtercloud.reflection.form.builder.jpa.retriever;

import de.richtercloud.reflection.form.builder.jpa.JPAFieldRetriever;
import de.richtercloud.reflection.form.builder.retriever.FieldOrderValidationException;
import de.richtercloud.reflection.form.builder.retriever.OrderedCachedFieldRetriever;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import javax.persistence.Id;
import javax.persistence.Transient;

/**
 *
 * @author richter
 */
public class JPAOrderedCachedFieldRetriever extends OrderedCachedFieldRetriever implements JPAFieldRetriever {

    public JPAOrderedCachedFieldRetriever(Set<Class<?>> entityClasses) throws FieldOrderValidationException {
        super(entityClasses);
    }

    public JPAOrderedCachedFieldRetriever(Map<Class<?>,
            List<Field>> fieldOrderMap,
            Set<Class<?>> entityClass) throws FieldOrderValidationException {
        super(fieldOrderMap,
                entityClass);
    }

    public JPAOrderedCachedFieldRetriever(Map<Class<?>, List<Field>> fieldOrderMap,
            Set<Class<?>> entityClasses,
            boolean visualizeDependencyGraphOnError) throws FieldOrderValidationException {
        super(fieldOrderMap,
                entityClasses,
                visualizeDependencyGraphOnError);
    }

    /**
     * Retrieves relevant fields from super class and removes fields with
     * {@link Transient} annotation because they're irrelevant in JPA.
     * @param entityClass the entity classes to retrieve from
     * @return the list of relevant fields
     */
    @Override
    public List<Field> retrieveRelevantFields(Class<?> entityClass) {
        if(entityClass == null) {
            throw new IllegalArgumentException("entityClass mustn't be null");
        }
        List<Field> relevantFields = super.retrieveRelevantFields(entityClass);
            //relevantFields/the return value should only be modified if the
            //result is stored with overwriteCachedResult and the next call to
            //retrieveRelevantFields avoids modifications which have already
            //been made since they will most likely cause a
            //ConcurrentModificationException; alternatively the value can be
            //copied here and be modified at will (shouldn't be done unless
            //necessary because of the minimal performance impact of the copy)
        ListIterator<Field> relevantFieldsIt = relevantFields.listIterator();
        while(relevantFieldsIt.hasNext()) {
            Field relevantFieldsNxt = relevantFieldsIt.next();
            if(relevantFieldsNxt.getAnnotation(Transient.class) != null) {
                relevantFieldsIt.remove();
            }
        }
        overwriteCachedResult(entityClass,
                relevantFields);
        return relevantFields;
    }

    /**
     * Get all fields annotated with {@link Id}.
     * @param entityClass the entity classes to retrieve from
     * @return the ID fields
     */
    @Override
    public Set<Field> getIdFields(Class<?> entityClass) {
        Set<Field> retValue = new HashSet<>();
        List<Field> fields = retrieveRelevantFields(entityClass);
        for(Field field : fields) {
            if(field.getAnnotation(Id.class) != null) {
                retValue.add(field);
            }
        }
        return retValue;
    }
}
