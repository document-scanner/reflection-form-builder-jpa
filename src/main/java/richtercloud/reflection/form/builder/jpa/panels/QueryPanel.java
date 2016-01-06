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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.metamodel.Metamodel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JTable;
import javax.swing.JTextArea;
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
 * Allows to run a JPQL query for a specific class while getting feedback about
 * the following errors:<ul>
 * <li>syntax errors in queries</li>
 * <li>queries with the wrong result type/class</li>
 * <li>(unexpected) errors which occured during execution of the query</li>
 * </ul> and an overview of the result in a table displaying all class fields
 * (as returned by {@link ReflectionFormBuilder#retrieveRelevantFields(java.lang.Class) }. The feedback is given in a scrollable (non-editable) label in order
 * to provide a fixed size layout.
 *
 * In favour of immutability as design principle QueryPanel needs to be
 * recreated in order to handle a different entity class.
 *
 * There's currently no feature to hide specific class fields.
 *
 * @author richter
 * @param <E> a generic type for the entity class
 */
public class QueryPanel<E> extends javax.swing.JPanel {
    private static final long serialVersionUID = 1L;
    private final static Logger LOGGER = LoggerFactory.getLogger(QueryPanel.class);
    private EntityManager entityManager;
    private Class<? extends E> entityClass;
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

    private final SortedComboBoxModel<HistoryEntry> queryComboBoxModel;
    /**
     * The result of the current query (needs to be stored separately because the table model is used to display field values, not instances)
     */
    private List<E> queryResults = new LinkedList<>(); //can be initialized because null needs to be expressed differently anyway
    private final QueryComboBoxEditor queryComboBoxEditor;
    private ReflectionFormBuilder reflectionFormBuilder;
    private ListSelectionModel queryResultTableSelectionModel = new DefaultListSelectionModel();
    private Set<QueryPanelUpdateListener> updateListeners = new HashSet<>();
    /**
     * the {@code query} argument of the last execution of {@link #executeQuery(javax.persistence.TypedQuery, int, java.lang.String) }
     */
    private TypedQuery<? extends E> lastQuery;
    /**
     * the {@code queryLimit} arugment of the last execution of {@link #executeQuery(javax.persistence.TypedQuery, int, java.lang.String) }
     */
    private int lastQueryLimit;
    /**
     * the {@code queryText} argument of the last execution of {@link #executeQuery(javax.persistence.TypedQuery, int, java.lang.String) }
     */
    private String lastQueryText;

    public QueryPanel(EntityManager entityManager,
            Class<? extends E> entityClass,
            ReflectionFormBuilder reflectionFormBuilder,
            E initialValue) throws IllegalArgumentException, IllegalAccessException {
        this(entityManager,
                entityClass,
                reflectionFormBuilder,
                initialValue,
                ListSelectionModel.SINGLE_SELECTION);
    }

    /**
     * Creates a mutable list with one {@link HistoryEntry} to select all
     * entities of type {@code entityClass}.
     * @param entityClass
     * @return
     */
    public static List<HistoryEntry> generateInitialHistoryDefault(Class<?> entityClass) {
        List<HistoryEntry> retValue = new ArrayList<>(Arrays.asList(new HistoryEntry(createQueryText(entityClass), //queryText
                        1, //usageCount
                        new Date() //lastUsage
                )));
        return retValue;
    }

    protected QueryPanel(EntityManager entityManager,
            Class<? extends E> entityClass,
            ReflectionFormBuilder reflectionFormBuilder,
            E initialValue,
            int queryResultTableSelectionMode) throws IllegalArgumentException, IllegalAccessException {
        this(entityManager,
                entityClass,
                reflectionFormBuilder,
                initialValue,
                queryResultTableSelectionMode,
                generateInitialHistoryDefault(entityClass), //initialHistory
                null, //initialSelectedHistoryEntry (null means point to the first item of initialHistory
                INITIAL_QUERY_LIMIT_DEFAULT);
    }

    public QueryPanel(EntityManager entityManager,
            Class<? extends E> entityClass,
            ReflectionFormBuilder reflectionFormBuilder,
            E initialValue,
            List<HistoryEntry> initialHistory,
            HistoryEntry initialSelectedHistoryEntry,
            int initialQueryLimit) throws IllegalArgumentException, IllegalAccessException {
        this(entityManager,
                entityClass,
                reflectionFormBuilder,
                initialValue,
                ListSelectionModel.SINGLE_SELECTION,
                initialHistory,
                initialSelectedHistoryEntry,
                initialQueryLimit);
    }

    /**
     *
     * @param entityManager
     * @param entityClass
     * @param reflectionFormBuilder
     * @param initialValue
     * @param queryResultTableSelectionMode
     * @param initialHistory a list of history entries which ought to be selectable in the query history combo box (won't be modified)
     * @param initialQueryLimit When the component is created an initial query is executed. This property
     * limits its result length. Set to {@code 0} in order to skip initial
     * query.
     * @param initialSelectedHistoryEntry the query which ought to be selected initially (if {@code null} the first item of {@code predefinedQueries} will be selected initially or there will be no selected item if {@code intiialHistory} is empty.
     * @throws java.lang.IllegalAccessException
     * @throws IllegalArgumentException if {@code initialSelectedHistoryEntry} is not {@code null}, but not contained in {@code initialHistory}
     */
    /*
    internal implementation notes:
    - it's necessary to use a copy of initialHistory in order to avoid ConcurrentModificationException when items are sorted in combobox model implementation
    */
    protected QueryPanel(EntityManager entityManager,
            Class<? extends E> entityClass,
            ReflectionFormBuilder reflectionFormBuilder,
            E initialValue,
            int queryResultTableSelectionMode,
            List<HistoryEntry> initialHistory,
            HistoryEntry initialSelectedHistoryEntry,
            int initialQueryLimit) throws IllegalArgumentException, IllegalAccessException {
        if(entityClass == null) {
            throw new IllegalArgumentException("entityClass mustn't be null");
        }
        validateEntityClass(entityClass, entityManager);
        this.entityClass = entityClass;
        //initialize with initial item in order to minimize trouble with null
        //being set as editor item in JComboBox.setEditor
        this.queryComboBoxModel = new SortedComboBoxModel<>(QUERY_HISTORY_COMPARATOR_USAGE, new LinkedList<>(initialHistory));
        this.queryComboBoxEditor = new QueryComboBoxEditor(entityClass);
                //before initComponents because it's used there (yet sets item
                //of editor to null, so statement after initComponent is
                //necessary
        this.initComponents();
        if(initialSelectedHistoryEntry != null) {
            if(!initialHistory.contains(initialSelectedHistoryEntry)) {
                throw new IllegalArgumentException("if initialSelectedHistoryEntry is != null it has to be contained in initialHistory");
            }
            this.queryComboBox.setSelectedItem(initialSelectedHistoryEntry);
        } else {
            if(!initialHistory.isEmpty()) {
                this.queryComboBox.setSelectedItem(initialHistory.get(0));
            }else {
                this.queryComboBox.setSelectedItem(null);
            }
        }
        this.queryResultTableSelectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                for(QueryPanelUpdateListener updateListener : updateListeners) {
                    LOGGER.debug("notifying update listener {} about selection change", updateListener);
                    updateListener.onUpdate(new QueryPanelUpdateEvent(QueryPanel.this.queryResults.get(e.getFirstIndex()),
                            QueryPanel.this));
                }
            }
        });
        this.entityManager = entityManager;
        this.reflectionFormBuilder = reflectionFormBuilder;
        initTableModel(this.queryResultTableModel, this.reflectionFormBuilder.getFieldRetriever().retrieveRelevantFields(entityClass));
        this.queryLabel.setText(String.format("%s query:", entityClass.getSimpleName()));
        String queryText = createQueryText(entityClass);
        TypedQuery<? extends E> query = entityManager.createQuery(queryText, entityClass);
        this.executeQuery(query, initialQueryLimit, queryText);
        this.queryResultTableSelectionModel.setSelectionMode(queryResultTableSelectionMode);
        if(initialValue != null) {
            if(!this.entityManager.contains(initialValue)) {
                this.queryResultLabel.setText(String.format("previously managed entity %s has been removed from persistent storage, ignoring", initialValue));
            }
            if(!this.queryResults.contains(initialValue)) {
                this.queryResults.add(initialValue); // ok to add initially (will be overwritten with the next query where the user has to specify a query which retrieves the initial value or not
            }
            int initialValueIndex = this.queryResults.indexOf(initialValue);
            this.queryResultTableSelectionModel.addSelectionInterval(initialValueIndex, initialValueIndex); //no need to clear selection because we're just initializing
        }
    }

    public static String generateEntityClassQueryIdentifier(Class<?> entityClass) {
        String retValue = String.valueOf(Character.toLowerCase(entityClass.getSimpleName().charAt(0)));
        return retValue;
    }

    private static String createQueryText(Class<?> entityClass) {
        //Criteria API doesn't allow retrieval of string/text from objects
        //created with CriteriaBuilder, but text should be the first entry in
        //the query combobox -> construct String instead of using
        //CriteriaBuilder
        String entityClassQueryIdentifier = generateEntityClassQueryIdentifier(entityClass);
        String retValue = String.format("SELECT %s from %s %s",
                entityClassQueryIdentifier,
                entityClass.getSimpleName(),
                entityClassQueryIdentifier);
        return retValue;
    }

    public static void initTableModel(DefaultTableModel tableModel, List<Field> entityClassFields) {
        for(Field field : entityClassFields) {
            tableModel.addColumn(field.getName());
        }
    }

    public List<HistoryEntry> getQueryHistory() {
        return new LinkedList<>(this.getQueryComboBoxModel().getItems());
    }

    public void addUpdateListener(QueryPanelUpdateListener updateListener) {
        this.updateListeners.add(updateListener);
    }

    public void removeUpdateListener(QueryPanelUpdateListener updateListener) {
        this.updateListeners.remove(updateListener);
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

    public void clearSelection() {
        this.queryResultTable.clearSelection();
    }

    /**
     * @return the queryComboBoxModel
     */
    /*
    internal implementation notes:
    - expose in order to be able to reuse/update queries
    */
    public SortedComboBoxModel<HistoryEntry> getQueryComboBoxModel() {
        return queryComboBoxModel;
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
        queryComboBox = new javax.swing.JComboBox<>();
        queryLimitSpinner = new javax.swing.JSpinner();
        queryLimitLabel = new javax.swing.JLabel();
        queryResultTableScrollPane = new javax.swing.JScrollPane();
        queryResultTable = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        queryStatusLabelScrollPane = new javax.swing.JScrollPane();
        queryStatusLabel = new javax.swing.JTextArea();

        queryLabel.setText("Query:");

        queryButton.setText("Run query");
        queryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                queryButtonActionPerformed(evt);
            }
        });

        queryResultLabel.setText("Query result:");

        queryComboBox.setEditable(true);
        queryComboBox.setModel(queryComboBoxModel);
        queryComboBox.setEditor(queryComboBoxEditor);

        queryLimitSpinner.setModel(queryLimitSpinnerModel);
        queryLimitSpinner.setValue(INITIAL_QUERY_LIMIT_DEFAULT);

        queryLimitLabel.setText("# of Results");

        queryResultTable.setModel(this.queryResultTableModel);
        queryResultTable.setSelectionModel(this.queryResultTableSelectionModel);
        queryResultTableScrollPane.setViewportView(queryResultTable);

        queryStatusLabelScrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        queryStatusLabel.setEditable(false);
        queryStatusLabel.setColumns(20);
        queryStatusLabel.setLineWrap(true);
        queryStatusLabel.setRows(2);
        queryStatusLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        queryStatusLabelScrollPane.setViewportView(queryStatusLabel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(queryResultTableScrollPane)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(queryLabel)
                        .addGap(18, 18, 18)
                        .addComponent(queryComboBox, 0, 283, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(queryLimitLabel)
                        .addGap(18, 18, 18)
                        .addComponent(queryLimitSpinner, javax.swing.GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(queryButton))
                    .addComponent(queryStatusLabelScrollPane)
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
                .addComponent(queryStatusLabelScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(separator, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(queryResultLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(queryResultTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
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
        }else if(queryComboBox.getSelectedIndex() >= 0) {
            HistoryEntry selectedHistoryEntry = this.queryComboBox.getItemAt(this.queryComboBox.getSelectedIndex());
            queryText = selectedHistoryEntry.getText();
        }else {
            this.queryStatusLabel.setText("No query entered or selected");
            return;
        }
        int queryLimit = (int) queryLimitSpinner.getValue();
        TypedQuery<? extends E> query = this.createQuery(queryText); //handles displaying
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
    private void executeQuery(TypedQuery<? extends E> query,
            int queryLimit,
            String queryText) {
        LOGGER.debug("executing query '{}'", queryText);
        while(this.queryResultTableModel.getRowCount() > 0) {
            this.queryResultTableModel.removeRow(0);
        }
        try {
            List<? extends E> queryResultTmp = query.setMaxResults(queryLimit).getResultList();
            queryResults.clear();
            queryResults.addAll(queryResultTmp);
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
                this.getQueryComboBoxModel().addElement(entry);
            }
            this.queryComboBoxEditor.setItem(null); //reset to indicate the need
                //to create a new item
        }catch(Exception ex) {
            LOGGER.info("an exception occured while executing the query", ex);
            this.queryStatusLabel.setText(generateStatusMessage(ex.getMessage()));
        }
        this.lastQuery = query;
        this.lastQueryLimit = queryLimit;
        this.lastQueryText = queryText;
    }

    public void repeatLastQuery() {
        executeQuery(lastQuery, lastQueryLimit, lastQueryText);
    }

    private TypedQuery<? extends E> createQuery(String queryText) {
        try {
            TypedQuery<? extends E> query = this.entityManager.createQuery(queryText, this.entityClass);
            return query;
        }catch(Exception ex) {
            LOGGER.info("an exception occured while executing the query", ex);
            this.queryStatusLabel.setText(generateStatusMessage(ex.getMessage()));
        }
        return null;
    }

    public DefaultTableModel getQueryResultTableModel() {
        return queryResultTableModel;
    }

    public List<? extends E> getQueryResults() {
        return queryResults;
    }

    public JTable getQueryResultTable() {
        return queryResultTable;
    }

    public JTextArea getQueryStatusLabel() {
        return queryStatusLabel;
    }

    public Class<? extends E> getEntityClass() {
        return entityClass;
    }

    protected static void handleInstanceToTableModel(DefaultTableModel queryResultTableModel,
            Object queryResult,
            ReflectionFormBuilder reflectionFormBuilder,
            Class<?> entityClass) throws IllegalArgumentException,
            IllegalAccessException {
        List<Object> queryResultValues = new LinkedList<>();
        for(Field field : reflectionFormBuilder.getFieldRetriever().retrieveRelevantFields(entityClass)) {
            queryResultValues.add(field.get(queryResult));
        }
        queryResultTableModel.addRow(queryResultValues.toArray(new Object[queryResultValues.size()]));
    }

    public static void validateEntityClass(Class<?> entityClass, EntityManager entityManager) {
        Metamodel meta = entityManager.getMetamodel();
        try {
            meta.entity(entityClass);
        }catch(IllegalArgumentException ex) {
            throw new IllegalArgumentException(String.format("entityClass %s is not a mapped entity", entityClass), ex);
        }
    }

    /**
     * Allows later changes to message generation depending on the mechanism
     * used for displaying (label (might require {@code <html></html>} tags around message), textarea, dialog, etc.)
     *
     * @param message
     * @return the generated status message
     */
    private String generateStatusMessage(String message) {
        return message;
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
    private javax.swing.JTextArea queryStatusLabel;
    private javax.swing.JScrollPane queryStatusLabelScrollPane;
    private javax.swing.JSeparator separator;
    // End of variables declaration//GEN-END:variables

}
