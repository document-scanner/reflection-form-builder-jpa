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

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import richtercloud.reflection.form.builder.ReflectionFormPanel;

/**
 *
 * @author richter
 */
/*
internal implementation notes:
- a dialog to edit an embeddable doesn't need a save button because all changes
are reflected in field updates and caller can update components providing values
based on reflection independently and non-redundant. Provide a cancel button,
though in order to avoid having the window close icon only as well as a hint
that updates occur "automatically".
- due to the fact that all GroupLayout setup has to be performed in code and not
with NetBeans GUI builder (because ReflectionFormPanel is passed in constructor)
(not necessarily a reason, but more intuitive and skill training anyway), make
this a non-NetBeans GUI builder class
*/
public class EmbeddableListPanelEditDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private final JButton closeButton = new JButton("Close");
    private final JLabel infoLabel = new JLabel("Updates to fields occur immediately without the need to explicitly save changes (and aren't reverted or discarded when the dialog is closed).");

    /**
     * Creates new {@code EmbeddableListPanelEditDialog}. This is always modal.
     * @param parent
     * @param reflectionFormPanel
     */
    public EmbeddableListPanelEditDialog(Frame parent, ReflectionFormPanel reflectionFormPanel) {
        super(parent,
                true //modal
        );
        GroupLayout layout = new GroupLayout(getContentPane());
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup().addContainerGap().addComponent(infoLabel).addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(closeButton))
                    .addComponent(reflectionFormPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                    .addComponent(infoLabel)
                    .addComponent(reflectionFormPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(closeButton))
        );
        setBounds(0, 0, 600, 500);

        this.closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EmbeddableListPanelEditDialog.this.setVisible(false);
            }
        });

        pack();
    }

}
