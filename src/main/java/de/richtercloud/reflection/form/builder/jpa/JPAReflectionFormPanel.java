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
package de.richtercloud.reflection.form.builder.jpa;

import de.richtercloud.reflection.form.builder.ReflectionFormPanel;
import de.richtercloud.reflection.form.builder.ReflectionFormPanelUpdateListener;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import de.richtercloud.reflection.form.builder.storage.Storage;
import java.lang.reflect.Field;
import java.util.Map;
import javax.swing.JComponent;

/**
 *
 * @author richter
 * @param <T> the type of entites to manage
 * @param <U> the type of update listener to expect
 */
public abstract class JPAReflectionFormPanel<T, U extends ReflectionFormPanelUpdateListener> extends ReflectionFormPanel<U> {
    private static final long serialVersionUID = 1L;
    private final Storage storage;

    /**
     * Creates a {@code JPAReflectionFormPanel}.
     * @param storage the storage to use
     * @param instance the instance to handle
     * @param entityClass the entity class
     * @param fieldMapping the field mapping
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
