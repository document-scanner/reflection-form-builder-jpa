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
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Embedded;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.swing.JComponent;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.ReflectionFormPanel;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import richtercloud.reflection.form.builder.message.MessageHandler;
import richtercloud.reflection.form.builder.panels.NumberPanel;

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

    public JPAReflectionFormBuilder(EntityManager entityManager,
            String fieldDescriptionDialogTitle,
            MessageHandler messageHandler,
            JPACachedFieldRetriever fieldRetriever) {
        super(fieldDescriptionDialogTitle,
                messageHandler,
                fieldRetriever);
        if(entityManager == null) {
            throw new IllegalArgumentException("entityManager mustn't be null");
        }
        this.entityManager = entityManager;
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
                editingMode,
                this.getFieldRetriever());
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
                fieldMapping);
        transformClass(embeddableClass,
                instance0,
                fieldMapping,
                retValue,
                fieldHandler);
        return retValue;
    }
}
