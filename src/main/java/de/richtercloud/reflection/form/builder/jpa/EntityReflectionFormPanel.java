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
package de.richtercloud.reflection.form.builder.jpa;

import de.richtercloud.message.handler.ConfirmMessageHandler;
import de.richtercloud.message.handler.ExceptionMessage;
import de.richtercloud.message.handler.IssueHandler;
import de.richtercloud.message.handler.Message;
import de.richtercloud.reflection.form.builder.ReflectionFormPanelUpdateEvent;
import de.richtercloud.reflection.form.builder.ResetException;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import de.richtercloud.reflection.form.builder.jpa.idapplier.IdApplicationException;
import de.richtercloud.reflection.form.builder.jpa.idapplier.IdApplier;
import de.richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import de.richtercloud.reflection.form.builder.storage.StorageException;
import java.awt.event.ActionEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.validation.groups.Default;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
internal implementation notes:
- consider moving idPanel to a subclass if there's need for an
EntityReflectionFormPanel which shouldn't support automatic ID generation or
can't be provided with an IDPanel reference
- bi-directional references have to be handled in QueryPanel and QueryListPanel
because handling them here doesn't make sense
*/
/**
 * A {@link JPAReflectionFormPanel} with an implementation to save and delete
 * entities (fails with an {@link Embeddable} because they're not stored like
 * entities). Also provides a "Reset" button to reset the form.
 *
 * When new or edited entities are saved, validation occurs for the
 * {@link Default} validation group which makes the save action fail if the
 * validation fails.
 *
 * In order to allow a warning about certain conditions to be
 * forwarded to the user (and eventually request her_his confirmation) both the
 * {@link Warning} validation group is validated and constraint validations are
 * handled with a {@link ConfirmMessageHandler} which allows to cancel saving in
 * it <i>and</i> {@link EntityValidator#handleWarnings(java.lang.Object) } is
 * used in order to allow forwarding warning which require resources which need
 * to be passed in a constructor (validation API doesn't allow retrieval of
 * resources except for injection in Java EE).
 *
 * @author richter
 */
public class EntityReflectionFormPanel extends JPAReflectionFormPanel<Object, EntityReflectionFormPanelUpdateListener> {
    private final static Logger LOGGER = LoggerFactory.getLogger(EntityReflectionFormPanel.class);
    private static final long serialVersionUID = 1L;
    private final JButton saveButton = new JButton("Save");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton resetButton = new JButton("Reset");
    private final IssueHandler issueHandler;
    private final boolean editingMode;
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
    private final IdApplier idApplier;
    private final JPAFieldRetriever fieldRetriever;

