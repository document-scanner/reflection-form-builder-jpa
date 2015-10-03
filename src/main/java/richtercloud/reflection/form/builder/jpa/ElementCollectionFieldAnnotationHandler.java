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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JLabel;
import richtercloud.reflection.form.builder.FieldAnnotationHandler;
import richtercloud.reflection.form.builder.FieldUpdateListener;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.jpa.panels.EmbeddableListPanel;
import richtercloud.reflection.form.builder.panels.ListPanelItemEvent;
import richtercloud.reflection.form.builder.panels.ListPanelItemListener;
import richtercloud.reflection.form.builder.panels.ListPanelTableCellEditor;
import richtercloud.reflection.form.builder.panels.ListPanelTableCellRenderer;

/**
 *
 * @author richter
 */
public class ElementCollectionFieldAnnotationHandler implements FieldAnnotationHandler<List<Object>, ElementCollectionFieldUpdateEvent> {
    private final static ElementCollectionFieldAnnotationHandler INSTANCE = new ElementCollectionFieldAnnotationHandler();

    public static ElementCollectionFieldAnnotationHandler getInstance() {
        return INSTANCE;
    }

    protected ElementCollectionFieldAnnotationHandler() {
    }

    @Override
    public JComponent handle(Type fieldClass,
            List<Object> fieldValue,
            Object entity,
            final FieldUpdateListener<ElementCollectionFieldUpdateEvent> updateListener,
            ReflectionFormBuilder reflectionFormBuilder) {
        Class<?> genericType;
        if(!(fieldClass instanceof ParameterizedType)) {
            /*a simple
            @ElementCollection
            private List objectList;
            declaration*/
            genericType = Object.class;
        }else {
            Type[] genericTypeArguments = ((ParameterizedType)fieldClass).getActualTypeArguments();
            if(genericTypeArguments.length == 0) {
                //can happen according to ParameterizedType.getActualTypeArguments
                genericType = Object.class;
            }else {
                genericType = (Class<?>) genericTypeArguments[0];
            }
        }
        EmbeddableListPanel retValue;
        try {
            retValue = new EmbeddableListPanel(reflectionFormBuilder,
                    new ListPanelTableCellEditor(new JLabel()) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected Object stopCellEditing0() {
                            //do nothing because there's no editing in the table,
                            //but in the dialog
                            return null;
                        }
                    },
                    new ListPanelTableCellRenderer(new JLabel()) {
                    },
                    genericType
            );
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
        retValue.addItemListener(new ListPanelItemListener<Object>() {

            @Override
            public void onItemAdded(ListPanelItemEvent<Object> event) {
                updateListener.onUpdate(new ElementCollectionFieldUpdateEvent(event.getItem()));
            }

            @Override
            public void onItemRemoved(ListPanelItemEvent<Object> event) {
                updateListener.onUpdate(new ElementCollectionFieldUpdateEvent(event.getItem()));
            }
        });
        return retValue;
    }

}
