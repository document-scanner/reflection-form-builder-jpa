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

import java.lang.reflect.InvocationTargetException;
import javax.swing.JComponent;
import richtercloud.reflection.form.builder.FieldAnnotationHandler;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;

/**
 *
 * @author richter
 */
public class EmbeddedFieldAnnotationHandler implements FieldAnnotationHandler {
    private final static EmbeddedFieldAnnotationHandler INSTANCE = new EmbeddedFieldAnnotationHandler();

    public static EmbeddedFieldAnnotationHandler getInstance() {
        return INSTANCE;
    }

    protected EmbeddedFieldAnnotationHandler() {
    }

    @Override
    public JComponent handle(Class<?> clazz, Object entity, ReflectionFormBuilder reflectionFormBuilder) {
        try {
            JComponent retValue = reflectionFormBuilder.transform(clazz);
            return retValue;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }
    }

}
