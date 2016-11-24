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
package richtercloud.reflection.form.builder.jpa.panels;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.table.DefaultTableModel;
import richtercloud.reflection.form.builder.FieldInfo;
import richtercloud.reflection.form.builder.FieldRetriever;

/**
 *
 * @author richter
 * @param <E> the entity type
 */
public class EntityTableModel<E> extends DefaultTableModel {

    private static final long serialVersionUID = 1L;
    private final List<E> entities;
    private final FieldRetriever fieldRetriever;
    private final Map<Integer, String> tooltipTextMap = new HashMap<>();
    /**
     * The list of fields of entities managed in this model mapping to the index
     * of the table column. This mapping allows to skip fields of superclasses
     * which don't have the fields of subclasses.
     */
    /*
    internal implementation notes:
    - needs to be a list in order to provide the same iteration order
    */
    private final Map<Field, Integer> fields = new HashMap<>();
    /**
     * Keep class information in class and update in {@link #updateColumns(java.util.List) }
     * in order to avoid unnecessary iterations.
     */
    private final Set<Class<?>> entityClasses = new HashSet<>();

    public EntityTableModel(FieldRetriever fieldRetriever) throws IllegalArgumentException, IllegalAccessException {
        this(new LinkedList<E>(),
                fieldRetriever);
    }

    /**
     * Checks whether it's necessary to add or remove columns from
     * {@code queryResultTableModel} after changing subtypes flag and eventually
     * including subclasses which weren't included in the last query result.
     *
     * Adds a discriminator column to {@code tableModel} of {@code queryResults}
     * contains more than one differentiable class.
     *
     * Since there's no way to remove columns once added to a
     * {@link DefaultTableModel} a model has to be recreated every time it ought
     * to be changed.
     *
     * @param initialEntities the query result returned from {@link QueryComponent}
     * (assumed to be of correct (sub)type(s) depending on whether
     * {@link QueryComponent} is configured to return subtypes or not) (this
     * model works with a copy of it in order to avoid
     * {@link ConcurrentModificationException})
     * @param fieldRetriever
     */
    /*
    internal implementation notes:
    - recreation of table model is tolerable effort and support KISS pattern
    */
    public EntityTableModel(List<E> initialEntities,
            FieldRetriever fieldRetriever) throws IllegalArgumentException, IllegalAccessException {
        this.fieldRetriever = fieldRetriever;

        for(Object entity: initialEntities) {
            entityClasses.add(entity.getClass());
        }
        this.entities = new LinkedList<>(initialEntities);
            //assign before updateColumns in order to get entities added (and
            //keep the routine in updateColumns)
        updateColumns(initialEntities);
            //adds entities if they're in this.entities
    }

    /**
     * Only configures the columns based on classes in {@code entities}, but
     * doesn't change the entities which provide the data of this model.
     * @param entities the entities which ought to be used to update the model
     */
    public void updateColumns(List<E> entities) throws IllegalArgumentException, IllegalAccessException {
        fields.clear();
        tooltipTextMap.clear();
        //Remove all rows and add them again after the columns have been changed
        while(this.getRowCount() > 0) {
            this.removeRow(0);
        }
        entityClasses.clear();
        for(Object entity: entities) {
            entityClasses.add(entity.getClass());
        }
        List<E> entitiesOriginal = new LinkedList<>(this.entities);
        this.entities.clear();
        int i=0;
        if(entityClasses.size() > 1) {
            this.addColumn("Type");
            i=1;
        }
        for(Class<?> entityClass : entityClasses) {
            for(Field field : fieldRetriever.retrieveRelevantFields(entityClass)) {
                if(fields.containsKey(field)) {
                    continue;
                }
                FieldInfo fieldInfo = field.getAnnotation(FieldInfo.class);
                if(fieldInfo != null) {
                    this.addColumn(fieldInfo.name());
                    tooltipTextMap.put(i, fieldInfo.description());
                }else {
                    this.addColumn(field.getName());
                    tooltipTextMap.put(i, "");
                }
                fields.put(field, i);
                i++;
            }
        }
        for(E entityOriginal : entitiesOriginal) {
            addEntity(entityOriginal);
        }
    }

    public void addEntity(E entity) throws IllegalArgumentException,
            IllegalAccessException {
        List<Class<?>> entityClassesHierarchy = new LinkedList<>(this.entityClasses);
        Collections.sort(entityClassesHierarchy, new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                if(o1.equals(o2)) {
                    return 0;
                }
                if(o1.isAssignableFrom(o2)) {
                    return -1;
                }
                return 1;
            }
        }); //It's fine to just sort entityClasses because their order doesn't
            //matter
        String[] fieldValues;
        if(entityClasses.size() == 1) {
            //save some comparisons
            fieldValues = new String[fields.size()];
            for(Field field : fields.keySet()) {
                Object fieldValue = field.get(entity);
                fieldValues[fields.get(field)] = fieldValue != null ? fieldValue.toString() : "";
            }
        }else {
            fieldValues = new String[fields.size()+1];
            fieldValues[0] = entity.getClass().getSimpleName(); //type column
            for(Field field : fields.keySet()) {
                if(this.fieldRetriever.retrieveRelevantFields(entity.getClass()).contains(field)) {
                    Object fieldValue = field.get(entity);
                    fieldValues[fields.get(field)] = fieldValue != null ? fieldValue.toString() : "";
                }
            }
        }
        addRow(fieldValues);
        entities.add(entity);
    }

    public void addAllEntities(Collection<E> entities) throws IllegalArgumentException, IllegalAccessException {
        for(E entity : this.entities) {
            addEntity(entity);
        }
    }

    public void removeEntity(E entity) {
        removeRow(entities.indexOf(entity));
        entities.remove(entity);
    }

    public void removeEntity(int index) {
        removeRow(index);
        entities.remove(index);
    }

    public List<E> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    public Map<Integer, String> getTooltipTextMap() {
        return Collections.unmodifiableMap(tooltipTextMap);
    }

    public void clear() {
        while(this.getRowCount() > 0) {
            this.removeRow(0);
        }
        this.entities.clear();
    }
}
