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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.message.handler.ConfirmMessageHandler;
import richtercloud.message.handler.Message;
import richtercloud.message.handler.MessageHandler;
import richtercloud.reflection.form.builder.FieldInfo;
import richtercloud.reflection.form.builder.FieldRetriever;

/**
 * Code reusage for the validation routine including handling of validation
 * violations messages with a {@link MessageHandler}.
 *
 * @author richter
 */
/*
internal implementation notes:
- References to entity properties can't be specified here because they aren't#
part of the framework -> use WarningHandler
*/
public class EntityValidator {
    private final static Logger LOGGER = LoggerFactory.getLogger(EntityValidator.class);
    private final FieldRetriever fieldRetriever;
    private final MessageHandler messageHandler;
    private final static ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private final static Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();
    private final ConfirmMessageHandler confirmMessageHandler;
    private final Map<Class<?>, WarningHandler<?>> warningHandlers;
    private final static Comparator<Class<?>> CLASS_COMPARATOR = new Comparator<Class<?>>() {
        @Override
        public int compare(Class<?> o1, Class<?> o2) {
            if(o1.equals(o2)) {
                return 0;
            }else if(o1.isAssignableFrom(o2)) {
                return 1;
            }
            return -1;
        }
    };

    public EntityValidator(FieldRetriever fieldRetriever,
            MessageHandler messageHandler,
            ConfirmMessageHandler confirmMessageHandler,
            Map<Class<?>, WarningHandler<?>> warningHandlers) {
        this.fieldRetriever = fieldRetriever;
        this.messageHandler = messageHandler;
        this.confirmMessageHandler = confirmMessageHandler;
        this.warningHandlers = warningHandlers;
    }

    /**
     * Validates {@code instance} contained in the {@link Warnings} validation
     * group and requests user input in a {@link ConfirmMessageHandler} if a
     * validation constraint is violated.
     *
     * @param instance
     * @return {@code false} if validation exception of the {@link Warnings}
     * group failed and the user canceled the saving or if a
     * {@link WarningHandler#handleWarning(java.lang.Object) } returned
     * {@code false} (indicating as well that a user canceled the saving),
     * {@code true} otherwise
     */
    public boolean handleWarnings(Object instance) {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(instance,
                Warnings.class);
        if(!violations.isEmpty()) {
            for(ConstraintViolation<Object> violation : violations) {
                int answer = confirmMessageHandler.confirm(new Message(violation.getMessage(),
                        JOptionPane.WARNING_MESSAGE,
                        "Warning"));
                if(answer != JOptionPane.YES_OPTION) {
                    return false;
                }
            }
        }
        WarningHandler warningHandler = warningHandlers.get(instance.getClass());
        if(warningHandler == null) {
            //figure out whether there's a WarningHandler for a subclass (in the
            //order or inheritance)
            List<Class<?>> warningHandlerClasses = new LinkedList<>(warningHandlers.keySet());
            warningHandlerClasses = warningHandlerClasses.stream().filter(clazz -> clazz.isAssignableFrom(instance.getClass())).collect(Collectors.toList());
            Collections.sort(warningHandlerClasses, CLASS_COMPARATOR);
            if(!warningHandlerClasses.isEmpty()) {
                warningHandler = warningHandlers.get(warningHandlerClasses.get(0));
            }
        }
        if(warningHandler == null) {
            LOGGER.debug(String.format("warningHandlers doesn't "
                    + "contain a %s for instances of type %s or superclasses", WarningHandler.class, instance.getClass()));
            return true;
        }
        return warningHandler.handleWarning(instance);
    }

    /**
     *
     * @param instance
     * @param groups
     */
    public void validate(Object instance, Class<?>... groups) throws EntityValidationException {
        Set violations = VALIDATOR.validate(instance,
                groups);
        if(!violations.isEmpty()) {
            String message = buildConstraintVioloationMessage(violations,
                    instance,
                    this.fieldRetriever);
            throw new EntityValidationException(message);
        }
    }

    /**
     * Builds a useful message from multiple constraint violations
     * @param violations the detected constraint violations to build the message
     * from
     * @param instance the instance which causes the constaint violation(s)
     * @param fieldRetriever the field retriever to use to enhance the message
     * with field information
     * @return the built message
     */
    public static String buildConstraintVioloationMessage(Set<ConstraintViolation<?>> violations,
            Object instance,
            FieldRetriever fieldRetriever) {
        StringBuilder messageBuilder = new StringBuilder(1000);
        messageBuilder.append("<html>");
        messageBuilder.append("The following constraints are violated:<br/>");
        for(ConstraintViolation<?> violation : violations) {
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
                    List<Field> classFields = fieldRetriever.retrieveRelevantFields(instance.getClass());
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
        return message;
    }
}
