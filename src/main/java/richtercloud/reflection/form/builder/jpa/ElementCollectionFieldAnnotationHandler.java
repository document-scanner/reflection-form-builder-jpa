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
import java.lang.reflect.Type;
import java.util.List;
import javax.swing.JComponent;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.fieldhandler.FieldAnnotationHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import richtercloud.reflection.form.builder.jpa.typehandler.ElementCollectionTypeHandler;

/**
 * Due to static precedences order of mapping in {@link ReflectionFormBuilder}
 * it is necessary to explicitly reuse specifications in the class mapping (in
 * the case of default implementations).
 * {@code ElementCollectionFieldAnnotationHandler} does that by taking all
 * matches of the class mapping for the field type. Mechanisms for more
 * selection possibilites might be added later if necessary.
 *
 * @author richter
 */
public class ElementCollectionFieldAnnotationHandler implements FieldAnnotationHandler<List<Object>, FieldUpdateEvent<List<Object>>, JPAReflectionFormBuilder> {
    private final ElementCollectionTypeHandler elementCollectionTypeHandler;

    public ElementCollectionFieldAnnotationHandler(ElementCollectionTypeHandler elementCollectionTypeHandler) {
        this.elementCollectionTypeHandler = elementCollectionTypeHandler;
    }

    /**
     * Assumes that the {@code field}  type is {@link List}.
     *
     * Checks whether {typeHandlerMapping} contains a key matching the field type,
     * and if it does, handles the field with it, otherwise applies this
     * proceedure recursively over the list of nested generics. @TODO: currently only creates a component for the first generic type or Object if none is specified.
     *
     * @param updateListener
     * @param reflectionFormBuilder
     * @return
     */
    @Override
    public JComponent handle(Field field,
            Object instance,
            final FieldUpdateListener<FieldUpdateEvent<List<Object>>> updateListener,
            JPAReflectionFormBuilder reflectionFormBuilder) throws FieldHandlingException, IllegalAccessException {
        Type fieldType = field.getGenericType();
        List<Object> fieldValue = (List<Object>) field.get(instance);
        return this.elementCollectionTypeHandler.handle(fieldType,
                fieldValue,
                field.getName(),
                field.getDeclaringClass(),
                updateListener,
                reflectionFormBuilder);
    }

}
