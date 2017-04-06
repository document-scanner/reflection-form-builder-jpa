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
import javax.swing.JComponent;
import org.apache.commons.lang3.tuple.Pair;
import richtercloud.message.handler.IssueHandler;
import richtercloud.reflection.form.builder.ComponentHandler;
import richtercloud.validation.tools.FieldRetriever;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import richtercloud.reflection.form.builder.fieldhandler.ResettableFieldHandler;
import richtercloud.reflection.form.builder.jpa.panels.StringAutoCompletePanel;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.reflection.form.builder.jpa.typehandler.JPAStringTypeHandler;

/**
 *
 * @author richter
 */
public class JPAStringFieldHandler extends ResettableFieldHandler<String, FieldUpdateEvent<String>, JPAReflectionFormBuilder, StringAutoCompletePanel> {
    private final JPAStringTypeHandler jPAStringTypeHandler;

    public JPAStringFieldHandler(PersistenceStorage storage,
            int initialQueryLimit,
            IssueHandler issueHandler,
            String bidirectionalHelpDialogTitle,
            FieldRetriever readOnlyFieldRetriever) {
        this.jPAStringTypeHandler = new JPAStringTypeHandler(storage,
                initialQueryLimit,
                issueHandler,
                bidirectionalHelpDialogTitle,
                readOnlyFieldRetriever);
    }

    public JPAStringFieldHandler(JPAStringTypeHandler jPAStringTypeHandler) {
        this.jPAStringTypeHandler = jPAStringTypeHandler;
    }

    @Override
    public Pair<JComponent, ComponentHandler<?>> handle0(Field field,
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

    @Override
    public void reset(StringAutoCompletePanel component) {
        this.jPAStringTypeHandler.reset(component);
    }

}
