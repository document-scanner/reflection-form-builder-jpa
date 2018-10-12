/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.richtercloud.reflection.form.builder.jpa.idapplier;

import de.richtercloud.reflection.form.builder.jpa.IdGenerationException;
import de.richtercloud.reflection.form.builder.jpa.IdGenerator;
import de.richtercloud.reflection.form.builder.jpa.panels.LongIdPanel;
import java.util.Set;

/**
 *
 * @author richter
 */
public class LongIdPanelIdApplier implements IdApplier<LongIdPanel> {
    private final IdGenerator<Long> idGenerator;

    public LongIdPanelIdApplier(IdGenerator<Long> idGenerator) {
        this.idGenerator = idGenerator;
    }

    /**
     * Only supports one value in {@code idFieldComponents}.
     * @param entity the entity to apply on
     * @param idFieldComponents the set of id field components
     * @throws IllegalArgumentException if {@code idFieldComponents} contains
     *     more than one value
     */
    @Override
    public void applyId(Object entity, Set<LongIdPanel> idFieldComponents) throws IdApplicationException,
            IllegalArgumentException {
        if(idFieldComponents.size() != 1) {
            throw new IllegalArgumentException("more than one item in idFieldComponents not supported yet");
        }
        LongIdPanel component = idFieldComponents.iterator().next();
        if(component.getValue() != null) {
            //id already set -> skip in order to avoid another id being assigned
            //during persisting
            return;
        }
        Long nextId;
        try {
            nextId = idGenerator.getNextId(entity);
        } catch (IdGenerationException ex) {
            throw new IdApplicationException(ex);
        }
        component.setValue(nextId);
    }
}
