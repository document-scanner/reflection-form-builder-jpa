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
package richtercloud.reflection.form.builder.jpa.idapplier;

import java.util.Set;
import javax.swing.JComponent;

/**
 *
 * @author richter
 * @param <C> allows to enforce an interface or component type in order to
 * coordinate actions which concern multiple ID field components (consider using
 * setter factories in order to overcome the shortcoming of Java Swing which
 * doesn't base JComponent on an interface)
 */
public interface IdApplier<C extends JComponent> {

    boolean applyId(Object entity, Set<C> idFieldComponents);
}
