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

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import javax.swing.JComponent;
import richtercloud.reflection.form.builder.FieldAnnotationHandler;
import richtercloud.reflection.form.builder.FieldUpdateEvent;
import richtercloud.reflection.form.builder.FieldUpdateListener;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.jpa.panels.IdFieldUpdateEvent;
import richtercloud.reflection.form.builder.jpa.panels.LongIdPanel;
import richtercloud.reflection.form.builder.jpa.panels.LongIdPanelUpdateEvent;
import richtercloud.reflection.form.builder.jpa.panels.LongIdPanelUpdateListener;

/**
 *
 * @author richter
 */
/*
internal implementation notes:
- in order to be able to map the annotation handler to the JPA Id annotation
there're no subtypes and the type of the id is retrieved from the fieldType
argument of the handle method
*/
public class IdFieldAnnoationHandler implements FieldAnnotationHandler<Object, FieldUpdateEvent<Object>> {
    private IdGenerator idGenerator;
    private String idValidationFailureDialogTitle;

    public IdFieldAnnoationHandler(IdGenerator idGenerator,
            String idValidationFailureDialogTitle) {
        this.idGenerator = idGenerator;
        this.idValidationFailureDialogTitle = idValidationFailureDialogTitle;
    }

    /**
     *
     * @param fieldClass
     * @param fieldValue
     * @param entity
     * @param updateListener
     * @param reflectionFormBuilder
     * @return
     */
    /*
    internal implementation notes:
    - due to the fact that information about the field type and the field value
    needs to be passed and that the field value can be null two parameter are
    passed (a java.lang.reflect.Field reference could be
    passed, but that'd be not good style)
    */
    @Override
    public JComponent handle(Type fieldClass,
            Object fieldValue,
            Object entity,
            final FieldUpdateListener<FieldUpdateEvent<Object>> updateListener,
            ReflectionFormBuilder reflectionFormBuilder) {
        if(fieldClass == null) {
            throw new IllegalArgumentException("fieldClass mustn't be null");
        }
        LongIdPanel retValue;
        if(fieldClass.equals(Long.class)) {
            retValue = new LongIdPanel(this.idGenerator,
                    entity,
                    (Long) fieldValue, //initialValue
                    this.idValidationFailureDialogTitle);
        }else {
            throw new IllegalArgumentException(String.format("field type %s is not supported", fieldValue.getClass()));
        }
        retValue.addUpdateListener(new LongIdPanelUpdateListener() {

            @Override
            public void onUpdate(LongIdPanelUpdateEvent event) {
                updateListener.onUpdate(new IdFieldUpdateEvent(event.getNewValue()));
            }
        });
        return retValue;
    }

}
