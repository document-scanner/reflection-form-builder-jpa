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

import java.util.Collections;
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
import richtercloud.reflection.form.builder.FieldInfo;
import richtercloud.reflection.form.builder.Tools;
import richtercloud.validation.tools.FieldRetriever;
import richtercloud.validation.tools.ValidationTools;

/**
 * Code reusage for the validation routine including handling of validation
 * violations messages with a {@link ConfirmMessageHandler}.
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
    private final static ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private final static Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();
    private final ConfirmMessageHandler confirmMessageHandler;
    private final Map<Class<?>, WarningHandler<?>> warningHandlers;

    public EntityValidator(FieldRetriever fieldRetriever,
            ConfirmMessageHandler confirmMessageHandler,
            Map<Class<?>, WarningHandler<?>> warningHandlers) {
        this.fieldRetriever = fieldRetriever;
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
            Collections.sort(warningHandlerClasses, Tools.CLASS_COMPARATOR_SUBCLASS_FIRST);
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
            String message = ValidationTools.buildConstraintVioloationMessage(violations,
                    instance,
                    this.fieldRetriever,
                violationField -> {
                    FieldInfo violationFieldInfo = violationField.getAnnotation(FieldInfo.class);
                    if(violationFieldInfo != null) {
                        return violationFieldInfo.name();
                    }
                    return null;
                },
                    true //html
            ); //@TODO: fix checkstyle indentation failure, see https://github.com/checkstyle/checkstyle/issues/3342
                //for issue report
            throw new EntityValidationException(message);
        }
    }
}
