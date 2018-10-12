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
package de.richtercloud.reflection.form.builder.jpa;

import de.richtercloud.message.handler.IssueHandler;
import de.richtercloud.reflection.form.builder.ComponentHandler;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import de.richtercloud.reflection.form.builder.fieldhandler.ResettableFieldHandler;
import de.richtercloud.reflection.form.builder.jpa.panels.StringAutoCompletePanel;
import de.richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import de.richtercloud.reflection.form.builder.jpa.typehandler.JPAStringTypeHandler;
import de.richtercloud.validation.tools.FieldRetriever;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import javax.swing.JComponent;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author richter
 */
public class JPAStringFieldHandler extends ResettableFieldHandler<String, FieldUpdateEvent<String>, JPAReflectionFormBuilder, StringAutoCompletePanel> {
    private final JPAStringTypeHandler jPAStringTypeHandler;

    public JPAStringFieldHandler(PersistenceStorage storage,
            int initialQueryLimit,
            IssueHandler issueHandler,
            FieldRetriever readOnlyFieldRetriever) {
        super();
        this.jPAStringTypeHandler = new JPAStringTypeHandler(storage,
                initialQueryLimit,
                issueHandler,
                readOnlyFieldRetriever);
    }

    public JPAStringFieldHandler(JPAStringTypeHandler jPAStringTypeHandler) {
        super();
        this.jPAStringTypeHandler = jPAStringTypeHandler;
    }

    @Override
    public Pair<JComponent, ComponentHandler<?>> handle0(Field field,
            Object instance,
            final FieldUpdateListener<FieldUpdateEvent<String>> updateListener,
            JPAReflectionFormBuilder reflectionFormBuilder) throws FieldHandlingException {
        String fieldValue;
        try {
            fieldValue = (String) field.get(instance);
        } catch (IllegalArgumentException
                | IllegalAccessException ex) {
            throw new FieldHandlingException(ex);
        }
        Type fieldType = field.getType();
        return this.jPAStringTypeHandler.handle(fieldType,
                fieldValue,
                field.getName(),
                field.getDeclaringClass(),
                updateListener,
                reflectionFormBuilder);
    }

    @Override
    public void reset(StringAutoCompletePanel component) {
        this.jPAStringTypeHandler.reset(component);
    }
}
