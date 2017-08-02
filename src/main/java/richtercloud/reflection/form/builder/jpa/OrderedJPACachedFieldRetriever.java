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
import java.util.Map;

/**
 * A {@link FieldRetriever} which allows callers to pass a reference to a
 * mutable map where they can specify order to field retrieval. The map can be
 * empty or miss keys for certain classes because they will be filled by values
 * from the superclass implementation.
 *
 * @author richter
 */
public class OrderedJPACachedFieldRetriever extends JPACachedFieldRetriever {
    private final Map<Class<?>, List<Field>> fieldOrderMap;

    public OrderedJPACachedFieldRetriever(Map<Class<?>, List<Field>> fieldOrderMap) {
        this.fieldOrderMap = fieldOrderMap;
    }

    @Override
    public List<Field> retrieveRelevantFields(Class<?> entityClass) {
        List<Field> retValue = fieldOrderMap.get(entityClass);
        if(retValue == null) {
            retValue = super.retrieveRelevantFields(entityClass);
            fieldOrderMap.put(entityClass,
                    retValue);
        }
        return retValue;
    }
}
