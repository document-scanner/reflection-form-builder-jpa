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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.swing.JComponent;
import richtercloud.message.handler.ConfirmMessageHandler;
import richtercloud.message.handler.MessageHandler;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.ReflectionFormPanel;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.MappedFieldUpdateEvent;
import richtercloud.reflection.form.builder.jpa.idapplier.IdApplier;

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
public class JPAReflectionFormBuilder extends ReflectionFormBuilder<JPACachedFieldRetriever> {
    private EntityManager entityManager;
    private final IdApplier idApplier;
    private final ConfirmMessageHandler confirmMessageHandler;
    private final Map<Class<?>, WarningHandler<?>> warningHandlers;

    public JPAReflectionFormBuilder(EntityManager entityManager,
            String fieldDescriptionDialogTitle,
            MessageHandler messageHandler,
            ConfirmMessageHandler confirmMessageHandler,
            JPACachedFieldRetriever fieldRetriever,
            IdApplier idApplier,
            Map<Class<?>, WarningHandler<?>> warningHandlers) {
        super(fieldDescriptionDialogTitle,
                messageHandler,
                fieldRetriever);
        if(entityManager == null) {
            throw new IllegalArgumentException("entityManager mustn't be null");
        }
        this.entityManager = entityManager;
        if(idApplier == null) {
            throw new IllegalArgumentException("idApplier mustn't be null");
        }
        this.idApplier = idApplier;
        this.confirmMessageHandler = confirmMessageHandler;
        this.warningHandlers = warningHandlers;
    }

    public ReflectionFormPanel transformEntityClass(Class<?> entityClass,
            Object entityToUpdate,
            boolean editingMode,
            FieldHandler fieldHandler) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, FieldHandlingException {
        final Map<Field, JComponent> fieldMapping = new HashMap<>();
        Object instance = prepareInstance(entityClass, entityToUpdate);
        ReflectionFormPanel retValue = new EntityReflectionFormPanel(entityManager,
                instance,
                entityClass,
                fieldMapping,
                this.getMessageHandler(),
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
        return retValue;
    }

    @Override
    public ReflectionFormPanel transformEntityClass(Class<?> entityClass,
            Object entityToUpdate,
            FieldHandler fieldHandler) throws InstantiationException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException,
            NoSuchMethodException,
            FieldHandlingException {
        return transformEntityClass(entityClass,
                entityToUpdate,
                false,
                fieldHandler);
    }

    public EmbeddableReflectionFormPanel<?> transformEmbeddable(Class<?> embeddableClass,
            Object instance,
            FieldHandler fieldHandler) throws IllegalAccessException,
            InvocationTargetException,
            NoSuchMethodException,
            FieldHandlingException,
            InstantiationException {
        final Map<Field, JComponent> fieldMapping = new HashMap<>();
        Object instance0 = prepareInstance(embeddableClass, instance);
        EmbeddableReflectionFormPanel<Object> retValue = new EmbeddableReflectionFormPanel<>(entityManager,
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

    @Override
    protected void onFieldUpdate(FieldUpdateEvent event, Field field, Object instance) throws IllegalArgumentException, IllegalAccessException {
        if(event instanceof MappedFieldUpdateEvent) {
            MappedFieldUpdateEvent eventCast = (MappedFieldUpdateEvent) event;
            if(eventCast.getMappedField() != null) {
                if(field.getAnnotation(OneToOne.class) != null) {
                    Object fieldValueOld = field.get(instance); //get old field value because event.getNewValue might be null
                    eventCast.getMappedField().set(fieldValueOld,
                            event.getNewValue());
                }else if(field.getAnnotation(OneToMany.class) != null
                        || field.getAnnotation(ManyToMany.class) != null) {
                    Collection fieldValueList = (Collection) field.get(instance);
                    Collection newValueList = (Collection) event.getNewValue();
                    for(Object fieldValue : fieldValueList) {
                        if(!newValueList.contains(fieldValue)) {
                            eventCast.getMappedField().set(fieldValue, null); //reference has been removed
                        }
                    }
                    if(field.getAnnotation(OneToMany.class) != null) {
                        for(Object newValue : newValueList) {
                            eventCast.getMappedField().set(newValue, instance);
                        }
                    }else {
                        //ManyToMany != null
                        for(Object newValue : newValueList) {
                            //add instance to list of reference on mapped site
                            //= get old field value, add instance and set the
                            //result as new value
                            //@TODO: figure out whether to add value is always appropriate (give different collection, like Set, List, etc.)
                            Collection mappedFieldValue = (Collection) eventCast.getMappedField().get(newValue);
                            mappedFieldValue.add(newValue);
                            eventCast.getMappedField().set(newValue,
                                    mappedFieldValue); //set reference on all element of event.newValue (will make unnecessary changes, but they shouldn't hurt)
                        }
                    }
                }else {
                    throw new IllegalArgumentException();
                }
            }
        }
        super.onFieldUpdate(event, field, instance);
    }
}
