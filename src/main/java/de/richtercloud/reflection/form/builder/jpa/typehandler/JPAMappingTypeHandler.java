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
package de.richtercloud.reflection.form.builder.jpa.typehandler;

import de.richtercloud.reflection.form.builder.ReflectionFormBuilder;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import de.richtercloud.reflection.form.builder.typehandler.MappingTypeHandler;

/**
 *
 * @author richter
 * @param <T> the type to handle
 * @param <E> the type of field update events to expect
 * @param <R> the type of reflection form builder to use
 */
public class JPAMappingTypeHandler<T, E extends FieldUpdateEvent<T>, R extends ReflectionFormBuilder> extends MappingTypeHandler<T,E,R> {

}
