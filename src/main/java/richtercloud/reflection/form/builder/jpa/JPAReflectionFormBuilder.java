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

import richtercloud.reflection.form.builder.jpa.panels.LongIdPanel;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.swing.JComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import richtercloud.reflection.form.builder.ClassAnnotationHandler;
import richtercloud.reflection.form.builder.FieldAnnotationHandler;
import richtercloud.reflection.form.builder.FieldHandler;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.ReflectionFormPanel;
import richtercloud.reflection.form.builder.jpa.retriever.IdPanelRetriever;
import richtercloud.reflection.form.builder.retriever.ValueRetriever;

/**
 * Handles generation of {@link JPAReflectionFormPanel} from root entity class
 * using JPA annoations like {@link Id} (using {@link LongIdPanel{@link Embedded} (using nested/recursive form generation).
 *
 * @author richter
 */
public class JPAReflectionFormBuilder extends ReflectionFormBuilder {
    public static final Map<Class<? extends JComponent>, ValueRetriever<?, ?>> VALUE_RETRIEVER_MAPPING_DEFAULT_JPA;
    static {
        Map<Class<? extends JComponent>, ValueRetriever<?, ?>> valueRetrieverMapping0 = new HashMap<>(ReflectionFormBuilder.VALUE_RETRIEVER_MAPPING_DEFAULT);
        valueRetrieverMapping0.put(LongIdPanel.class, IdPanelRetriever.getInstance());
        VALUE_RETRIEVER_MAPPING_DEFAULT_JPA = Collections.unmodifiableMap(valueRetrieverMapping0);
    }
    public final static List<Pair<Class<? extends Annotation>, FieldAnnotationHandler>> FIELD_ANNOTATION_MAPPING_DEFAULT_JPA;
    static {
        List<Pair<Class<? extends Annotation>, FieldAnnotationHandler>> jpaFieldAnnotationMapping0 = new LinkedList<>(ReflectionFormBuilder.FIELD_ANNOTATION_MAPPING_DEFAULT);
        jpaFieldAnnotationMapping0.add(new ImmutablePair<Class<? extends Annotation>, FieldAnnotationHandler>(Embedded.class, EmbeddedFieldAnnotationHandler.getInstance()));
        FIELD_ANNOTATION_MAPPING_DEFAULT_JPA = Collections.unmodifiableList(jpaFieldAnnotationMapping0);
    }

    private static List<Pair<Class<? extends Annotation>, FieldAnnotationHandler>> generateFieldAnnotationMapping(IdFieldAnnoationHandler idFieldAnnoationHandler) {
        List<Pair<Class<? extends Annotation>, FieldAnnotationHandler>> retValue = new LinkedList<>();
        retValue.addAll(FIELD_ANNOTATION_MAPPING_DEFAULT_JPA);
        retValue.add(new ImmutablePair<Class<? extends Annotation>, FieldAnnotationHandler>(Id.class, idFieldAnnoationHandler));
        return retValue;
    }

    private static List<Pair<Class<? extends Annotation>, ClassAnnotationHandler<?>>> generateClassAnnotationMapping(EntityClassAnnotationHandler entityClassAnnotationHandler) {
        List<Pair<Class<? extends Annotation>, ClassAnnotationHandler<?>>> retValue = new LinkedList<>();
        retValue.addAll(CLASS_ANNOTATION_MAPPING_DEFAULT);
        retValue.add(new ImmutablePair<Class<? extends Annotation>, ClassAnnotationHandler<?>>(Entity.class, entityClassAnnotationHandler));
        return retValue;
    }
    private EntityManager entityManager;
    private String persistFailureDialogTitle;

    public JPAReflectionFormBuilder(EntityManager entityManager,
            List<Pair<Class<? extends Annotation>, FieldAnnotationHandler>> fieldAnnotationMapping,
            List<Pair<Class<? extends Annotation>, ClassAnnotationHandler<?>>> classAnnotationMapping,
            String persistFailureDialogTitle,
            String idValidationFailureDialogTitle) {
        this(CLASS_MAPPING_DEFAULT, PRIMITIVE_MAPPING_DEFAULT, VALUE_RETRIEVER_MAPPING_DEFAULT_JPA, entityManager, fieldAnnotationMapping, classAnnotationMapping, persistFailureDialogTitle);
    }

