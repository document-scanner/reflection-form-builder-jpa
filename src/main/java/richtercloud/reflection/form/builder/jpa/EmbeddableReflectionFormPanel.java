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

import java.lang.reflect.Field;
import java.util.Map;
import javax.swing.JComponent;
import richtercloud.reflection.form.builder.ReflectionFormPanelUpdateListener;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;

/**
 * A panel to manage components to set fields of an {@link Embeddable} class.
 * Contains no save button, because all changes are written immediately, but a
 * label explaining that. Has a
 * close button in order to facilitate navigation (to avoid just have an
 * unfocusable window close icon).
 * @author richter
 * @param <T>
 */
/*
internal implementation notes:
- un- and redoing should be implemented for the whole application after a major
release
*/
public class EmbeddableReflectionFormPanel<T> extends JPAReflectionFormPanel<T, ReflectionFormPanelUpdateListener> {
    private static final long serialVersionUID = 1L;

    public EmbeddableReflectionFormPanel(PersistenceStorage storage,
            T instance,
            Class<? extends T> entityClass,
            Map<Field, JComponent> fieldMapping,
            FieldHandler fieldHandler) {
        super(storage,
                instance,
                entityClass,
                fieldMapping,
                fieldHandler);
    }

}
