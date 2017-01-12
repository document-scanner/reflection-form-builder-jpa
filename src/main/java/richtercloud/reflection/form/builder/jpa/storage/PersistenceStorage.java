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

import java.util.List;
import javax.persistence.EntityManager;
import richtercloud.reflection.form.builder.storage.Storage;
import richtercloud.reflection.form.builder.storage.StorageException;

/**
 *
 * @author richter
 */
/*
internal implementation notes:
- Where to place a method for fetching lazy entity fields:
- here: allows access to EntityManager in implementations without exposing it in
the interface, but requires dependency on JPA provider's custom classes (like
Session and such) since there's no portable way in JPA -> inacceptable
- leaving the method unimplemented, thus making all implementations in this
module abstract and providing a module with implementations for every+
persistence provider makes things tricky with inheritance hierarchies (either
the initialize method has to be implemented redundantly in every
PersistenceStorage implementation, an ugly static workaround has to be used to
the initialize method has to be sorted out into an interface which then doesn't
have easy access to EntityManager)
-> make PersistenceStorage JPA-aware because it just doesn't make sense another
way -> Then it's very easy to expose EntityManager (while this interface still
makes sense because implementations handle a lot of workarounds as well as start
and shutdown routines) and leave initialization to a FieldInitializer interface
which can get the EntityManager to unwrap JPA provider specific classes.
- handle check of existence of sequence and creation in SequenceManager in order
to avoid messing up PersistenceStorage inheritance chain (composition over
inheritance)
*/
public interface PersistenceStorage extends Storage<Object, AbstractPersistenceStorageConf> {

    <T> List<T> runQuery(String queryString,
            Class<T> clazz,
            int queryLimit) throws StorageException;

    <T> List<T> runQuery(String attribueName,
            String attributeValue,
            Class<T> clazz) throws StorageException;

    <T> List<T> runQueryAll(Class<T> clazz);

    /**
     * Checks whether {@code clazz} is a managed entity.
     * @param clazz
     * @return {@code true} if {@code clazz} is a managed entity, {@code false}
     * otherwise
     */
    boolean isClassSupported(Class<?> clazz);

    /**
     * Checks whether {@code object} in managed in a JPA context.
     * @param object
     * @return {@code true} if {@code object} is managed, {@code false}
     * otherwise
     */
    boolean isManaged(Object object);

    EntityManager retrieveEntityManager();
}
