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

/**
 *
 * @author richter
 * @param <T> the type of the ID (if you want to generate an ID which covers
 * multiple fields, return a {@link Entry} or a custom data container instance)
 */
/*
internal implementation notes:
- passing instances to components only requires reflection based creation which
needs to be ensured for JPA anyway
- This interface is no longer used in document-scanner where it was used for
programmatic ID generation (with the option of manual ID assignment), but it
will be kept in the reflection-form-builder library
*/
public interface IdGenerator<T> {

    /**
     * Get the next available Id for the entity {@code instance}.
     * @param instance
     * @return the next id
     */
    T getNextId(Object instance);
}
