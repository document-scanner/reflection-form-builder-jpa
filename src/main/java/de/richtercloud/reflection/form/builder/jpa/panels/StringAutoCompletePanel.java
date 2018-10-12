/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.richtercloud.reflection.form.builder.jpa.panels;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;
import ca.odell.glazedlists.swing.DefaultEventComboBoxModel;
import de.richtercloud.message.handler.ExceptionMessage;
import de.richtercloud.message.handler.IssueHandler;
import de.richtercloud.message.handler.Message;
import de.richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import de.richtercloud.reflection.form.builder.storage.StorageException;
import de.richtercloud.validation.tools.FieldRetriever;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
internal implementation notes:
- SwingX's AutoCompleteDecorator only allows entering values which are in the
model and thus can't be used for suggestions
- A previous undocumented implementation added entities.toString in the popup
instead of the field values. Adding field values makes more sense.
- removed parameter T because it didn't seem to have any use, document well when
adding again
*/
/**
 * Displays existing values in the database in a popup menu in order to inform
 * about similar or equal values which have already been used and persisted.
 *
 * Performance depends on {@link PersistenceStorage#runQuery(java.lang.String, java.lang.Class, int, int) }
 * and the right priority since {@code comboBox} queries for every key-pressed
 * event.
 *
 * Uses glazedLists under the hood which sets a
 * {@link javax.swing.text.DocumentFilter} on the editor component of the
 * central combo box of this component which might be relevant when working with
 * document filters in callers.
 *
 * @author richter
 */
@SuppressWarnings({"PMD.UnnecessaryFullyQualifiedName",
    "PMD.FieldDeclarationsShouldBeAtStartOfClass",
    "PMD.SingularField"
})
public class StringAutoCompletePanel extends AbstractStringPanel {
    private static final long serialVersionUID = 1L;
    private final static Logger LOGGER = LoggerFactory.getLogger(StringAutoCompletePanel.class);
    private final EventList<String> comboBoxEventList = new BasicEventList<>();
    private final DefaultEventComboBoxModel<String> comboBoxModel = new DefaultEventComboBoxModel<>(comboBoxEventList);
    private List<?> lastCheckResults = new LinkedList<>();
    private final IssueHandler issueHandler;
    /**
     * A flag which allows to avoid starting more than one query which
     * is overkill and not constructive.
     */
    private boolean queryRunning;
    private QueryThread pendingThread;

    private static Field retrieveFieldByName(FieldRetriever fieldRetriever,
            Class<?> entityClass,
            String fieldName) {
        Field retValue = null;
        List<Field> entityClassFields = fieldRetriever.retrieveRelevantFields(entityClass);
        for(Field entityClassField : entityClassFields) {
            if(entityClassField.getName().equals(fieldName)) {
                if(!entityClassField.getDeclaringClass().equals(entityClass)) {
                    throw new IllegalArgumentException();
                }
                retValue = entityClassField;
                break;
            }
        }
        return retValue;
    }

