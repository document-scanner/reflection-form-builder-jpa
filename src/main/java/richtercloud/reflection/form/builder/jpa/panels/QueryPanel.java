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
package richtercloud.reflection.form.builder.jpa.panels;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.swing.GroupLayout;
import javax.swing.LayoutStyle;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.message.handler.IssueHandler;
import richtercloud.reflection.form.builder.jpa.storage.FieldInitializer;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.validation.tools.FieldRetrievalException;
import richtercloud.validation.tools.FieldRetriever;

/**
 * Allows to run a JPQL query for a specific class while getting feedback about
 * the following errors:<ul>
 * <li>syntax errors in queries</li>
 * <li>queries with the wrong result type/class</li>
 * <li>(unexpected) errors which occured during execution of the query</li>
 * </ul> and an overview of the result in a table displaying all class fields
 * (as returned by
 * {@link FieldRetriever#retrieveRelevantFields(java.lang.Class) }.
 * The feedback is given in a scrollable (non-editable) label in order to
 * provide a fixed size layout.
 *
 * In favour of immutability as design principle QueryPanel needs to be
 * recreated in order to handle a different entity class.
 *
 * There's currently no feature to hide specific class fields.
 *
 * @author richter
 * @param <E> a generic type for the entity class
 */
public class QueryPanel<E> extends AbstractQueryPanel<E> {
    private static final long serialVersionUID = 1L;
    private final static Logger LOGGER = LoggerFactory.getLogger(QueryPanel.class);

    /**
     * Checks every field of the type of every field of {@code entityClass} if
     * it's assignable from (i.e. a superclass) of {@code entityClass} so that
     * it can be assumed that a relationship can be defined (this avoids a
     * reference to a declaring class being passed to the constructor and thus
     * arbitrary nesting of type handling).
     *
     * @param entityClass
     * @param entityClassFields
     * @return
     */
    public static Set<Field> retrieveMappedFieldCandidates(Class<?> entityClass,
            List<Field> entityClassFields) {
        Set<Field> retValue = new HashSet<>();
        for(Field entityClassField : entityClassFields) {
            for(Field entityClassFieldField : entityClassField.getType().getDeclaredFields()) {
                OneToOne entityClassFieldOneToOne = entityClassField.getAnnotation(OneToOne.class);
                //ManyToOne doesn't have a mappedBy field, but it needs to be
                //checked to be offered for a user-defined mapping
                ManyToOne entityClassFieldManyToOne = entityClassField.getAnnotation(ManyToOne.class);
                if(entityClassFieldOneToOne != null || entityClassFieldManyToOne != null) {
                    if(entityClass.isAssignableFrom(entityClassFieldField.getType())) {
                        retValue.add(entityClassField);
                    }
                }
            }
        }
        return retValue;
    }

    /**
     *
     * @param entityClassFields
     * @return
     */
    public static Field retrieveMappedByFieldPanel(List<Field> entityClassFields) {
        for(Field entityClassField : entityClassFields) {
            OneToOne entityClassFieldOneToOne = entityClassField.getAnnotation(OneToOne.class);
            if(entityClassFieldOneToOne != null) {
                String mappedBy = entityClassFieldOneToOne.mappedBy();
                if(mappedBy != null && !mappedBy.isEmpty()) {
                    //if mappedBy is specified the user isn't given a choice
                    return entityClassField;
                }
            }
        }
        return null;
    }

    private final E initialValue;

    public QueryPanel(PersistenceStorage storage,
            Class<E> entityClass,
            IssueHandler issueHandler,
            FieldRetriever fieldRetriever,
            E initialValue,
            BidirectionalControlPanel bidirectionalControlPanel,
            int queryResultTableSelectionMode,
            FieldInitializer fieldInitializer,
            QueryHistoryEntryStorage entryStorage) throws IllegalArgumentException, IllegalAccessException, FieldRetrievalException {
        this(storage,
                entityClass,
                issueHandler,
                fieldRetriever,
                initialValue,
                bidirectionalControlPanel,
                QUERY_RESULT_TABLE_HEIGHT_DEFAULT,
                queryResultTableSelectionMode,
                fieldInitializer,
                entryStorage);
    }

    public QueryPanel(PersistenceStorage storage,
            Class<E> entityClass,
            IssueHandler issueHandler,
            FieldRetriever fieldRetriever,
            E initialValue,
            int queryResultTableHeight,
            String bidirectionalHelpDialogTitle,
            int queryResultTableSelectionMode,
            FieldInitializer fieldInitializer,
            QueryHistoryEntryStorage entryStorage) throws IllegalArgumentException, IllegalAccessException, FieldRetrievalException {
        this(storage,
                entityClass,
                issueHandler,
                fieldRetriever,
                initialValue,
                new BidirectionalControlPanel(entityClass,
                        bidirectionalHelpDialogTitle,
                        retrieveMappedByFieldPanel(fieldRetriever.retrieveRelevantFields(entityClass)),
                        retrieveMappedFieldCandidates(entityClass,
                                fieldRetriever.retrieveRelevantFields(entityClass))),
                queryResultTableHeight,
                queryResultTableSelectionMode,
                fieldInitializer,
                entryStorage);
    }

