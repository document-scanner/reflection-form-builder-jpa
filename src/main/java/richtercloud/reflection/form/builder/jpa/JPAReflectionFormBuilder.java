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

import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import richtercloud.message.handler.ConfirmMessageHandler;
import richtercloud.message.handler.IssueHandler;
import richtercloud.message.handler.Message;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.ReflectionFormPanel;
import richtercloud.reflection.form.builder.ResetException;
import richtercloud.reflection.form.builder.TransformationException;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.MappedFieldUpdateEvent;
import richtercloud.reflection.form.builder.jpa.annotations.UsedUpdate;
import richtercloud.reflection.form.builder.jpa.idapplier.IdApplicationException;
import richtercloud.reflection.form.builder.jpa.idapplier.IdApplier;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.reflection.form.builder.storage.StorageException;

/**
 * Handles generation of {@link JPAReflectionFormPanel} from root entity class
 * using JPA annoations like {@link Id} (using {@link NumberPanel{@link Embedded} (using nested/recursive form generation).
 *
 * A default value for the {@code classMapping} property isn't provided, but one
 * can easily be created with {@link #generateClassMappingDefaultJPA(richtercloud.reflection.form.builder.message.MessageHandler) }, {@link #generateClassMapping(richtercloud.reflection.form.builder.message.MessageHandler, richtercloud.reflection.form.builder.components.AmountMoneyUsageStatisticsStorage, richtercloud.reflection.form.builder.components.AmountMoneyCurrencyStorage) }, {@link #generateClassMappingAmountMoneyFieldHandler(richtercloud.reflection.form.builder.components.AmountMoneyUsageStatisticsStorage, richtercloud.reflection.form.builder.components.AmountMoneyCurrencyStorage, richtercloud.reflection.form.builder.message.MessageHandler) } or {@link #generateClassMappingDefault(richtercloud.reflection.form.builder.message.MessageHandler) }.
 *
 * @author richter
 */
/*
internal implementation notes:
- before changing method signatures, see internal implementation notes of
FieldHandler for how to provide a portable interface
*/
public class JPAReflectionFormBuilder extends ReflectionFormBuilder<JPAFieldRetriever> {
    private PersistenceStorage storage;
    private final IdApplier idApplier;
    private final ConfirmMessageHandler confirmMessageHandler;
    private final Map<Class<?>, WarningHandler<?>> warningHandlers;
    private final Map<Object, Set<Component>> idFieldComponentMap = new HashMap<>();

    public JPAReflectionFormBuilder(PersistenceStorage storage,
            String fieldDescriptionDialogTitle,
            IssueHandler issueHandler,
            ConfirmMessageHandler confirmMessageHandler,
            JPAFieldRetriever fieldRetriever,
            IdApplier idApplier,
            Map<Class<?>, WarningHandler<?>> warningHandlers) {
        super(fieldDescriptionDialogTitle,
                issueHandler,
                fieldRetriever);
        if(storage == null) {
            throw new IllegalArgumentException("entityManager mustn't be null");
        }
        this.storage = storage;
        if(idApplier == null) {
            throw new IllegalArgumentException("idApplier mustn't be null");
        }
        this.idApplier = idApplier;
        this.confirmMessageHandler = confirmMessageHandler;
        this.warningHandlers = warningHandlers;
    }

    public Map<Object, Set<Component>> getIdFieldComponentMap() {
        return idFieldComponentMap;
    }

    public ReflectionFormPanel transformEntityClass(Class<?> entityClass,
            Object entityToUpdate,
            boolean editingMode,
            FieldHandler fieldHandler) throws TransformationException,
            NoSuchFieldException,
            ResetException {
        final Map<Field, JComponent> fieldMapping = new HashMap<>();
        Object instance = prepareInstance(entityClass, entityToUpdate);
        ReflectionFormPanel retValue = new EntityReflectionFormPanel(storage,
                instance,
                entityClass,
                fieldMapping,
                this.getIssueHandler(),
                confirmMessageHandler,
                editingMode,
                this.getFieldRetriever(),
                fieldHandler,
                this.idApplier,
                this.warningHandlers);
        transformClass(entityClass,
                instance,
                fieldMapping,
                retValue,
                fieldHandler);
        Set<Component> idFieldComponents = idFieldComponentMap.get(instance);
        try {
            idApplier.applyId(instance,
                    idFieldComponents);
                //can't be moved to prepareInstance because idFieldComponentMap
                //is still empty then
        } catch (IdApplicationException ex) {
            throw new TransformationException(ex);
        }
        return retValue;
    }

