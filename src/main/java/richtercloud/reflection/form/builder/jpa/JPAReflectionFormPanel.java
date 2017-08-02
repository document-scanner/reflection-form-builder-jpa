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
import richtercloud.reflection.form.builder.ReflectionFormPanel;
import richtercloud.reflection.form.builder.ReflectionFormPanelUpdateListener;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import richtercloud.reflection.form.builder.storage.Storage;

/**
 *
 * @author richter
 */
public abstract class JPAReflectionFormPanel<T, U extends ReflectionFormPanelUpdateListener> extends ReflectionFormPanel<U> {
    private static final long serialVersionUID = 1L;
    private Storage storage;

    /**
     * Creates a {@code JPAReflectionFormPanel}.
     * @param entityManager
     * @param instance
     * @param entityClass
     * @param fieldMapping
     * @param fieldHandler the {@link FieldHandler} to perform reset actions
     */
    public JPAReflectionFormPanel(Storage storage,
            T instance,
            Class<? extends T> entityClass,
            Map<Field, JComponent> fieldMapping,
            FieldHandler fieldHandler) {
        super(fieldMapping,
                instance,
                entityClass,
                fieldHandler);
        this.storage = storage;
    }

    public Storage getStorage() {
        return storage;
    }
}
