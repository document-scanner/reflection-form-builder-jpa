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
package richtercloud.reflection.form.builder.jpa;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.swing.ComboBoxEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richter
 * @param <E> a generic type for the entity class
 */
public class QueryPanel<E> extends javax.swing.JPanel {
    private static final long serialVersionUID = 1L;
    private final static Logger LOGGER = LoggerFactory.getLogger(QueryPanel.class);
    private EntityManager entityManager;
    private Class<E> entityClass;
    /*
    internal implementation notes:
    - set a model stub initially, overwrite in construction in order to allow
    initialization with parameterless constructor and initComponents
    */
    private DefaultTableModel queryResultTableModel = new DefaultTableModel();
    /**
     * in order to guarantee the order to fields (which is not guaranteed by
     * {@link Class#getDeclaredFields() }, save the initial invokation here and
     * rely to it
     */
    private List<Field> entityClassFields;
    /**
     * the default value for the initial query limit (see {@link #QueryPanel(javax.persistence.EntityManager, java.lang.Class, int) } for details
     */
    public static final int INITIAL_QUERY_LIMIT_DEFAULT = 20;
    private final static Comparator<HistoryEntry> QUERY_HISTORY_COMPARATOR_USAGE = new Comparator<HistoryEntry>() {
        @Override
        public int compare(HistoryEntry o1, HistoryEntry o2) {
            return Integer.compare(o1.getUsageCount(), o2.getUsageCount());
        }
    };
    private final static Comparator<HistoryEntry> QUERY_HISTORY_COMPARATOR_DATE = new Comparator<HistoryEntry>() {
        @Override
        public int compare(HistoryEntry o1, HistoryEntry o2) {
            return o1.getLastUsage().compareTo(o2.getLastUsage());
        }
    };

    /*
    internal implementation notes:
    - due to the fact that the interface defines index based methods, a
    PriorityQueue can't be used for item storage -> use a List and List.sort at
    every model change
    */
    private class SortedComboBoxModel<E> extends DefaultComboBoxModel<E> {
        private static final long serialVersionUID = 1L;
        private final List<E> items;
        private final Comparator<E> comparator;

        /*
        internal implementation notes:
        - comparator can only be assigned at instantiation of PriorityQueue, so
        it has to be set here
        */
        SortedComboBoxModel(Comparator<E> comparator) {
            this.comparator = comparator;
            this.items = new ArrayList<>();
        }

        @Override
        public void addElement(E item) {
            this.items.add(item);
            Collections.sort(this.items, this.comparator);
            super.addElement(item);
        }

        @Override
        public void removeElement(Object obj) {
            this.items.remove(obj);
            super.removeElement(obj);
        }

        @Override
        public void insertElementAt(E item, int index) {
            this.items.add(index, item);
            Collections.sort(this.items, this.comparator);
            super.insertElementAt(item, index);
        }

        @Override
        public void removeElementAt(int index) {
            this.items.remove(index);
            super.removeElementAt(index);
        }

        @Override
        public int getSize() {
            return this.items.size();
        }

        @Override
        public E getElementAt(int index) {
            return this.items.get(index);
        }

        List<E> getItems() {
            return Collections.unmodifiableList(this.items);
        }
    }

    private final SortedComboBoxModel<HistoryEntry> queryComboBoxModel = new SortedComboBoxModel<>(QUERY_HISTORY_COMPARATOR_USAGE);

    /**
     * Creates new form QueryPanel
     */
    public QueryPanel() {
        this.initComponents();
    }

    @SuppressWarnings("unchecked")
    public QueryPanel(EntityManager entityManager, List<Field> entityClassFields, Class<E> entityClass) {
        this(entityManager, entityClassFields, entityClass, Collections.EMPTY_LIST, INITIAL_QUERY_LIMIT_DEFAULT);
    }

    /**
     *
     * @param entityManager
     * @param entityClassFields
     * @param entityClass
     * @param initialHistory might be modified
     * @param initialQueryLimit When the component is created an initial query is executed. This property
     * limits its result length. Set to {@code 0} in order to skip initial
     * query.
     */
    public QueryPanel(EntityManager entityManager, List<Field> entityClassFields, Class<E> entityClass, List<HistoryEntry> initialHistory, int initialQueryLimit) {
        this();
        this.init0(entityManager, entityClassFields, entityClass, initialHistory, initialQueryLimit);
    }

    private final ComboBoxEditor QUERY_COMBO_BOX_EDITOR = new ComboBoxEditor() {
        private final JTextField editorComponent = new JTextField();
        private HistoryEntry item;
        private final Set<ActionListener> actionListeners = new HashSet<>();

        @Override
        public Component getEditorComponent() {
            return this.editorComponent;
        }

        @Override
        public void setItem(Object anObject) {
            assert anObject instanceof HistoryEntry;
            this.item = (HistoryEntry) anObject;
            if(this.item != null) {
                this.editorComponent.setText(this.item.getText());
            }
        }

        @Override
        public Object getItem() {
            return this.item;
        }

        @Override
        public void selectAll() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void addActionListener(ActionListener l) {
            this.actionListeners.add(l);
        }

        @Override
        public void removeActionListener(ActionListener l) {
            this.actionListeners.remove(l);
        }
    };

