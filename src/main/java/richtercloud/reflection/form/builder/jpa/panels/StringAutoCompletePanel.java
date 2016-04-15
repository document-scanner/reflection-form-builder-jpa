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
import javax.persistence.EntityManager;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import richtercloud.reflection.form.builder.FieldRetriever;

/**
 *
 * @author richter
 * @param <T>
 */
/*
internal implementation notes:
- SwingX's AutoCompleteDecorator only allows entering values which are in the
model and thus can't be used for suggestions
- glazedlists not checked for far because installation is fairly uncomfortable
(i.e. more complicated than implementing auto-completion)
*/
public class StringAutoCompletePanel<T> extends AbstractStringPanel<T> {
    private static final long serialVersionUID = 1L;
    private final EventList<String> comboBoxEventList = new BasicEventList<>();
    private final DefaultEventComboBoxModel<String> comboBoxModel = new DefaultEventComboBoxModel<>(comboBoxEventList);
    private final FieldRetriever fieldRetriever;
    private List<T> lastCheckResults = new LinkedList<>();

    private static Field retrieveFieldByName(FieldRetriever fieldRetriever, Class<?> entityClass, String fieldName) {
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
     * @param entityManager
     * @param entityClass
     * @param fieldName
     * @param initialQueryLimit
     * @param fieldRetriever
     */
    public StringAutoCompletePanel(EntityManager entityManager,
            Class<T> entityClass,
            String fieldName,
            int initialQueryLimit,
            FieldRetriever fieldRetriever) {
        super(entityManager, entityClass, fieldName, initialQueryLimit);
        this.fieldRetriever = fieldRetriever;
        initComponents();
        this.comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for(StringPanelUpdateListener updateListener : getUpdateListeners()) {
                    updateListener.onUpdate(new StringPanelUpdateEvent((String) StringAutoCompletePanel.this.comboBox.getSelectedItem()));
                }
            }
        });
        final Field field = retrieveFieldByName(fieldRetriever, entityClass, fieldName);
        if(field == null) {
            throw new IllegalArgumentException();
        }

        if(SwingUtilities.isEventDispatchThread()) {
            installAutocomplete();
        }else {
            try {
                EventQueue.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        installAutocomplete();
                    }
                });
            } catch (InterruptedException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }

        this.comboBox.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                try {
                    List<T> checkResults = check((String)comboBox.getEditor().getItem());
                    if(!lastCheckResults.equals(checkResults)) {
                        comboBoxEventList.clear();
                        for(T checkResult : checkResults) {
                            comboBoxEventList.add(checkResult.toString());
                        }
                        lastCheckResults = checkResults;
                    }
                } catch (SecurityException | IllegalArgumentException ex) {
                    throw new RuntimeException(ex);
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
