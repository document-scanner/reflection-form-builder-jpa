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

import java.awt.LayoutManager;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.swing.DefaultListSelectionModel;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import static richtercloud.reflection.form.builder.jpa.panels.QueryPanel.handleInstanceToTableModel;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * The base class for {@link QueryPanel} and {@link QueryListPanel}.
 *
 * @author richter
 */
public abstract class AbstractQueryPanel<E> extends JPanel {
    private static final long serialVersionUID = 1L;
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractQueryPanel.class);
    private Set<QueryPanelUpdateListener> updateListeners = new HashSet<>();
    private final JSeparator bidirectionalControlPanelSeparator;
    private final BidirectionalControlPanel bidirectionalControlPanel;
    private final JLabel queryResultLabel;
    private final JTable queryResultTable;
    private final JScrollPane queryResultTableScrollPane;
    private ListSelectionModel queryResultTableSelectionModel = new DefaultListSelectionModel();
    /*
    internal implementation notes:
    - set a model stub initially, overwrite in construction in order to allow
    initialization with parameterless constructor and initComponents
    */
    private DefaultTableModel queryResultTableModel = new DefaultTableModel();
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

    public AbstractQueryPanel(BidirectionalControlPanel bidirectionalControlPanel,
            QueryComponent<E> queryComponent,
            ReflectionFormBuilder reflectionFormBuilder,
            Class<?> entityClass,
            EntityManager entityManager,
            int queryResultTableSelectionMode) {
        super();
        this.bidirectionalControlPanel = bidirectionalControlPanel;
        this.queryComponent = queryComponent;
        this.reflectionFormBuilder = reflectionFormBuilder;
        this.entityClass = entityClass;
        this.entityManager = entityManager;
        this.bidirectionalControlPanelSeparator = new JSeparator();
        this.separator = new JSeparator();
        this.queryResultTableSelectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                for(QueryPanelUpdateListener updateListener : updateListeners) {
                    LOGGER.debug("notifying update listener {} about selection change", updateListener);
                    updateListener.onUpdate(new QueryPanelUpdateEvent(AbstractQueryPanel.this.queryResults.get(e.getFirstIndex()),
                            AbstractQueryPanel.this));
                }
            }
        });
        queryResultLabel = new JLabel();
        queryResultTableScrollPane = new JScrollPane();
        queryResultTable = new JTable() {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        queryResultLabel.setText("Query result:");
        queryResultTable.setModel(this.queryResultTableModel);
        queryResultTable.setSelectionModel(this.queryResultTableSelectionModel);
        queryResultTableScrollPane.setViewportView(queryResultTable);
        this.queryResultTableSelectionModel.setSelectionMode(queryResultTableSelectionMode);
        this.queryComponent.addListener(new QueryComponentListener<E>() {
            @Override
            public void onQueryExecuted(QueryComponentEvent<E> event) {
                List<E> queryResults = event.getQueryResults();
                while(AbstractQueryPanel.this.queryResultTableModel.getRowCount() > 0) {
                    AbstractQueryPanel.this.queryResultTableModel.removeRow(0);
                }
                for(E queryResult : queryResults) {
                    try {
                        handleInstanceToTableModel(AbstractQueryPanel.this.queryResultTableModel,
                                queryResult,
                                AbstractQueryPanel.this.reflectionFormBuilder,
                                AbstractQueryPanel.this.entityClass);
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
    constructor is called
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
        return updateListeners;
    }

    public void clearSelection() {
        this.queryResultTable.clearSelection();
    }

    public QueryComponent<E> getQueryComponent() {
        return queryComponent;
    }

    public DefaultTableModel getQueryResultTableModel() {
        return queryResultTableModel;
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
