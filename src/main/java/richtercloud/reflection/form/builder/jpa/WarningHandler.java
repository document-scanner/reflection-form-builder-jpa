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
 * The purpose of the class is to allow references to resource without the need
 * for injection in Java EE.
 *
 * @author richter
 */
public interface WarningHandler<T> {

    /**
     * Allows to perform whether specific warnings ought to be given for an
     * instance before saving it after creation or editing.
     * @param instance
     * @return {@code true} if no warning has caused the user to cancel the
     * saving, {@code false} otherwise
     */
    boolean handleWarning(T instance);
}