    @Override
    public ReflectionFormPanel transformEntityClass(Class<?> entityClass,
            Object entityToUpdate,
            FieldHandler fieldHandler) throws TransformationException,
            NoSuchFieldException,
            ResetException {
        return transformEntityClass(entityClass,
                entityToUpdate,
                false, //editingMode
                fieldHandler);
    }

    public EmbeddableReflectionFormPanel<?> transformEmbeddable(Class<?> embeddableClass,
            Object instance,
            FieldHandler fieldHandler) throws TransformationException,
            NoSuchFieldException,
            ResetException {
        final Map<Field, JComponent> fieldMapping = new HashMap<>();
        Object instance0 = prepareInstance(embeddableClass, instance);
        EmbeddableReflectionFormPanel<Object> retValue = new EmbeddableReflectionFormPanel<>(storage,
                instance0,
                embeddableClass,
                fieldMapping,
                fieldHandler);
        transformClass(embeddableClass,
                instance0,
                fieldMapping,
                retValue,
                fieldHandler);
        return retValue;
    }

    /**
     * Sets the new field value from {@code event} on {@code field} using
     * {@code instance} (see
     * {@link Field#set(java.lang.Object, java.lang.Object) } for details).
     *
     * Checks whether {@code event} is a {@link MappedFieldUpdateEvent} in which
     * case it handles setting values on mapped fields (as specified in
     * {@link MappedFieldUpdateEvent#getMappedField() } as follows:<ul>
     * <li>It fails if the mapped field's value is {@code null} (this avoids
     * dealing with instantiation of collection fields and uninitialized fields
     * are rare in entites). This might be handled one day.</li>
     * <li>In a many-to-many and one-to-many relationship it adds a reference to
     * {@code instance} to all items in {@code event}'s value.</li>
     * <li>In a one-to-one and many-to-one relationship it sets the value of the
     * mapped field to {@code instance}</li>
     * </ul>
     * @param event
     * @param field
     * @param instance
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    @Override
    protected void onFieldUpdate(FieldUpdateEvent event,
            Field field,
            Object instance) throws IllegalArgumentException,
            IllegalAccessException,
            InvocationTargetException {
        Object eventNewValue = event.getNewValue();
        Class<?> fieldType = field.getType();
        if(eventNewValue != null) {
            //handle UsedUpdate annotated method
            Class<?> fieldTypePointer = fieldType;
            List<Method> fieldTypeMethods = new LinkedList<>();
            while(fieldTypePointer != null
                    && !fieldTypePointer.equals(Object.class)) {
                fieldTypeMethods.addAll(Arrays.asList(fieldTypePointer.getDeclaredMethods()));
                fieldTypePointer = fieldTypePointer.getSuperclass();
            }
            Method usedUpdateMethod = null;
            for(Method fieldClassMethod : fieldTypeMethods) {
                if(fieldClassMethod.getDeclaredAnnotation(UsedUpdate.class) != null) {
                    if(usedUpdateMethod != null) {
                        throw new IllegalStateException(String.format("Both methods %s and %s of class %s have annotation %s",
                                fieldClassMethod,
                                usedUpdateMethod,
                                field.getType().getName(),
                                UsedUpdate.class.getName()));
                    }
                    usedUpdateMethod = fieldClassMethod;
                }
            }
            if(usedUpdateMethod != null) {
                if(!Modifier.isPrivate(usedUpdateMethod.getModifiers())) {
                    throw new IllegalStateException(String.format("method %s of class %s has annotation %s and isn't private",
                            usedUpdateMethod,
                            fieldType.getName(),
                            UsedUpdate.class.getName()));
                }
                if(!usedUpdateMethod.getReturnType().equals(void.class)) {
                    throw new IllegalArgumentException(String.format("method %s of class %s has annotation %s and doesn't return void",
                            usedUpdateMethod,
                            fieldType.getName(),
                            UsedUpdate.class.getName()));
                }
                if(usedUpdateMethod.getParameterCount() != 0) {
                    throw new IllegalStateException(String.format("method %s of class %s has annotation %s and declares arguments",
                            usedUpdateMethod,
                            fieldType.getName(),
                            UsedUpdate.class.getName()));
                }
                usedUpdateMethod.setAccessible(true);
                usedUpdateMethod.invoke(eventNewValue);
                try {
                    storage.registerPostStoreCallback(instance, (object) -> {
                        try {
                            storage.update(eventNewValue);
                        } catch (StorageException ex) {
                            getIssueHandler().handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
                        }
                    });
                } catch (StorageException ex) {
                    getIssueHandler().handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
                }
            }
        } //eventNewValue != null

        //handle MappedFieldUpdateEvent
        if(event instanceof MappedFieldUpdateEvent) {
            MappedFieldUpdateEvent eventCast = (MappedFieldUpdateEvent) event;
            field.setAccessible(true);
            Field mappedField = eventCast.getMappedField();
            if(mappedField != null) {
                Object fieldCurrentValue = field.get(instance);
                Set<Field> fieldTypeFields = new HashSet<>(Arrays.asList(field.getDeclaringClass().getDeclaredFields()));
                if(fieldTypeFields.contains(mappedField)) {
                    throw new IllegalArgumentException(String.format("the "
                            + "mapped field %s.%s of event %s is declared in "
                            + "the field's class %s",
                            field.getDeclaringClass().getName(),
                            field.getName(),
                            event,
                            fieldType.getName()));
                }
                mappedField.setAccessible(true);
                if(field.getAnnotation(OneToOne.class) != null) {
                    if(eventNewValue != null) {
                        mappedField.set(eventNewValue,
                                instance);
                        try {
                            storage.registerPostStoreCallback(instance, (object) -> {
                                try {
                                    storage.update(eventNewValue);
                                } catch (StorageException ex) {
                                    getIssueHandler().handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
                                }
                            });
                        } catch (StorageException ex) {
                            getIssueHandler().handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
                        }
                    }else {
                        //eventNewValue == null
                        mappedField.set(fieldCurrentValue,
                                null);
                        try {
                            storage.registerPostStoreCallback(instance, (object) -> {
                                try {
                                    storage.update(fieldCurrentValue);
                                } catch (StorageException ex) {
                                    getIssueHandler().handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
                                }
                            });
                        } catch (StorageException ex) {
                            getIssueHandler().handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
                        }
                    }
                }else if(field.getAnnotation(ManyToOne.class) != null) {
                    Collection mappedFieldValue;
                    if(eventNewValue != null) {
                        mappedFieldValue = (Collection) mappedField.get(eventNewValue);
                    }else {
                        mappedFieldValue = (Collection) mappedField.get(fieldCurrentValue);
                    }
                    if(mappedFieldValue == null) {
                        throw new IllegalArgumentException(String.format("the mapped field %s.%s of field %s.%s mustn't be null",
                                mappedField.getDeclaringClass().getName(),
                                mappedField.getName(),
                                fieldType.getName(),
                                field.getName()));
                    }
                    if(eventNewValue != null) {
                        mappedFieldValue.add(instance);
                        try {
                            storage.registerPostStoreCallback(instance, (object) -> {
                                try {
                                    storage.update(eventNewValue);
                                } catch (StorageException ex) {
                                    getIssueHandler().handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
                                }
                            });
                        } catch (StorageException ex) {
                            getIssueHandler().handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
                        }
                    }else {
                        mappedFieldValue.remove(instance);
                        try {
                            storage.registerPostStoreCallback(instance, (object) -> {
                                try {
                                    storage.update(fieldCurrentValue);
                                } catch (StorageException ex) {
                                    getIssueHandler().handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
                                }
                            });
                        } catch (StorageException ex) {
                            getIssueHandler().handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
                        }
                    }
                }else if(field.getAnnotation(OneToMany.class) != null
                        || field.getAnnotation(ManyToMany.class) != null) {
                    if(eventNewValue == null) {
                        throw new IllegalArgumentException(String.format(
                                "setting null on x-to-many relationship field "
                                        + "%s.%s is not supported",
                                fieldType.getName(),
                                field.getName()));
                    }

                    Collection fieldValues = (Collection) field.get(instance);
                    Collection newValues = (Collection) eventNewValue;
                    if(fieldValues == null) {
                        throw new IllegalArgumentException(String.format(
                                "fields annotated with %s or %s are expected "
                                + "to be initialized with an empty instance of "
                                + "the field type collection",
                                OneToMany.class,
                                ManyToMany.class));
                    }
                    //cannot use ListIterator to remove because we're operating
                    //on Collection
                    List<Object> fieldValuesToRemove = new LinkedList<>();
                    for(Object fieldValue : fieldValues) {
                        if(!newValues.contains(fieldValue)) {
                            fieldValuesToRemove.add(fieldValue);
                        }
                    }
                    for(Object fieldValueToRemove : fieldValuesToRemove) {
                        fieldValues.remove(fieldValueToRemove);
                    }

                    if(field.getAnnotation(OneToMany.class) != null) {
                        for(Object newValue : newValues) {
                            mappedField.set(newValue, instance);
                            try {
                                storage.registerPostStoreCallback(instance, (object) -> {
                                    try {
                                        storage.update(newValue);
                                    } catch (StorageException ex) {
                                        getIssueHandler().handle(new Message(ex,
                                                JOptionPane.ERROR_MESSAGE));
                                    }
                                });
                            } catch (StorageException ex) {
                                getIssueHandler().handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
                            }
                        }
                    }else {
                        //ManyToMany != null
                        //In a many-to-many relationship simply add all values
                        //which are added on one side on the other as well
                        for(Object newValue : newValues) {
                            //add instance to list of reference on mapped site
                            //= get old field value, add instance and set the
                            //result as new value
                            //@TODO: figure out whether to add value is always appropriate (give different collection, like Set, List, etc.)
                            Collection mappedFieldValue = (Collection) mappedField.get(newValue);
                            if(mappedFieldValue == null) {
                                throw new IllegalArgumentException(String.format("the mapped field %s.%s of field %s.%s mustn't be null",
                                        mappedField.getDeclaringClass().getName(),
                                        mappedField.getName(),
                                        fieldType.getName(),
                                        field.getName()));
                            }
                            mappedFieldValue.add(instance);
                            try {
                                storage.registerPostStoreCallback(instance, (object) -> {
                                    try {
                                        storage.update(newValue);
                                    } catch (StorageException ex) {
                                        getIssueHandler().handle(new Message(ex,
                                                JOptionPane.ERROR_MESSAGE));
                                    }
                                });
                            } catch (StorageException ex) {
                                getIssueHandler().handle(new Message(ex, JOptionPane.ERROR_MESSAGE));
                            }
                        }
                    }
                }else {
                    throw new IllegalArgumentException();
                }
            } //mappedField != null
        } //event instanceof MappedFieldUpdateEvent
        super.onFieldUpdate(event, field, instance);
    }

    @Override
    protected Object prepareInstance(Class<?> entityClass, Object entityToUpdate) throws TransformationException {
        //PersistenceStorage.update might cause validation exception since it
        //refers to a freshly created instance -> can't use any EntityManager
        //factory methods -> use PersistenceStroage.registerStoreCallback where
        //it makes sense (not necessary to persist bidirectional storage with or
        //without mappedBy attribute as set in
        //JPAReflectionFormBuilder.onFieldUpdate as shown in
        //JPAReflectionFormBuilderIT
        Object retValue = super.prepareInstance(entityClass,
                entityToUpdate);
        return retValue;
    }

    @Override
    protected JComponent getClassComponent(Field field,
            Class<?> entityClass,
            Object instance,
            FieldHandler fieldHandler) throws IllegalAccessException,
            FieldHandlingException,
            IllegalArgumentException,
            InvocationTargetException,
            NoSuchMethodException,
            InstantiationException,
            NoSuchFieldException,
            ResetException {
        JComponent retValue = super.getClassComponent(field,
                entityClass,
                instance,
                fieldHandler);
        if(field.getAnnotation(Id.class) != null) {
            registerIdFieldComponent(instance,
                    retValue //fieldComponent
            );
        }
        return retValue;
    }

    protected void registerIdFieldComponent(Object instance,
            JComponent fieldComponent) {
        Set<Component> idFieldComponents = this.idFieldComponentMap.get(instance);
        if(idFieldComponents == null) {
            idFieldComponents = new HashSet<>();
            idFieldComponentMap.put(instance,
                    idFieldComponents);
        }
        idFieldComponents.add(fieldComponent);
    }
}