    public JPAReflectionFormBuilder(EntityManager entityManager,
            String persistFailureDialogTitle,
            IdFieldAnnoationHandler idFieldAnnoationHandler,
            EntityClassAnnotationHandler entityClassAnnotationHandler) {
        this(CLASS_MAPPING_DEFAULT, PRIMITIVE_MAPPING_DEFAULT, VALUE_RETRIEVER_MAPPING_DEFAULT_JPA, entityManager, generateFieldAnnotationMapping(idFieldAnnoationHandler), generateClassAnnotationMapping(entityClassAnnotationHandler), persistFailureDialogTitle);
    }

    public JPAReflectionFormBuilder(Map<Type, FieldHandler<?>> classMapping,
            Map<Class<?>, FieldHandler<?>> primitiveMapping,
            Map<Class<? extends JComponent>, ValueRetriever<?, ?>> valueRetrieverMapping,
            EntityManager entityManager,
            String persistFailureDialogTitle,
            IdFieldAnnoationHandler idFieldAnnoationHandler,
            EntityClassAnnotationHandler entityClassAnnotationHandler) {
        this(classMapping, primitiveMapping, valueRetrieverMapping, entityManager, generateFieldAnnotationMapping(idFieldAnnoationHandler), generateClassAnnotationMapping(entityClassAnnotationHandler), persistFailureDialogTitle);
    }

    public JPAReflectionFormBuilder(Map<Type, FieldHandler<?>> classMapping,
            Map<Class<?>, FieldHandler<?>> primitiveMapping,
            Map<Class<? extends JComponent>, ValueRetriever<?, ?>> valueRetrieverMapping,
            EntityManager entityManager,
            List<Pair<Class<? extends Annotation>, FieldAnnotationHandler>> fieldAnnotationMapping,
            List<Pair<Class<? extends Annotation>, ClassAnnotationHandler<?>>> classAnnotationMapping,
            String persistFailureDialogTitle) {
        super(classMapping, primitiveMapping, valueRetrieverMapping, fieldAnnotationMapping, classAnnotationMapping);
        if(entityManager == null) {
            throw new IllegalArgumentException("entityManager mustn't be null");
        }
        this.entityManager = entityManager;
        this.persistFailureDialogTitle = persistFailureDialogTitle;
    }

    @Override
    public List<Field> retrieveRelevantFields(Class<?> entityClass) {
        List<Field> relevantFields = super.retrieveRelevantFields(entityClass);
        ListIterator<Field> relevantFieldsIt = relevantFields.listIterator();
        while(relevantFieldsIt.hasNext()) {
            Field relevantFieldsNxt = relevantFieldsIt.next();
            if(relevantFieldsNxt.getAnnotation(Transient.class) != null) {
                relevantFieldsIt.remove();
            }
        }
        relevantFieldsIt = relevantFields.listIterator();
        while(relevantFieldsIt.hasNext()) {
            Field relevantFieldsNxt = relevantFieldsIt.next();
            if(relevantFieldsNxt.getAnnotation(Id.class) != null) {
                relevantFieldsIt.remove();
                relevantFields.add(0, relevantFieldsNxt);
                break;
            }
        }

        return relevantFields;
    }

    @Override
    public JPAReflectionFormPanel transform(Class<?> entityClass) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
        ReflectionFormPanel reflectionFormPanel = super.transform(entityClass);
        JPAReflectionFormPanel retValue = new JPAReflectionFormPanel(entityManager,
                reflectionFormPanel,
                entityClass,
                reflectionFormPanel.getFieldMapping(),
                reflectionFormPanel.getValueRetrieverMapping(),
                reflectionFormPanel.getClassMapping(),
                this.persistFailureDialogTitle);
        return retValue;
    }


}
