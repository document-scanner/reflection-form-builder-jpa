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
package richtercloud.reflection.form.builder.jpa.storage;


/**
 * Initializes fields in a runtime-configurable way which is interesting for
 * lazily fetched fields of JPA-managed entities.
 *
 * Note that initializing fields through reflection doesn't work in Hibernate
 * 5.0.11.Final, maybe in Hibernate or JPA in general.
 *
 * @author richter
 */
/*
internal implementation notes:
- see internal implementation notes of PersistenceStorage for important details
*/
public interface FieldInitializer {

    /**
     * Fetches all field values which are marked {@link FetchType#LAZY}.
     *
     * @param entity the entity to initialize
     */
    /*
    internal implementation notes:
    - not a good idea to put this in a separate interface (see class comment for
    details)
    */
    void initialize(Object entity) throws IllegalArgumentException,
            IllegalAccessException;
}
