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

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import richtercloud.reflection.form.builder.jpa.EntityValidator;
import richtercloud.reflection.form.builder.jpa.IdGenerator;
import richtercloud.reflection.form.builder.jpa.panels.IdGenerationValidation;
import richtercloud.reflection.form.builder.jpa.panels.LongIdPanel;

/**
 *
 * @author richter
 */
public class LongIdPanelIdApplier implements IdApplier<LongIdPanel> {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private final Map<Field, JComponent> fieldMapping;
    private final EntityValidator entityValidator;
    private final IdGenerator<Long> idGenerator;

    public LongIdPanelIdApplier(Map<Field, JComponent> fieldMapping,
            IdGenerator<Long> idGenerator,
            EntityValidator entityValidator) {
        this.fieldMapping = fieldMapping;
        this.idGenerator = idGenerator;
        this.entityValidator = entityValidator;
    }

    /**
     * Only supports one value in {@code idFieldComponents}.
     * @param entity
     * @param idFieldComponents
     * @throws IllegalArgumentException if {@code idFieldComponents} contains
     * more than one value
     * @return {@code true} if the ID was applied successfully, {@code false}
     * otherwise
     */
    @Override
    public boolean applyId(Object entity, Set<LongIdPanel> idFieldComponents) {
        if(idFieldComponents.size() != 1) {
            throw new IllegalArgumentException("idFieldComponents has to contain exactly 1 component");
        }
        if(!this.entityValidator.validate(entity, IdGenerationValidation.class)) {
            return false;
        }
        Long nextId = idGenerator.getNextId(entity);
        LongIdPanel component = idFieldComponents.iterator().next();
        component.setValue(nextId);
        return true;
    }
}
