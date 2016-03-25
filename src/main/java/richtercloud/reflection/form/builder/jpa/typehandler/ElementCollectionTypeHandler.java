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

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import richtercloud.reflection.form.builder.ComponentResettable;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import richtercloud.reflection.form.builder.jpa.JPAReflectionFormBuilder;
import richtercloud.reflection.form.builder.jpa.panels.EmbeddableListPanel;
import richtercloud.reflection.form.builder.message.MessageHandler;
import richtercloud.reflection.form.builder.panels.ListPanelItemEvent;
import richtercloud.reflection.form.builder.panels.ListPanelItemListener;
import richtercloud.reflection.form.builder.typehandler.GenericListTypeHandler;
import richtercloud.reflection.form.builder.typehandler.TypeHandler;

/**
 * First checks a {@code fieldTypeHandlerMapping} first for a match of the field
 * type. If that match doesn't exist, tries to find a match with the generic
 * type of the field in {@code genericsTypeHandlerMapping}.
 *
 * Handle {@link ElementCollection} annotated fields whose generic types are of
 * primitive wrapper or {@code String} in callers. This makes handling with
 * {@code List<Object>} in this class much easier.
 *
 * @author richter
 */
public class ElementCollectionTypeHandler extends GenericListTypeHandler<JPAReflectionFormBuilder, EmbeddableListPanel> {
    private final MessageHandler messageHandler;
    private final FieldHandler embeddableFieldHandler;

    public ElementCollectionTypeHandler(Map<Type, TypeHandler<?, ?,?, ?>> genericsTypeHandlerMapping,
            Map<Type, TypeHandler<?,?,?, ?>> fieldTypeHandlerMapping,
            MessageHandler messageHandler,
            FieldHandler embeddableFieldHandler) {
        super(genericsTypeHandlerMapping, fieldTypeHandlerMapping);
        this.messageHandler = messageHandler;
        this.embeddableFieldHandler = embeddableFieldHandler;
    }

    @Override
    public void reset(EmbeddableListPanel component) {
        component.reset();
    }

    /**
     *
     * @param type
     * @param fieldValue
     * @param fieldName
     * @param declaringClass
     * @param updateListener
     * @param reflectionFormBuilder
     * @throws IllegalArgumentException if the generic type of {@code type} as
     * returned by {@link #retrieveTypeGenericType(java.lang.reflect.Type) } is
     * {@code String} or a primitive wrapper
     * @return the result of the handling
     */
    @Override
    protected Pair<JComponent, ComponentResettable<?>> handleGenericType(Type type, List<Object> fieldValue,
            String fieldName,
            Class<?> declaringClass,
            final FieldUpdateListener<FieldUpdateEvent<List<Object>>> updateListener,
            JPAReflectionFormBuilder reflectionFormBuilder) {
        Type genericType = retrieveTypeGenericType(type);
        if(genericType.equals(String.class)
                || genericType.equals(Byte.class)
                || genericType.equals(Boolean.class)
                || genericType.equals(Character.class)
                || genericType.equals(Integer.class)
                || genericType.equals(Long.class)
                || genericType.equals(Short.class)
                || genericType.equals(Double.class)
                || genericType.equals(Float.class)) {
            throw new IllegalArgumentException(String.format("generic type %s or primitive wrapper not supported (is %s)", String.class, genericType));
        }
        EmbeddableListPanel retValue = new EmbeddableListPanel(reflectionFormBuilder,
                (Class<?>) genericType,
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
        return new ImmutablePair<JComponent, ComponentResettable<?>>(retValue, this);
    }
}
