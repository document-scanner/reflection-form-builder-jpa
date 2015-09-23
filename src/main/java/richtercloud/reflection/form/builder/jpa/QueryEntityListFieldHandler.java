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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import javax.persistence.EntityManager;
import javax.swing.JComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.AbstractListFieldHandler;
import richtercloud.reflection.form.builder.EntityFieldUpdateEvent;
import richtercloud.reflection.form.builder.EntityListFieldUpdateEvent;
import richtercloud.reflection.form.builder.FieldHandler;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.UpdateListener;
import richtercloud.reflection.form.builder.jpa.panels.ListQueryPanel;
import richtercloud.reflection.form.builder.jpa.panels.ListQueryPanelUpdateEvent;
import richtercloud.reflection.form.builder.jpa.panels.ListQueryPanelUpdateListener;

/**
 * Handles fields with type lists of entity with a list of {@link QueryPanel}s.
 * Cannot be instantiated without a reference to {@link EntityManager}.
 * @author richter
 */
public class QueryEntityListFieldHandler extends AbstractListFieldHandler<EntityListFieldUpdateEvent> implements FieldHandler<EntityListFieldUpdateEvent> {
    private final static Logger LOGGER = LoggerFactory.getLogger(QueryEntityListFieldHandler.class);
    private EntityManager entityManager;

    public QueryEntityListFieldHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public JComponent handle0(Type type, final UpdateListener<EntityListFieldUpdateEvent> updateListener, ReflectionFormBuilder reflectionFormBuilder) {
        LOGGER.debug("handling type {}", type);
        //don't assert that type is instanceof ParameterizedType because a
        //simple List can be treated as List<Object>
        Class<?> entityClass;
        if(type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if(parameterizedType.getActualTypeArguments().length == 0) {
                //can happen according to docs
                entityClass = Object.class;
            }else if(parameterizedType.getActualTypeArguments().length > 1) {
                throw new IllegalArgumentException(String.format("can't handle more than one type argument with a %s (type is %s)", List.class, type));
            }
            Type listGenericType = parameterizedType.getActualTypeArguments()[0];
            if(!(listGenericType instanceof Class)) {
                throw new IllegalArgumentException(String.format("first type argument of type %s isn't an instance of %s", type, Class.class));
            }
            entityClass = (Class<?>) listGenericType;
        }else {
            entityClass = Object.class;
        }
        final ListQueryPanel retValue = new ListQueryPanel(this.entityManager, reflectionFormBuilder, entityClass);
        retValue.addUpdateListener(new ListQueryPanelUpdateListener() {
            @Override
            public void onUpdate(ListQueryPanelUpdateEvent event) {
                updateListener.onUpdate(new EntityListFieldUpdateEvent(event.getNewSelectionItem()));
            }
        });
        return retValue;
    }

}

