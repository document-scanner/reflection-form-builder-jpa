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
package de.richtercloud.reflection.form.builder.jpa.idapplier;

import java.util.Set;
import javax.swing.JComponent;

/**
 * Manages evaluation of values of components which compose an entity ID and
 * decides whether an ID ought to be assigned or changed or not (because the ID
 * isn't already set, already taken, etc. - that's up to implementations).
 *
 * The source for IDs is most likely an
 * {@link richtercloud.reflection.form.builder.jpa.IdGenerator} which you can
 * also use to assign IDs in a situation which doesn't involve GUI components.
 *
 * @author richter
 * @param <C> allows to enforce an interface or component type in order to
 *     coordinate actions which concern multiple ID field components (consider
 *     using setter factories in order to overcome the shortcoming of Java Swing
 *     which doesn't base JComponent on an interface)
 */
public interface IdApplier<C extends JComponent> {

    /**
     * Checks whether an ID has already been set on {@code idFieldComponents}
     * and returns immediately if this is the case (it's up to implementations
     * to decide how to determine that an ID has been set - most likely they'll
     * assume that it isn't set if one or all fields are {@code null}), then
     * retrieves a valid ID from a source (e.g. a {@link IdGenerator} and sets
     * it on {@code idFieldComponents}.
     *
     * @param entity the entity to apply to
     * @param idFieldComponents the set of id field components
     * @throws IdApplicationException if an exception during the application of
     *     the ID occured
     */
    void applyId(Object entity, Set<C> idFieldComponents) throws IdApplicationException;
}
