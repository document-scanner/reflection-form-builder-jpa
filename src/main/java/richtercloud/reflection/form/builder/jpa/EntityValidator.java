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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import richtercloud.reflection.form.builder.FieldInfo;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.message.Message;
import richtercloud.reflection.form.builder.message.MessageHandler;

/**
 * Code reusage for the validation routine including handling of validation
 * violations messages with a {@link MessageHandler}.
 *
 * @author richter
 */
public class EntityValidator {
    private final FieldRetriever fieldRetriever;
    private final MessageHandler messageHandler;

    public EntityValidator(FieldRetriever fieldRetriever,
            MessageHandler messageHandler) {
        this.fieldRetriever = fieldRetriever;
        this.messageHandler = messageHandler;
    }

    /**
     *
     * @param instance
     * @param groups
     * @return {@code true} if the validation passed without any violations,
     * {@code false} otherwise
     */
    public boolean validate(Object instance, Class<?>... groups) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<Object>> violations = validator.validate(instance,
                groups);
        if(!violations.isEmpty()) {
            StringBuilder messageBuilder = new StringBuilder(1000);
            messageBuilder.append("<html>");
            messageBuilder.append("The following constraints are violated:<br/>");
            for(ConstraintViolation<Object> violation : violations) {
                String propertyPath = violation.getPropertyPath().toString();
                List<String> propertyPathSplit = new LinkedList<>(Arrays.asList(propertyPath.split("\\."))); //an empty string causes a list with "" to be returned by "".split("\\."") (not necessarily intuitive)
                String fieldName = propertyPath;
                //there's no way to retrieve information about the field or
                //class on which the validation annotation has been specified
                //-> retrieval of this information is done with the property
                //path of the ConstraintViolation

                //violations which occur at the class level of the root instance
                //have an empty property path -> just display the message in a
                //separate line
                //note: both violations which occur at the root and at
                //a property have a property path with 1 node (which doesn't
                //necessarily make sense) -> there's no way to evaluate retrieve
                //the field names if a nested violation occurs -> split
                //propertyPath.toString() at `.`.
                if(!propertyPath.isEmpty()) {
                    if(propertyPathSplit.size() > 1) {
                        throw new IllegalArgumentException("Property path of violation is larger than 2 nodes. This isn't supported yet.");
                    }
                    if(!propertyPathSplit.isEmpty()) {
                        //should be always true because it's already checked
                        //that propertyPath.toString isn't empty, but check
                        //nevertheless
                        String violationFieldName = propertyPathSplit.get(0);
                        Field violationField = null;
                        List<Field> classFields = this.fieldRetriever.retrieveRelevantFields(instance.getClass());
                        for(Field classField : classFields) {
                            if(classField.getName().equals(violationFieldName)) {
                                violationField = classField;
                                break;
                            }
                        }
                        if(violationField == null) {
                            throw new IllegalArgumentException("validation violoation constraint on field which isn't part of the validated instance");
                        }
                        FieldInfo violationFieldInfo = violationField.getAnnotation(FieldInfo.class);
                        if(violationFieldInfo != null) {
                            fieldName = violationFieldInfo.name();
                        }
                        messageBuilder.append(fieldName);
                        messageBuilder.append(": ");
                    }
                }
                messageBuilder.append(violation.getMessage());
                messageBuilder.append("<br/>");
            }
            messageBuilder.append("Fix the corresponding values in the components.");
            messageBuilder.append("</html>");
            String message = messageBuilder.toString();
            this.messageHandler.handle(new Message(message,
                    JOptionPane.WARNING_MESSAGE,
                    "Validation failed"));
            return false;
        }
        return true;
    }
}
