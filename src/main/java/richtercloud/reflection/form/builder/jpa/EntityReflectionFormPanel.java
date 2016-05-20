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
package richtercloud.reflection.form.builder.jpa;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.lang.reflect.Field;
import java.util.Map;
import javax.persistence.Embeddable;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.validation.groups.Default;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.ReflectionFormPanelUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import richtercloud.reflection.form.builder.jpa.panels.IdPanel;
import richtercloud.reflection.form.builder.message.Message;
import richtercloud.reflection.form.builder.message.MessageHandler;

/**
 * A {@link JPAReflectionFormPanel} with an implementation to save and delete
 * entities (fails with an {@link Embeddable} because they're not stored like
 * entities). Also provides a "Reset" button to reset the form.
 *
 * @author richter
 */
/*
internal implementation notes:
- consider moving idPanel to a subclass if there's need for an
EntityReflectionFormPanel which shouldn't support automatic ID generation or
can't be provided with an IDPanel reference
- bi-directional references have to be handled in QueryPanel and QueryListPanel
because handling them here doesn't make sense
*/
public class EntityReflectionFormPanel extends JPAReflectionFormPanel<Object, EntityReflectionFormPanelUpdateListener> {
    private final static Logger LOGGER = LoggerFactory.getLogger(EntityReflectionFormPanel.class);
    private static final long serialVersionUID = 1L;
    private final JButton saveButton = new JButton("Save");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton resetButton = new JButton("Reset");
    private final MessageHandler messageHandler;
    private final boolean editingMode;
    private final FieldRetriever fieldRetriever;
    /**
     * {@link EntityReflectionFormPanel} needs an {@link IdPanel} if facilities
     * for automatic ID generation at saving should be provided.
     */
    private IdPanel idPanel;
    private final EntityValidator entityValidator;
    /*
    internal implementation notes:
    - keep a reference to the outer GroupLayout group because groups of
    GroupLayout can't be retrieved
    - don't overwrite super methods because they're used to arrage components of
    the main center part
    */
    private final GroupLayout.Group horizontalEntityControlsGroup = getLayout().createParallelGroup();
    private final GroupLayout.Group verticalEntityControlsGroup = getLayout().createSequentialGroup();

