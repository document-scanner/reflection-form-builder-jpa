/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package richtercloud.reflection.form.builder.jpa;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.Transient;
import javax.swing.JComponent;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.ValueRetriever;

/**
 *
 * @author richter
 */
public class JPAReflectionFormBuilder<E> extends ReflectionFormBuilder<E> {
    private EntityManager entityManager;
    
    public JPAReflectionFormBuilder(EntityManager entityManager) {
        this(CLASS_MAPPING_DEFAULT, VALUE_RETRIEVER_MAPPING_DEFAULT, entityManager);
    }
    
    
    public JPAReflectionFormBuilder(Map<Class<?>, Class<? extends JComponent>> classMapping, Map<Class<? extends JComponent>, ValueRetriever<?, ?>> valueRetrieverMapping, EntityManager entityManager) {
        super(classMapping, valueRetrieverMapping);
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

    @Override
    protected JComponent getClassComponent(Class<?> type) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<? extends JComponent> clazz = getClassMapping().get(type);
        JComponent retValue;
        if(clazz == null) {
            clazz = ReflectionFormBuilder.CLASS_MAPPING_DEFAULT.get(type);
        }
        if(clazz == null) {
            retValue = new QueryPanel<>(this.entityManager, getEntityClassFields(), type);
        } else {
            retValue = clazz.getConstructor().newInstance();
        }
        return retValue;
    }

}
