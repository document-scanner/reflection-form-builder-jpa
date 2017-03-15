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
package richtercloud.reflection.form.builder.jpa.panels;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;
import ca.odell.glazedlists.swing.DefaultEventComboBoxModel;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import richtercloud.message.handler.ExceptionMessage;
import richtercloud.message.handler.IssueHandler;
import richtercloud.message.handler.Message;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.reflection.form.builder.storage.StorageException;

/**
 * Displays existing values in the database in a popup menu in order to inform
 * about similar or equal values which have already been used and persisted.
 *
 * Performance depends on {@link PersistenceStorage#runQuery(java.lang.String, java.lang.Class, int, int) }
 * and the right priority since {@code comboBox} queries for every key-pressed
 * event.
 *
 * @author richter
 */
/*
internal implementation notes:
- SwingX's AutoCompleteDecorator only allows entering values which are in the
model and thus can't be used for suggestions
- glazedlists not checked for far because installation is fairly uncomfortable
(i.e. more complicated than implementing auto-completion)
- A previous undocumented implementation added entities.toString in the popup
instead of the field values. Adding field values makes more sense.
- removed parameter T because it didn't seem to have any use, document well when
adding again
*/
public class StringAutoCompletePanel extends AbstractStringPanel {
    private static final long serialVersionUID = 1L;
    private final static Logger LOGGER = LoggerFactory.getLogger(StringAutoCompletePanel.class);
    private final EventList<String> comboBoxEventList = new BasicEventList<>();
    private final DefaultEventComboBoxModel<String> comboBoxModel = new DefaultEventComboBoxModel<>(comboBoxEventList);
    private final FieldRetriever fieldRetriever;
    private List<?> lastCheckResults = new LinkedList<>();
    private final IssueHandler issueHandler;

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
     * @param storage
     * @param entityClass
     * @param initialValue
     * @param fieldName
     * @param initialQueryLimit
     * @param fieldRetriever the {@link FieldRetriever} to use for searching the
     * field with {@code fieldName}
     */
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
        this.fieldRetriever = fieldRetriever;
        if(issueHandler == null) {
            throw new IllegalArgumentException("issueHandler mustn't be null");
        }
        this.issueHandler = issueHandler;
        initComponents();
        this.comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for(StringPanelUpdateListener updateListener : getUpdateListeners()) {
                    updateListener.onUpdate(new StringPanelUpdateEvent((String) StringAutoCompletePanel.this.comboBox.getSelectedItem()));
                }
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
                EventQueue.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        StringAutoCompletePanel.this.comboBoxEventList.add(initialValue);
                            //avoid `java.lang.IllegalStateException: Events to DefaultEventComboBoxModel must arrive on the EDT - consider adding GlazedListsSwing.swingThreadProxyList(source) somewhere in your list pipeline`
                        installAutocomplete();
                    }
                });
            } catch (InterruptedException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }

        this.comboBox.setSelectedItem(initialValue);
        this.comboBox.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            private boolean queryRunning = false;
            @Override
            public void keyReleased(KeyEvent e) {
                //Listen to keyReleased rather than keyPressed in order to avoid
                //listening to Ctrl being pressed when using Ctrl+V or else.
                //Since queries might be slow (later if the database is full or
                //far), add skipping function which can only be realized with
                //a thread.
                String textFieldText = ((JTextComponent)comboBox.getEditor().getEditorComponent()).getText();
                assert textFieldText != null;
                if(!queryRunning) {
                    queryRunning = true;
                    LOGGER.trace(String.format("checking auto-completion for text field text '%s'", textFieldText));
                    Thread checkThread = new Thread(() -> {
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
                                    throw new RuntimeException(ex);
                                }
                                lastCheckResults = checkResults;
                            }
                        }catch(StorageException ex) {
                            LOGGER.error("an exception during storage occured", ex);
                            issueHandler.handle(new Message(ex));
                        } catch(Throwable ex) {
                            LOGGER.error("an unexpected exception during retrieval of auto-completion check results occured", ex);
                            issueHandler.handleUnexpectedException(new ExceptionMessage(ex));
                        }finally {
                            queryRunning = false;
                        }
                    },
                            "string-auto-complete-panel-check-thread");
                    checkThread.start();
                }else {
                    LOGGER.trace(String.format("skipping auto-completion check for text field text '%s'", textFieldText));
                }
            }
        });
    }

    public JComboBox<String> getComboBox() {
        return comboBox;
    }

    private void installAutocomplete() {
        AutoCompleteSupport<String> autocomplete = AutoCompleteSupport.install(StringAutoCompletePanel.this.comboBox,
                comboBoxEventList,
                new StringTextFilterator());
        autocomplete.setFilterMode(TextMatcherEditor.CONTAINS);
    }

    private class StringTextFilterator implements TextFilterator<String> {

        StringTextFilterator() {
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
