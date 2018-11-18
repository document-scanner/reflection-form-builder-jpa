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

import de.richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Set;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JOptionPane;

/*
internal implementation notes:
- separating into a different class follows composition-over-inheritance (this
is a good idea no matter whether QueryListPanel would extend QueryPanel or not)
*/
/**
 * Provides control to trigger bidirectional mapping and set the field to be
 * used for such a mapping on the corresponding class. In case a relationship
 * annotation ({@link OneToOne}, {@link OneToMany} or {@link ManyToMany})
 * configures the mapping with a valid {@code mappedBy} parameter, the controls
 * to set the mapped field are disabled and display the field which is
 * configured because if JPA overwrites the field there's no sense in letting
 * the user configure anything.
 *
 * This component will probably be used in a {@link QueryPanel} or
 * {@link QueryListPanel}.
 *
 * @author richter
 */
@SuppressWarnings({"PMD.SingularField",
    "PMD.FieldDeclarationsShouldBeAtStartOfClass"
})
public class BidirectionalControlPanel extends javax.swing.JPanel {
    private static final long serialVersionUID = 1L;
    private final static String BIDIRECTIONAL_HELP_DIALOG_TEXT_STANDARD = "If this relationship is supposed to be bi-directional changes to this field will be reflected in the selected mapped field";
    private final Set<Field> mappedFieldCandidates;
    /**
     * The field on which a OneToOne annotation has been specified which will be
     * the selected item in the disabled field combobox (since the user doesn't
     * have a choice if mappedBy is specified).
     */
    private final Field mappedByField;
    private String bidirectionalHelpDialogText = BIDIRECTIONAL_HELP_DIALOG_TEXT_STANDARD;
    private final String bidirectionalHelpDialogTitle;
    private final DefaultComboBoxModel<Field> mappedFieldComboBoxModel = new DefaultComboBoxModel<>();
    private final DefaultListCellRenderer mappedFieldComboBoxRenderer = new DefaultListCellRenderer() {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            //apparently this method gets called with the `value` `" "` if the
            //model is empty which doesn't make any sense and sucks
            if(value instanceof Field) {
                Field valueCast = (Field) value;
                return super.getListCellRendererComponent(list,
                        String.format("%s.%s", valueCast.getDeclaringClass().getSimpleName(), valueCast.getName()),
                        index,
                        isSelected,
                        cellHasFocus);
            }else {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        }

    };
    private final Class<?> entityClass;

    private static String generateBidirectionalHelpDialogText(Class<?> entityClass) {
        return String.format("A bidirectional relationship can't be configured "
                + "because the entity class %s doesn't declare a corresponding "
                + "field",
                entityClass.getSimpleName());
    }

    /*
    internal implementation notes:
    - there's no need for a flag whether the components ought to be disabled or
    not because mappedByField can simply be checked
    */
    /**
     * Creates new form BidirectionalControlPanel
     * @param entityClass the entity class
     * @param bidirectionalHelpDialogTitle the help dialog title
     * @param mappedByField the field which is annotated with relationshiop
     *     annotation ({@link OneToOne}, {@link OneToMany} or
     *     {@link ManyToMany}) and has a valid {@code mappedBy} parameter
     * @param mappedFieldCandidates candidates for choice which can be mapped to
     *     the field this {@code BidirectionalControlPanel} is used for (can
     *     include {@code mappedByField} because properties can be mapped to
     *     themselves)
     * @throws FieldHandlingException if an exception occurs during field access
     */
    public BidirectionalControlPanel(Class<?> entityClass,
            String bidirectionalHelpDialogTitle,
            Field mappedByField,
            Set<Field> mappedFieldCandidates) throws FieldHandlingException {
        super();
        this.bidirectionalHelpDialogTitle = bidirectionalHelpDialogTitle;
        this.entityClass = entityClass;
        this.mappedByField = mappedByField;
        if(mappedFieldCandidates == null) {
            throw new IllegalArgumentException("mappedFieldCandidates mustn't be null");
        }
        this.mappedFieldCandidates = mappedFieldCandidates;
        for(Field mappedFieldCandidate : this.mappedFieldCandidates) {
            this.mappedFieldComboBoxModel.addElement(mappedFieldCandidate);
        }
        initComponents();
        if(mappedByField != null) {
            OneToOne mappedByFieldOneToOne = mappedByField.getAnnotation(OneToOne.class);
            OneToMany mappedByFieldOneToMany = mappedByField.getAnnotation(OneToMany.class);
            ManyToMany mappedByFieldManyToMany = mappedByField.getAnnotation(ManyToMany.class);

            Class<?> mappedByTargetClass = null;
            String mappedByTargetName = null;
            if(mappedByFieldOneToOne != null) {
                mappedByTargetClass = mappedByFieldOneToOne.targetEntity();
                mappedByTargetName = mappedByFieldOneToOne.mappedBy();
            }else if(mappedByFieldOneToMany != null) {
                mappedByTargetClass = mappedByFieldOneToMany.targetEntity();
                mappedByTargetName = mappedByFieldOneToMany.mappedBy();
            }else if(mappedByFieldManyToMany != null) {
                mappedByTargetClass = mappedByFieldManyToMany.targetEntity();
                mappedByTargetName = mappedByFieldManyToMany.mappedBy();
            }
            if(mappedByTargetName != null) {
                //mappedBy parameter has been specified on a relationship annotation
                if(mappedByTargetClass == null
                        || void.class.equals(mappedByTargetClass //indicates to use the generic type of the collection type of the field (see source of relationshiop annotations for details)
                        )) {
                    //targetEntity might not have been specified by retrieved from
                    //field generic type
                    if(mappedByField.getGenericType() instanceof ParameterizedType) {
                        ParameterizedType mappedByFieldParameterizedType = (ParameterizedType)mappedByField.getGenericType();
                        if(mappedByFieldParameterizedType.getActualTypeArguments().length != 1) {
                            throw new IllegalArgumentException(); //@TODO:
                        }
                        if(!(mappedByFieldParameterizedType.getActualTypeArguments()[0] instanceof Class)) {
                            throw new IllegalArgumentException(); //@TODO:
                        }
                        mappedByTargetClass = (Class<?>) mappedByFieldParameterizedType.getActualTypeArguments()[0];
                    }
                    if(mappedByTargetClass == null) {
                        throw new IllegalArgumentException("field annotated with replationship annotation and mappedBy attribute offers no way to figure out the target entity class"); //this shouldn't happen with a working JPA provider
                    }
                    Field mappedByTargetField;
                    try {
                        mappedByTargetField = mappedByTargetClass.getDeclaredField(mappedByTargetName);
                    } catch (NoSuchFieldException
                            | SecurityException ex) {
                        throw new FieldHandlingException(ex);
                    }
                    this.mappedFieldComboBoxModel.addElement(mappedByTargetField);
                    this.mappedFieldComboBox.setEnabled(false);
                    this.bidirectionalCheckBox.setEnabled(false);
                }
            }else {
                updateMappedFieldComponents();
            }
        }else {
            updateMappedFieldComponents();
        }
    }

