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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.panels.AbstractListPanel;
import richtercloud.reflection.form.builder.panels.ListPanelItemEvent;
import richtercloud.reflection.form.builder.panels.ListPanelItemListener;

/**
 * Provides a {@link QueryPanel} and controls to add and remove stored
 * entities from a list.
 *
 * Currently there're no editing facilities.
 *
 * @author richter
 */
/*
internal implementtion notes:
- can't be used in NetBeans GUI builder because it requires a zero-argument
constructor in QueryPanel which is used as imported component here -> the
requirement to learn GroupLayout is very legitimate
*/
public class QueryListPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final static Logger LOGGER = LoggerFactory.getLogger(QueryListPanel.class);
    private final DefaultTableModel resultTableModel = new DefaultTableModel();
    private final ReflectionFormBuilder reflectionFormBuilder;
    private final Class<?> entityClass;
    private final Set<ListPanelItemListener<Object>> updateListeners = new HashSet<>();
    /**
     * A constantly up-to-date list of selected references (the "result" of the
     * panel)
     */
    private final List<Object> resultList = new LinkedList<>();
    private final JButton addButton;
    private final QueryPanel<Object> queryPanel;
    private final JButton removeButton;
    private final JTable resultTable;
    private final JLabel resultTableLabel;
    private final JScrollPane resultTableScrollPane;
    private final List<Object> initialValues;

    /**
     *
     * @param entityManager
     * @param reflectionFormBuilder
     * @param entityClass the class for which to the panel for
     * @param declaringClass the class containing the field of type
     * {@code entityClass} (used for bidirectional relationships)
     * @param initialValues
     * @param bidirectionalHelpDialogTitle
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public QueryListPanel(EntityManager entityManager,
            ReflectionFormBuilder reflectionFormBuilder,
            Class<?> entityClass,
            List<Object> initialValues,
            String bidirectionalHelpDialogTitle) throws IllegalArgumentException, IllegalAccessException {
        if(entityManager == null) {
            throw new IllegalArgumentException("entityManager mustn't be null");
        }
        if(reflectionFormBuilder == null) {
            throw new IllegalArgumentException("reflectionFormBuilder mustn't be null");
        }
        if(entityClass == null) {
            throw new IllegalArgumentException("entityClass mustn't be null");
        }
        removeButton = new JButton();
        addButton = new JButton();
        resultTableLabel = new JLabel();
        resultTableScrollPane = new JScrollPane();
        resultTable = new JTable() {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        List<Field> entityClassFields = reflectionFormBuilder.getFieldRetriever().retrieveRelevantFields(entityClass);
        Set<Field> mappedFieldCandidates = retrieveMappedFieldCandidates(entityClass,
                        entityClassFields,
                        reflectionFormBuilder.getFieldRetriever());
        BidirectionalControlPanel bidirectionalControlPanel = new BidirectionalControlPanel(entityClass,
                bidirectionalHelpDialogTitle,
                retrieveMappedByField(entityClassFields, mappedFieldCandidates),
                mappedFieldCandidates);
        QueryPanel.validateEntityClass(entityClass, entityManager);
        this.reflectionFormBuilder = reflectionFormBuilder;
        this.entityClass = entityClass;
        QueryPanel.initTableModel(this.resultTableModel, this.reflectionFormBuilder.getFieldRetriever().retrieveRelevantFields(entityClass));
        this.initialValues = initialValues;
        reset();
        queryPanel = new QueryPanel<>(entityManager,
                entityClass,
                reflectionFormBuilder,
                null, //initialValue
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION,
                bidirectionalControlPanel);
        initComponents();
        this.resultTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    /**
     * Checks every field of the type of every field of {@code entityClass} if
     * it's assignable from (i.e. a superclass) of {@code entityClass} so that
     * it can be assumed that a relationship can be defined (this avoids a
     * reference to a declaring class being passed to the constructor and thus
     * arbitrary nesting of type handling).
     *
     * Only JPA-annotated field are used whereas it'd be possbile to check based
     * on field types as well. The implementation naively assumes that generic
     * types and targetEntity attributes of annotations are correct and
     * validated by JPA providers.
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
            OneToMany entityClassFieldOneToMany = entityClassField.getAnnotation(OneToMany.class);
            ManyToMany entityClassFieldManyToMany = entityClassField.getAnnotation(ManyToMany.class);
            if(entityClassFieldOneToMany != null || entityClassFieldManyToMany != null) {
                Class<?> entityClassFieldType = null;
                if(entityClassFieldOneToMany != null) {
                    Class<?> targetEntity = entityClassFieldOneToMany.targetEntity();
                    if(targetEntity != null) {
                        if(!targetEntity.equals(void.class)) {
                            //if targetEntity isn't specified it is void for
                            //some reason
                            entityClassFieldType = targetEntity;
                        }
                    }
                }
                if(entityClassFieldManyToMany != null) {
                    Class<?> targetEntity = entityClassFieldManyToMany.targetEntity();
                    if(targetEntity != null) {
                        if(!targetEntity.equals(void.class)) {
                            //if targetEntity isn't specified it is void for
                            //some reason
                            entityClassFieldType = targetEntity;
                        }
                    }
                }
                if(List.class.isAssignableFrom(entityClassField.getType())) {
                    Type entityClassFieldListType = entityClassField.getGenericType();
                    if(!(entityClassFieldListType instanceof ParameterizedType)) {
                        throw new IllegalArgumentException(String.format("field %s isn't declared as parameterized type and doesn't have a target annotation, can't handle field", entityClassField));
                    }
                    ParameterizedType entityClassFieldListParameterizedType = (ParameterizedType) entityClassFieldListType;
                    Type[] entityClassFieldListParameterizedTypeArguments = entityClassFieldListParameterizedType.getActualTypeArguments();
                    if(entityClassFieldListParameterizedTypeArguments.length == 0) {
                        throw new IllegalArgumentException();
                    }
                    if(entityClassFieldListParameterizedTypeArguments.length > 1) {
                        throw new IllegalArgumentException();
                    }
                    if(!(entityClassFieldListParameterizedTypeArguments[0] instanceof Class)) {
                        throw new IllegalArgumentException();
                    }
                    Class<?> entityClassFieldFieldParameterizedTypeArgument = (Class<?>) entityClassFieldListParameterizedTypeArguments[0];
                    entityClassFieldType = entityClassFieldFieldParameterizedTypeArgument;
                }else {
                    throw new IllegalArgumentException(String.format("collection type %s of field %s not supported", entityClassField.getType(), entityClassField));
                }

                for(Field entityClassFieldField : fieldRetriever.retrieveRelevantFields(entityClassFieldType)) {
                    //OneToOne and OneToMany don't make sense
                    ManyToOne entityClassFieldFieldManyToOne = entityClassFieldField.getAnnotation(ManyToOne.class);
                    ManyToMany entityClassFieldFieldManyToMany = entityClassFieldField.getAnnotation(ManyToMany.class);
                    if(entityClassFieldFieldManyToOne != null || entityClassFieldFieldManyToMany != null) {
                        if(entityClassFieldFieldManyToOne != null) {
                            Class<?> targetEntity = entityClassFieldFieldManyToOne.targetEntity();
                            if(targetEntity != null) {
                                if(!targetEntity.equals(void.class)) {
                                    retValue.add(entityClassField);
                                    continue;
                                }
                            }
                            Class<?> entityClassFieldFieldType = entityClassFieldField.getType();
                            if(entityClassFieldType.isAssignableFrom(entityClassFieldFieldType)) {
                                retValue.add(entityClassField);
                            }
                        }
                        if(entityClassFieldFieldManyToMany != null) {
                            Class<?> targetEntity = entityClassFieldFieldManyToMany.targetEntity();
                            if(targetEntity != null) {
                                if(!targetEntity.equals(void.class)) {
                                    retValue.add(entityClassField);
                                    continue;
                                }
                            }
                            if(List.class.isAssignableFrom(entityClassField.getType())) {
                                Type entityClassFieldListType = entityClassField.getGenericType();
                                if(!(entityClassFieldListType instanceof ParameterizedType)) {
                                    throw new IllegalArgumentException(String.format("field %s isn't declared as parameterized type and doesn't have a target annotation, can't handle field", entityClassField));
                                }
                                ParameterizedType entityClassFieldListParameterizedType = (ParameterizedType) entityClassFieldListType;
                                Type[] entityClassFieldListParameterizedTypeArguments = entityClassFieldListParameterizedType.getActualTypeArguments();
                                if(entityClassFieldListParameterizedTypeArguments.length == 0) {
                                    throw new IllegalArgumentException();
                                }
                                if(entityClassFieldListParameterizedTypeArguments.length > 1) {
                                    throw new IllegalArgumentException();
                                }
                                if(!(entityClassFieldListParameterizedTypeArguments[0] instanceof Class)) {
                                    throw new IllegalArgumentException();
                                }
                                Class<?> entityClassFieldFieldParameterizedTypeArgument = (Class<?>) entityClassFieldListParameterizedTypeArguments[0];
                                Class<?> entityClassFieldFieldType = entityClassFieldFieldParameterizedTypeArgument;
                                if(entityClassFieldType.isAssignableFrom(entityClassFieldFieldType)) {
                                    retValue.add(entityClassField);
                                }
                            }else {
                                throw new IllegalArgumentException(String.format("collection type %s of field %s not supported", entityClassField.getType(), entityClassField));
                            }
                        }
                    }
                }
            }
        }
        return retValue;
    }

    /**
     * checks both the {@code entityClass} fields' annotations and the mapped
     * field candidates annotations for XToMany annoations with {@code mappedBy}
     * attribute
     *
     * @param entityClassFields
     * @return
     */
    public static Field retrieveMappedByField(List<Field> entityClassFields, Set<Field> mappedFieldCandidates) {
        //check entityClass fields' annotations
        for(Field entityClassField : entityClassFields) {
            Field retValue = checkMappedByField(entityClassField);
            if(retValue != null) {
                return retValue;
            }
        }
        //check mapped field candidates annotations
        for(Field mappedFieldCandidate : mappedFieldCandidates) {
            Field retValue = checkMappedByField(mappedFieldCandidate);
            if(retValue != null) {
                return retValue;
            }
        }
        return null;
    }

    private static Field checkMappedByField(Field field) {
        OneToMany entityClassFieldOneToMany = field.getAnnotation(OneToMany.class);
        //ManyToOne doesn't have a mappedBy field, but it needs to be
        //checked to be offered for a user-defined mapping
        ManyToMany entityClassFieldManyToMany = field.getAnnotation(ManyToMany.class);
        if(entityClassFieldOneToMany != null) {
            String mappedBy = entityClassFieldOneToMany.mappedBy();
            if(mappedBy != null && !mappedBy.isEmpty()) {
                //if mappedBy is specified the user isn't given a choice
                return field;
            }
        }else if(entityClassFieldManyToMany != null) {
            String mappedBy = entityClassFieldManyToMany.mappedBy();
            if(mappedBy != null && !mappedBy.isEmpty()) {
                //if mappedBy is specified the user isn't given a choice
                return field;
            }
        }
        return null;
    }

    public void addItemListener(ListPanelItemListener<Object> updateListener) {
        this.updateListeners.add(updateListener);
    }

    public void removeItemListener(ListPanelItemListener<Object> updateListener) {
        this.updateListeners.remove(updateListener);
    }

    private void initComponents() throws IllegalArgumentException, IllegalAccessException {
        removeButton.setText("Remove");
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        addButton.setText("Add");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        resultTableLabel.setText("Selected entities:");

        resultTable.setModel(this.resultTableModel);
        resultTableScrollPane.setViewportView(resultTable);

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(queryPanel, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(resultTableScrollPane, GroupLayout.DEFAULT_SIZE, 573, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(resultTableLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(addButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(removeButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(queryPanel, GroupLayout.PREFERRED_SIZE, 222, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(removeButton)
                    .addComponent(addButton)
                    .addComponent(resultTableLabel))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(resultTableScrollPane, GroupLayout.DEFAULT_SIZE, 167, Short.MAX_VALUE)
                .addContainerGap())
        );
    }

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int[] indices = this.queryPanel.getQueryResultTable().getSelectedRows();
        for(int index : indices) {
            Object queryResult = this.queryPanel.getQueryResults().get(index);
            try {
                QueryPanel.handleInstanceToTableModel(this.resultTableModel, queryResult, reflectionFormBuilder, entityClass);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                LOGGER.info("an exception occured while executing the query", ex);
                this.queryPanel.getQueryStatusLabel().setText(String.format("<html>%s</html>", ex.getMessage()));
            }
            this.resultList.add(queryResult);
            for(ListPanelItemListener<Object> updateListener : updateListeners) {
                updateListener.onItemAdded(new ListPanelItemEvent<>(ListPanelItemEvent.EVENT_TYPE_ADDED, index, resultList));
            }
        }
    }

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int[] selectedRows = this.resultTable.getSelectedRows();
        if(selectedRows.length == 0) {
            return;
        }
        //need to sort in order to remove from highest to lowest value
        List<Integer> selectedRowsSorted = new LinkedList<>();
        for(int selectedRow : selectedRows) {
            selectedRowsSorted.add(selectedRow);
        }
        Collections.sort(selectedRowsSorted, AbstractListPanel.DESCENDING_ORDER);
        for(int selectedRow : selectedRowsSorted) {
            this.resultTableModel.removeRow(selectedRow);
            Object queryResult = this.queryPanel.getQueryResults().get(selectedRow);
            this.resultList.remove(queryResult);
            for(ListPanelItemListener<Object> updateListener : updateListeners) {
                updateListener.onItemRemoved(new ListPanelItemEvent<>(ListPanelItemEvent.EVENT_TYPE_REMOVED, selectedRow, this.resultList));
            }
        }
    }

    public void reset() {
        this.resultList.clear();
        if(initialValues != null) {
            this.resultList.addAll(initialValues); //before initComponents (simply use resultList for initialization)
        }
    }
}
