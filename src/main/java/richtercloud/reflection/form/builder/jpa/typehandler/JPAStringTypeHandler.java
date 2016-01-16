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
package richtercloud.reflection.form.builder.jpa.typehandler;

import java.lang.reflect.Type;
import javax.persistence.EntityManager;
import javax.swing.JComponent;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import richtercloud.reflection.form.builder.jpa.JPAReflectionFormBuilder;
import richtercloud.reflection.form.builder.jpa.panels.StringCheckPanel;
import richtercloud.reflection.form.builder.jpa.panels.StringCheckPanelUpdateEvent;
import richtercloud.reflection.form.builder.jpa.panels.StringCheckPanelUpdateListener;
import richtercloud.reflection.form.builder.message.MessageHandler;
import richtercloud.reflection.form.builder.typehandler.TypeHandler;

/**
 *
 * @author richter
 */
public class JPAStringTypeHandler implements TypeHandler<String, FieldUpdateEvent<String>,JPAReflectionFormBuilder, StringCheckPanel<?>> {
    private final EntityManager entityManager;
    private final int initialQueryLimit;
    private final MessageHandler messageHandler;
    private final String bidirectionalHelpDialogTitle;

    public JPAStringTypeHandler(EntityManager entityManager,
            int initialQueryLimit,
            MessageHandler messageHandler,
            String bidirectionalHelpDialogTitle) {
        this.entityManager = entityManager;
        this.initialQueryLimit = initialQueryLimit;
        this.messageHandler = messageHandler;
        this.bidirectionalHelpDialogTitle = bidirectionalHelpDialogTitle;
    }

    @Override
    public JComponent handle(Type type,
            String fieldValue,
            String fieldName,
            Class<?> declaringClass,
            final FieldUpdateListener<FieldUpdateEvent<String>> updateListener,
            JPAReflectionFormBuilder reflectionFormBuilder) throws IllegalArgumentException,
            IllegalAccessException,
            FieldHandlingException {
        final StringCheckPanel<?> retValue = new StringCheckPanel(entityManager,
                declaringClass, //declaringClass
                reflectionFormBuilder,
                fieldValue, //fieldValue
                fieldName, //fieldName
                this.initialQueryLimit, // initialQueryLimit
                bidirectionalHelpDialogTitle
        );
        retValue.addUpdateListener(new StringCheckPanelUpdateListener() {
            @Override
            public void onUpdate(StringCheckPanelUpdateEvent event) {
                updateListener.onUpdate(new FieldUpdateEvent<>(event.getNewValue()));
            }
        });
        return  retValue;
    }

    @Override
    public void reset(StringCheckPanel<?> component) {
        component.reset();
    }

}
