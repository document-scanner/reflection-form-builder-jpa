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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.EntityManager;
import javax.swing.JComponent;
import org.apache.commons.lang3.tuple.Pair;
import richtercloud.reflection.form.builder.ClassAnnotationHandler;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.components.AmountMoneyCurrencyStorage;
import richtercloud.reflection.form.builder.components.AmountMoneyUsageStatisticsStorage;
import richtercloud.reflection.form.builder.fieldhandler.AmountMoneyFieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldAnnotationHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import richtercloud.reflection.form.builder.fieldhandler.MappingFieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.factory.AmountMoneyMappingFieldHandlerFactory;
import richtercloud.reflection.form.builder.jpa.IdGenerator;
import richtercloud.reflection.form.builder.jpa.JPAReflectionFormBuilder;
import richtercloud.reflection.form.builder.jpa.fieldhandler.factory.JPAAmountMoneyMappingClassAnnotationFactory;
import richtercloud.reflection.form.builder.jpa.fieldhandler.factory.JPAAmountMoneyMappingFieldAnnotationFactory;
import richtercloud.reflection.form.builder.jpa.fieldhandler.factory.JPAAmountMoneyMappingFieldHandlerFactory;
import richtercloud.reflection.form.builder.jpa.fieldhandler.factory.JPAAmountMoneyMappingTypeHandlerFactory;
import richtercloud.reflection.form.builder.jpa.typehandler.ElementCollectionTypeHandler;
import richtercloud.reflection.form.builder.message.MessageHandler;

/**
 * Handles entities and embeddables differently based on two type component-{@link FieldHandler} mappings.
 *
 * @author richter
 */
public class JPAMappingFieldHandler<T, E extends FieldUpdateEvent<T>> extends MappingFieldHandler<T,E, JPAReflectionFormBuilder> {
    private final ElementCollectionTypeHandler elementCollectionTypeHandler;
    private final Map<Type, FieldHandler<?,?,?>> embeddableMapping;

    public static JPAMappingFieldHandler create(EntityManager entityManager,
            int initialQueryLimit,
            MessageHandler messageHandler,
            FieldRetriever fieldRetriever,
            AmountMoneyUsageStatisticsStorage amountMoneyUsageStatisticsStorage,
            AmountMoneyCurrencyStorage amountMoneyCurrencyStorage,
            IdGenerator idGenerator) {
        JPAAmountMoneyMappingFieldHandlerFactory jPAAmountMoneyClassMappingFactory = new JPAAmountMoneyMappingFieldHandlerFactory(entityManager,
                initialQueryLimit,
                messageHandler,
                amountMoneyUsageStatisticsStorage,
                amountMoneyCurrencyStorage);
        AmountMoneyMappingFieldHandlerFactory amountMoneyClassMappingFactory = new AmountMoneyMappingFieldHandlerFactory(amountMoneyUsageStatisticsStorage,
                amountMoneyCurrencyStorage,
                messageHandler);
        JPAAmountMoneyMappingFieldAnnotationFactory jPAAmountMoneyFieldAnnotationMappingFactory = JPAAmountMoneyMappingFieldAnnotationFactory.create(idGenerator,
                messageHandler,
                fieldRetriever,
                initialQueryLimit,
                entityManager,
                amountMoneyUsageStatisticsStorage,
                amountMoneyCurrencyStorage);
        JPAAmountMoneyMappingClassAnnotationFactory jPAAmountMoneyClassAnnotationMappingFactory = JPAAmountMoneyMappingClassAnnotationFactory.create(entityManager);
        JPAAmountMoneyMappingTypeHandlerFactory jPAAmountMoneyTypeHandlerMappingFactory = new JPAAmountMoneyMappingTypeHandlerFactory(entityManager, initialQueryLimit, messageHandler);
        AmountMoneyFieldHandler amountMoneyFieldHandler = new AmountMoneyFieldHandler(amountMoneyUsageStatisticsStorage, amountMoneyCurrencyStorage, messageHandler);
        ElementCollectionTypeHandler elementCollectionTypeHandler = new ElementCollectionTypeHandler(jPAAmountMoneyTypeHandlerMappingFactory.generateTypeHandlerMapping(),
                jPAAmountMoneyTypeHandlerMappingFactory.generateTypeHandlerMapping(),
                messageHandler,
                amountMoneyFieldHandler);
        return new JPAMappingFieldHandler(jPAAmountMoneyClassMappingFactory.generateClassMapping(),
                amountMoneyClassMappingFactory.generateClassMapping(),
                jPAAmountMoneyClassMappingFactory.generatePrimitiveMapping(),
                jPAAmountMoneyFieldAnnotationMappingFactory.generateFieldAnnotationMapping(),
                jPAAmountMoneyClassAnnotationMappingFactory.generateClassAnnotationMapping(),
                elementCollectionTypeHandler);
    }

    public JPAMappingFieldHandler(Map<Type, FieldHandler<?, ?,?>> classMapping,
            Map<Type, FieldHandler<?,?,?>> embeddableMapping,
            Map<Class<?>, FieldHandler<?, ?,?>> primitiveMapping,
            List<Pair<Class<? extends Annotation>, FieldAnnotationHandler>> fieldAnnotationMapping,
            List<Pair<Class<? extends Annotation>, ClassAnnotationHandler<Object, FieldUpdateEvent<Object>>>> classAnnotationMapping,
            ElementCollectionTypeHandler elementCollectionTypeHandler) {
        super(classMapping,
                primitiveMapping,
                fieldAnnotationMapping,
                classAnnotationMapping);
        this.elementCollectionTypeHandler = elementCollectionTypeHandler;
        this.embeddableMapping = embeddableMapping;
    }

    @Override
    public JComponent handle(Field field,
            Object instance,
            FieldUpdateListener updateListener,
            JPAReflectionFormBuilder reflectionFormBuilder) throws IllegalArgumentException,
            IllegalAccessException,
            FieldHandlingException,
            InvocationTargetException,
            NoSuchMethodException,
            InstantiationException {
        if(field.getAnnotation(ElementCollection.class) != null) {
            //can't be handled differently because otherwise a QueryPanel would
            //be tried to be used and IllegalArgumentException thrown at
            //initialization
            JComponent retValue = this.elementCollectionTypeHandler.handle(field.getGenericType(),
                    (List<Object>)field.get(instance),
                    field.getName(),
                    field.getDeclaringClass(),
                    updateListener,
                    reflectionFormBuilder);
            return retValue;
        }
        Class<?> fieldType = field.getType();
        if(fieldType.getAnnotation(Embeddable.class) != null) {
            FieldHandler fieldHandler = embeddableMapping.get(fieldType);
            JComponent retValue = fieldHandler.handle(field, instance, updateListener, reflectionFormBuilder);
            return retValue;
        }
        return super.handle(field, instance, updateListener, reflectionFormBuilder);
    }
}