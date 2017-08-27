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

import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.message.handler.ExceptionMessage;
import richtercloud.message.handler.IssueHandler;
import richtercloud.message.handler.Message;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.reflection.form.builder.storage.StorageException;

/**
 * A component which provides a text field to enter JPQL queries for a
 * pre-defined class, a spinner to control the length the result set and
 * methods to retrieve the query result to be used in a component which
 * visualizes it.
 *
 * Query results are generally limited to exact type of {@code entityClass} or
 * subtypes in order to minimize validation efforts (there should be no use case
 * where types are needed which don't match the type of field they're assigned
 * to). Whether the exact type or subtypes ought to be matched is controlled
 * with a checkbox which allows the user to choose. This exposes the object
 * oriented structure of entites to the user, but that's not a problem and not
 * worth to be hidden.
 *
 * @author richter
 * @param <E> the type of the entity to query
 */
/*
internal implementation notes:
- Subtypes checkbox also easily solves the issue that a exact type switch is
necessary to be specified at instantiation of QueryComponent which is usually
done in type handlers which don't know about field annotations (could be fixed,
though).
*/
public class QueryComponent<E> extends JPanel {
    private final static Logger LOGGER = LoggerFactory.getLogger(QueryComponent.class);
    /**
     * the default value for the initial query limit (see {@link #QueryPanel(javax.persistence.EntityManager, java.lang.Class, int) } for details
     */
    public static final int INITIAL_QUERY_LIMIT_DEFAULT = 20;
    private final static Comparator<QueryHistoryEntry> QUERY_HISTORY_COMPARATOR_USAGE = new Comparator<QueryHistoryEntry>() {
        @Override
        public int compare(QueryHistoryEntry o1, QueryHistoryEntry o2) {
            return Integer.compare(o2.getUsageCount(), o1.getUsageCount());
                //make highest usage count appear at the top
        }
    };
    private static final long serialVersionUID = 1L;
    public final static String SUBTYPES_ALLOW = "Allow subtypes";
    public final static String SUBTYPES_FILTER = "Filter subtypes";
    public final static String SUBTYPES_FORBID = "Forbid/Fail on subtypes";
    public final static String SUBTYPES_DEFAULT = SUBTYPES_ALLOW;

    public static void validateEntityClass(Class<?> entityClass,
            PersistenceStorage storage) {
        if(!storage.isClassSupported(entityClass)) {
            throw new IllegalArgumentException(String.format("entityClass %s is not a mapped entity", entityClass));
        }
    }

    private PersistenceStorage storage;
    private Class<E> entityClass;
    private final SpinnerModel queryLimitSpinnerModel = new SpinnerNumberModel(INITIAL_QUERY_LIMIT_DEFAULT, //value
            1, //min
            null, //max
            1 //stepSize
    );
    private final SortedComboBoxModel<QueryHistoryEntry> queryComboBoxModel;
    private final QueryComboBoxEditor queryComboBoxEditor;
    /**
     * the {@code queryLimit} arugment of the last execution of {@link #executeQuery(javax.persistence.TypedQuery, int, java.lang.String) }
     */
    private int lastQueryLimit;
    /**
     * the {@code queryText} argument of the last execution of {@link #executeQuery(javax.persistence.TypedQuery, int, java.lang.String) }
     */
    private String lastQueryText;
    private final JButton queryButton;
    private final JComboBox<QueryHistoryEntry> queryComboBox;
    private final JLabel queryLabel;
    private final JLabel queryLimitLabel;
    private final JSpinner queryLimitSpinner;
    /**
     * Allows handling for different proceedure for subtypes in queries (allow,
     * filter, fail on occurance).
     */
    private final JComboBox<String> subtypeComboBox = new JComboBox<>(new DefaultComboBoxModel<>(new String[]{SUBTYPES_ALLOW,
        SUBTYPES_FILTER,
        SUBTYPES_FORBID}));
    private final JTextArea queryStatusLabel;
    private final JScrollPane queryStatusLabelScrollPane;
    private final Set<QueryComponentListener<E>> listeners = new HashSet<>();
    private final IssueHandler issueHandler;
    /**
     * The text which is the initial query. {@code null} indicates that
     * {@link #createQueryText(java.lang.Class, boolean) } ought to be used to
     * create the text.
     */
    private final QueryHistoryEntryStorage entryStorage;

    public QueryComponent(PersistenceStorage storage,
            Class<E> entityClass,
            IssueHandler issueHandler,
            boolean async,
            QueryHistoryEntryStorage entryStorage) throws IllegalArgumentException, IllegalAccessException {
        this(storage,
                entityClass,
                issueHandler,
                INITIAL_QUERY_LIMIT_DEFAULT,
                async,
                entryStorage);
    }

