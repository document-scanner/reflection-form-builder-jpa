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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.panels.AbstractListPanel;
import richtercloud.reflection.form.builder.panels.ListPanelItemEvent;
import richtercloud.reflection.form.builder.panels.ListPanelItemListener;

/**
 * Provides a {@link QueryPanel} and controls to add and remove stored
 * entities from a list.
 *
 * Currently there're no editing facilities.
 *
 * @author richter
 */
/*
internal implementtion notes:
- can't be used in NetBeans GUI builder because it requires a zero-argument
constructor in QueryPanel which is used as imported component here -> the
requirement to learn GroupLayout is very legitimate
*/
public class QueryListPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final static Logger LOGGER = LoggerFactory.getLogger(QueryListPanel.class);
    private DefaultTableModel resultTableModel = new DefaultTableModel();
    private EntityManager entityManager;
    private ReflectionFormBuilder reflectionFormBuilder;
    private Class<?> entityClass;
    private Set<ListPanelItemListener<Object>> updateListeners = new HashSet<>();
    /**
     * A constantly up-to-date list of selected references (the "result" of the
     * panel)
     */
    private List<Object> resultList = new LinkedList<>();
    private JButton addButton;
    private QueryPanel<Object> queryPanel;
    private JButton removeButton;
    private JTable resultTable;
    private JLabel resultTableLabel;
    private JScrollPane resultTableScrollPane;

    public QueryListPanel(EntityManager entityManager,
            ReflectionFormBuilder reflectionFormBuilder,
            Class<?> entityClass,
            List<Object> initialValues) throws IllegalArgumentException, IllegalAccessException {
        if(entityManager == null) {
            throw new IllegalArgumentException("entityManager mustn't be null");
        }
        if(reflectionFormBuilder == null) {
            throw new IllegalArgumentException("reflectionFormBuilder mustn't be null");
        }
        if(entityClass == null) {
            throw new IllegalArgumentException("entityClass mustn't be null");
        }
        QueryPanel.validateEntityClass(entityClass, entityManager);
        this.entityManager = entityManager;
        this.reflectionFormBuilder = reflectionFormBuilder;
        this.entityClass = entityClass;
        QueryPanel.initTableModel(this.resultTableModel, this.reflectionFormBuilder.getFieldRetriever().retrieveRelevantFields(entityClass));
        if(initialValues != null) {
            this.resultList.addAll(initialValues); //before initComponents (simply use resultList for initialization)
        }
        initComponents();
        this.resultTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    public void addItemListener(ListPanelItemListener<Object> updateListener) {
        this.updateListeners.add(updateListener);
    }

    public void removeItemListener(ListPanelItemListener<Object> updateListener) {
        this.updateListeners.remove(updateListener);
    }

    private QueryPanel<Object> createQueryPanel() throws IllegalArgumentException, IllegalAccessException {
        return new QueryPanel<>(entityManager,
                entityClass,
                reflectionFormBuilder,
                null, //initialValue
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() throws IllegalArgumentException, IllegalAccessException {

        queryPanel = createQueryPanel();
        removeButton = new JButton();
        addButton = new JButton();
        resultTableLabel = new JLabel();
        resultTableScrollPane = new JScrollPane();
        resultTable = new JTable() {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

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

        resultTable.setModel(this.resultTableModel);
        resultTableScrollPane.setViewportView(resultTable);

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(queryPanel, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(resultTableScrollPane, GroupLayout.DEFAULT_SIZE, 573, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(resultTableLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(addButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(removeButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(queryPanel, GroupLayout.PREFERRED_SIZE, 222, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(removeButton)
                    .addComponent(addButton)
                    .addComponent(resultTableLabel))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(resultTableScrollPane, GroupLayout.DEFAULT_SIZE, 167, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int[] indices = this.queryPanel.getQueryResultTable().getSelectedRows();
        for(int index : indices) {
            Object queryResult = this.queryPanel.getQueryResults().get(index);
            try {
                QueryPanel.handleInstanceToTableModel(this.resultTableModel, queryResult, reflectionFormBuilder, entityClass);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                LOGGER.info("an exception occured while executing the query", ex);
                this.queryPanel.getQueryStatusLabel().setText(String.format("<html>%s</html>", ex.getMessage()));
            }
            this.resultList.add(queryResult);
            for(ListPanelItemListener<Object> updateListener : updateListeners) {
                updateListener.onItemAdded(new ListPanelItemEvent<>(ListPanelItemEvent.EVENT_TYPE_ADDED, index, resultList));
            }
        }
    }

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
            this.resultTableModel.removeRow(selectedRow);
            Object queryResult = this.queryPanel.getQueryResults().get(selectedRow);
            this.resultList.remove(queryResult);
            for(ListPanelItemListener<Object> updateListener : updateListeners) {
                updateListener.onItemRemoved(new ListPanelItemEvent<>(ListPanelItemEvent.EVENT_TYPE_REMOVED, selectedRow, this.resultList));
            }
        }
    }
}
