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
package richtercloud.reflection.form.builder.jpa.typehandler;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import richtercloud.reflection.form.builder.jpa.JPAReflectionFormBuilder;
import richtercloud.reflection.form.builder.typehandler.TypeHandler;
import richtercloud.reflection.form.builder.jpa.panels.EmbeddableListPanel;
import richtercloud.reflection.form.builder.message.MessageHandler;
import richtercloud.reflection.form.builder.panels.ListPanelItemEvent;
import richtercloud.reflection.form.builder.panels.ListPanelItemListener;

/**
 * First checks a {@code fieldTypeHandlerMapping} first for a match of the field
 * type. If that match doesn't exist, tries to find a match with the generic
 * type of the field in {@code genericsTypeHandlerMapping}.
 * @author richter
 */
public class ElementCollectionTypeHandler implements TypeHandler<List<Object>, FieldUpdateEvent<List<Object>>,JPAReflectionFormBuilder>{
    /**
     * The {@link TypeHandler} mapping for the generic type of the field (excluding the file type itself. Will be used if there's no match in {@code fieldTypeHandlerMapping}.
     */
    private final Map<Type, TypeHandler<?, ?,?>> genericsTypeHandlerMapping;
    /**
     * A mapping for {@link TypeHandler which is used is the field type matches
     * exactly.
     */
    private final Map<Type, TypeHandler<?,?,?>> fieldTypeHandlerMapping;
    private final MessageHandler messageHandler;
    private final FieldHandler embeddableFieldHandler;

    public ElementCollectionTypeHandler(Map<Type, TypeHandler<?, ?,?>> genericsTypeHandlerMapping,
            Map<Type, TypeHandler<?,?,?>> fieldTypeHandlerMapping,
            MessageHandler messageHandler,
            FieldHandler embeddableFieldHandler) {
        this.genericsTypeHandlerMapping = genericsTypeHandlerMapping;
        this.fieldTypeHandlerMapping = fieldTypeHandlerMapping;
        this.messageHandler = messageHandler;
        this.embeddableFieldHandler = embeddableFieldHandler;
    }

    @Override
    public JComponent handle(Type type,
            List<Object> fieldValue,
            String fieldName,
            Class<?> declaringClass,
            final FieldUpdateListener<FieldUpdateEvent<List<Object>>> updateListener,
            JPAReflectionFormBuilder reflectionFormBuilder) throws IllegalArgumentException,
            IllegalAccessException,
            FieldHandlingException {
        if(fieldValue == null) {
            fieldValue = new LinkedList<>(); //this is legitimate because all
                    //mechanisms will fail if the field value (retrieved with
                    //Field.get) is null (this allows to manage updates with
                    //this value which can be passed to updateListener then)
        }

        TypeHandler fieldTypeHandler = this.fieldTypeHandlerMapping.get(type);
        if(fieldTypeHandler != null) {
            JComponent retValue = fieldTypeHandler.handle(type,
                    fieldValue,
                    fieldName,
                    declaringClass,
                    updateListener,
                    reflectionFormBuilder);
            return retValue;
        }

        Class<?> fieldClass;
        if(!(type instanceof ParameterizedType)) {
            /*a simple
            @ElementCollection
            private List objectList;
            declaration*/
            fieldClass = Object.class;
        }else {
            //check class mapping first because if there's a match there's no need
            //to figure out the generic type of the list
            ParameterizedType fieldTypeParameterized = (ParameterizedType) type;
            TypeHandler typeHandler = this.genericsTypeHandlerMapping.get(fieldTypeParameterized);
            if(typeHandler != null) {
                return typeHandler.handle(fieldTypeParameterized,
                        fieldValue, //here we use the field value because
                            //this refers to the top level
                        fieldName,
                        declaringClass,
                        new FieldUpdateListener<FieldUpdateEvent<List<Object>>>() {
                            @Override
                            public void onUpdate(FieldUpdateEvent<List<Object>> event) {
                                updateListener.onUpdate(new FieldUpdateEvent<>(event.getNewValue()));
                            }
                        },
                        reflectionFormBuilder);
            }


            Type[] genericTypeArguments = fieldTypeParameterized.getActualTypeArguments();
            if(genericTypeArguments.length == 0) {
                //can happen according to ParameterizedType.getActualTypeArguments
                fieldClass = Object.class;
            }else {
                fieldClass = (Class<?>) genericTypeArguments[0];
            }
        }
        EmbeddableListPanel retValue = new EmbeddableListPanel(reflectionFormBuilder,
                fieldClass,
                fieldValue,
                messageHandler,
                embeddableFieldHandler
        );
        retValue.addItemListener(new ListPanelItemListener<Object>() {

            @Override
            public void onItemAdded(ListPanelItemEvent<Object> event) {
                updateListener.onUpdate(new FieldUpdateEvent<>(event.getItem()));
            }

            @Override
            public void onItemRemoved(ListPanelItemEvent<Object> event) {
                updateListener.onUpdate(new FieldUpdateEvent<>(event.getItem()));
            }
        });
        return retValue;
    }

}
