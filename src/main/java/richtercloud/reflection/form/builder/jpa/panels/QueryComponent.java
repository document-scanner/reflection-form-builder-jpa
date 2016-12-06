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

import java.util.ArrayList;
import java.util.Arrays;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.message.handler.Message;
import richtercloud.message.handler.MessageHandler;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.jpa.HistoryEntry;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;

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
    private static final long serialVersionUID = 1L;

    /**
     * Creates a mutable list with one {@link HistoryEntry} to select all
     * entities of type {@code entityClass}.
     * @param entityClass
     * @return
     */
    public static List<HistoryEntry> generateInitialHistoryDefault(Class<?> entityClass) {
        List<HistoryEntry> retValue = new ArrayList<>(Arrays.asList(new HistoryEntry(createQueryText(entityClass,
                                false //forbidSubtypes
                        ), //queryText
                        1, //usageCount
                        new Date() //lastUsage
                ),
                new HistoryEntry(createQueryText(entityClass,
                                true //forbidSubtypes
                        ),
                        1,
                        new Date())));
        return retValue;
    }

    public static String generateEntityClassQueryIdentifier(Class<?> entityClass) {
        String retValue = String.valueOf(Character.toLowerCase(entityClass.getSimpleName().charAt(0)));
        return retValue;
    }

    /**
     * This doesn't work with Hibernate 5.1.0 as JPA provider due to bug
     * https://hibernate.atlassian.net/browse/HHH-10653!
     *
     * Since it's possible to use {@link Class#getSimpleName() } to identify
     * classes it's not necessary to use parameters which provides queries which
     * are much more readable if plain text and simple names are used. Note that
     * JPA 2.1 query API and CriteriaBuilder API are seriously incapable of
     * retrieving the text of the query (both Query and TypedQuery) after it has
     * been created with parameters so that it'd be necessary to store
     * parameters like {@code entityClass} in {@link HistoryEntry}s which is
     * quite unelegant or keep the parameter escape string (e.g.
     * {@code :entityClass} in the query).
     *
     * @param entityManager
     * @param entityClass
     * @return
     */
    private static String createQueryText(Class<?> entityClass,
            boolean forbidSubtypes) {
        //Criteria API doesn't allow retrieval of string/text from objects
        //created with CriteriaBuilder, but text should be the first entry in
        //the query combobox -> construct String instead of using
        //CriteriaBuilder
        String entityClassQueryIdentifier = generateEntityClassQueryIdentifier(entityClass);
        String retValue = String.format("SELECT %s FROM %s %s%s",
                entityClassQueryIdentifier,
                entityClass.getSimpleName(),
                entityClassQueryIdentifier,
                forbidSubtypes
                        ? String.format(" WHERE TYPE(%s) = %s",
                                entityClassQueryIdentifier,
                                entityClass.getSimpleName())
                        : "");
        return retValue;
    }

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
    private final SortedComboBoxModel<HistoryEntry> queryComboBoxModel;
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
    private final JComboBox<HistoryEntry> queryComboBox;
    private final JLabel queryLabel;
    private final JLabel queryLimitLabel;
    private final JSpinner queryLimitSpinner;
    public final static String SUBTYPES_ALLOW = "Allow subtypes";
    public final static String SUBTYPES_FILTER = "Filter subtypes";
    public final static String SUBTYPES_FORBID = "Forbid/Fail on subtypes";
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
    private final MessageHandler messageHandler;
    private final FieldRetriever fieldRetriever;

    public QueryComponent(PersistenceStorage storage,
            Class<E> entityClass,
            MessageHandler messageHandler,
            FieldRetriever fieldRetriever) throws IllegalArgumentException, IllegalAccessException {
        this(storage,
                entityClass,
                messageHandler,
                generateInitialHistoryDefault(entityClass),
                null, //initialSelectedHistoryEntry (null means point to the first item of initialHistory
                INITIAL_QUERY_LIMIT_DEFAULT,
                fieldRetriever);
    }

    /**
     * Creates a {@code QueryComponent}.
     * @param entityManager
     * @param entityClass
     * @param messageHandler
     * @param initialHistory
     * @param initialSelectedHistoryEntry
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
            MessageHandler messageHandler,
            List<HistoryEntry> initialHistory,
            HistoryEntry initialSelectedHistoryEntry,
            int initialQueryLimit,
            FieldRetriever fieldRetriever) throws IllegalArgumentException {
        if(entityClass == null) {
            throw new IllegalArgumentException("entityClass mustn't be null");
        }
        if(fieldRetriever == null) {
            throw new IllegalArgumentException("fieldRetriever mustn't be null");
        }
        this.fieldRetriever = fieldRetriever;
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
        this.storage = storage;
        if(messageHandler == null) {
            throw new IllegalArgumentException("messageHandler mustn't be null");
        }
        this.messageHandler = messageHandler;
        this.subtypeComboBox.setSelectedItem(SUBTYPES_ALLOW);
        this.queryLabel.setText(String.format("%s query:", entityClass.getSimpleName()));
        String queryText = createQueryText(entityClass,
                false //forbidSubtypes
        );
        SwingUtilities.invokeLater(() -> {
            this.executeQuery(initialQueryLimit, queryText);
            LOGGER.debug("Query finished executing");
        }); //avoid delay on Query.getResultList
        LOGGER.debug("Executing query asynchronously");
    }

    public List<HistoryEntry> getQueryHistory() {
        return new LinkedList<>(this.getQueryComboBoxModel().getItems());
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
                        150,
                        Short.MAX_VALUE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(subtypeComboBox)
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
        if(!async) {
            LOGGER.debug("running query synchronously");
            this.executeQuery( queryLimit, queryText);
        }else {
            LOGGER.debug("running query asynchronously");
            this.setEnabled(false);
            SwingUtilities.invokeLater(() -> {
                QueryComponent.this.executeQuery( queryLimit, queryText);
                QueryComponent.this.setEnabled(true);
            });
        }
    }

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
    private void executeQuery(int queryLimit,
            String queryText) {
        LOGGER.debug("executing query '{}'", queryText);
        try {
            List<E> queryResults = storage.runQuery(queryText, entityClass, queryLimit);
            ListIterator<E> queryResultsItr = queryResults.listIterator();
            while(queryResultsItr.hasNext()) {
                E queryResult = queryResultsItr.next();
                //first check whether query requests are assignable from entity
                //class in order to avoid nonsense - or in the case of
                //SUBTYPES_FORBID for equality...
                assert subtypeComboBox.getSelectedItem() != null;
                if(subtypeComboBox.getSelectedItem().equals(SUBTYPES_FORBID)) {
                    if(!queryResult.getClass().equals(entityClass)) {
                        this.messageHandler.handle(new Message("The query result "
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
                        this.messageHandler.handle(new Message(String.format("The query result "
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
        this.lastQueryLimit = queryLimit;
        this.lastQueryText = queryText;
    }

    public void repeatLastQuery() {
        executeQuery(lastQueryLimit, lastQueryText);
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
