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

import java.awt.Component;
import java.awt.LayoutManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.DefaultListSelectionModel;
import javax.swing.GroupLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.message.handler.MessageHandler;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.reflection.form.builder.jpa.storage.FieldInitializer;

/**
 * The base class for {@link QueryPanel} and {@link QueryListPanel}.
 *
 * @author richter
 * @param <E>
 */
public abstract class AbstractQueryPanel<E> extends JPanel {
    private static final long serialVersionUID = 1L;
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractQueryPanel.class);
    public final static int QUERY_RESULT_TABLE_HEIGHT_DEFAULT = 100;
    private final Set<QueryPanelUpdateListener> updateListeners = new HashSet<>();
    private final JSeparator bidirectionalControlPanelSeparator;
    private final JLabel queryResultLabel;
    private final EntityTable<E> queryResultTable;
    private final JScrollPane queryResultTableScrollPane;
    private final ListSelectionModel queryResultTableSelectionModel = new DefaultListSelectionModel();
    private final QueryComponent<E> queryComponent;
    private final ReflectionFormBuilder reflectionFormBuilder;
    private final Class<?> entityClass;
    private final PersistenceStorage storage;
    private final JSeparator separator;
    private final GroupLayout.SequentialGroup verticalSequentialGroup;
    private final GroupLayout.ParallelGroup horizontalParallelGroup;
    private final BidirectionalControlPanel bidirectionalControlPanel;
    private final MessageHandler messageHandler;
    private final List<E> initialValues;

    /**
     * Creates an {@code AbstractQueryPanel}.
     * @param bidirectionalControlPanel
     * @param queryComponent
     * @param reflectionFormBuilder
     * @param entityClass
     * @param entityManager
     * @param messageHandler
     * @param queryResultTableSelectionMode
     * @param initialValues the initial values (to be selected in the query
     * result if they're present) (subclasses which only support one selected
     * value should pass this item in a list - KISS)
     */
    public AbstractQueryPanel(BidirectionalControlPanel bidirectionalControlPanel,
            QueryComponent<E> queryComponent,
            final ReflectionFormBuilder reflectionFormBuilder,
            final Class<?> entityClass,
            PersistenceStorage storage,
            FieldInitializer fieldInitializer,
            MessageHandler messageHandler,
            int queryResultTableSelectionMode,
            List<E> initialValues) {
        super();
        if(storage == null) {
            throw new IllegalArgumentException("entityManager mustn't be null");
        }
        if(reflectionFormBuilder == null) {
            throw new IllegalArgumentException("reflectionFormBuilder mustn't be null");
        }
        if(entityClass == null) {
            throw new IllegalArgumentException("entityClass mustn't be null");
        }
        this.queryComponent = queryComponent;
        this.reflectionFormBuilder = reflectionFormBuilder;
        this.entityClass = entityClass;
        this.storage = storage;
        if(messageHandler == null) {
            throw new IllegalArgumentException("messageHandler mustn't be null");
        }
        this.messageHandler = messageHandler;
        this.initialValues = initialValues;
        this.bidirectionalControlPanelSeparator = new JSeparator();
        this.separator = new JSeparator();
        queryResultLabel = new JLabel();
        queryResultTableScrollPane = new JScrollPane();
        try {
            queryResultTable = new EntityTable<E>(new EntityTableModel<E>(reflectionFormBuilder.getFieldRetriever())) {
                private static final long serialVersionUID = 1L;
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }

        QueryComponent.validateEntityClass(entityClass,
                storage);

        queryResultLabel.setText("Query result:");
        queryResultTable.setSelectionModel(this.queryResultTableSelectionModel);
        queryResultTableScrollPane.setViewportView(queryResultTable);
        this.queryResultTableSelectionModel.setSelectionMode(queryResultTableSelectionMode);
        this.queryComponent.addListener(new QueryComponentListener<E>() {
            @Override
            public void onQueryExecuted(QueryComponentEvent<E> event) {
                List<E> queryResults = event.getQueryResults();
                EntityTableModel<E> queryResultModel;
                try {
                    for(E queryResult : queryResults) {
                        fieldInitializer.initialize(queryResult);
                            //every result retrieved for the query should be
                            //initialized
                    }
                    queryResultModel = new EntityTableModel<>(queryResults,
                            reflectionFormBuilder.getFieldRetriever());
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
                AbstractQueryPanel.this.queryResultTable.setModel(queryResultModel);
                for(E initialValue : AbstractQueryPanel.this.initialValues) {
                    int initialValueIndex = queryResultModel.getEntities().indexOf(initialValue);
                    AbstractQueryPanel.this.queryResultTable.getSelectionModel().addSelectionInterval(initialValueIndex,
                            initialValueIndex);
                }
            }
        });

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        this.horizontalParallelGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        if(bidirectionalControlPanel != null) {
            horizontalParallelGroup.addComponent(bidirectionalControlPanel)
                    .addGap(18, 18, 18)
                    .addComponent(bidirectionalControlPanelSeparator);
        }
        horizontalParallelGroup
                .addComponent(queryComponent)
                .addComponent(separator)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(queryResultLabel)
                        .addGap(0, 0, Short.MAX_VALUE));

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(horizontalParallelGroup)
                                .addContainerGap())
        );
        this.verticalSequentialGroup = layout.createSequentialGroup();
        if(bidirectionalControlPanel != null) {
            verticalSequentialGroup.addComponent(bidirectionalControlPanel)
                    .addGap(18, 18, 18)
                    .addComponent(bidirectionalControlPanelSeparator,
                            GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE);
        }
        verticalSequentialGroup.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(queryComponent)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(separator,
                        GroupLayout.PREFERRED_SIZE,
                        10,
                        GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(queryResultLabel);
        layout.setVerticalGroup(verticalSequentialGroup);

        final TableCellRenderer tableCellRenderer = queryResultTable.getTableHeader().getDefaultRenderer();
        queryResultTable.getTableHeader().setDefaultRenderer(new TableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object o, boolean isSelected, boolean hasFocus, int row, int column) {
                Component retValue = tableCellRenderer.getTableCellRendererComponent(table, o, isSelected, hasFocus, row, column);
                if(retValue instanceof JComponent) {
                    //might not be the case for some Look and Feels
                    ((JComponent)retValue).setToolTipText(queryResultTable.getModel().getTooltipTextMap().get(column));
                }
                return retValue;
            }
        });
        queryResultTable.setDefaultRenderer(byte[].class,
                new DefaultTableCellRenderer() {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        assert value instanceof byte[];
                        byte[] valueCast = (byte[]) value;
                        return super.getTableCellRendererComponent(table,
                                String.format("%d bytes binary data", valueCast.length),
                                isSelected,
                                hasFocus,
                                row,
                                column);
                    }
                });
        //handle initialization of lazily fetched fields in
        //PersistenceStorage.initialize because the table cell renderer has no
        //knowledge of the field the value belongs to (and there's no sense in
        //making the effort to get it this knowledge)
        //In case this is reverted at any point in time note:
        //Some JPA implementations like EclipseLink use IndirectList which
        //implements Collection, but doesn't trigger the default renderer for
        //Collection.class -> register for Object and check type in conditional
        //statements
        this.bidirectionalControlPanel = bidirectionalControlPanel;
    }

    /**
     *
     * @param mgr
     */
    /*
    internal implementation notes:
    - can't add a check whether mgr is a GroupLayout and fail with
    IllegalArgumentException because setLayout is called in JPanel's constructor
    and requires a reference to this which can't be referenced because super
    constructor is called -> keep this implementation as marker for this note
    */
    @Override
    public void setLayout(LayoutManager mgr) {
        super.setLayout(mgr);
    }

    @Override
    public GroupLayout getLayout() {
        return (GroupLayout) super.getLayout();
    }

    public GroupLayout.SequentialGroup getVerticalSequentialGroup() {
        return verticalSequentialGroup;
    }

    public GroupLayout.ParallelGroup getHorizontalParallelGroup() {
        return horizontalParallelGroup;
    }

    public void addUpdateListener(QueryPanelUpdateListener updateListener) {
        this.updateListeners.add(updateListener);
    }

    public void removeUpdateListener(QueryPanelUpdateListener updateListener) {
        this.updateListeners.remove(updateListener);
    }

    public Set<QueryPanelUpdateListener> getUpdateListeners() {
        return Collections.unmodifiableSet(updateListeners);
    }

    public void clearSelection() {
        this.queryResultTable.clearSelection();
    }

    public QueryComponent<E> getQueryComponent() {
        return queryComponent;
    }

    public EntityTable<E> getQueryResultTable() {
        return queryResultTable;
    }

    public JScrollPane getQueryResultTableScrollPane() {
        return queryResultTableScrollPane;
    }

    public PersistenceStorage getStorage() {
        return storage;
    }

    public JLabel getQueryResultLabel() {
        return queryResultLabel;
    }

    public ReflectionFormBuilder getReflectionFormBuilder() {
        return reflectionFormBuilder;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public ListSelectionModel getQueryResultTableSelectionModel() {
        return queryResultTableSelectionModel;
    }

    public BidirectionalControlPanel getBidirectionalControlPanel() {
        return bidirectionalControlPanel;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    /**
     * Runs the query on the {@link QueryComponent}.
     */
    public void runQuery(boolean async) {
        getQueryComponent().runQuery(async);
    }
}
