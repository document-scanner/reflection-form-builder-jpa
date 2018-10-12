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

import de.richtercloud.message.handler.Message;
import de.richtercloud.message.handler.MessageHandler;
import de.richtercloud.reflection.form.builder.jpa.idapplier.IdApplicationException;
import de.richtercloud.reflection.form.builder.jpa.idapplier.IdApplier;
import de.richtercloud.reflection.form.builder.panels.NumberPanel;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashSet;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.LayoutStyle;

/**
 *
 * @author richter
 */
public class LongIdPanel extends NumberPanel<Long> {
    private static final long serialVersionUID = 1L;
    private final Object entity;
    private final MessageHandler messageHandler;
    private final JButton nextIdButton = new JButton();
    private final IdApplier idApplier;

    public LongIdPanel(Object entity,
            Long initialValue,
            MessageHandler messageHandler,
            boolean readOnly,
            IdApplier idApplier) {
        super(initialValue,
                readOnly);
        this.entity = entity;
        this.messageHandler = messageHandler;
        if(idApplier == null) {
            throw new IllegalArgumentException("idApplier mustn't be null");
        }
        this.idApplier = idApplier;
        nextIdButton.setText("Next id");
        nextIdButton.addActionListener((ActionEvent evt) -> {
            nextIdButtonActionPerformed(evt);
        });

        GroupLayout layout = getLayout();
        layout.setHorizontalGroup(layout.createSequentialGroup().addGroup(this.getLayoutHorizontalGroup())
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(nextIdButton)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(this.getLayoutVerticalGroup())
                .addComponent(nextIdButton)
        );
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void nextIdButtonActionPerformed(ActionEvent evt) {
        try {
            idApplier.applyId(entity, new HashSet<>(Arrays.asList(this)));
        } catch (IdApplicationException ex) {
            messageHandler.handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
        }
    }
}
