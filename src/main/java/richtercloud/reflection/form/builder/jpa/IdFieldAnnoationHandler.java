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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JComponent;
import richtercloud.reflection.form.builder.FieldAnnotationHandler;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;

/**
 *
 * @author richter
 */
public class IdFieldAnnoationHandler implements FieldAnnotationHandler {
    private IdGenerator idGenerator;
    private String idValidationFailureDialogTitle;

    public IdFieldAnnoationHandler(IdGenerator idGenerator, String idValidationFailureDialogTitle) {
        this.idGenerator = idGenerator;
        this.idValidationFailureDialogTitle = idValidationFailureDialogTitle;
    }


    @Override
    public JComponent handle(Class<?> clazz, Object entity, ReflectionFormBuilder reflectionFormBuilder) {
        JComponent retValue;
        retValue = new IdPanel(this.idGenerator, entity, this.idValidationFailureDialogTitle);
        return retValue;
    }

}
