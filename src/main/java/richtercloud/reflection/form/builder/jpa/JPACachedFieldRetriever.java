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
import java.util.List;
import java.util.ListIterator;
import javax.persistence.Id;
import javax.persistence.Transient;
import richtercloud.reflection.form.builder.CachedFieldRetriever;

/**
 *
 * @author richter
 */
public class JPACachedFieldRetriever extends CachedFieldRetriever {

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
        relevantFieldsIt = relevantFields.listIterator();
        while(relevantFieldsIt.hasNext()) {
            Field relevantFieldsNxt = relevantFieldsIt.next();
            if(relevantFieldsNxt.getAnnotation(Id.class) != null) {
                relevantFieldsIt.remove();
                relevantFields.add(0, relevantFieldsNxt);
                break;
            }
        }

        return relevantFields;
    }
}
