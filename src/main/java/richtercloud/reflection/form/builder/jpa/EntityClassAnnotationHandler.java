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

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.swing.JComponent;
import richtercloud.reflection.form.builder.ClassAnnotationHandler;
import richtercloud.reflection.form.builder.FieldUpdateEvent;
import richtercloud.reflection.form.builder.FieldUpdateListener;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.SimpleEntityFieldUpdateEvent;
import richtercloud.reflection.form.builder.jpa.panels.QueryPanel;
import richtercloud.reflection.form.builder.jpa.panels.QueryPanelUpdateEvent;
import richtercloud.reflection.form.builder.jpa.panels.QueryPanelUpdateListener;

/**
 * Handles fields with a type which have a {@link Entity} class annotation.
 * @author richter
 */
public class EntityClassAnnotationHandler implements ClassAnnotationHandler<Object, FieldUpdateEvent<Object>> {
    private EntityManager entityManager;

    public EntityClassAnnotationHandler(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public JComponent handle(Class<? extends Object> fieldType,
            Object fieldValue,
            final FieldUpdateListener<FieldUpdateEvent<Object>> updateListener,
            ReflectionFormBuilder reflectionFormBuilder) {
        try {
            QueryPanel<Object> retValue = new QueryPanel<>(this.entityManager,
                    fieldType,
                    reflectionFormBuilder,
                    fieldValue
            );
            retValue.addUpdateListener(new QueryPanelUpdateListener() {
                @Override
                public void onUpdate(QueryPanelUpdateEvent event) {
                    updateListener.onUpdate(new SimpleEntityFieldUpdateEvent(event.getNewSelectionItem()));
                }
            });
            return retValue;
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

}
