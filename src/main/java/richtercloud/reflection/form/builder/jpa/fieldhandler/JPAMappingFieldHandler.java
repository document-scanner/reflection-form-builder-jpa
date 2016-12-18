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
package richtercloud.reflection.form.builder.jpa.fieldhandler;

import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.swing.JComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import richtercloud.message.handler.MessageHandler;
import richtercloud.reflection.form.builder.ComponentHandler;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.components.money.AmountMoneyCurrencyStorage;
import richtercloud.reflection.form.builder.components.money.AmountMoneyExchangeRateRetriever;
import richtercloud.reflection.form.builder.components.money.AmountMoneyUsageStatisticsStorage;
import richtercloud.reflection.form.builder.fieldhandler.AmountMoneyFieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import richtercloud.reflection.form.builder.fieldhandler.MappingFieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.factory.AmountMoneyMappingFieldHandlerFactory;
import richtercloud.reflection.form.builder.jpa.JPAReflectionFormBuilder;
import richtercloud.reflection.form.builder.jpa.fieldhandler.factory.JPAAmountMoneyMappingFieldHandlerFactory;
import richtercloud.reflection.form.builder.jpa.idapplier.IdApplier;
import richtercloud.reflection.form.builder.jpa.panels.LongIdPanel;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.reflection.form.builder.jpa.typehandler.ElementCollectionTypeHandler;
import richtercloud.reflection.form.builder.jpa.typehandler.ToManyTypeHandler;
import richtercloud.reflection.form.builder.jpa.typehandler.ToOneTypeHandler;
import richtercloud.reflection.form.builder.jpa.typehandler.factory.JPAAmountMoneyMappingTypeHandlerFactory;
import richtercloud.reflection.form.builder.panels.NumberPanel;
import richtercloud.reflection.form.builder.panels.NumberPanelUpdateEvent;
import richtercloud.reflection.form.builder.panels.NumberPanelUpdateListener;
import richtercloud.reflection.form.builder.jpa.storage.FieldInitializer;

/**
 * Handles entities and embeddables differently based on two type component-{@link FieldHandler} mappings.
 *
 * @author richter
 */
public class JPAMappingFieldHandler<T, E extends FieldUpdateEvent<T>> extends MappingFieldHandler<T,E, JPAReflectionFormBuilder, Component> {
    private final static ComponentHandler<LongIdPanel> LONG_ID_PANEL_COMPONENT_RESETTER = new ComponentHandler<LongIdPanel>() {
        @Override
        public void reset(LongIdPanel component) {
            component.reset();
        }
    };
    private final ElementCollectionTypeHandler elementCollectionTypeHandler;
    private final Map<Type, FieldHandler<?,?,?, ?>> embeddableMapping;
    private final ToManyTypeHandler toManyTypeHandler;
    private final ToOneTypeHandler toOneTypeHandler;
    private final MessageHandler messageHandler;
    private final FieldRetriever fieldRetriever;
    private final IdApplier idApplier;

    public static JPAMappingFieldHandler create(PersistenceStorage storage,
            int initialQueryLimit,
            MessageHandler messageHandler,
            FieldRetriever fieldRetriever,
            AmountMoneyUsageStatisticsStorage amountMoneyUsageStatisticsStorage,
            AmountMoneyCurrencyStorage amountMoneyCurrencyStorage,
            AmountMoneyExchangeRateRetriever amountMoneyExchangeRateRetriever,
            String bidirectionalHelpDialogTitle,
            IdApplier idApplier,
            FieldInitializer fieldInitializer) {
        JPAAmountMoneyMappingFieldHandlerFactory jPAAmountMoneyClassMappingFactory = new JPAAmountMoneyMappingFieldHandlerFactory(storage,
                initialQueryLimit,
                messageHandler,
                amountMoneyUsageStatisticsStorage,
                amountMoneyCurrencyStorage,
                amountMoneyExchangeRateRetriever,
                bidirectionalHelpDialogTitle);
        AmountMoneyMappingFieldHandlerFactory amountMoneyClassMappingFactory = new AmountMoneyMappingFieldHandlerFactory(amountMoneyUsageStatisticsStorage,
                amountMoneyCurrencyStorage,
                amountMoneyExchangeRateRetriever,
                messageHandler);
        JPAAmountMoneyMappingTypeHandlerFactory jPAAmountMoneyTypeHandlerMappingFactory = new JPAAmountMoneyMappingTypeHandlerFactory(storage,
                initialQueryLimit,
                messageHandler,
                bidirectionalHelpDialogTitle);
        AmountMoneyFieldHandler amountMoneyFieldHandler = new AmountMoneyFieldHandler(amountMoneyUsageStatisticsStorage,
                amountMoneyExchangeRateRetriever,
                amountMoneyCurrencyStorage,
                messageHandler);
        ElementCollectionTypeHandler elementCollectionTypeHandler = new ElementCollectionTypeHandler(jPAAmountMoneyTypeHandlerMappingFactory.generateTypeHandlerMapping(),
                jPAAmountMoneyTypeHandlerMappingFactory.generateTypeHandlerMapping(),
                messageHandler,
                amountMoneyFieldHandler);
        ToManyTypeHandler toManyTypeHandler = new ToManyTypeHandler(storage,
                messageHandler,
                jPAAmountMoneyTypeHandlerMappingFactory.generateTypeHandlerMapping(),
                jPAAmountMoneyTypeHandlerMappingFactory.generateTypeHandlerMapping(),
                bidirectionalHelpDialogTitle,
                fieldInitializer);
        ToOneTypeHandler toOneTypeHandler = new ToOneTypeHandler(storage,
                messageHandler,
                bidirectionalHelpDialogTitle,
                fieldInitializer);
        return new JPAMappingFieldHandler(jPAAmountMoneyClassMappingFactory.generateClassMapping(),
                amountMoneyClassMappingFactory.generateClassMapping(),
                jPAAmountMoneyClassMappingFactory.generatePrimitiveMapping(),
                elementCollectionTypeHandler,
                toManyTypeHandler,
                toOneTypeHandler,
                messageHandler,
                fieldRetriever,
                idApplier);
    }