    /*
    internal implementation notes:
    - no need to pass refences to @Id annotated fields because they can be
    retrieved from JPACachedFieldRetriever
    */
    /**
     * Creates a new EntityReflectionFormPanel.
     * @param storage the storage to use
     * @param instance the instance to use
     * @param entityClass the entity class
     * @param fieldMapping the field mapping
     * @param confirmMessageHandler the confirm message handler
     * @param issueHandler the issue handler to use
     * @param editingMode if {@code true} the save button with update an exiting
     *     entity and a delete button will be provided, otherwise it will
     *     persist a new entity and no delete button will be provided
     * @param fieldRetriever the field retriever
     * @param idApplier the id applier
     * @param warningHandlers the warning handlers
     * @param fieldHandler the {@link FieldHandler} to perform reset actions
     * @throws IllegalArgumentException if {@code issueHandler} is {@code null}
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public EntityReflectionFormPanel(PersistenceStorage storage,
            Object instance,
            Class<?> entityClass,
            Map<Field, JComponent> fieldMapping,
            IssueHandler issueHandler,
            ConfirmMessageHandler confirmMessageHandler,
            boolean editingMode,
            JPAFieldRetriever fieldRetriever,
            FieldHandler fieldHandler,
            IdApplier idApplier,
            Map<Class<?>, WarningHandler<?>> warningHandlers) {
        super(storage,
                instance,
                entityClass,
                fieldMapping,
                fieldHandler);
        if(issueHandler == null) {
            throw new IllegalArgumentException("messageHandler mustn't be null");
        }
        this.issueHandler = issueHandler;
        if(fieldRetriever == null) {
            throw new IllegalArgumentException("fieldRetriever mustn't be null");
        }
        this.editingMode = editingMode;
        if(idApplier == null) {
            throw new IllegalArgumentException("idApplier mustn't be null");
        }
        this.idApplier = idApplier;
        if(fieldRetriever == null) {
            throw new IllegalArgumentException("fieldRetriever mustn't be null");
        }
        this.fieldRetriever = fieldRetriever;
        saveButton.addActionListener((ActionEvent evt) -> {
            saveButtonActionPerformed(evt);
        });
        resetButton.addActionListener((ActionEvent e) -> {
            for(EntityReflectionFormPanelUpdateListener updateListener : getUpdateListeners()) {
                updateListener.onReset(new ReflectionFormPanelUpdateEvent(ReflectionFormPanelUpdateEvent.INSTANCE_RESET,
                        null,
                        null));
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
            deleteButton.addActionListener((ActionEvent evt) -> {
                deleteButtonActionPerformed(evt);
            });
            buttonGroupHorizontal.addComponent(deleteButton);
            buttonGroupVertical.addComponent(deleteButton);
        }
        resetButton.addActionListener((ActionEvent e) -> {
            try {
                reset();
            } catch (ResetException ex) {
                LOGGER.error("unexpected exception during resetting of component occured",
                        ex);
                issueHandler.handleUnexpectedException(new ExceptionMessage(ex));
            }
        });
        buttonGroupHorizontal.addComponent(resetButton);
        buttonGroupVertical.addComponent(resetButton);
        horizontalEntityControlsGroup.addGroup(buttonGroupHorizontal);
        verticalEntityControlsGroup.addGroup(buttonGroupVertical);
        getLayout().setHorizontalGroup(horizontalEntityControlsGroup);
        getLayout().setVerticalGroup(verticalEntityControlsGroup);
        this.entityValidator = new EntityValidator(fieldRetriever,
                confirmMessageHandler,
                warningHandlers);
    }

    public GroupLayout.Group getVerticalEntityControlsGroup() {
        return verticalEntityControlsGroup;
    }

    public GroupLayout.Group getHorizontalEntityControlsGroup() {
        return horizontalEntityControlsGroup;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    protected void deleteButtonActionPerformed(ActionEvent evt) {
        Object instance = this.retrieveInstance();
        //check getEntityManager.contains is unnecessary because a instances should be managed
        try {
            getStorage().store(instance);
            this.issueHandler.handle(new Message(String.format("<html>removed entity of type '%s' successfully</html>", this.getEntityClass()),
                    JOptionPane.INFORMATION_MESSAGE,
                    "Removal succeeded"));
        }catch(StorageException ex) {
            handlePersistenceException(ex);
        }
        for(EntityReflectionFormPanelUpdateListener updateListener : this.getUpdateListeners()) {
            updateListener.onUpdate(new ReflectionFormPanelUpdateEvent(ReflectionFormPanelUpdateEvent.INSTANCE_DELETED,
                    null,
                    instance));
        }
    }

    private Set<JComponent> retrieveIdFieldComponents(Set<Field> idFields) {
        Set<JComponent> retValue = new HashSet<>();
        for(Field idField : idFields) {
            JComponent idFieldComponent = this.getFieldMapping().get(idField);
            assert idFieldComponent != null;
            retValue.add(idFieldComponent);
        }
        return retValue;
    }

    /*
    internal implementation notes:
    - It's fine to keep the JPA validation routines here because this is the
    JPA module and validation is legitimate no matter which Storage backend is
    used.
    */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    protected void saveButtonActionPerformed(ActionEvent evt) {
        Object instance = this.retrieveInstance();
            //might be in all sorts of JPA states (attached, detached, etc.)

        if(!editingMode) {
            //only (try to) change id if not in editing mode
            Set<Field> idFields = this.fieldRetriever.getIdFields(getEntityClass());
            Set<JComponent> idFieldComponents = retrieveIdFieldComponents(idFields);
            try {
                idApplier.applyId(instance,
                        idFieldComponents);
            } catch (IdApplicationException ex) {
                issueHandler.handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
            }
        }

        try {
            this.entityValidator.validate(instance, Default.class);
        }catch(EntityValidationException ex) {
            issueHandler.handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
            return;
        }
        if(!this.entityValidator.handleWarnings(instance)) {
            return;
        }
        //There's no sense in checking whether instance is contained in
        //entityManager because this test is only relevant in order to avoid
        //persisting instances with duplicate IDs which can just be tried and
        //fail with useful feedback
        if(!editingMode) {
            try {
                //check whether the entity with the specified ID has already
                //been persisted in order to avoid a potentially incomprehensive
                //error message
                Object primaryKey;
                Set<Field> idFields = fieldRetriever.getIdFields(getEntityClass());
                assert !idFields.isEmpty();
                boolean keySet = false;
                if(idFields.size() == 1) {
                    try {
                        primaryKey = idFields.iterator().next().get(instance);
                        if(primaryKey != null) {
                            keySet = true;
                        }
                    } catch (IllegalArgumentException | IllegalAccessException ex) {
                        LOGGER.error("unexpected exception during saving of entity changes occured",
                                ex);
                        issueHandler.handleUnexpectedException(new ExceptionMessage(ex));
                        return;
                    }
                }else {
                    IdClass idClass = getEntityClass().getDeclaredAnnotation(IdClass.class);
                    if(idClass == null) {
                        throw new IllegalStateException(String.format("class '%s' with more than one fields annotated with '%s' has no '%s' annotation which violates JPA specifications", getEntityClass(), Id.class, IdClass.class));
                    }
                    Class<?> primaryKeyClass = idClass.value();
                    Constructor<?> primaryKeyClassConstructor;
                    try {
                        primaryKeyClassConstructor = primaryKeyClass.getDeclaredConstructor();
                    } catch (NoSuchMethodException | SecurityException ex) {
                        throw new IllegalStateException(String.format("Class '%s' which is used as value of '%s' annotation on '%s' failed. Make sure it has an accessible zero-argument constructor.",
                                primaryKeyClass,
                                IdClass.class,
                                getEntityClass()),
                                ex);
                    }
                    try {
                        primaryKey = primaryKeyClassConstructor.newInstance();
                        for(Field idField : idFields) {
                            Field primaryKeyIdField;
                            try {
                                primaryKeyIdField = primaryKeyClass.getDeclaredField(idField.getName());
                            } catch (NoSuchFieldException | SecurityException ex) {
                                LOGGER.error("unexpected exception during saving of entity occured",
                                        ex);
                                issueHandler.handleUnexpectedException(new ExceptionMessage(ex));
                                return;
                            }
                            Object primaryKeyIdFieldValue = idField.get(instance);
                            if(primaryKeyIdFieldValue != null) {
                                keySet = true;
                            }
                            primaryKeyIdField.set(primaryKey,
                                    primaryKeyIdFieldValue);
                        }
                    } catch (InstantiationException
                            | IllegalAccessException
                            | IllegalArgumentException
                            | InvocationTargetException ex) {
                        LOGGER.error("unexpected exception during saving of entity occured",
                                ex);
                        issueHandler.handleUnexpectedException(new ExceptionMessage(ex));
                        return;
                    }
                }
                if(keySet) {
                    //otherwise either the one ID field value is null or all
                    //ID field values are null
                    Object existingInstance = getStorage().retrieve(primaryKey,
                            getEntityClass());
                    if(existingInstance != null) {
                        this.issueHandler.handle(new Message(String.format("An instance of type '%s' with ID '%s' has already been persisted. Change the ID or edit the existing instance in editing mode.",
                                        getEntityClass(),
                                        primaryKey),
                                JOptionPane.ERROR_MESSAGE,
                                "Entity already persisted"));
                        return;
                    }
                }

                //persist
                getStorage().store(instance);
                this.issueHandler.handle(new Message(String.format("<html>persisted entity of type '%s' successfully</html>", this.getEntityClass()),
                        JOptionPane.INFORMATION_MESSAGE,
                        "Instance persisted successfully"));
            }catch(StorageException ex) {
                handlePersistenceException(ex);
            }
        } else {
            try {
                getStorage().update(instance);
                this.issueHandler.handle(new Message(String.format("<html>Updated entity of type '%s' successfully.</html>", this.getEntityClass()),
                        JOptionPane.INFORMATION_MESSAGE,
                        "Instance updated successfully"));
            }catch(StorageException ex) {
                handlePersistenceException(ex);
            }
        }
    }

    private void handlePersistenceException(Exception ex) {
        String message = String.format("the following exception occured during persisting entity of type '%s': %s",
                this.getEntityClass(),
                ExceptionUtils.getRootCauseMessage(ex) //since ExceptionUtils.getRootCause returns null if ex doesn't have a cause use ExceptionUtils.getRootCauseMessage (which always works)
        );
        LOGGER.debug(message, ex);
        this.issueHandler.handle(new Message(message,
                JOptionPane.ERROR_MESSAGE,
                "Exception occured"));
    }

}
