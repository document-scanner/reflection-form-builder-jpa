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
package de.richtercloud.reflection.form.builder.jpa;

/**
 * An {@link IdGenerator} which does nothing (returns {@code null} as ID which
 * can be used if ID generation is handled by the JPA provider.
 *
 * @author richter
 */
public class GeneratedValueIdGenerator implements IdGenerator<Object> {

    @Override
    public Object getNextId(Object instance) throws IdGenerationException {
        return null;
    }
}
