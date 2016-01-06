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
import java.lang.reflect.InvocationTargetException;
import javax.swing.JComponent;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import richtercloud.reflection.form.builder.fieldhandler.FieldAnnotationHandler;

/**
 *
 * @author richter
 */
public class EmbeddedFieldAnnotationHandler implements FieldAnnotationHandler<Object, FieldUpdateEvent<Object>, JPAReflectionFormBuilder> {
    private FieldHandler embeddableFieldHandler;

    public EmbeddedFieldAnnotationHandler(FieldHandler fieldHandler) {
        this.embeddableFieldHandler = fieldHandler;
    }

    @Override
    public JComponent handle(Field field,
            Object instance,
            FieldUpdateListener<FieldUpdateEvent<Object>> updateListener,
            JPAReflectionFormBuilder reflectionFormBuilder) throws FieldHandlingException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        if(field == null) {
            throw new IllegalArgumentException("fieldClass mustn't be null");
        }
        Object fieldValue = field.get(instance);
        JComponent retValue = reflectionFormBuilder.transformEmbeddable(field.getDeclaringClass(),
                fieldValue,
                embeddableFieldHandler);
        return retValue;
    }
}
