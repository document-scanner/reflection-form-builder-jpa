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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.swing.GroupLayout;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;

/**
 * Allows to run a JPQL query for a specific class while getting feedback about
 * the following errors:<ul>
 * <li>syntax errors in queries</li>
 * <li>queries with the wrong result type/class</li>
 * <li>(unexpected) errors which occured during execution of the query</li>
 * </ul> and an overview of the result in a table displaying all class fields
 * (as returned by {@link ReflectionFormBuilder#retrieveRelevantFields(java.lang.Class) }. The feedback is given in a scrollable (non-editable) label in order
 * to provide a fixed size layout.
 *
 * In favour of immutability as design principle QueryPanel needs to be
 * recreated in order to handle a different entity class.
 *
 * There's currently no feature to hide specific class fields.
 *
 * @author richter
 * @param <E> a generic type for the entity class
 */
public class QueryPanel<E> extends AbstractQueryPanel {
    private static final long serialVersionUID = 1L;
    private final static Logger LOGGER = LoggerFactory.getLogger(QueryPanel.class);

    protected static void handleInstanceToTableModel(DefaultTableModel queryResultTableModel,
            Object queryResult,
            ReflectionFormBuilder reflectionFormBuilder,
            Class<?> entityClass) throws IllegalArgumentException,
            IllegalAccessException {
        List<Object> queryResultValues = new LinkedList<>();
        for(Field field : reflectionFormBuilder.getFieldRetriever().retrieveRelevantFields(entityClass)) {
            queryResultValues.add(field.get(queryResult));
        }
        queryResultTableModel.addRow(queryResultValues.toArray(new Object[queryResultValues.size()]));
    }
    public static void initTableModel(DefaultTableModel tableModel, List<Field> entityClassFields) {
        for(Field field : entityClassFields) {
            tableModel.addColumn(field.getName());
        }
    }

    /**
     * Checks every field of the type of every field of {@code entityClass} if
     * it's assignable from (i.e. a superclass) of {@code entityClass} so that
     * it can be assumed that a relationship can be defined (this avoids a
     * reference to a declaring class being passed to the constructor and thus
     * arbitrary nesting of type handling).
     *
     * @param entityClass
     * @param entityClassFields
     * @return
     */
    public static Set<Field> retrieveMappedFieldCandidates(Class<?> entityClass,
            List<Field> entityClassFields,
            FieldRetriever fieldRetriever) {
        Set<Field> retValue = new HashSet<>();
        for(Field entityClassField : entityClassFields) {
            for(Field entityClassFieldField : entityClassField.getType().getDeclaredFields()) {
                OneToOne entityClassFieldOneToOne = entityClassField.getAnnotation(OneToOne.class);
                //ManyToOne doesn't have a mappedBy field, but it needs to be
                //checked to be offered for a user-defined mapping
                ManyToOne entityClassFieldManyToOne = entityClassField.getAnnotation(ManyToOne.class);
                if(entityClassFieldOneToOne != null || entityClassFieldManyToOne != null) {
                    if(entityClass.isAssignableFrom(entityClassFieldField.getType())) {
                        retValue.add(entityClassField);
                    }
                }
            }
        }
        return retValue;
    }

    public static Field retrieveMappedByField(List<Field> entityClassFields) {
        Field retValue = null;
        for(Field entityClassField : entityClassFields) {
            OneToOne entityClassFieldOneToOne = entityClassField.getAnnotation(OneToOne.class);
            if(entityClassFieldOneToOne != null) {
                String mappedBy = entityClassFieldOneToOne.mappedBy();
                if(mappedBy != null) {
                    //if mappedBy is specified the user isn't given a choice
                    retValue = entityClassField;
                }
            }
        }
        return retValue;
    }
    private final E initialValue;
    /**
     *
     * @param entityManager
     * @param entityClass the class for which to the panel for
     * @param reflectionFormBuilder
     * @param initialValue
     * @param queryResultTableSelectionMode
     * @param initialHistory a list of history entries which ought to be selectable in the query history combo box (won't be modified)
     * @param initialQueryLimit When the component is created an initial query is executed. This property
     * limits its result length. Set to {@code 0} in order to skip initial
     * query.
     * @param initialSelectedHistoryEntry the query which ought to be selected initially (if {@code null} the first item of {@code predefinedQueries} will be selected initially or there will be no selected item if {@code intiialHistory} is empty.
     * @param bidirectionalControlPanel
     * @throws java.lang.IllegalAccessException
     * @throws IllegalArgumentException if {@code initialSelectedHistoryEntry} is not {@code null}, but not contained in {@code initialHistory}
     */
    /*
    internal implementation notes:
    - it's necessary to use a copy of initialHistory in order to avoid ConcurrentModificationException when items are sorted in combobox model implementation
    - enforce passing of BidirectionalControlPanel in order to maximize
    reusability (and because it's perfectly legitimate due to
    composition-over-inheritance)
    */
    public QueryPanel(EntityManager entityManager,
            Class<? extends E> entityClass,
            ReflectionFormBuilder reflectionFormBuilder,
            E initialValue,
            BidirectionalControlPanel bidirectionalControlPanel) throws IllegalArgumentException, IllegalAccessException {
        super(bidirectionalControlPanel,
                new QueryComponent<E>(entityManager, entityClass),
                reflectionFormBuilder,
                entityClass,
                entityManager,
                ListSelectionModel.SINGLE_SELECTION);
        List<Field> entityClassFields = reflectionFormBuilder.getFieldRetriever().retrieveRelevantFields(entityClass);
        initTableModel(this.getQueryResultTableModel(),
                entityClassFields);
        this.initialValue = initialValue;

        GroupLayout.ParallelGroup horizontalParallelGroup = getLayout().createParallelGroup();
        horizontalParallelGroup.addGroup(super.getHorizontalParallelGroup())
                .addComponent(getQueryResultTableScrollPane(), GroupLayout.Alignment.TRAILING);
        getLayout().setHorizontalGroup(horizontalParallelGroup);

        GroupLayout.SequentialGroup verticalSequentialGroup = getLayout().createSequentialGroup();
        verticalSequentialGroup
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(super.getVerticalSequentialGroup())
                .addComponent(getQueryResultTableScrollPane(), GroupLayout.DEFAULT_SIZE, 251, Short.MAX_VALUE)
                .addContainerGap();
        getLayout().setVerticalGroup(verticalSequentialGroup);

        reset();
    }

    public Object getSelectedObject() {
        int index = this.getQueryResultTable().getSelectedRow();
        if(index < 0) {
            //can happen during layout validation/initialization
            return null;
        }
        //assume that if index is >= 0 that this.queryResults is != null as well
        Object retValue = this.getQueryResults().get(index);
        return retValue;
    }

    public void reset() {
        while(this.getQueryResultTableModel().getRowCount() > 0) {
            this.getQueryResultTableModel().removeRow(0);
        }
        if(initialValue != null) {
            if(!this.getEntityManager().contains(initialValue)) {
                this.getQueryResultLabel().setText(String.format("previously managed entity %s has been removed from persistent storage, ignoring", initialValue));
            }
            if(!this.getQueryResults().contains(initialValue)) {
                this.getQueryResults().add(initialValue); // ok to add initially (will be overwritten with the next query where the user has to specify a query which retrieves the initial value or not
            }
            int initialValueIndex = this.getQueryResults().indexOf(initialValue);
            this.getQueryResultTableSelectionModel().addSelectionInterval(initialValueIndex, initialValueIndex); //no need to clear selection because we're just initializing
        }
    }
}
