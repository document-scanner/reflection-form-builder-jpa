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
 * An {@link IdApplier} which relies on the {@link GeneratedValue} annotation
 * which is managed by the JPA provider.
 *
 * Note that if you can't use a business key as JPA ID, you'll most likely have
 * to manually assign your IDs and thus this implementation can't be used.
 *
 * @author richter
 */
/*
internal implementation notes:
- it might be possible to ensure that the value is displayed if it's retrieved
from the JPA providers value generator and applied using the ValueSetter
profile from document-scanner; also figure out whether FieldUpdates could be
used
*/
public class GeneratedValueIdApplier implements IdApplier<JComponent> {

    @Override
    public void applyId(Object entity, Set<JComponent> component) {
        //nothing to do
    }
}
