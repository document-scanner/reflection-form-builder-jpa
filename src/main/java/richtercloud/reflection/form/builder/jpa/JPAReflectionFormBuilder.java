/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package richtercloud.reflection.form.builder.jpa;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.swing.JComponent;
import javax.swing.JLabel;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.components.OCRResultPanelRetriever;
import richtercloud.reflection.form.builder.components.ScanResultPanelRetriever;
import richtercloud.reflection.form.builder.retriever.ValueRetriever;

/**
 *
 * @author richter
 * @param <E> the type of the root entity
 */
public class JPAReflectionFormBuilder<E> extends ReflectionFormBuilder<E> {
    private EntityManager entityManager;

    public JPAReflectionFormBuilder(EntityManager entityManager, OCRResultPanelRetriever oCRResultPanelRetriever, ScanResultPanelRetriever scanResultPanelRetriever) {
        this(CLASS_MAPPING_DEFAULT, VALUE_RETRIEVER_MAPPING_DEFAULT, entityManager, oCRResultPanelRetriever, scanResultPanelRetriever);
    }


    public JPAReflectionFormBuilder(Map<Class<?>, Class<? extends JComponent>> classMapping, Map<Class<? extends JComponent>, ValueRetriever<?, ?>> valueRetrieverMapping, EntityManager entityManager, OCRResultPanelRetriever oCRResultPanelRetriever, ScanResultPanelRetriever scanResultPanelRetriever) {
        super(classMapping, valueRetrieverMapping, oCRResultPanelRetriever, scanResultPanelRetriever);
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
     * @param field
     * @return
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Override
    protected JComponent getClassComponent(Field field) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        JComponent retValue;
        if(field.getAnnotation(Id.class) != null) {
            retValue = new IdPanel(IdGenerator.getInstance());
            return retValue;
        }
        Class<? extends JComponent> clazz = getClassMapping().get(field.getType());
        if(clazz == null) {
            clazz = ReflectionFormBuilder.CLASS_MAPPING_DEFAULT.get(field.getType());
        }
        if(clazz == null) {
            if(field.getType().getAnnotation(Entity.class) != null) {
                retValue = new QueryPanel<>(this.entityManager, getEntityClassFields(), field.getType());
            }else {
                return new JLabel(field.getType().getName());
            }
        } else {
            Constructor<? extends JComponent> clazzConstructor = clazz.getDeclaredConstructor();
            retValue = clazzConstructor.newInstance();
        }
        return retValue;
    }

}
