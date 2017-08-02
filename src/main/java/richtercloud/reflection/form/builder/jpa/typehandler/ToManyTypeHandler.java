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
import richtercloud.message.handler.IssueHandler;
import richtercloud.reflection.form.builder.ComponentHandler;
import richtercloud.reflection.form.builder.ResetException;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import richtercloud.reflection.form.builder.fieldhandler.MappedFieldUpdateEvent;
import richtercloud.reflection.form.builder.jpa.JPAReflectionFormBuilder;
import richtercloud.reflection.form.builder.jpa.panels.QueryHistoryEntryStorage;
import richtercloud.reflection.form.builder.jpa.panels.QueryListPanel;
import richtercloud.reflection.form.builder.jpa.storage.FieldInitializer;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.reflection.form.builder.panels.ListPanelItemEvent;
import richtercloud.reflection.form.builder.panels.ListPanelItemListener;
import richtercloud.reflection.form.builder.typehandler.GenericListTypeHandler;
import richtercloud.reflection.form.builder.typehandler.TypeHandler;
import richtercloud.validation.tools.FieldRetriever;

/**
 *
 * @author richter
 */
public class ToManyTypeHandler extends GenericListTypeHandler<JPAReflectionFormBuilder, QueryListPanel> {
    private final PersistenceStorage storage;
    private final String bidirectionalHelpDialogTitle;
    private final IssueHandler issueHandler;
    private final FieldInitializer fieldInitializer;
    private final QueryHistoryEntryStorage entryStorage;
    private final FieldRetriever readOnlyFieldRetriever;

    public ToManyTypeHandler(PersistenceStorage storage,
            IssueHandler issueHandler,
            Map<Type, TypeHandler<?, ?, ?, ?>> genericsTypeHandlerMapping,
            Map<Type, TypeHandler<?, ?, ?, ?>> fieldTypeHandlerMapping,
            String bidirectionalHelpDialogTitle,
            FieldInitializer fieldInitializer,
            QueryHistoryEntryStorage entryStorage,
            FieldRetriever readOnlyFieldRetriever) {
        super(genericsTypeHandlerMapping,
                fieldTypeHandlerMapping);
        if(storage == null) {
            throw new IllegalArgumentException("storage mustn't be null");
        }
        this.storage = storage;
        if(issueHandler == null) {
            throw new IllegalArgumentException("messageHandler mustn't be null");
        }
        this.issueHandler = issueHandler;
        this.bidirectionalHelpDialogTitle = bidirectionalHelpDialogTitle;
        this.fieldInitializer = fieldInitializer;
        this.entryStorage = entryStorage;
        this.readOnlyFieldRetriever = readOnlyFieldRetriever;
    }

    @Override
    protected Pair<JComponent, ComponentHandler<?>> handleGenericType(Type type,
            List<Object> fieldValue,
            String fieldName,
            Class<?> declaringClass,
            final FieldUpdateListener<FieldUpdateEvent<List<Object>>> updateListener,
            JPAReflectionFormBuilder reflectionFormBuilder) throws IllegalAccessException,
            NoSuchFieldException,
            ResetException {
        Type genericType = retrieveTypeGenericType(type);
        if(!(genericType instanceof Class)) {
            throw new IllegalArgumentException("the generic type of type has to be instanceof Class");
        }
        final QueryListPanel retValue = new QueryListPanel(storage,
                readOnlyFieldRetriever,
                (Class<?>) type,
                issueHandler,
                fieldValue,
                bidirectionalHelpDialogTitle,
                fieldInitializer,
                entryStorage);
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
    public void reset(QueryListPanel component) throws ResetException {
        component.reset();
    }

}
