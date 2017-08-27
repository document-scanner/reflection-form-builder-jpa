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
import richtercloud.validation.tools.FieldRetriever;

/**
 * Initializes lazy fields through reflection. This doesn't work in Hibernate
 * with compile-time bytecode enhanced classes.
 * @author richter
 */
public class ReflectionFieldInitializer implements FieldInitializer {
    private final FieldRetriever fieldRetriever;

    public ReflectionFieldInitializer(FieldRetriever fieldRetriever) {
        this.fieldRetriever = fieldRetriever;
    }

    public FieldRetriever getFieldRetriever() {
        return fieldRetriever;
    }

    @Override
    public void initialize(Object entity) throws IllegalArgumentException,
            IllegalAccessException {
        if(entity == null) {
            throw new IllegalArgumentException("entity mustn't be null");
        }
        for(Field field : fieldRetriever.retrieveRelevantFields(entity.getClass())) {
            if(!initializeField(field)) {
                continue;
            }
            field.setAccessible(true);
            field.get(entity);
            if(Collection.class.isAssignableFrom(field.getType())) {
                Collection fieldValue = ((Collection)field.get(entity));
                if(fieldValue != null) {
                    fieldValue.size();
                        //need to explicitly call Collection.size on the field value
                        //in order to get it initialized
                }
            }
        }
    }

    protected boolean initializeField(Field field) {
        return true;
    }
}