    /**
     * Creates a {@code QueryComponent}.
     * @param entityManager
     * @param entityClass
     * @param issueHandler
     * @param initialHistory
     * @param initialSelectedQueryHistoryEntry
     * @param initialQueryLimit
     * @throws IllegalArgumentException
     */
    /*
    internal implementation notes:
    - There's no sense in passing initial listeners in the constructor because
    their method implementations most likely require usage of variables which
    aren't available until the call of the superclass constructor has returned
    */
    protected QueryComponent(PersistenceStorage storage,
            Class<E> entityClass,
            IssueHandler issueHandler,
            int initialQueryLimit,
            boolean async,
            QueryHistoryEntryStorage entryStorage) throws IllegalArgumentException {
        if(entityClass == null) {
            throw new IllegalArgumentException("entityClass mustn't be null");
        }
        this.entryStorage = entryStorage;
        queryLabel = new JLabel();
        queryButton = new JButton();
        queryComboBox = new JComboBox<>();
        queryLimitSpinner = new JSpinner();
        queryLimitLabel = new JLabel();
        queryStatusLabelScrollPane = new JScrollPane();
        queryStatusLabel = new JTextArea();
        storage.isClassSupported(entityClass);
        this.entityClass = entityClass;
        //initialize with initial item in order to minimize trouble with null
        //being set as editor item in JComboBox.setEditor
        List<QueryHistoryEntry> initialHistory = entryStorage.retrieve(entityClass);
        this.queryComboBoxModel = new SortedComboBoxModel<>(QUERY_HISTORY_COMPARATOR_USAGE,
                new LinkedList<>(initialHistory));
        this.queryComboBoxEditor = new QueryComboBoxEditor();
                //before initComponents because it's used there (yet sets item
                //of editor to null, so statement after initComponent is
                //necessary
        this.queryComboBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                //get latest updates from other query components
                List<QueryHistoryEntry> entries = entryStorage.retrieve(entityClass);
                for(QueryHistoryEntry entry : entries) {
                    if(!queryComboBoxModel.contains(entry)) {
                        queryComboBoxModel.addElement(entry);
                    }
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                //nothing to do
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                //nothing to do
            }
        });
        this.initComponents();
        QueryHistoryEntry initiallySelectedEntry = entryStorage.getInitialEntry(entityClass);
        if(initiallySelectedEntry != null) {
            if(!initialHistory.contains(initiallySelectedEntry)) {
                throw new IllegalArgumentException("if initialSelectedQueryHistoryEntry is != null it has to be contained in initialHistory");
            }
            this.queryComboBox.setSelectedItem(initiallySelectedEntry);
        } else {
            if(!initialHistory.isEmpty()) {
                this.queryComboBox.setSelectedItem(initialHistory.get(0));
            }else {
                this.queryComboBox.setSelectedItem(null);
            }
        }
        this.storage = storage;
        if(issueHandler == null) {
            throw new IllegalArgumentException("messageHandler mustn't be null");
        }
        this.issueHandler = issueHandler;
        this.subtypeComboBox.setSelectedItem(SUBTYPES_DEFAULT);
        this.queryLabel.setText(String.format("%s query:", entityClass.getSimpleName()));
        if(initiallySelectedEntry != null) {
            String queryText = initiallySelectedEntry.getText();
            executeQuery(initialQueryLimit,
                    queryText,
                    async);
        }
    }

    public List<QueryHistoryEntry> getQueryHistory() {
        return new LinkedList<>(this.getQueryComboBoxModel().getItems());
    }

    /**
     * @return the queryComboBoxModel
     */
    /*
    internal implementation notes:
    - expose in order to be able to reuse/update queries
    */
    public SortedComboBoxModel<QueryHistoryEntry> getQueryComboBoxModel() {
        return queryComboBoxModel;
    }

    public QueryHistoryEntryStorage getEntryStorage() {
        return entryStorage;
    }

    private void initComponents() {

        queryLabel.setText("Query:");

        queryButton.setText("Run query");
        queryButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                queryButtonActionPerformed(evt);
            }
        });

        queryComboBox.setEditable(true);
        queryComboBox.setModel(queryComboBoxModel);
        queryComboBox.setEditor(queryComboBoxEditor);

        queryLimitSpinner.setModel(queryLimitSpinnerModel);
        queryLimitSpinner.setValue(INITIAL_QUERY_LIMIT_DEFAULT);

        queryLimitLabel.setText("# of Results");

        queryStatusLabelScrollPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        queryStatusLabel.setEditable(false);
        queryStatusLabel.setColumns(20);
        queryStatusLabel.setLineWrap(true);
        queryStatusLabel.setRows(2);
        queryStatusLabel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        queryStatusLabelScrollPane.setViewportView(queryStatusLabel);

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        GroupLayout.ParallelGroup horizontalParallelGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        horizontalParallelGroup.addGroup(layout.createSequentialGroup()
                .addComponent(queryLabel)
                .addGap(18, 18, 18)
                .addComponent(queryComboBox,
                        0,
                        250,
                        Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(queryLimitLabel)
                .addGap(18, 18, 18)
                .addComponent(queryLimitSpinner,
                        0,
                        GroupLayout.PREFERRED_SIZE+100, //initial preferred size
                            //referes to spinner with value 0
                        GroupLayout.PREFERRED_SIZE+100)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(subtypeComboBox,
                        0,
                        GroupLayout.PREFERRED_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(queryButton)
        ).addComponent(queryStatusLabelScrollPane, GroupLayout.Alignment.TRAILING)
                .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE));

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(horizontalParallelGroup)
                                .addContainerGap())
        );

        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(queryLabel)
                                .addComponent(queryButton)
                                .addComponent(queryComboBox,
                                        GroupLayout.PREFERRED_SIZE,
                                        GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.PREFERRED_SIZE)
                                .addComponent(queryLimitSpinner, GroupLayout.PREFERRED_SIZE,
                                        GroupLayout.DEFAULT_SIZE,
                                        GroupLayout.PREFERRED_SIZE)
                                .addComponent(queryLimitLabel)
                                .addComponent(subtypeComboBox))
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(queryStatusLabelScrollPane,
                                GroupLayout.PREFERRED_SIZE,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE)));
    }

    public void runQuery(boolean async) {
        //although queryComboBox's model should never be empty in the current
        //implementation, there's check nevertheless so that future changes
        //don't cause too much trouble (adding a function to delete history
        //makes sense)
        String queryText;
        //there's no good way to tell if the ComboBox is currently being edited
        //(the JComboBox doesn't know and thus doesn't change the selected item
        //and selected index property and the editor can't tell because it
        //doesn't know the model) -> use editor to get the info
        QueryHistoryEntry queryComboBoxEditorItem = queryComboBoxEditor.getItem();
        if(queryComboBoxEditorItem != null && !queryComboBoxModel.contains(queryComboBoxEditorItem)) {
            queryText = queryComboBoxEditorItem.getText();
            if(queryText == null || queryText.isEmpty()) {
                queryStatusLabel.setText("Enter a query");
                return;
            }
        }else if(queryComboBox.getSelectedIndex() >= 0) {
            QueryHistoryEntry selectedQueryHistoryEntry = this.queryComboBox.getItemAt(this.queryComboBox.getSelectedIndex());
            queryText = selectedQueryHistoryEntry.getText();
        }else {
            this.queryStatusLabel.setText("No query entered or selected");
            return;
        }
        int queryLimit = (int) queryLimitSpinner.getValue();
        this.executeQuery(queryLimit,
                queryText,
                async //async
        );
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void queryButtonActionPerformed(java.awt.event.ActionEvent evt) {
        runQuery(false //async
        );
    }

    public void addListener(QueryComponentListener<E> listener) {
        this.listeners.add(listener);
    }

    public void removeListener(QueryComponentListener<E> listener) {
        this.listeners.remove(listener);
    }

    /**
     * Executes JQPL query {@code query}. Creates a {@link QueryHistoryEntry} in the
     * {@code queryComboBoxModel} and resets the current item of
     * {@code queryComboBoxEditor}.
     * @param query
     */
    /*
    internal implementation notes:
    - in order to produce QueryHistoryEntrys from every query it's necessary to pass
    the text of the query because there's no way to retrieve text from Criteria
    objects
    */
    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    private void executeQuery(int queryLimit,
            String queryText,
            boolean async) {
        if(!async) {
            try {
                LOGGER.debug("running query synchronously");
                List<E> queryResult = executeQueryNonGUI(queryLimit, queryText);
                executeQueryGUI(queryResult, queryText);
            }catch(StorageException ex) {
                LOGGER.info("an exception occured while executing the query", ex);
                this.queryStatusLabel.setText(generateStatusMessage(ex.getMessage()));
            }
        }else {
            LOGGER.debug("running query asynchronously");
            this.setEnabled(false);
            Thread queryThread = new Thread(() -> {
                List<E> queryResult;
                try {
                    queryResult = executeQueryNonGUI(queryLimit, queryText);
                } catch (StorageException ex) {
                    LOGGER.info("an exception occured while executing the query", ex);
                    this.queryStatusLabel.setText(generateStatusMessage(ex.getMessage()));
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    try {
                        executeQueryGUI(queryResult, queryText);
                        QueryComponent.this.setEnabled(true);
                    }catch(Throwable ex) {
                        LOGGER.error("an unexpected exception occured during query execution GUI callback",
                                ex);
                        issueHandler.handleUnexpectedException(new ExceptionMessage(ex));
                    }
                });
            },
                    "query-thread");
            queryThread.start();
        }
    }

    /**
     * The non-GUI part of {@link #executeQuery(int, java.lang.String, boolean) }.
     * @param queryLimit
     * @param queryText
     * @return
     * @throws StorageException
     */
    private List<E> executeQueryNonGUI(int queryLimit,
            String queryText) throws StorageException {
        LOGGER.debug("executing query '{}'", queryText);
        List<E> queryResults = storage.runQuery(queryText, entityClass, queryLimit);
        this.lastQueryLimit = queryLimit;
        this.lastQueryText = queryText;
        return queryResults;
    }

    /**
     * The GUI-part of {@link #executeQuery(int, java.lang.String, boolean) }.
     * @param queryResults the query results which should have been retrieved
     * in the non-GUI routine of executing queries, i.e.
     * {@link #executeQueryNonGUI(int, java.lang.String) }
     * @param queryText the text used to retrieve the query results
     */
    private void executeQueryGUI(List<E> queryResults,
            String queryText) {
        ListIterator<E> queryResultsItr = queryResults.listIterator();
        while(queryResultsItr.hasNext()) {
            E queryResult = queryResultsItr.next();
            //first check whether query requests are assignable from entity
            //class in order to avoid nonsense - or in the case of
            //SUBTYPES_FORBID for equality...
            assert subtypeComboBox.getSelectedItem() != null;
            if(subtypeComboBox.getSelectedItem().equals(SUBTYPES_FORBID)) {
                if(!queryResult.getClass().equals(entityClass)) {
                    this.issueHandler.handle(new Message("The query result "
                            + "contained entities which are not of the extact "
                            + "type of this query panel (super and subclasses "
                            + "aren't allow, consider adding a "
                            + "`WHERE TYPE([identifier]) = [entity class]` "
                            + "clause to the query)",
                            JOptionPane.ERROR_MESSAGE,
                            "Query error"));
                    return;
                }
            } else {
                if(!entityClass.isAssignableFrom(queryResult.getClass())) {
                    this.issueHandler.handle(new Message(String.format("The query result "
                            + "contained entities which are not a subtype of "
                            + "the entity class %s.", entityClass.getSimpleName()),
                            JOptionPane.ERROR_MESSAGE,
                            "Query error"));
                    return;
                }
            }
            //...then eventually filter
            if(subtypeComboBox.getSelectedItem().equals(SUBTYPES_FILTER)) {
                if(!queryResult.getClass().equals(entityClass)) {
                    queryResultsItr.remove();
                }
            }
        }
        for(QueryComponentListener<E> listener : listeners) {
            listener.onQueryExecuted(new QueryComponentEvent<>(queryResults));
        }
        this.queryStatusLabel.setText("Query executed successfully.");
        //- Rather than figuring out which (badly documented) JComboBox function
        //returns which value in which state (editing, selected, initially
        //empty, etc.) check that the value is added to the model and
        //entry storage or - if already present - that it's usage count is
        //increased
        //- Assume that is model and store are in sync
        QueryHistoryEntry modelEntry = null;
        for(QueryHistoryEntry entry : queryComboBoxModel.getItems()) {
            if(entry.getText().equals(queryText)) {
                modelEntry = entry;
            }
        }
        if(modelEntry == null) {
            QueryHistoryEntry newEntry = new QueryHistoryEntry(queryText,
                    1, //usageCount
                    new Date() //lastUsed
            );
            this.getQueryComboBoxModel().addElement(newEntry);
            try {
                this.entryStorage.store(this.entityClass,
                        newEntry);
            } catch (QueryHistoryEntryStorageException ex) {
                issueHandler.handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
            }
        }else {
            modelEntry.setUsageCount(modelEntry.getUsageCount()+1);
            try {
                this.entryStorage.store(this.entityClass,
                        modelEntry);
            } catch (QueryHistoryEntryStorageException ex) {
                issueHandler.handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
            }
        }
        this.queryComboBoxModel.sort();
            //sort no matter whether item has been added or usageCount or
            //lastUsed has been updated
        this.queryComboBoxEditor.setItem(null); //reset to indicate the need
            //to create a new item
    }

    public void repeatLastQuery() {
        executeQuery(lastQueryLimit,
                lastQueryText,
                false //async
        );
    }

    public JTextArea getQueryStatusLabel() {
        return queryStatusLabel;
    }

    public Class<? extends E> getEntityClass() {
        return entityClass;
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
}