    /**
     * Creates new form StringAutoCompletePanel
     * @param storage the storage to use
     * @param entityClass the entity classes
     * @param initialValue the initial value
     * @param fieldName the field name
     * @param initialQueryLimit the initial query limit
     * @param fieldRetriever the {@link FieldRetriever} to use for searching the
     *     field with {@code fieldName}
     * @param issueHandler the issue handler to use
     */
    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops",
        "PMD.AccessorMethodGeneration"
    })
    public StringAutoCompletePanel(PersistenceStorage storage,
            String initialValue,
            Class<?> entityClass,
            String fieldName,
            int initialQueryLimit,
            FieldRetriever fieldRetriever,
            IssueHandler issueHandler) {
        super(storage,
                entityClass,
                fieldName,
                initialQueryLimit);
        this.issueHandler = issueHandler;
        initComponents();
        this.comboBox.addActionListener((ActionEvent e) -> {
            for(StringPanelUpdateListener updateListener : getUpdateListeners()) {
                updateListener.onUpdate(new StringPanelUpdateEvent((String) this.comboBox.getSelectedItem()));
            }
        });
        final Field field = retrieveFieldByName(fieldRetriever,
                entityClass,
                fieldName);
        if(field == null) {
            throw new IllegalArgumentException();
        }

        if(SwingUtilities.isEventDispatchThread()) {
            installAutocomplete();
            this.comboBoxEventList.add(initialValue);
                //avoid `java.lang.IllegalStateException: Events to DefaultEventComboBoxModel must arrive on the EDT - consider adding GlazedListsSwing.swingThreadProxyList(source) somewhere in your list pipeline`
        }else {
            try {
                EventQueue.invokeAndWait(() -> {
                    this.comboBoxEventList.add(initialValue);
                    //avoid `java.lang.IllegalStateException: Events to DefaultEventComboBoxModel must arrive on the EDT - consider adding GlazedListsSwing.swingThreadProxyList(source) somewhere in your list pipeline`
                    installAutocomplete();
                });
            } catch (InterruptedException | InvocationTargetException ex) {
                LOGGER.error("unexpected exception during initialization of auto-complete support occured",
                        ex);
                issueHandler.handleUnexpectedException(new ExceptionMessage(ex));
                return;
                    //It's fine to return if the thread has been interrupted
                    //which is an unexpected condition anyway and can't be
                    //handled better than reporting an unexpected exception
            }
        }

        this.comboBox.setSelectedItem(initialValue);
        this.comboBox.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            /**
             * Handles querying the storage with the text of the text field
             * right after the update. The query happens on a separate thread.
             * If a query is running while the method is invoked from the EDT,
             * a reference for processing is stored and started from within the
             * currently running query thread. This reference is overwritten by
             * any newer key release events which happen while the first query
             * is still running, so that the result of the last update doesn't
             * get lost like it would if the all updates would be discarded
             * while a query is running.
             *
             * The fact that the next query thread is started from within the
             * previous one avoids the need for a separate thread polling from a
             * queue which is more intuitive, but unnecessary and requires
             * overriding finalize.
             *
             * @param keyEvent the key event passed from Swing
             */
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                //Listen to keyReleased rather than keyPressed in order to avoid
                //listening to Ctrl being pressed when using Ctrl+V or else.
                //Since queries might be slow (later if the database is full or
                //far), add skipping function which can only be realized with
                //a thread.
                String textFieldText = ((JTextComponent)comboBox.getEditor().getEditorComponent()).getText();
                assert textFieldText != null;
                LOGGER.trace(String.format("checking auto-completion for text field text '%s'",
                        textFieldText));
                pendingThread = new QueryThread(textFieldText);
                if(!queryRunning) {
                    queryRunning = true;
                    pendingThread.start();
                }else {
                    LOGGER.trace(String.format("queuing auto-completion check "
                            + "for text field text '%s' (will be discarded if "
                            + "a newer update arrives before the currently "
                            + "running query terminates)",
                            textFieldText));
                }
            }
        });
    }

    public JComboBox<String> getComboBox() {
        return comboBox;
    }

    private void installAutocomplete() {
        AutoCompleteSupport<String> autocomplete = AutoCompleteSupport.install(this.comboBox,
                comboBoxEventList,
                new StringTextFilterator());
        autocomplete.setFilterMode(TextMatcherEditor.CONTAINS);
    }

    private class StringTextFilterator implements TextFilterator<String> {

        protected StringTextFilterator() {
        }

        @Override
        public void getFilterStrings(List<String> baseList, String element) {
            baseList.add(element);
        }
    }

    @Override
    public void reset() {
        comboBox.setSelectedIndex(-1);
        comboBoxEventList.clear();
    }

    /**
     * A thread which handles queries for auto-completion candidates after text
     * field updates. Starts a new thread as last statement if
     * {@code pendingThread} has been overwritten by an update event.
     */
    @SuppressWarnings("PMD.AccessorMethodGeneration")
    private class QueryThread extends Thread {
        private final String textFieldText;

        protected QueryThread(String textFieldText) {
            super("string-auto-complete-panel-check-thread");
            this.textFieldText = textFieldText;
        }

        @Override
        @SuppressWarnings("PMD.AvoidCatchingThrowable")
        public void run() {
            List<String> checkResults;
            try {
                checkResults = check(textFieldText);
                if(!lastCheckResults.equals(checkResults)) {
                    try {
                        SwingUtilities.invokeAndWait(() -> {
                            //avoid `IllegalStateException: Events to DefaultEventComboBoxModel must arrive on the EDT - consider adding GlazedListsSwing.swingThreadProxyList(source) somewhere in your list pipeline`
                            comboBoxEventList.clear();
                            for(String checkResult : checkResults) {
                                comboBoxEventList.add(checkResult);
                            }
                        });
                    } catch (InterruptedException | InvocationTargetException ex) {
                        LOGGER.error("unexpected exception during update of auto-complete component occured",
                                ex);
                        issueHandler.handleUnexpectedException(new ExceptionMessage(ex));
                        return;
                            //It's fine to return if the thread has been interrupted
                            //which is an unexpected condition anyway and can't be
                            //handled better than reporting an unexpected exception
                    }
                    lastCheckResults = checkResults;
                }
            }catch(StorageException ex) {
                LOGGER.error("an exception during storage occured", ex);
                issueHandler.handle(new Message(ex));
            } catch(Throwable ex) {
                LOGGER.error("an unexpected exception during retrieval of auto-completion check results occured",
                        ex);
                issueHandler.handleUnexpectedException(new ExceptionMessage(ex));
                throw ex;
            }finally {
                if(pendingThread != null
                        && pendingThread != this) {
                    pendingThread.start();
                }else {
                    queryRunning = false;
                }
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        comboBox = new javax.swing.JComboBox<>();
        checkButton = new javax.swing.JButton();

        comboBox.setEditable(true);
        comboBox.setModel(comboBoxModel);

        checkButton.setText("Check");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(comboBox, 0, 338, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkButton))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(comboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(checkButton))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton checkButton;
    private javax.swing.JComboBox<String> comboBox;
    // End of variables declaration//GEN-END:variables
}
