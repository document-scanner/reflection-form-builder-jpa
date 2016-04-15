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

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.LayoutStyle;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.jpa.EntityValidator;
import richtercloud.reflection.form.builder.jpa.IdGenerator;
import richtercloud.reflection.form.builder.message.MessageHandler;
import richtercloud.reflection.form.builder.panels.NumberPanel;
import richtercloud.reflection.form.builder.panels.NumberPanelUpdateListener;

/**
 *
 * @author richter
 */
public class LongIdPanel extends NumberPanel<Long> implements IdPanel {
    private static final long serialVersionUID = 1L;
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private final IdGenerator idGenerator;
    private final Object entity;
    private final MessageHandler messageHandler;
    private JButton nextIdButton = new JButton();
    private final EntityValidator entityValidator;

    public LongIdPanel(IdGenerator idGenerator,
            Object entity,
            Long initialValue,
            MessageHandler messageHandler,
            FieldRetriever fieldRetriever) {
        super(initialValue);
        this.idGenerator = idGenerator;
        this.entity = entity;
        this.messageHandler = messageHandler;
        nextIdButton.setText("Next id");
        nextIdButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextIdButtonActionPerformed(evt);
            }
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

        this.entityValidator = new EntityValidator(fieldRetriever, messageHandler);
    }

    @Override
    public boolean applyNextId() {
        if(!this.entityValidator.validate(entity, IdGenerationValidation.class)) {
            return false;
        }
        Long nextId = idGenerator.getNextId(this.entity);
        this.setValue(nextId);
        return true;
    }

    private void nextIdButtonActionPerformed(java.awt.event.ActionEvent evt) {
        applyNextId();
    }

}
