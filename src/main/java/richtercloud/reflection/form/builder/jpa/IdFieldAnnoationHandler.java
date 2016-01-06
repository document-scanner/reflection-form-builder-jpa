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

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import javax.swing.JComponent;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import richtercloud.reflection.form.builder.fieldhandler.FieldAnnotationHandler;
import richtercloud.reflection.form.builder.jpa.panels.LongIdPanel;
import richtercloud.reflection.form.builder.message.MessageHandler;
import richtercloud.reflection.form.builder.panels.NumberPanel;
import richtercloud.reflection.form.builder.panels.NumberPanelUpdateEvent;
import richtercloud.reflection.form.builder.panels.NumberPanelUpdateListener;

/**
 *
 * @author richter
 */
/*
internal implementation notes:
- in order to be able to map the annotation handler to the JPA Id annotation
there're no subtypes and the type of the id is retrieved from the fieldType
argument of the handle method
- Currently enforce Long as id type. Other types are possible, abstract if necessary
*/
public class IdFieldAnnoationHandler implements FieldAnnotationHandler<Long, FieldUpdateEvent<Long>, JPAReflectionFormBuilder> {
    private final IdGenerator idGenerator;
    private final MessageHandler messageHandler;
    private final FieldRetriever fieldRetriever;

    public IdFieldAnnoationHandler(IdGenerator idGenerator,
            MessageHandler messageHandler,
            FieldRetriever fieldRetriever) {
        this.idGenerator = idGenerator;
        this.messageHandler = messageHandler;
        this.fieldRetriever = fieldRetriever;
    }

    /**
     *
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
    public JComponent handle(Field field,
            Object instance,
            final FieldUpdateListener<FieldUpdateEvent<Long>> updateListener,
            JPAReflectionFormBuilder reflectionFormBuilder) throws IllegalAccessException {
        if(field == null) {
            throw new IllegalArgumentException("fieldClass mustn't be null");
        }
        Type fieldType = field.getGenericType();
        Long fieldValue = (Long) field.get(instance);
        NumberPanel<Long> retValue;
        if(fieldType.equals(Long.class)) {
            retValue = new LongIdPanel(this.idGenerator,
                    instance, fieldValue, //initialValue
                    messageHandler,
                    fieldRetriever);
        }else {
            throw new IllegalArgumentException(String.format("field type %s is not supported", fieldValue.getClass()));
        }
        retValue.addUpdateListener(new NumberPanelUpdateListener<Long>() {

            @Override
            public void onUpdate(NumberPanelUpdateEvent<Long> event) {
                updateListener.onUpdate(new FieldUpdateEvent<>(event.getNewValue()));
            }
        });
        return retValue;
    }

}
