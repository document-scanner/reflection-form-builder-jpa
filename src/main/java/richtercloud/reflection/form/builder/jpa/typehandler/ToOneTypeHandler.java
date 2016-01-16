/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package richtercloud.reflection.form.builder.jpa.typehandler;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.swing.JComponent;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import richtercloud.reflection.form.builder.jpa.JPAReflectionFormBuilder;
import richtercloud.reflection.form.builder.jpa.panels.BidirectionalControlPanel;
import richtercloud.reflection.form.builder.jpa.panels.QueryPanel;
import richtercloud.reflection.form.builder.jpa.panels.QueryPanelUpdateEvent;
import richtercloud.reflection.form.builder.jpa.panels.QueryPanelUpdateListener;
import richtercloud.reflection.form.builder.typehandler.TypeHandler;

/**
 *
 * @author richter
 */
public class ToOneTypeHandler implements TypeHandler<Object, FieldUpdateEvent<Object>, JPAReflectionFormBuilder, QueryPanel>{
    private final EntityManager entityManager;
    private final String bidirectionalHelpDialogTitle;

    public ToOneTypeHandler(EntityManager entityManager,
            String bidirectionalHelpDialogTitle) {
        this.entityManager = entityManager;
        this.bidirectionalHelpDialogTitle = bidirectionalHelpDialogTitle;
    }

    @Override
    public JComponent handle(Type type,
            Object fieldValue,
            String fieldName,
            Class<?> declaringClass,
            final FieldUpdateListener<FieldUpdateEvent<Object>> updateListener,
            JPAReflectionFormBuilder reflectionFormBuilder) throws IllegalArgumentException,
            IllegalAccessException,
            FieldHandlingException,
            InstantiationException,
            InvocationTargetException {
        if(!(type instanceof Class)) {
            throw new IllegalArgumentException("the generic type of type has to be instanceof Class");
        }
        Class<?> entityClass = (Class<?>)type;
        List<Field> entityClassFields = reflectionFormBuilder.getFieldRetriever().retrieveRelevantFields(entityClass);
        Set<Field> mappedFieldCandidates = QueryPanel.retrieveMappedFieldCandidates(entityClass,
                        entityClassFields,
                        reflectionFormBuilder.getFieldRetriever());
        BidirectionalControlPanel bidirectionalControlPanel = new BidirectionalControlPanel(declaringClass, bidirectionalHelpDialogTitle, QueryPanel.retrieveMappedByField(entityClassFields), mappedFieldCandidates);
        QueryPanel retValue = new QueryPanel(entityManager,
                entityClass,
                reflectionFormBuilder,
                fieldValue,
                bidirectionalControlPanel);
        retValue.addUpdateListener(new QueryPanelUpdateListener() {
            @Override
            public void onUpdate(QueryPanelUpdateEvent event) {
                updateListener.onUpdate(new FieldUpdateEvent<>(event.getNewSelectionItem()));
            }
        });
        return retValue;
    }

    @Override
    public void reset(QueryPanel component) {
        component.reset();
    }
}