    private void init0(EntityManager entityManager, List<Field> entityClassFields, Class<E> entityClass, List<HistoryEntry> initialHistory, int initialQueryLimit) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
        for(HistoryEntry initialHistoryEntry : initialHistory) {
            this.queryComboBoxModel.addElement(initialHistoryEntry);
        }
        this.entityClassFields = entityClassFields;
        this.initTableModel(this.entityClassFields);
        this.queryLabel.setText(String.format("%s query:", entityClass.getSimpleName()));
        CriteriaQuery<E> criteriaQuery = entityManager.getCriteriaBuilder().createQuery(entityClass);
        Root<E> criteriaRoot = criteriaQuery.from(entityClass);
        criteriaQuery.select(criteriaRoot);
        TypedQuery<E> query = entityManager.createQuery(criteriaQuery).setMaxResults(initialQueryLimit);
        this.executeQuery(query);
    }

    public void init(EntityManager entityManager, List<Field> entityClassFields, Class<E> entityClass, List<HistoryEntry> initialHistory, int initialQueryLimit) {
        this.init0(entityManager, entityClassFields, entityClass, initialHistory, initialQueryLimit);
    }

    private void initTableModel(List<Field> entityClassFields) {
        for(Field field : entityClassFields) {
            this.queryResultTableModel.addColumn(field.getName());
        }
    }

    public List<HistoryEntry> getQueryHistory() {
        return new LinkedList<>(this.queryComboBoxModel.getItems());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        queryLabel = new javax.swing.JLabel();
        queryButton = new javax.swing.JButton();
        separator = new javax.swing.JSeparator();
        queryResultLabel = new javax.swing.JLabel();
        queryResultTableScrollPane = new javax.swing.JScrollPane();
        queryResultTable = new javax.swing.JTable();
        queryStatusLabel = new javax.swing.JLabel();
        queryComboBox = new javax.swing.JComboBox();

        queryLabel.setText("Query:");

        queryButton.setText("Run query");
        queryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                queryButtonActionPerformed(evt);
            }
        });

        queryResultLabel.setText("Query result:");

        queryResultTable.setModel(queryResultTableModel);
        queryResultTableScrollPane.setViewportView(queryResultTable);

        queryStatusLabel.setText(" ");

        queryComboBox.setEditable(true);
        queryComboBox.setModel(queryComboBoxModel);
        queryComboBox.setEditor(QUERY_COMBO_BOX_EDITOR);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(queryStatusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(queryLabel)
                        .addGap(18, 18, 18)
                        .addComponent(queryComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(queryButton))
                    .addComponent(separator)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(queryResultLabel)
                        .addGap(0, 297, Short.MAX_VALUE))
                    .addComponent(queryResultTableScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(queryLabel)
                    .addComponent(queryButton)
                    .addComponent(queryComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(queryStatusLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(separator, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(queryResultLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(queryResultTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void queryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_queryButtonActionPerformed
        Object queryComboBoxSelectedItem = this.queryComboBox.getSelectedItem();
        assert queryComboBoxSelectedItem instanceof HistoryEntry;
        HistoryEntry selectedHistoryEntry = (HistoryEntry) queryComboBoxSelectedItem;
        String queryText = selectedHistoryEntry.getText();
        TypedQuery<E> query = this.createQuery(queryText);
        if(query != null) {
            this.executeQuery(query);
            this.queryComboBoxModel.addElement(new HistoryEntry(queryText, 1, new Date()));
        }
    }//GEN-LAST:event_queryButtonActionPerformed

    /**
     * executes {@code query}
     * @param query
     */
    /*
    internal implementation notes:
    - it'd be nice to log the text of the query, but that's no possible because
    JPA doesn't allow retrieval of text from TypedQuery object; passing a
    String with the text (dirty because used by logger exclusively) doesn't work
    neither because there're cases (e.g. in constructor where no String of the
    query exists)
    */
    private void executeQuery(TypedQuery<E> query) {
        while(this.queryResultTableModel.getRowCount() > 0) {
            this.queryResultTableModel.removeRow(0);
        }
        try {
            List<E> queryResults = query.getResultList();
            for(E queryResult : queryResults) {
                List<Object> queryResultValues = new LinkedList<>();
                for(Field field : this.entityClassFields) {
                    queryResultValues.add(field.get(queryResult));
                }
                this.queryResultTableModel.addRow(queryResultValues.toArray(new Object[queryResultValues.size()]));
            }
            this.queryStatusLabel.setText("Query executed successfully.");
        }catch(Exception ex) {
            LOGGER.info("an exception occured while executing the query", ex);
            this.queryStatusLabel.setText(String.format("<html>%s</html>", ex.getMessage()));
        }
    }

    private TypedQuery<E> createQuery(String queryText) {
        try {
            TypedQuery<E> query = this.entityManager.createQuery(queryText, this.entityClass);
            return query;
        }catch(Exception ex) {
            LOGGER.info("an exception occured while executing the query", ex);
            this.queryStatusLabel.setText(String.format("<html>%s</html>", ex.getMessage()));
        }
        return null;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton queryButton;
    private javax.swing.JComboBox queryComboBox;
    private javax.swing.JLabel queryLabel;
    private javax.swing.JLabel queryResultLabel;
    private javax.swing.JTable queryResultTable;
    private javax.swing.JScrollPane queryResultTableScrollPane;
    private javax.swing.JLabel queryStatusLabel;
    private javax.swing.JSeparator separator;
    // End of variables declaration//GEN-END:variables
}
