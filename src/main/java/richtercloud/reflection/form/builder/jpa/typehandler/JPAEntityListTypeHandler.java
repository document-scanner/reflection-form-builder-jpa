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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import javax.persistence.EntityManager;
import javax.swing.JComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import richtercloud.reflection.form.builder.jpa.JPAReflectionFormBuilder;
import richtercloud.reflection.form.builder.jpa.panels.QueryListPanel;
import richtercloud.reflection.form.builder.message.MessageHandler;
import richtercloud.reflection.form.builder.panels.ListPanelItemEvent;
import richtercloud.reflection.form.builder.panels.ListPanelItemListener;
import richtercloud.reflection.form.builder.typehandler.AbstractListTypeHandler;

/**
 *
 * @author richter
 */
public class JPAEntityListTypeHandler extends AbstractListTypeHandler<List<Object>, FieldUpdateEvent<List<Object>>,JPAReflectionFormBuilder> {
    private final static Logger LOGGER = LoggerFactory.getLogger(JPAEntityListTypeHandler.class);
    private final EntityManager entityManager;
    private final String bidirectionalHelpDialogTitle;

    public JPAEntityListTypeHandler(EntityManager entityManager,
            MessageHandler messageHandler,
            String bidirectionalHelpDialogTitle) {
        super(messageHandler);
        this.entityManager = entityManager;
        this.bidirectionalHelpDialogTitle = bidirectionalHelpDialogTitle;
    }

    @Override
    public JComponent handle0(Type type,
            List<Object> fieldValue,
            final FieldUpdateListener<FieldUpdateEvent<List<Object>>> updateListener,
            ReflectionFormBuilder reflectionFormBuilder) throws IllegalArgumentException, IllegalAccessException {
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
        final QueryListPanel retValue = new QueryListPanel(entityManager,
                reflectionFormBuilder,
                entityClass,
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

}
