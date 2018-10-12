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
package de.richtercloud.reflection.form.builder.jpa.sequence;

import de.richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.EntityManager;

/**
 *
 * @author richter
 * @param <T> the type of sequence values to generate
 */
public abstract class AbstractSequenceManager<T> implements SequenceManager<T> {
    private final PersistenceStorage storage;

    public AbstractSequenceManager(PersistenceStorage storage) {
        this.storage = storage;
    }

    public PersistenceStorage getStorage() {
        return storage;
    }

    protected <T> T doSQLTask(SQLTask<T> task) throws SequenceManagementException {
        EntityManager entityManager = getStorage().retrieveEntityManager();
        entityManager.getTransaction().begin();
        try(Connection connection = entityManager.unwrap(Connection.class)) {
            try {
                return task.run(connection);
            } catch (SQLException ex) {
                throw new SequenceManagementException(ex);
                    //everything will be wrapped into a SequenceManagementException
                    //anyway, so the wrapping might as well occur here
            } finally {
                entityManager.getTransaction().commit();
            }
        }catch(SQLException ex) {
            throw new SequenceManagementException(ex);
        }
    }
}
