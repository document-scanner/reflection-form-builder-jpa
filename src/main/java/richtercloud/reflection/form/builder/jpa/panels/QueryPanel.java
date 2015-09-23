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

import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
import javax.swing.ComboBoxEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.jpa.HistoryEntry;

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
    private final SpinnerModel queryLimitSpinnerModel = new SpinnerNumberModel(INITIAL_QUERY_LIMIT_DEFAULT, //value
            1, //min
            null, //max
            1 //stepSize
    );

    private final SortedComboBoxModel<HistoryEntry> queryComboBoxModel = new SortedComboBoxModel<>(QUERY_HISTORY_COMPARATOR_USAGE);
    /**
     * The result of the current query (needs to be stored separately because the table model is used to display field values, not instances)
     */
    private List<E> queryResults;
    private final QueryComboBoxEditor queryComboBoxEditor = new QueryComboBoxEditor();
    private ReflectionFormBuilder reflectionFormBuilder;
    private ListSelectionModel queryResultTableSelectionModel = new DefaultListSelectionModel();
    private Set<QueryPanelUpdateListener> updateListeners = new HashSet<>();

    /**
     * Creates new form QueryPanel
     */
    /*
    internal implementation notes:
    - public constructor necessary for NetBeans GUI builder (protected no
    sufficient for use in other projects)
    */
    public QueryPanel() {
        this.initComponents();
        this.queryResultTableSelectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                for(QueryPanelUpdateListener updateListener : updateListeners) {
                    LOGGER.debug("notifying update listener {} about selection change", updateListener);
                    updateListener.onUpdate(new QueryPanelUpdateEvent(QueryPanel.this.queryResults.get(e.getFirstIndex())));
                }
            }
        });
    }
    public QueryPanel(EntityManager entityManager,
            Class<E> entityClass,
            ReflectionFormBuilder reflectionFormBuilder) {
        this(entityManager, entityClass, reflectionFormBuilder, ListSelectionModel.SINGLE_SELECTION);
    }

    protected QueryPanel(EntityManager entityManager,
            Class<E> entityClass,
            ReflectionFormBuilder reflectionFormBuilder,
            int queryResultTableSelectionMode) {
        this(entityManager,
                entityClass,
                reflectionFormBuilder,
                queryResultTableSelectionMode,
                new LinkedList<HistoryEntry>(),
                INITIAL_QUERY_LIMIT_DEFAULT);
    }

    public QueryPanel(EntityManager entityManager,
            Class<E> entityClass,
            ReflectionFormBuilder reflectionFormBuilder,
            List<HistoryEntry> initialHistory,
            int initialQueryLimit) {
        this(entityManager, entityClass, reflectionFormBuilder, ListSelectionModel.SINGLE_SELECTION, initialHistory, initialQueryLimit);
    }

    /**
     *
     * @param entityManager
     * @param entityClass
     * @param reflectionFormBuilder
     * @param queryResultTableSelectionMode
     * @param initialHistory might be modified
     * @param initialQueryLimit When the component is created an initial query is executed. This property
     * limits its result length. Set to {@code 0} in order to skip initial
     * query.
     */
    protected QueryPanel(EntityManager entityManager,
            Class<E> entityClass,
            ReflectionFormBuilder reflectionFormBuilder,
            int queryResultTableSelectionMode,
            List<HistoryEntry> initialHistory,
            int initialQueryLimit) {
        this();
        this.entityManager = entityManager;
        this.entityClass = entityClass;
        this.reflectionFormBuilder = reflectionFormBuilder;
        for(HistoryEntry initialHistoryEntry : initialHistory) {
            this.queryComboBoxModel.addElement(initialHistoryEntry);
        }
        initTableModel(this.queryResultTableModel, this.reflectionFormBuilder.retrieveRelevantFields(entityClass));
        this.queryLabel.setText(String.format("%s query:", entityClass.getSimpleName()));
        //Criteria API doesn't allow retrieval of string/text from objects
        //created with CriteriaBuilder, but text should be the first entry in
        //the query combobox -> construct String instead of using
        //CriteriaBuilder
        String entityClassQueryIdentifier = String.valueOf(Character.toLowerCase(entityClass.getSimpleName().charAt(0)));
        String queryText = String.format("SELECT %s from %s %s", entityClassQueryIdentifier, entityClass.getSimpleName(), entityClassQueryIdentifier);
        TypedQuery<E> query = entityManager.createQuery(queryText, entityClass);
        this.executeQuery(query, initialQueryLimit, queryText);
        this.queryResultTableSelectionModel.setSelectionMode(queryResultTableSelectionMode);
    }

    protected static void initTableModel(DefaultTableModel tableModel, List<Field> entityClassFields) {
        for(Field field : entityClassFields) {
            tableModel.addColumn(field.getName());
        }
    }

    public List<HistoryEntry> getQueryHistory() {
        return new LinkedList<>(this.queryComboBoxModel.getItems());
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

    public Object getSelectedObject() {
        int index = this.queryResultTable.getSelectedRow();
        if(index < 0) {
            //can happen during layout validation/initialization
            return null;
        }
        //assume that if index is >= 0 that this.queryResults is != null as well
        Object retValue = this.getQueryResults().get(index);
        return retValue;
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
        queryStatusLabel = new javax.swing.JLabel();
        queryComboBox = new javax.swing.JComboBox<HistoryEntry>();
        queryLimitSpinner = new javax.swing.JSpinner();
        queryLimitLabel = new javax.swing.JLabel();
        queryResultTableScrollPane = new javax.swing.JScrollPane();
        queryResultTable = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        queryLabel.setText("Query:");

        queryButton.setText("Run query");
        queryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                queryButtonActionPerformed(evt);
            }
        });

        queryResultLabel.setText("Query result:");

        queryStatusLabel.setText(" ");

        queryComboBox.setEditable(true);
        queryComboBox.setModel(queryComboBoxModel);
        queryComboBox.setEditor(queryComboBoxEditor);

        queryLimitSpinner.setModel(queryLimitSpinnerModel);
        queryLimitSpinner.setValue(INITIAL_QUERY_LIMIT_DEFAULT);

        queryLimitLabel.setText("# of Results");

        queryResultTable.setModel(this.queryResultTableModel);
        queryResultTable.setSelectionModel(this.queryResultTableSelectionModel);
        queryResultTableScrollPane.setViewportView(queryResultTable);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(queryResultTableScrollPane)
                    .addComponent(queryStatusLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(queryLabel)
                        .addGap(18, 18, 18)
                        .addComponent(queryComboBox, 0, 285, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(queryLimitLabel)
                        .addGap(18, 18, 18)
                        .addComponent(queryLimitSpinner, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(queryButton))
                    .addComponent(separator, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(queryResultLabel)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(queryLabel)
                    .addComponent(queryButton)
                    .addComponent(queryComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(queryLimitSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(queryLimitLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(queryStatusLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(separator, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(queryResultLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(queryResultTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 352, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void queryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_queryButtonActionPerformed
        //although queryComboBox's model should never be empty in the current
        //implementation, there's check nevertheless so that future changes
        //don't cause too much trouble (adding a function to delete history
        //makes sense)
        String queryText;
        //there's no good way to tell if the ComboBox is currently being edited
        //(the JComboBox doesn't know and thus doesn't change the selected item
        //and selected index property and the editor can't tell because it
        //doesn't know the model)
        HistoryEntry queryComboBoxEditorItem = queryComboBoxEditor.getItem();
        if(queryComboBoxEditorItem != null && !queryComboBoxModel.contains(queryComboBoxEditorItem)) {
            queryText = queryComboBoxEditorItem.getText();
            if(queryText == null || queryText.isEmpty()) {
                queryStatusLabel.setText("Enter a query");
                return;
            }
        }else {
            HistoryEntry selectedHistoryEntry = this.queryComboBox.getItemAt(this.queryComboBox.getSelectedIndex());
            queryText = selectedHistoryEntry.getText();
        }
        int queryLimit = (int) queryLimitSpinner.getValue();
        TypedQuery<E> query = this.createQuery(queryText); //handles displaying
            //exceptions which occured during query execution (explaining
            //syntax errors)
        if(query != null) {
            this.executeQuery(query, queryLimit, queryText);
        }
    }//GEN-LAST:event_queryButtonActionPerformed

    /**
     * Executes JQPL query {@code query}. Creates a {@link HistoryEntry} in the
     * {@code queryComboBoxModel} and resets the current item of
     * {@code queryComboBoxEditor}.
     * @param query
     */
    /*
    internal implementation notes:
    - in order to produce HistoryEntrys from every query it's necessary to pass
    the text of the query because there's no way to retrieve text from Criteria
    objects
    */
    private void executeQuery(TypedQuery<E> query, int queryLimit, String queryText) {
        LOGGER.debug("executing query '{}'", queryText);
        while(this.queryResultTableModel.getRowCount() > 0) {
            this.queryResultTableModel.removeRow(0);
        }
        try {
            queryResults = query.setMaxResults(queryLimit).getResultList();
            for(E queryResult : queryResults) {
                handleInstanceToTableModel(this.queryResultTableModel, queryResult, reflectionFormBuilder, entityClass);
            }
            this.queryStatusLabel.setText("Query executed successfully.");
            HistoryEntry entry = queryComboBoxEditor.getItem();
            if(entry == null) {
                //if the query came from a HistoryEntry from the combo box model
                entry = new HistoryEntry(queryText, 1, new Date());
            }
            if(!this.queryComboBoxModel.contains(entry)) {
                this.queryComboBoxModel.addElement(entry);
            }
            this.queryComboBoxEditor.setItem(null); //reset to indicate the need
                //to create a new item
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

    public DefaultTableModel getQueryResultTableModel() {
        return queryResultTableModel;
    }

    public List<E> getQueryResults() {
        return queryResults;
    }

    public JTable getQueryResultTable() {
        return queryResultTable;
    }

    public JLabel getQueryStatusLabel() {
        return queryStatusLabel;
    }

    protected static void handleInstanceToTableModel(DefaultTableModel queryResultTableModel, Object queryResult, ReflectionFormBuilder reflectionFormBuilder, Class<?> entityClass) throws IllegalArgumentException, IllegalAccessException {
        List<Object> queryResultValues = new LinkedList<>();
        for(Field field : reflectionFormBuilder.retrieveRelevantFields(entityClass)) {
            queryResultValues.add(field.get(queryResult));
        }
        queryResultTableModel.addRow(queryResultValues.toArray(new Object[queryResultValues.size()]));
    }

    private class QueryComboBoxEditor implements ComboBoxEditor {
        private final JTextField editorComponent = new JTextField();
        /**
         * the new item which is about to be created and not (yet) part of the
         * model of the {@link JComboBox} (can be retrieved with
         * {@link #getItem() }).
         */
        private HistoryEntry item = new HistoryEntry(TOOL_TIP_TEXT_KEY, WIDTH, null);
        private final Set<ActionListener> actionListeners = new HashSet<>();

        QueryComboBoxEditor() {
            this.editorComponent.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    if(QueryComboBoxEditor.this.item == null) {
                        QueryComboBoxEditor.this.item = new HistoryEntry(QueryComboBoxEditor.this.editorComponent.getText(), 1, new Date());
                    }else {
                        QueryComboBoxEditor.this.item.setText(QueryComboBoxEditor.this.editorComponent.getText());
                    }
                }
            });
        }

        @Override
        public JTextField getEditorComponent() {
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
        public HistoryEntry getItem() {
            return this.item;
        }

        @Override
        public void selectAll() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        /**
         * Adds {@code l} to the set of notified {@link ActionListener}s.
         * Doesn't have any effect if {@code l} already is a registered
         * listener.
         * @param l
         */
        /*
        internal implementation notes:
        - seems to be called without any precaution if listener has been added
        before (throwing exception if listener is already registered thus
        doesn't make sense)
        */
        @Override
        public void addActionListener(ActionListener l) {
            this.actionListeners.add(l);
        }

        /**
         * Removes {@code l} from the set of notified {@link ActionListener}s.
         * Doesn't have any effect if {@code l} isn't a registered listener.
         * @param l
         */
        /*
        internal implementation notes:
        - seems to be called without any precaution if listener has been added
        before (throwing exception if listener isn't registered thus doesn't
        make sense)
        */
        @Override
        public void removeActionListener(ActionListener l) {
            this.actionListeners.remove(l);
        }
    }

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

        boolean contains(E element) {
            return this.items.contains(element);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton queryButton;
    private javax.swing.JComboBox<HistoryEntry> queryComboBox;
    private javax.swing.JLabel queryLabel;
    private javax.swing.JLabel queryLimitLabel;
    private javax.swing.JSpinner queryLimitSpinner;
    private javax.swing.JLabel queryResultLabel;
    private javax.swing.JTable queryResultTable;
    private javax.swing.JScrollPane queryResultTableScrollPane;
    private javax.swing.JLabel queryStatusLabel;
    private javax.swing.JSeparator separator;
    // End of variables declaration//GEN-END:variables
}
