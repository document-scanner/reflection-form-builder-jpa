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
import richtercloud.reflection.form.builder.storage.Storage;
import richtercloud.reflection.form.builder.storage.StorageException;

/**
 *
 * @author richter
 */
/*
internal implementation notes:
- this interface currently only directly exposes EntityManager which makes quite
ambiguous, but it allows to create query components (similar to
reflection-form-builders QueryPanel) on XML files, etc. For this remove exposure
of Entity
- A method to fetch lazy entity fields can be here because it can be handled
well using reflection and PersistenceStorage can easily get knowledge about the
owning field of a value
*/
public interface PersistenceStorage extends Storage<Object, AbstractPersistenceStorageConf> {

    <T> List<T> runQuery(String queryString,
            Class<T> clazz,
            int queryLimit) throws StorageException;

    <T> List<T> runQuery(String attribueName,
            String attributeValue,
            Class<T> clazz) throws StorageException;

    <T> List<T> runQueryAll(Class<T> clazz);

    boolean isClassSupported(Class<?> clazz);

    boolean isManaged(Object object);
}