    /**
     *
     * @param entityManager
     * @param instance
     * @param entityClass
     * @param fieldMapping
     * @param messageHandler
     * @param editingMode if {@code true} the save button with update an exiting entity and a delete button will be provided, otherwise it will persist a new entity and no delete button will be provided
     * @param fieldRetriever
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public EntityReflectionFormPanel(EntityManager entityManager,
            Object instance,
            Class<?> entityClass,
            Map<Field, JComponent> fieldMapping,
            MessageHandler messageHandler,
            boolean editingMode,
            FieldRetriever fieldRetriever,
            FieldHandler fieldHandler) throws IllegalArgumentException, IllegalAccessException {
        super(entityManager,
                instance,
                entityClass,
                fieldMapping,
                fieldHandler);
        if(messageHandler == null) {
            throw new IllegalArgumentException("messageHandler mustn't be null");
        }
        this.messageHandler = messageHandler;
        if(fieldRetriever == null) {
            throw new IllegalArgumentException("fieldRetriever mustn't be null");
        }
        this.fieldRetriever = fieldRetriever;
        this.editingMode = editingMode;
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for(EntityReflectionFormPanelUpdateListener updateListener : getUpdateListeners()) {
                    updateListener.onReset(new ReflectionFormPanelUpdateEvent(ReflectionFormPanelUpdateEvent.INSTANCE_RESET,
                            null,
                            null));
                }
            }
        });
        horizontalEntityControlsGroup.addGroup(getHorizontalMainGroup());
        verticalEntityControlsGroup.addGroup(getVerticalMainGroup());
        //create button group
        GroupLayout.SequentialGroup buttonGroupHorizontal = getLayout().createSequentialGroup();
        GroupLayout.ParallelGroup buttonGroupVertical = getLayout().createParallelGroup(GroupLayout.Alignment.BASELINE);
        buttonGroupHorizontal.addComponent(saveButton);
        buttonGroupVertical.addComponent(saveButton);
        if(editingMode) {
            deleteButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    deleteButtonActionPerformed(evt);
                }
            });
            buttonGroupHorizontal.addComponent(deleteButton);
            buttonGroupVertical.addComponent(deleteButton);
        }
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reset();
            }
        });
        buttonGroupHorizontal.addComponent(resetButton);
        buttonGroupVertical.addComponent(resetButton);
        horizontalEntityControlsGroup.addGroup(buttonGroupHorizontal);
        verticalEntityControlsGroup.addGroup(buttonGroupVertical);
        getLayout().setHorizontalGroup(horizontalEntityControlsGroup);
        getLayout().setVerticalGroup(verticalEntityControlsGroup);
        this.validate();
        this.addContainerListener(new ContainerListener() {
            @Override
            public void componentAdded(ContainerEvent e) {
                if(!(e.getChild() instanceof IdPanel)) {
                    return;
                }
                if(idPanel == null) {
                    idPanel = (IdPanel) e.getChild();
                }else {
                    throw new IllegalArgumentException("This EntityReflectionFormPanel already contains an IdPanel. Adding a further IdPanel isn't allowed");
                }
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                if(e.getChild() instanceof IdPanel) {
                    idPanel = null;
                }
            }
        });
        this.entityValidator = new EntityValidator(fieldRetriever, messageHandler);
    }

    public GroupLayout.Group getVerticalEntityControlsGroup() {
        return verticalEntityControlsGroup;
    }

    public GroupLayout.Group getHorizontalEntityControlsGroup() {
        return horizontalEntityControlsGroup;
    }

    protected void deleteButtonActionPerformed(ActionEvent evt) {
        Object instance;
        try {
            instance = this.retrieveInstance();
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            String message = String.format("The following exception occured during persisting entity of type '%s': %s", this.getEntityClass(), ExceptionUtils.getRootCauseMessage(ex));
            LOGGER.debug(message, ex);
            messageHandler.handle(new Message(message,
                    JOptionPane.WARNING_MESSAGE,
                    "Persisting failed"));
            return;
        }
        //check getEntityManager.contains is unnecessary because a instances should be managed
        try {
            getEntityManager().getTransaction().begin();
            getEntityManager().remove(instance);
            getEntityManager().getTransaction().commit();
            this.messageHandler.handle(new Message(String.format("<html>removed entity of type '%s' successfully</html>", this.getEntityClass()),
                    JOptionPane.INFORMATION_MESSAGE,
                    "Removal failed"));
        }catch(EntityExistsException ex) {
            getEntityManager().getTransaction().rollback();
            handlePersistenceException(ex);

        }catch(RollbackException ex) {
             //cannot call entityManager.getTransaction().rollback() here because transaction isn' active
            handlePersistenceException(ex);
        }
        for(EntityReflectionFormPanelUpdateListener updateListener : this.getUpdateListeners()) {
            updateListener.onUpdate(new ReflectionFormPanelUpdateEvent(ReflectionFormPanelUpdateEvent.INSTANCE_DELETED,
                    null,
                    instance));
        }
    }

    protected void saveButtonActionPerformed(ActionEvent evt) {
        if(!this.idPanel.applyNextId()) {
            //can be called without any precautions
            //because it can be invoked multiple times and it doesn't matter if
            //the concrete ID value changes
            return;
        }
        Object instance;
        try {
            instance = this.retrieveInstance();
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException(ex); //doesn't make sense to expose to user
        }

        if(!this.entityValidator.validate(instance, Default.class)) {
            return;
        }
        if(!editingMode) {
            if(getEntityManager().contains(instance)) {
                this.messageHandler.handle(new Message("The instance is already saved. In order to edit an already saved instance use the editing mode.",
                        JOptionPane.WARNING_MESSAGE,
                        "Instance already saved"));
                return;
            }
            try {
                getEntityManager().getTransaction().begin();
                getEntityManager().persist(instance);
                getEntityManager().getTransaction().commit();
                this.messageHandler.handle(new Message(String.format("<html>persisted entity of type '%s' successfully</html>", this.getEntityClass()),
                        JOptionPane.INFORMATION_MESSAGE,
                        "Instance persisted successfully"));
            }catch(EntityExistsException ex) {
                getEntityManager().getTransaction().rollback();
                handlePersistenceException(ex);

            }catch(RollbackException ex) {
                 //cannot call entityManager.getTransaction().rollback() here because transaction isn' active
                handlePersistenceException(ex);
            }
        } else {
            if(!getEntityManager().contains(instance)) {
                this.messageHandler.handle(new Message("The instance is a new instance. In order to save a new instance use the creation mode.",
                        JOptionPane.WARNING_MESSAGE,
                        "Need to use creation mode"));
                return;
            }
            try {
                getEntityManager().getTransaction().begin();
                getEntityManager().merge(instance);
                getEntityManager().getTransaction().commit();
                this.messageHandler.handle(new Message(String.format("<html>updated entity of type '%s' successfully</html>", this.getEntityClass()),
                        JOptionPane.INFORMATION_MESSAGE,
                        "Instance updated successfully"));
            }catch(EntityExistsException ex) {
                getEntityManager().getTransaction().rollback();
                handlePersistenceException(ex);

            }catch(RollbackException ex) {
                 //cannot call entityManager.getTransaction().rollback() here because transaction isn' active
                handlePersistenceException(ex);
            }catch(IllegalStateException ex) {
                //if transaction is already active
                getEntityManager().getTransaction().rollback();
            }
        }
    }

    private void handlePersistenceException(Exception ex) {
        String message = String.format("the following exception occured during persisting entity of type '%s': %s",
                this.getEntityClass(),
                ExceptionUtils.getRootCauseMessage(ex) //since ExceptionUtils.getRootCause returns null if ex doesn't have a cause use ExceptionUtils.getRootCauseMessage (which always works)
        );
        LOGGER.debug(message, ex);
        this.messageHandler.handle(new Message(message,
                JOptionPane.ERROR_MESSAGE,
                "Exception occured"));
    }

}
