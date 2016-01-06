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
package richtercloud.reflection.form.builder.jpa.fieldhandler.factory;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.components.AmountMoneyCurrencyStorage;
import richtercloud.reflection.form.builder.components.AmountMoneyUsageStatisticsStorage;
import richtercloud.reflection.form.builder.fieldhandler.FieldAnnotationHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.MappingFieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.factory.AmountMoneyMappingFieldHandlerFactory;
import richtercloud.reflection.form.builder.fieldhandler.factory.MappingClassAnnotationFactory;
import richtercloud.reflection.form.builder.fieldhandler.factory.MappingFieldAnnotationFactory;
import richtercloud.reflection.form.builder.jpa.ElementCollectionFieldAnnotationHandler;
import richtercloud.reflection.form.builder.jpa.EmbeddedFieldAnnotationHandler;
import richtercloud.reflection.form.builder.jpa.IdFieldAnnoationHandler;
import richtercloud.reflection.form.builder.jpa.IdGenerator;
import richtercloud.reflection.form.builder.jpa.typehandler.ElementCollectionTypeHandler;
import richtercloud.reflection.form.builder.message.MessageHandler;

/**
 *
 * @author richter
 */
public class JPAAmountMoneyMappingFieldAnnotationFactory extends MappingFieldAnnotationFactory {
    private final IdFieldAnnoationHandler idFieldAnnoationHandler;
    private final ElementCollectionFieldAnnotationHandler elementCollectionFieldAnnotationHandler;
    private final EmbeddedFieldAnnotationHandler embeddedFieldAnnotationHandler;

    /**
     * Create a {@code JPAAmountMoneyMappingFieldAnnotationFactory} with default
     * dependencies.
     *
     * @param idGenerator
     * @param messageHandler
     * @param initialQueryLimit
     * @param entityManager
     * @param amountMoneyUsageStatisticsStorage
     * @param amountMoneyCurrencyStorage
     * @return
     */
    public static JPAAmountMoneyMappingFieldAnnotationFactory create(IdGenerator idGenerator,
            MessageHandler messageHandler,
            FieldRetriever fieldRetriever,
            int initialQueryLimit,
            EntityManager entityManager,
            AmountMoneyUsageStatisticsStorage amountMoneyUsageStatisticsStorage,
            AmountMoneyCurrencyStorage amountMoneyCurrencyStorage) {
        JPAAmountMoneyMappingTypeHandlerFactory jPAAmountMoneyTypeHandlerMappingFactory = new JPAAmountMoneyMappingTypeHandlerFactory(entityManager, initialQueryLimit, messageHandler);
        AmountMoneyMappingFieldHandlerFactory amountMoneyClassMappingFactory = new AmountMoneyMappingFieldHandlerFactory(amountMoneyUsageStatisticsStorage, amountMoneyCurrencyStorage, messageHandler);
        MappingFieldAnnotationFactory fieldAnnotationMappingFactory = new MappingFieldAnnotationFactory();
        MappingClassAnnotationFactory classAnnotationMappingFactory = new MappingClassAnnotationFactory();
        FieldHandler embeddableFieldHandler = new MappingFieldHandler(amountMoneyClassMappingFactory.generateClassMapping(),
                amountMoneyClassMappingFactory.generatePrimitiveMapping(),
                fieldAnnotationMappingFactory.generateFieldAnnotationMapping(),
                classAnnotationMappingFactory.generateClassAnnotationMapping());
        EmbeddedFieldAnnotationHandler embeddedFieldAnnotationHandler = new EmbeddedFieldAnnotationHandler(embeddableFieldHandler);
        return new JPAAmountMoneyMappingFieldAnnotationFactory(new IdFieldAnnoationHandler(idGenerator,
                messageHandler,
                fieldRetriever),
                new ElementCollectionFieldAnnotationHandler(new ElementCollectionTypeHandler(jPAAmountMoneyTypeHandlerMappingFactory.generateTypeHandlerMapping(),
                        jPAAmountMoneyTypeHandlerMappingFactory.generateTypeHandlerMapping(),
                        messageHandler,
                        embeddableFieldHandler)),
                embeddedFieldAnnotationHandler);
    }

    public JPAAmountMoneyMappingFieldAnnotationFactory(IdFieldAnnoationHandler idFieldAnnoationHandler,
            ElementCollectionFieldAnnotationHandler elementCollectionFieldAnnotationHandler,
            EmbeddedFieldAnnotationHandler embeddedFieldAnnotationHandler) {
        this.idFieldAnnoationHandler = idFieldAnnoationHandler;
        this.elementCollectionFieldAnnotationHandler = elementCollectionFieldAnnotationHandler;
        this.embeddedFieldAnnotationHandler = embeddedFieldAnnotationHandler;
    }

    /**
     *
     * @return
     */
    /*
    internal implementation notes:
    - return a modifiable view of the collections because protecting against
    writes doesn't increase any security or enforce any interface
    */
    @Override
    public List<Pair<Class<? extends Annotation>, FieldAnnotationHandler>> generateFieldAnnotationMapping() {
        List<Pair<Class<? extends Annotation>, FieldAnnotationHandler>> jpaFieldAnnotationMapping0 = new LinkedList<>();
        jpaFieldAnnotationMapping0.add(new ImmutablePair<Class<? extends Annotation>, FieldAnnotationHandler>(Id.class, idFieldAnnoationHandler));
        jpaFieldAnnotationMapping0.add(new ImmutablePair<Class<? extends Annotation>, FieldAnnotationHandler>(ElementCollection.class, elementCollectionFieldAnnotationHandler));
        jpaFieldAnnotationMapping0.add(new ImmutablePair<Class<? extends Annotation>, FieldAnnotationHandler>(Embedded.class, embeddedFieldAnnotationHandler));
        return Collections.unmodifiableList(jpaFieldAnnotationMapping0);
    }
}
