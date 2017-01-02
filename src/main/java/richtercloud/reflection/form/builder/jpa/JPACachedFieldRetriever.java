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
package richtercloud.reflection.form.builder.jpa;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import javax.persistence.Id;
import javax.persistence.Transient;
import richtercloud.reflection.form.builder.CachedFieldRetriever;

/**
 *
 * @author richter
 */
public class JPACachedFieldRetriever extends CachedFieldRetriever implements JPAFieldRetriever {

    /**
     * Retrieves relevant fields from super class and removes fields with
     * {@link Transient} annotation because they're irrelevant in JPA. Moves
     * fields annotated with {@link Id} to the beginning of the returned list.
     * @param entityClass
     * @return the list of relevant fields
     */
    @Override
    public List<Field> retrieveRelevantFields(Class<?> entityClass) {
        if(entityClass == null) {
            throw new IllegalArgumentException("entityClass mustn't be null");
        }
        List<Field> relevantFields = super.retrieveRelevantFields(entityClass);
        ListIterator<Field> relevantFieldsIt = relevantFields.listIterator();
        while(relevantFieldsIt.hasNext()) {
            Field relevantFieldsNxt = relevantFieldsIt.next();
            if(relevantFieldsNxt.getAnnotation(Transient.class) != null) {
                relevantFieldsIt.remove();
            }
        }
        //move @Id annotated fields to the beginning of the list
        Set<Field> idFields = new HashSet<>();
        relevantFieldsIt = relevantFields.listIterator();
        while(relevantFieldsIt.hasNext()) {
            Field relevantFieldsNxt = relevantFieldsIt.next();
            if(relevantFieldsNxt.getAnnotation(Id.class) != null) {
                relevantFieldsIt.remove();
                idFields.add(relevantFieldsNxt);
            }
        }
        for(Field idField : idFields) {
            relevantFields.add(0, idField);
        }
        return relevantFields;
    }

    /**
     * Get all fields annotated with {@link Id}.
     * @param entityClass
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
