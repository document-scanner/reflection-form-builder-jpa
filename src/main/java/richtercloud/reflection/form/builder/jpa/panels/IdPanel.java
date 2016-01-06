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
package richtercloud.reflection.form.builder.jpa.panels;

import richtercloud.reflection.form.builder.jpa.EntityReflectionFormPanel;

/**
 * An interface to be able to trigger ID generation from both within GUI components (e.g. {@link LongIdPanel} and at saving (e.g. in {@link EntityReflectionFormPanel} when automatic generation of IDs at saving is desired).
 *
 * @author richter
 */
public interface IdPanel {

    /**
     * Applies to the next ID to the component or the components in panel which
     * are used to generate it based on its or their values. Can be called
     * multiple times and cause the same or different IDs to be applied.
     *
     * @return {@code true} if the application has been successful,
     * {@code false} otherwise
     */
    /*
    internal implementation notes:
    - return value is necessary in order to react to handling of validation
    which might not be noticable for the program (e.g. displaying a dialog)
    */
    boolean applyNextId();
}
