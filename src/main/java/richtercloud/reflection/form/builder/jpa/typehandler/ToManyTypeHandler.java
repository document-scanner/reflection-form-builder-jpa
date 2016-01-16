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
package richtercloud.reflection.form.builder.jpa.typehandler;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.swing.JComponent;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import richtercloud.reflection.form.builder.jpa.JPAReflectionFormBuilder;
import richtercloud.reflection.form.builder.jpa.panels.QueryListPanel;
import richtercloud.reflection.form.builder.panels.ListPanelItemEvent;
import richtercloud.reflection.form.builder.panels.ListPanelItemListener;
import richtercloud.reflection.form.builder.typehandler.GenericListTypeHandler;
import richtercloud.reflection.form.builder.typehandler.TypeHandler;

/**
 *
 * @author richter
 */
public class ToManyTypeHandler extends GenericListTypeHandler<JPAReflectionFormBuilder, QueryListPanel> {
    private final EntityManager entityManager;
    private final String bidirectionalHelpDialogTitle;

    public ToManyTypeHandler(EntityManager entityManager,
            Map<Type, TypeHandler<?, ?, ?, ?>> genericsTypeHandlerMapping,
            Map<Type, TypeHandler<?, ?, ?, ?>> fieldTypeHandlerMapping,
            String bidirectionalHelpDialogTitle) {
        super(genericsTypeHandlerMapping,
                fieldTypeHandlerMapping);
        this.entityManager = entityManager;
        this.bidirectionalHelpDialogTitle = bidirectionalHelpDialogTitle;
    }

    @Override
    protected JComponent handleGenericType(Type type,
            List<Object> fieldValue,
            String fieldName,
            Class<?> declaringClass,
            final FieldUpdateListener<FieldUpdateEvent<List<Object>>> updateListener,
            JPAReflectionFormBuilder reflectionFormBuilder) throws IllegalAccessException {
        Type genericType = retrieveTypeGenericType(type);
        if(!(genericType instanceof Class)) {
            throw new IllegalArgumentException("the generic type of type has to be instanceof Class");
        }
        QueryListPanel retValue = new QueryListPanel(entityManager,
                reflectionFormBuilder,
                (Class<?>) type,
                fieldValue,
                bidirectionalHelpDialogTitle);
        retValue.addItemListener(new ListPanelItemListener<Object>() {
            @Override
            public void onItemAdded(ListPanelItemEvent<Object> event) {
                updateListener.onUpdate(new FieldUpdateEvent<>(event.getItem()));
            }

            @Override
            public void onItemRemoved(ListPanelItemEvent<Object> event) {
                updateListener.onUpdate(new FieldUpdateEvent<>(event.getItem()));
            }
        });
        return retValue;
    }

    @Override
    public void reset(QueryListPanel component) {
        component.reset();
    }

}
