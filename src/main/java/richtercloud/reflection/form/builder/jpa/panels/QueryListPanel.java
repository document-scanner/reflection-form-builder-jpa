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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.message.handler.ExceptionMessage;
import richtercloud.message.handler.IssueHandler;
import richtercloud.message.handler.Message;
import richtercloud.reflection.form.builder.ResetException;
import richtercloud.reflection.form.builder.jpa.ReflectionFormBuilderHelperJPA;
import richtercloud.reflection.form.builder.jpa.storage.FieldInitializer;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.reflection.form.builder.panels.AbstractListPanel;
import richtercloud.reflection.form.builder.panels.ListPanelItemEvent;
import richtercloud.reflection.form.builder.panels.ListPanelItemEventVetoException;
import richtercloud.reflection.form.builder.panels.ListPanelItemListener;
import richtercloud.validation.tools.FieldRetriever;

/**
 * Provides a {@link QueryComponent} and controls to add and remove stored
 * entities from a query result list to selection result list with an add and
 * remove button. The add button adds entities which are selected in the query
 * result table to the selection result table and the remove button removes from
 * them. The query result is read-only.
 *
 * Currently there're no editing facilities.
 *
 * @author richter
 */
public class QueryListPanel<E> extends AbstractQueryPanel<E> {
    private static final long serialVersionUID = 1L;
    private final static Logger LOGGER = LoggerFactory.getLogger(QueryListPanel.class);

    private static BidirectionalControlPanel generateBidirectionalControlPanel(Class<?> entityClass,
            FieldRetriever fieldRetriever,
            String bidirectionalHelpDialogTitle) throws NoSuchFieldException {
        List<Field> entityClassFields = fieldRetriever.retrieveRelevantFields(entityClass);
        Set<Field> mappedFieldCandidates = ReflectionFormBuilderHelperJPA.retrieveMappedFieldCandidates(entityClass,
                entityClassFields,
                fieldRetriever);
        BidirectionalControlPanel bidirectionalControlPanel = new BidirectionalControlPanel(entityClass,
                bidirectionalHelpDialogTitle,
                ReflectionFormBuilderHelperJPA.retrieveMappedByFieldListPanel(entityClassFields, mappedFieldCandidates),
                mappedFieldCandidates);
        return bidirectionalControlPanel;
    }

    private final JButton addButton;
    private final JButton removeButton;
    private final EntityTable<E> resultTable;
    private final EntityTableModel<E> resultTableModel;
    private final JLabel resultTableLabel;
    private final JScrollPane resultTableScrollPane;
    private final List<E> initialValues;
    private final Set<ListPanelItemListener<E>> updateListeners = new HashSet<>();
    private final JSplitPane resultSplitPane;
    private final JPanel resultPanel;

    public QueryListPanel(PersistenceStorage storage,
            FieldRetriever fieldRetriever,
            Class<E> entityClass,
            IssueHandler issueHandler,
            List<E> initialValues,
            String bidirectionalHelpDialogTitle,
            FieldInitializer fieldInitializer,
            QueryHistoryEntryStorage entryStorage) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, ResetException {
        this(storage,
                fieldRetriever,
                entityClass,
                issueHandler,
                initialValues,
                bidirectionalHelpDialogTitle,
                QUERY_RESULT_TABLE_HEIGHT_DEFAULT,
                fieldInitializer,
                entryStorage);
    }

