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
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.FieldInfo;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
    private final JTable queryResultTable;
    private final JScrollPane queryResultTableScrollPane;
    private final ListSelectionModel queryResultTableSelectionModel = new DefaultListSelectionModel();
    private final QueryComponent<E> queryComponent;
    /**
     * Pointer to the last query results received as QueryComponent event.
     */
    private final List<E> queryResults = new LinkedList<>();
    private final ReflectionFormBuilder reflectionFormBuilder;
    private final Class<?> entityClass;
    private final EntityManager entityManager;
    private final JSeparator separator;
    private final GroupLayout.SequentialGroup verticalSequentialGroup;
    private final GroupLayout.ParallelGroup horizontalParallelGroup;
    private final Map<Integer, String> queryResultTableTooltipTextMap = new HashMap<>();

    public AbstractQueryPanel(BidirectionalControlPanel bidirectionalControlPanel,
            QueryComponent<E> queryComponent,
            final ReflectionFormBuilder reflectionFormBuilder,
            Class<?> entityClass,
            EntityManager entityManager,
            int queryResultTableSelectionMode) {
        super();
        if(entityManager == null) {
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
        this.entityManager = entityManager;
        this.bidirectionalControlPanelSeparator = new JSeparator();
        this.separator = new JSeparator();
        queryResultLabel = new JLabel();
        queryResultTableScrollPane = new JScrollPane();
        queryResultTable = new JTable() {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        QueryComponent.validateEntityClass(entityClass, entityManager);

        queryResultLabel.setText("Query result:");
        queryResultTable.setModel(new DefaultTableModel());
        queryResultTable.setSelectionModel(this.queryResultTableSelectionModel);
        queryResultTableScrollPane.setViewportView(queryResultTable);
        this.queryResultTableSelectionModel.setSelectionMode(queryResultTableSelectionMode);
        this.queryComponent.addListener(new QueryComponentListener<E>() {
            @Override
            public void onQueryExecuted(QueryComponentEvent<E> event) {
                List<? extends E> queryResults = event.getQueryResults();
                AbstractQueryPanel.this.queryResults.clear();
                AbstractQueryPanel.this.queryResults.addAll(queryResults);
                DefaultTableModel queryResultModel = initTableModel(queryResults,
                        reflectionFormBuilder.getFieldRetriever());
                AbstractQueryPanel.this.queryResultTable.setModel(queryResultModel);
                for(E queryResult : queryResults) {
                    try {
                        AbstractQueryPanel.this.handleInstanceToTableModel(queryResultModel,
                                queryResult);
                    } catch (IllegalArgumentException | IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    }
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
                    .addComponent(bidirectionalControlPanelSeparator, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
        }
        verticalSequentialGroup.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(queryComponent)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(separator, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
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
                    ((JComponent)retValue).setToolTipText(queryResultTableTooltipTextMap.get(column));
                }
                return retValue;
            }
        });
    }

    private final Set<Class<?>> lastQueryClasses = new HashSet<>();

    /**
     * Checks whether it's necessary to add or remove columns from
     * {@code queryResultTableModel} after changing subtypes flag and eventually
     * including subclasses which weren't included in the last query result.
     *
     * Adds a discriminator column to {@code tableModel} of {@code queryResults}
     * contains more than one differentiable class.
     *
     * Since there's no way to remove columns once added to a
     * {@link DefaultTableModel} a model has to be recreated every time it ought
     * to be changed.
     *
     * @param tableModel
     * @param queryResults the query result returned from {@link QueryComponent}
     * (assumed to be of correct (sub)type(s) depending on whether
     * {@link QueryComponent} is configured to return subtypes or not)
     */
    /*
    internal implementation notes:
    - recreation of table model is tolerable effort and support KISS pattern
    */
    protected DefaultTableModel initTableModel(List<?> queryResults,
            FieldRetriever fieldRetriever) {
        DefaultTableModel tableModel = new DefaultTableModel();
        lastQueryClasses.clear();
        for(Object queryResult: queryResults) {
            if(!lastQueryClasses.contains(queryResult.getClass())) {
                lastQueryClasses.add(queryResult.getClass());
            }
        }
        if(lastQueryClasses.size() > 1) {
            tableModel.addColumn("Type");
        }

        Set<Field> seenFields = new HashSet<>();
        int i=1;
        for(Class<?> lastQueryClass : lastQueryClasses) {
            for(Field field : fieldRetriever.retrieveRelevantFields(lastQueryClass)) {
                if(seenFields.contains(field)) {
                    continue;
                }
                FieldInfo fieldInfo = field.getAnnotation(FieldInfo.class);
                if(fieldInfo != null) {
                    tableModel.addColumn(fieldInfo.name());
                    queryResultTableTooltipTextMap.put(i, fieldInfo.description());
                }else {
                    tableModel.addColumn(field.getName());
                    queryResultTableTooltipTextMap.put(i, "");
                }
                i++;
                seenFields.add(field);
            }
        }
        return tableModel;
    }

    protected void handleInstanceToTableModel(DefaultTableModel tableModel,
            Object queryResult) throws IllegalArgumentException,
            IllegalAccessException {
        List<Object> queryResultValues = new LinkedList<>();
        for(Field field : reflectionFormBuilder.getFieldRetriever().retrieveRelevantFields(entityClass)) {
            queryResultValues.add(field.get(queryResult));
        }
        tableModel.addRow(queryResultValues.toArray(new Object[queryResultValues.size()]));
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

    /**
     *
     * @return
     */
    /*
    internal implementation notes:
    - return value must not be unmodifiable because subclasses might want to
    clean queryResults
    */
    public List<E> getQueryResults() {
        return queryResults;
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

    public JTable getQueryResultTable() {
        return queryResultTable;
    }

    public JScrollPane getQueryResultTableScrollPane() {
        return queryResultTableScrollPane;
    }

    public EntityManager getEntityManager() {
        return entityManager;
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
}
