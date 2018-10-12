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
package de.richtercloud.reflection.form.builder.jpa.typehandler;

import de.richtercloud.message.handler.IssueHandler;
import de.richtercloud.reflection.form.builder.ComponentHandler;
import de.richtercloud.reflection.form.builder.ResetException;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import de.richtercloud.reflection.form.builder.fieldhandler.MappedFieldUpdateEvent;
import de.richtercloud.reflection.form.builder.jpa.JPAReflectionFormBuilder;
import de.richtercloud.reflection.form.builder.jpa.panels.BidirectionalControlPanel;
import de.richtercloud.reflection.form.builder.jpa.panels.QueryHistoryEntryStorage;
import de.richtercloud.reflection.form.builder.jpa.panels.QueryPanel;
import de.richtercloud.reflection.form.builder.jpa.panels.QueryPanelUpdateEvent;
import de.richtercloud.reflection.form.builder.jpa.panels.QueryPanelUpdateListener;
import de.richtercloud.reflection.form.builder.jpa.storage.FieldInitializer;
import de.richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import de.richtercloud.reflection.form.builder.typehandler.TypeHandler;
import de.richtercloud.validation.tools.FieldRetriever;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.ListSelectionModel;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author richter
 */
public class ToOneTypeHandler implements TypeHandler<Object, FieldUpdateEvent<Object>, JPAReflectionFormBuilder, QueryPanel>{
    private final PersistenceStorage storage;
    private final String bidirectionalHelpDialogTitle;
    private final IssueHandler issueHandler;
    private final FieldInitializer fieldInitializer;
    private final QueryHistoryEntryStorage entryStorage;
    private final FieldRetriever readOnlyFieldRetriever;

    public ToOneTypeHandler(PersistenceStorage storage,
            IssueHandler issueHandler,
            String bidirectionalHelpDialogTitle,
            FieldInitializer fieldInitializer,
            QueryHistoryEntryStorage initialQueryTextGenerator,
            FieldRetriever readOnlyFieldRetriever) {
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
        this.entryStorage = initialQueryTextGenerator;
        this.readOnlyFieldRetriever = readOnlyFieldRetriever;
    }

    @Override
    public Pair<JComponent, ComponentHandler<?>> handle(Type type,
            Object fieldValue,
            String fieldName,
            Class<?> declaringClass,
            final FieldUpdateListener<FieldUpdateEvent<Object>> updateListener,
            JPAReflectionFormBuilder reflectionFormBuilder) throws FieldHandlingException,
            ResetException {
        if(!(type instanceof Class)) {
            throw new IllegalArgumentException("the generic type of type has to be instanceof Class");
        }
        Class<?> entityClass = (Class<?>)type;
        List<Field> entityClassFields = readOnlyFieldRetriever.retrieveRelevantFields(entityClass);
        Set<Field> mappedFieldCandidates = QueryPanel.retrieveMappedFieldCandidates(entityClass,
                        entityClassFields);
        BidirectionalControlPanel bidirectionalControlPanel;
        bidirectionalControlPanel = new BidirectionalControlPanel(declaringClass,
                bidirectionalHelpDialogTitle,
                QueryPanel.retrieveMappedByFieldPanel(entityClassFields),
                mappedFieldCandidates);
        final QueryPanel retValue = new QueryPanel(storage,
                entityClass,
                issueHandler,
                readOnlyFieldRetriever,
                fieldValue,
                bidirectionalControlPanel,
                ListSelectionModel.SINGLE_SELECTION,
                fieldInitializer,
                entryStorage);
        retValue.addUpdateListener(new QueryPanelUpdateListener() {
            @Override
            public void onUpdate(QueryPanelUpdateEvent event) {
                Object newSelectionItem = event.getNewSelectionItem();
                updateListener.onUpdate(new MappedFieldUpdateEvent<>(newSelectionItem,
                        retValue.getBidirectionalControlPanel().getMappedField()));
            }
        });
        return new ImmutablePair<JComponent, ComponentHandler<?>>(retValue, this);
    }

    @Override
    public void reset(QueryPanel component) throws ResetException {
        component.reset();
    }
}