    /**
     *
     * @param entityManager
     * @param reflectionFormBuilder
     * @param entityClass the class for which to the panel for
     * @param issueHandler
     * @param initialValues
     * @param bidirectionalHelpDialogTitle
     * @param queryResultTableHeight
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public QueryListPanel(PersistenceStorage storage,
            FieldRetriever fieldRetriever,
            Class<E> entityClass,
            IssueHandler issueHandler,
            List<E> initialValues,
            String bidirectionalHelpDialogTitle,
            int queryResultTableHeight,
            FieldInitializer fieldInitializer,
            QueryHistoryEntryStorage entryStorage) throws IllegalArgumentException,
            IllegalAccessException,
            NoSuchFieldException,
            ResetException {
        super(generateBidirectionalControlPanel(entityClass,
                fieldRetriever,
                bidirectionalHelpDialogTitle),
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
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
                initialValues);
        LOGGER.debug(String.format("creating %s for entity class %s with initial value %s", QueryListPanel.class, entityClass, initialValues));
        removeButton = new JButton();
        addButton = new JButton();
        resultTableLabel = new JLabel();
        resultTableScrollPane = new JScrollPane();
        this.resultTableModel = new EntityTableModel<>(getFieldRetriever());
        resultTable = new EntityTable<E>(this.resultTableModel) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.resultSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        this.resultPanel = new JPanel();

        this.getQueryComponent().addListener(new QueryComponentListener<E>() {
            /**
             * After a query is executed (again) update the model (add or remove
             * columns which represent fields which are no longer contained in
             * the queried entities)
             * @param event
             */
            @Override
            public void onQueryExecuted(QueryComponentEvent<E> event) {
                try {
                    resultTable.getModel().updateColumns(event.getQueryResults());
                }catch(IllegalAccessException ex) {
                    LOGGER.error("unexpected exception during query execution occured",
                            ex);
                    issueHandler.handleUnexpectedException(new ExceptionMessage(ex));
                }
            }
        });
        getQueryComponent().runQuery(true, //async (at creation)
                true //skipHistoryEntryUsageCountIncrement (at creation)
        ); //after adding QueryComponentListeners
            //(can't be run in AbstractListPanel because listeners can not be
            //passed to superclass and be able to reference variables in
            //QueryListPanel)

        this.initialValues = initialValues;
        initComponents(queryResultTableHeight);
        reset0();
        this.resultTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    private void initComponents(int queryResultTableHeight) throws IllegalArgumentException, IllegalAccessException {
        removeButton.setText("Remove");
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        addButton.setText("Add");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        resultTableLabel.setText("Selected entities:");

        resultTableScrollPane.setViewportView(resultTable);

        GroupLayout resultPanelLayout = new GroupLayout(resultPanel);
        resultPanel.setLayout(resultPanelLayout);
        resultPanelLayout.setHorizontalGroup(resultPanelLayout.createParallelGroup()
                .addGroup(resultPanelLayout.createSequentialGroup()
                        .addComponent(resultTableLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(addButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(removeButton))
                .addComponent(resultTableScrollPane, GroupLayout.Alignment.TRAILING));
        resultPanelLayout.setVerticalGroup(resultPanelLayout.createSequentialGroup()
                .addGroup(resultPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(removeButton)
                    .addComponent(addButton)
                    .addComponent(resultTableLabel))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(resultTableScrollPane, //component
                        0, //min
                        queryResultTableHeight, //pref
                        Short.MAX_VALUE //max
                )
                .addContainerGap());
        resultSplitPane.setLeftComponent(getQueryResultTableScrollPane());
        resultSplitPane.setRightComponent(resultPanel);

        //need to reuse the layout from superclass because otherwise the
        //components aren't visible (only the space is reserved without any
        //further notice
        GroupLayout.ParallelGroup horizontalParallelGroup = getLayout().createParallelGroup();
        horizontalParallelGroup.addGroup(getLayout().createParallelGroup()
            .addGroup(super.getHorizontalParallelGroup())
            .addGroup(getLayout().createSequentialGroup()
                .addContainerGap()
                .addGroup(getLayout().createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(GroupLayout.Alignment.LEADING, super.getHorizontalParallelGroup())
                    .addComponent(resultSplitPane,
                            GroupLayout.DEFAULT_SIZE,
                            queryResultTableHeight,
                            Short.MAX_VALUE))
                .addContainerGap()));
        getLayout().setHorizontalGroup(horizontalParallelGroup);
        GroupLayout.SequentialGroup verticalSequentialGroup = getLayout().createSequentialGroup();
        verticalSequentialGroup.addGroup(getLayout().createSequentialGroup()
                .addGroup(super.getVerticalSequentialGroup())
                .addComponent(resultSplitPane,
                            GroupLayout.DEFAULT_SIZE, //min
                            queryResultTableHeight+resultPanel.getPreferredSize().height+resultSplitPane.getDividerSize(), //pref
                            Short.MAX_VALUE//max
                            ));
        getLayout().setVerticalGroup(verticalSequentialGroup);
        this.resultSplitPane.setDividerLocation(queryResultTableHeight);
            //- JSplitPane.setDividerLocation(double) doesn't have any effect if
            //the split pane isn't displayed (see Javadoc for details)
            //- There's no point in trying to figure out the height determined
            //by layout if queryResultTableHeight is available
    }

    public void addItemListener(ListPanelItemListener<E> updateListener) {
        this.updateListeners.add(updateListener);
    }

    public void removeItemListener(ListPanelItemListener<E> updateListener) {
        this.updateListeners.remove(updateListener);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int[] indices = this.getQueryResultTable().getSelectedRows();
        List<E> eventItems = new LinkedList<>(); //a list to pass to the item
            //event ought to be maintained before adding to resultTable's model
        for(int index : indices) {
            int convertedIndex = this.getQueryResultTable().convertRowIndexToModel(index);
                //necessary since sorting is possible
            E queryResult = this.getQueryResultTable().getModel().getEntities().get(convertedIndex);
            eventItems.add(queryResult);
            for(ListPanelItemListener<E> updateListener : updateListeners) {
                try {
                    updateListener.onItemAdded(new ListPanelItemEvent<>(ListPanelItemEvent.EVENT_TYPE_ADDED,
                            convertedIndex,
                            eventItems));
                } catch (ListPanelItemEventVetoException ex) {
                    getMessageHandler().handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
                    return;
                }
            }
            try {
                this.resultTable.getModel().addEntity(queryResult);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                LOGGER.info("an exception occured while executing the query", ex);
                this.getQueryComponent().getQueryStatusLabel().setText(String.format("<html>%s</html>", ex.getMessage()));
            }
        }
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int[] selectedRows = this.resultTable.getSelectedRows();
        if(selectedRows.length == 0) {
            return;
        }
        //need to sort in order to remove from highest to lowest value
        List<Integer> selectedRowsSorted = new LinkedList<>();
        for(int selectedRow : selectedRows) {
            selectedRowsSorted.add(selectedRow);
        }
        Collections.sort(selectedRowsSorted, AbstractListPanel.DESCENDING_ORDER);
        for(int selectedRow : selectedRowsSorted) {
            resultTableModel.removeEntity(selectedRow);
            for(ListPanelItemListener<E> updateListener : updateListeners) {
                try {
                    updateListener.onItemRemoved(new ListPanelItemEvent<>(ListPanelItemEvent.EVENT_TYPE_REMOVED,
                            selectedRow,
                            new LinkedList<>(this.resultTableModel.getEntities())));
                } catch (ListPanelItemEventVetoException ex) {
                    getMessageHandler().handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
                    return;
                }
            }
        }
    }

    public void reset() throws ResetException {
        reset0();
    }

    private void reset0() throws ResetException {
        this.resultTable.getModel().clear();
        this.resultTableModel.clear();
        if(initialValues != null) {
            try {
                this.resultTableModel.updateColumns(initialValues);
                this.resultTable.getModel().addAllEntities(initialValues); //before initComponents (simply use resultList for initialization)
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                throw new ResetException(ex);
            }
            for(E initialValue : initialValues) {
                if(!this.getStorage().isManaged(initialValue)) {
                    this.getQueryResultLabel().setText(String.format("previously managed entity %s has been removed from persistent storage, ignoring", initialValue));
                }
                int initialValueIndex = this.resultTable.getModel().getEntities().indexOf(initialValue);
                this.getQueryResultTableSelectionModel().addSelectionInterval(initialValueIndex, initialValueIndex); //no need to clear selection because we're just initializing
            }
        }
    }

    /**
     * The elements in the result table which have been previously added.
     * @return the list of selected entities of the query result
     */
    public List<E> getSelectedEntities() {
        return new LinkedList<>(resultTable.getModel().getEntities());
    }
}
