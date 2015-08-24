/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package richtercloud.reflection.form.builder.jpa;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.swing.JComponent;
import org.apache.commons.lang3.tuple.Pair;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.retriever.ValueRetriever;

/**
 *
 * @author richter
 * @param <E> the type of the root entity
 */
public class JPAReflectionFormBuilder<E> extends ReflectionFormBuilder<E> {
    private EntityManager entityManager;

    public JPAReflectionFormBuilder(EntityManager entityManager, List<Pair<Class<? extends Annotation>, Callable<? extends JComponent>>> annotationMapping) {
        this(CLASS_MAPPING_DEFAULT, VALUE_RETRIEVER_MAPPING_DEFAULT, entityManager, annotationMapping);
    }


    public JPAReflectionFormBuilder(Map<Class<?>, Class<? extends JComponent>> classMapping, Map<Class<? extends JComponent>, ValueRetriever<?, ?>> valueRetrieverMapping, EntityManager entityManager, List<Pair<Class<? extends Annotation>, Callable<? extends JComponent>>> annotationMapping) {
        super(classMapping, valueRetrieverMapping, annotationMapping);
        if(entityManager == null) {
            throw new IllegalArgumentException("entityManager mustn't be null");
        }
        this.entityManager = entityManager;
    }

    @Override
    public List<Field> retrieveRelevantFields(Class<? extends E> entityClass) {
        List<Field> relevantFields = super.retrieveRelevantFields(entityClass);
        ListIterator<Field> relevantFieldsIt = relevantFields.listIterator();
        while(relevantFieldsIt.hasNext()) {
            Field relevantFieldsNxt = relevantFieldsIt.next();
            if(relevantFieldsNxt.getAnnotation(Transient.class) != null) {
                relevantFieldsIt.remove();
            }
        }
        return relevantFields;
    }

    /**
     * {@inheritDoc }
     *
     * Checks (in order): <ol>
     * <li>if a {@link JComponent} in associated in {@link #getClassComponent(java.lang.reflect.Field) },</li>
     * <li>otherwise tries to retrieve from {@link ReflectionFormBuilder#CLASS_MAPPING_DEFAULT}</li>
     * <li>checks if the field is an entity (i.e. annotated with {@link Entity}) and returns a {@link QueryPanel}</li>
     * <li>returns a label indicating the type of the field</li>
     * </ol>
     *
     * @param field
     * @return
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    /*
    @TODO: check if there's a possiblity that a Field is both annotated with @Id
    and its type an entity
    */
    @Override
    protected JComponent getClassComponent(Field field) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        JComponent retValue = super.getClassComponent(field); //never null
        if(field.getAnnotation(Id.class) != null) {
            retValue = new IdPanel(IdGenerator.getInstance());
            return retValue;
        }
        if(field.getType().getAnnotation(Entity.class) != null) {
            retValue = new QueryPanel<>(this.entityManager, getEntityClassFields(), field.getType());
        }
        return retValue;
    }

}
