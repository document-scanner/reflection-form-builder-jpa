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

/*
internal implementation notes:
- passing instances to components only requires reflection based creation which
needs to be ensured for JPA anyway
- This interface is no longer used in document-scanner where it was used for
programmatic ID generation (with the option of manual ID assignment), but it
will be kept in the reflection-form-builder library
- could be handled in PersistenceStorage, but a separate interface follows
composition-over-inheritance
*/
/**
 *
 * @author richter
 * @param <T> the type of the ID (if you want to generate an ID which covers
 *     multiple fields, return a {@link Entry} or a custom data container
 *     instance)
 */
public interface IdGenerator<T> {

    /**
     * Get the next available Id for the entity {@code instance}.
     * @param instance the instance to get the next id for
     * @return the next id
     * @throws IdGenerationException wraps any exception which occurs during ID
     *     generation
     */
    T getNextId(Object instance) throws IdGenerationException;
}