    /**
     *
     * @param entityManager
     * @param entityClass the class for which to the panel for
     * @param issueHandler used in {@link QueryComponent}s added to this
     * {@code QueryPanel}
     * @param reflectionFormBuilder
     * @param initialValue
     * @param queryResultTableSelectionMode
     * @param queryResultTableHeight
     * @param bidirectionalControlPanel
     * @throws java.lang.IllegalAccessException
     * @throws IllegalArgumentException if {@code initialSelectedHistoryEntry} is not {@code null}, but not contained in {@code initialHistory}
     */
    /*
    internal implementation notes:
    - it's necessary to use a copy of initialHistory in order to avoid ConcurrentModificationException when items are sorted in combobox model implementation
    - enforce passing of BidirectionalControlPanel in order to maximize
    reusability (and because it's perfectly legitimate due to
    composition-over-inheritance)
    */
    public QueryPanel(PersistenceStorage storage,
            Class<E> entityClass,
            IssueHandler issueHandler,
            FieldRetriever fieldRetriever,
            E initialValue,
            BidirectionalControlPanel bidirectionalControlPanel,
            int queryResultTableHeight,
            int queryResultTableSelectionMode,
            FieldInitializer fieldInitializer,
            QueryHistoryEntryStorage entryStorage) throws IllegalArgumentException, IllegalAccessException, FieldRetrievalException {
        super(bidirectionalControlPanel,
                new QueryComponent<>(storage,
                        entityClass,
                        issueHandler,
                        true, //async
                        entryStorage
                ),
                fieldRetriever,
                entityClass,
                storage,
                fieldInitializer,
                issueHandler,
                queryResultTableSelectionMode,
                new LinkedList<>(Arrays.asList(initialValue)));
        this.initialValue = initialValue;

        GroupLayout.ParallelGroup horizontalParallelGroup = getLayout().createParallelGroup();
        horizontalParallelGroup.addGroup(super.getHorizontalParallelGroup())
                .addComponent(getQueryResultTableScrollPane(), GroupLayout.Alignment.TRAILING);
        getLayout().setHorizontalGroup(horizontalParallelGroup);

        GroupLayout.SequentialGroup verticalSequentialGroup = getLayout().createSequentialGroup();
        verticalSequentialGroup
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(super.getVerticalSequentialGroup())
                .addComponent(getQueryResultTableScrollPane(),
                        GroupLayout.DEFAULT_SIZE,
                        queryResultTableHeight,
                        Short.MAX_VALUE)
                .addContainerGap();
        getLayout().setVerticalGroup(verticalSequentialGroup);

        reset0();

        //only QueryPanel specific
        this.getQueryResultTableSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                //ListSelectionEvent.getFirstIndex and
                //ListSelectionEvent.getLastIndex include the previously
                //selected index which make them not suitable to figure out the
                //new selection
                int newSelectionIndex = QueryPanel.this.getQueryResultTableSelectionModel().getMinSelectionIndex();
                if(newSelectionIndex == -1) {
                    //no selection (might occur during initialization)
                    return;
                }
                Object newSelectionItem = QueryPanel.this.getQueryResultTable().getModel().getEntities().get(newSelectionIndex);
                for(QueryPanelUpdateListener updateListener : getUpdateListeners()) {
                    LOGGER.debug("notifying update listener {} about selection change", updateListener);
                    updateListener.onUpdate(new QueryPanelUpdateEvent(newSelectionItem,
                            QueryPanel.this));
                }
            }
        });
        getQueryComponent().runQuery(true //async (at creation)
        );
    }

    public List<Object> getSelectedObjects() {
        int[] indeces = this.getQueryResultTable().getSelectedRows();
        //assume that if index is >= 0 that this.queryResults is != null as well
        List<Object> retValue = new LinkedList<>();
        for(int index : indeces) {
            int convertedIndex = this.getQueryResultTable().convertRowIndexToModel(index);
                //necessary since sorting is possible
            Object selectedValue = this.getQueryResultTable().getModel().getEntities().get(convertedIndex);
            retValue.add(selectedValue);
        }
        return retValue;
    }

    public void reset() throws FieldRetrievalException {
        reset0();
    }

    private void reset0() throws FieldRetrievalException {
        this.getQueryResultTableModel().clear();
            //no need to update columns (with EntityTableModel.updateColumns)
            //because that will only happen if the next query is run (which is
            //not part of resetting)
        if(initialValue != null) {
            if(!this.getStorage().isManaged(initialValue)) {
                this.getQueryResultLabel().setText(String.format("previously managed entity %s has been removed from persistent storage, ignoring", initialValue));
            }
            if(!this.getQueryResultTable().getModel().getEntities().contains(initialValue)) {
                try {
                    this.getQueryResultTable().getModel().addEntity(initialValue); // ok to add initially (will be overwritten with the next query where the user has to specify a query which retrieves the initial value or not
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            }
            int initialValueIndex = this.getQueryResultTable().getModel().getEntities().indexOf(initialValue);
            this.getQueryResultTableSelectionModel().addSelectionInterval(initialValueIndex, initialValueIndex); //no need to clear selection because we're just initializing
        }
    }
}