    public JPAMappingFieldHandler(Map<Type, FieldHandler<?, ?,?, ?>> classMapping,
            Map<Type, FieldHandler<?,?,?, ?>> embeddableMapping,
            Map<Class<?>, FieldHandler<?, ?,?, ?>> primitiveMapping,
            ElementCollectionTypeHandler elementCollectionTypeHandler,
            ToManyTypeHandler oneToManyTypeHandler,
            ToOneTypeHandler toOneTypeHandler,
            MessageHandler messageHandler,
            FieldRetriever fieldRetriever,
            IdApplier idApplier) {
        super(classMapping,
                primitiveMapping);
        this.elementCollectionTypeHandler = elementCollectionTypeHandler;
        this.embeddableMapping = embeddableMapping;
        this.toManyTypeHandler = oneToManyTypeHandler;
        this.toOneTypeHandler = toOneTypeHandler;
        this.messageHandler = messageHandler;
        this.fieldRetriever = fieldRetriever;
        this.idApplier = idApplier;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public IdApplier getIdApplier() {
        return idApplier;
    }

    @Override
    protected Pair<JComponent, ComponentHandler<?>> handle0(Field field,
            Object instance,
            final FieldUpdateListener updateListener,
            JPAReflectionFormBuilder reflectionFormBuilder) throws IllegalArgumentException,
            IllegalAccessException,
            FieldHandlingException,
            InvocationTargetException,
            NoSuchMethodException,
            InstantiationException {
        if(field == null) {
            throw new IllegalArgumentException("fieldClass mustn't be null");
        }
        Type fieldType = field.getGenericType();
        Object fieldValue = field.get(instance);
        String fieldName = field.getName();
        Class<?> fieldDeclaringClass = field.getDeclaringClass();
        if(field.getAnnotation(Id.class) != null) {
            if(!(fieldType instanceof Class)) {
                throw new IllegalArgumentException("@Id annotated field has to be a class");
            }
            Class<?> fieldTypeClass = (Class<?>) fieldType;
            if(fieldTypeClass.equals(Long.class)) {
                Long fieldValueCast = (Long) field.get(instance);
                NumberPanel<Long> retValue;
                if(fieldType.equals(Long.class)) {
                    retValue = new LongIdPanel(instance,
                            fieldValueCast, //initialValue
                            messageHandler,
                            fieldRetriever,
                            false, //readOnly
                            idApplier);
                }else {
                    throw new IllegalArgumentException(String.format("field type %s is not supported", fieldValue.getClass()));
                }
                retValue.addUpdateListener(new NumberPanelUpdateListener<Long>() {

                    @Override
                    public void onUpdate(NumberPanelUpdateEvent<Long> event) {
                        updateListener.onUpdate(new FieldUpdateEvent<>(event.getNewValue()));
                    }
                });
                return new ImmutablePair<JComponent, ComponentHandler<?>>(retValue, LONG_ID_PANEL_COMPONENT_RESETTER);
            }else {
                throw new IllegalArgumentException(String.format("@Id annotated field type %s not supported", field.getGenericType()));
            }
        }
        if(field.getAnnotation(ElementCollection.class) != null) {
            //can't be handled differently because otherwise a QueryPanel would
            //be tried to be used and IllegalArgumentException thrown at
            //initialization
            if(fieldValue != null && !(fieldValue instanceof List)) {
                throw new IllegalArgumentException("field values isn't an instance of List");
            }
            Pair<JComponent, ComponentHandler<?>> retValue = this.elementCollectionTypeHandler.handle(field.getGenericType(),
                    (List<Object>)fieldValue,
                    fieldName,
                    fieldDeclaringClass,
                    updateListener,
                    reflectionFormBuilder);
            return retValue;
        }
        if(field.getAnnotation(OneToMany.class) != null || field.getAnnotation(ManyToMany.class) != null) {
            Pair<JComponent, ComponentHandler<?>> retValue = this.toManyTypeHandler.handle(field.getGenericType(),
                    (List<Object>)fieldValue,
                    fieldName,
                    fieldDeclaringClass,
                    updateListener,
                    reflectionFormBuilder);
            return retValue;
        }
        if(field.getAnnotation(OneToOne.class) != null || field.getAnnotation(ManyToOne.class) != null) {
            Pair<JComponent, ComponentHandler<?>> retValue = this.toOneTypeHandler.handle(field.getGenericType(),
                    fieldValue,
                    fieldName,
                    fieldDeclaringClass,
                    updateListener,
                    reflectionFormBuilder);
            return retValue;
        }
        if(field.getType() instanceof Class) {
            Class<?> fieldTypeClass = field.getType();
            if(fieldTypeClass.getAnnotation(Embeddable.class) != null) {
                FieldHandler fieldHandler = embeddableMapping.get(fieldType);
                JComponent retValue = fieldHandler.handle(field,
                        instance,
                        updateListener,
                        reflectionFormBuilder);
                return new ImmutablePair<JComponent, ComponentHandler<?>>(retValue,
                        fieldHandler);
            }
        }
        return super.handle0(field,
                instance,
                updateListener,
                reflectionFormBuilder);
    }
}