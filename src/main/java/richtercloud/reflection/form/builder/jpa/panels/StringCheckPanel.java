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

import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.swing.GroupLayout;
import javax.swing.ListSelectionModel;
import richtercloud.message.handler.MessageHandler;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.jpa.HistoryEntry;

/**
 * A panel to include an automatic check for existing entites with the value
 * entered in the textfield. Since it's too complicating and disturbing for the
 * user if an overview of existing entity's matching a property after a value
 * change, only a label with information how many entities exist is displayed as
 * well as a button to display a QueryPanel in a dialog.
 *
 * The QueryPanel will have the {@code LIKE}-query which has been used to check
 * if entities exist in the database and a query to select all entities of the
 * type declaring the string field in the list of predefined queries ("initial
 * history).
 *
 * Since the check button is always displayed the status label has to provide a
 * feedback for every change and check.
 *
 * @author richter
 * @param <T> the class containing the {@code String} field
 */
public class StringCheckPanel<T> extends AbstractStringPanel<T> {
    private static final long serialVersionUID = 1L;
    private QueryPanel<T> queryPanel;
    private final String initialValue;

    public StringCheckPanel(EntityManager entityManager,
            Class<T> entityClass,
            MessageHandler messageHandler,
            ReflectionFormBuilder reflectionFormBuilder,
            String initialValue,
            String fieldName,
            int initialQueryLimit,
            String bidirectionalHelpDialogTitle) throws IllegalArgumentException, IllegalAccessException {
        super(entityManager, entityClass, fieldName, initialQueryLimit);
        initComponents();
        this.queryPanel = new QueryPanel<>(entityManager,
                entityClass,
                messageHandler,
                reflectionFormBuilder,
                null, //initialValue
                null, //bidirectionalControlPanel (needs to be read-only)
                ListSelectionModel.SINGLE_SELECTION
        ); //will be reused by manipulating the queryComboBoxModel
        GroupLayout queryPanelDialogLayout = new GroupLayout(queryPanelDialog.getContentPane());
        queryPanelDialog.getContentPane().setLayout(queryPanelDialogLayout);
        queryPanelDialogLayout.setHorizontalGroup(
            queryPanelDialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(queryPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        queryPanelDialogLayout.setVerticalGroup(
            queryPanelDialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(queryPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        this.queryPanelDialog.pack();
        this.textField.setText(initialValue);
        this.initialValue = initialValue;
        reset0();
    }

    public String retrieveValue() {
        return this.textField.getText();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        queryPanelDialog = new javax.swing.JDialog();
        checkButton = new javax.swing.JButton();
        textField = new javax.swing.JTextField();
        showButton = new javax.swing.JButton();
        statusLabelScrollPane = new javax.swing.JScrollPane();
        statusLabel = new javax.swing.JTextArea();
        statusLabelLabel = new javax.swing.JLabel();

        queryPanelDialog.setBounds(new java.awt.Rectangle(0, 0, 500, 400));
        queryPanelDialog.setModal(true);

        javax.swing.GroupLayout queryPanelDialogLayout = new javax.swing.GroupLayout(queryPanelDialog.getContentPane());
        queryPanelDialog.getContentPane().setLayout(queryPanelDialogLayout);
        queryPanelDialogLayout.setHorizontalGroup(
            queryPanelDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        queryPanelDialogLayout.setVerticalGroup(
            queryPanelDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        checkButton.setText("Check");
        checkButton.setEnabled(false);
        checkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkButtonActionPerformed(evt);
            }
        });

        textField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textFieldKeyReleased(evt);
            }
        });

        showButton.setText("Show");
        showButton.setEnabled(false);
        showButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showButtonActionPerformed(evt);
            }
        });

        statusLabelScrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        statusLabel.setEditable(false);
        statusLabel.setColumns(20);
        statusLabel.setLineWrap(true);
        statusLabel.setRows(2);
        statusLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        statusLabelScrollPane.setViewportView(statusLabel);

        statusLabelLabel.setText("Quick search result:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(textField)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(checkButton))
            .addGroup(layout.createSequentialGroup()
                .addComponent(statusLabelLabel)
                .addGap(18, 18, 18)
                .addComponent(statusLabelScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 232, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(showButton))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(checkButton)
                    .addComponent(textField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(showButton)
                    .addComponent(statusLabelScrollPane)
                    .addComponent(statusLabelLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void checkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkButtonActionPerformed
        updateStatusLabel();
    }//GEN-LAST:event_checkButtonActionPerformed

    private void showButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showButtonActionPerformed
        this.queryPanel.getQueryComponent().getQueryComboBoxModel().addElement(new HistoryEntry(generateQueryText(this.textField.getText()),
                1, //usageCount
                new Date() //lastUsage
        ));
        this.queryPanelDialog.setVisible(true);
    }//GEN-LAST:event_showButtonActionPerformed

    private void textFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textFieldKeyReleased
        //has to be key released because otherwise the text of the text field isn't up-to-date
        this.checkButton.setEnabled(!this.textField.getText().isEmpty());
        updateStatusLabel();
        for(StringPanelUpdateListener updateListener : this.getUpdateListeners()) {
            updateListener.onUpdate(new StringPanelUpdateEvent(this.textField.getText()));
        }
    }//GEN-LAST:event_textFieldKeyReleased

    private void updateStatusLabel() {
        if(this.textField.getText().isEmpty()) {
            this.statusLabel.setText(" ");
            this.showButton.setEnabled(false);
            return;
        }
        List<T> checkResult = check(this.textField.getText());
        if(checkResult.isEmpty()) {
            this.statusLabel.setText(String.format("no existing entities with the specified value for this property are found in the database"));
            this.showButton.setEnabled(false);
        }else {
            this.statusLabel.setText(String.format("%d entities with the specified value for this property exist already in the database", checkResult.size()));
            this.showButton.setEnabled(true);
        }
    }

    public void reset() {
        reset0();
    }

    private void reset0() {
        if(initialValue != null && !initialValue.isEmpty()) {
            this.textField.setText(initialValue);
            this.checkButton.setEnabled(true);
        }else {
            this.textField.setText("");
            this.checkButton.setEnabled(false);
        }
        updateStatusLabel();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton checkButton;
    private javax.swing.JDialog queryPanelDialog;
    private javax.swing.JButton showButton;
    private javax.swing.JTextArea statusLabel;
    private javax.swing.JLabel statusLabelLabel;
    private javax.swing.JScrollPane statusLabelScrollPane;
    private javax.swing.JTextField textField;
    // End of variables declaration//GEN-END:variables
}