    public Field getMappedField() {
        return (Field) this.mappedFieldComboBoxModel.getSelectedItem();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked",
        "PMD.AccessorMethodGeneration"
    })
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bidirectionalHelpButton = new javax.swing.JButton();
        bidirectionalCheckBox = new javax.swing.JCheckBox();
        mappedFieldComboBoxLabel = new javax.swing.JLabel();
        mappedFieldComboBox = new javax.swing.JComboBox<>();

        bidirectionalHelpButton.setText("?");
        bidirectionalHelpButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bidirectionalHelpButtonActionPerformed(evt);
            }
        });

        bidirectionalCheckBox.setText("Bidirectional");
        bidirectionalCheckBox.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bidirectionalCheckBoxActionPerformed(evt);
            }
        });

        mappedFieldComboBoxLabel.setText("Mapped field:");
        mappedFieldComboBoxLabel.setEnabled(false);

        mappedFieldComboBox.setModel(mappedFieldComboBoxModel);
        mappedFieldComboBox.setEnabled(false);
        mappedFieldComboBox.setRenderer(mappedFieldComboBoxRenderer);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(bidirectionalCheckBox)
                .addGap(18, 18, 18)
                .addComponent(bidirectionalHelpButton)
                .addGap(18, 18, 18)
                .addComponent(mappedFieldComboBoxLabel)
                .addGap(18, 18, 18)
                .addComponent(mappedFieldComboBox, 0, 429, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(bidirectionalCheckBox)
                    .addComponent(mappedFieldComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(mappedFieldComboBoxLabel)
                    .addComponent(bidirectionalHelpButton))
                .addGap(0, 0, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void bidirectionalHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bidirectionalHelpButtonActionPerformed
        JOptionPane.showMessageDialog(this,
            bidirectionalHelpDialogText,
            bidirectionalHelpDialogTitle,
            JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_bidirectionalHelpButtonActionPerformed

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void bidirectionalCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bidirectionalCheckBoxActionPerformed
        updateMappedFieldComponents();
    }//GEN-LAST:event_bidirectionalCheckBoxActionPerformed

    private void updateMappedFieldComponents() {
        if(this.mappedByField != null) {
            //the mappedBy argument has been specified on a OneToOne annotation
            //on a field of the opposite class
            if(this.mappedFieldComboBoxModel.getIndexOf(this.mappedByField) == -1) {
                //if mappedByField isn't contained JComboBox.setSelectedItem
                //fails
                this.mappedFieldComboBoxModel.addElement(mappedByField);
            }
            this.bidirectionalCheckBox.setEnabled(true);
            this.bidirectionalHelpDialogText = String.format("A field with the mappedBy "
                    + "argument has been specified on a field of %s. This means "
                    + "that the mapped field can't be selected by the user", entityClass.getSimpleName());
            mappedFieldComboBoxLabel.setEnabled(false);
            mappedFieldComboBox.setSelectedItem(this.mappedByField);
        }else {
            if(this.mappedFieldCandidates.isEmpty()) {
                this.bidirectionalCheckBox.setEnabled(false);
                this.bidirectionalHelpDialogText = generateBidirectionalHelpDialogText(entityClass);
                mappedFieldComboBoxLabel.setEnabled(false);
                mappedFieldComboBox.setEnabled(false);
            }else {
                mappedFieldComboBoxLabel.setEnabled(bidirectionalCheckBox.isSelected());
                mappedFieldComboBox.setEnabled(bidirectionalCheckBox.isSelected());
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox bidirectionalCheckBox;
    private javax.swing.JButton bidirectionalHelpButton;
    private javax.swing.JComboBox<Field> mappedFieldComboBox;
    private javax.swing.JLabel mappedFieldComboBoxLabel;
    // End of variables declaration//GEN-END:variables
}
