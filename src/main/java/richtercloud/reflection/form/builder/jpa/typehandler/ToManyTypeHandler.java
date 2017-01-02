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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import richtercloud.message.handler.MessageHandler;
import richtercloud.reflection.form.builder.ComponentHandler;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import richtercloud.reflection.form.builder.fieldhandler.MappedFieldUpdateEvent;
import richtercloud.reflection.form.builder.jpa.JPAReflectionFormBuilder;
import richtercloud.reflection.form.builder.jpa.panels.InitialQueryTextGenerator;
import richtercloud.reflection.form.builder.jpa.panels.QueryListPanel;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.reflection.form.builder.panels.ListPanelItemEvent;
import richtercloud.reflection.form.builder.panels.ListPanelItemListener;
import richtercloud.reflection.form.builder.typehandler.GenericListTypeHandler;
import richtercloud.reflection.form.builder.typehandler.TypeHandler;
import richtercloud.reflection.form.builder.jpa.storage.FieldInitializer;

/**
 *
 * @author richter
 */
public class ToManyTypeHandler extends GenericListTypeHandler<JPAReflectionFormBuilder, QueryListPanel> {
    private final PersistenceStorage storage;
    private final String bidirectionalHelpDialogTitle;
    private final MessageHandler messageHandler;
    private final FieldInitializer fieldInitializer;
    private final InitialQueryTextGenerator initialQueryTextGenerator;
    private final FieldRetriever readOnlyFieldRetriever;

    public ToManyTypeHandler(PersistenceStorage storage,
            MessageHandler messageHandler,
            Map<Type, TypeHandler<?, ?, ?, ?>> genericsTypeHandlerMapping,
            Map<Type, TypeHandler<?, ?, ?, ?>> fieldTypeHandlerMapping,
            String bidirectionalHelpDialogTitle,
            FieldInitializer fieldInitializer,
            InitialQueryTextGenerator initialQueryTextGenerator,
            FieldRetriever readOnlyFieldRetriever) {
        super(genericsTypeHandlerMapping,
                fieldTypeHandlerMapping);
        if(storage == null) {
            throw new IllegalArgumentException("storage mustn't be null");
        }
        this.storage = storage;
        if(messageHandler == null) {
            throw new IllegalArgumentException("messageHandler mustn't be null");
        }
        this.messageHandler = messageHandler;
        this.bidirectionalHelpDialogTitle = bidirectionalHelpDialogTitle;
        this.fieldInitializer = fieldInitializer;
        this.initialQueryTextGenerator = initialQueryTextGenerator;
        this.readOnlyFieldRetriever = readOnlyFieldRetriever;
    }

    @Override
    protected Pair<JComponent, ComponentHandler<?>> handleGenericType(Type type,
            List<Object> fieldValue,
            String fieldName,
            Class<?> declaringClass,
            final FieldUpdateListener<FieldUpdateEvent<List<Object>>> updateListener,
            JPAReflectionFormBuilder reflectionFormBuilder) throws IllegalAccessException {
        Type genericType = retrieveTypeGenericType(type);
        if(!(genericType instanceof Class)) {
            throw new IllegalArgumentException("the generic type of type has to be instanceof Class");
        }
        final QueryListPanel retValue = new QueryListPanel(storage,
                readOnlyFieldRetriever,
                (Class<?>) type,
                messageHandler,
                fieldValue,
                bidirectionalHelpDialogTitle,
                fieldInitializer,
                initialQueryTextGenerator);
        retValue.addItemListener(new ListPanelItemListener<Object>() {
            @Override
            public void onItemAdded(ListPanelItemEvent<Object> event) {
                updateListener.onUpdate(new MappedFieldUpdateEvent<List<Object>>(new LinkedList<>(event.getItem()),
                        retValue.getBidirectionalControlPanel().getMappedField()));
            }

            @Override
            public void onItemRemoved(ListPanelItemEvent<Object> event) {
                updateListener.onUpdate(new MappedFieldUpdateEvent<List<Object>>(new LinkedList<>(event.getItem()),
                        retValue.getBidirectionalControlPanel().getMappedField()));
            }
        });
        return new ImmutablePair<JComponent, ComponentHandler<?>>(retValue, this);
    }

    @Override
    public void reset(QueryListPanel component) {
        component.reset();
    }

}
