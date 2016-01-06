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

import richtercloud.reflection.form.builder.jpa.typehandler.JPAStringTypeHandler;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import javax.persistence.EntityManager;
import javax.swing.JComponent;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import richtercloud.reflection.form.builder.message.MessageHandler;

/**
 *
 * @author richter
 */
public class JPAStringFieldHandler implements FieldHandler<String, FieldUpdateEvent<String>, JPAReflectionFormBuilder> {
    private final JPAStringTypeHandler jPAStringTypeHandler;

    public JPAStringFieldHandler(EntityManager entityManager,
            int initialQueryLimit,
            MessageHandler messageHandler) {
        this.jPAStringTypeHandler = new JPAStringTypeHandler(entityManager,
                initialQueryLimit,
                messageHandler);
    }

    public JPAStringFieldHandler(JPAStringTypeHandler jPAStringTypeHandler) {
        this.jPAStringTypeHandler = jPAStringTypeHandler;
    }

    @Override
    public JComponent handle(Field field,
            Object instance,
            final FieldUpdateListener<FieldUpdateEvent<String>> updateListener,
            JPAReflectionFormBuilder reflectionFormBuilder) throws IllegalArgumentException, IllegalAccessException, FieldHandlingException {
        Type fieldType = field.getType();
        String fieldValue = (String) field.get(instance);
        return this.jPAStringTypeHandler.handle(fieldType,
                fieldValue,
                field.getName(),
                field.getDeclaringClass(),
                updateListener,
                reflectionFormBuilder);
    }

}
