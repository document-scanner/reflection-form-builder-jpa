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
package richtercloud.reflection.form.builder.jpa.sequence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.persistence.EntityManager;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;

/**
 *
 * @author richter
 */
public class PostgresqlSequenceManager extends HibernateWrapperSequenceManager {

    public PostgresqlSequenceManager(PersistenceStorage storage) {
        super(storage);
    }

    @Override
    public void createSequence(String sequenceName) throws SequenceManagementException {
        EntityManager entityManager = getStorage().retrieveEntityManager();
        entityManager.getTransaction().begin();
        Connection connection = entityManager.unwrap(Connection.class);
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(String.format("CREATE SEQUENCE IF NOT EXISTS %s", sequenceName));
        } catch (SQLException ex) {
            throw new SequenceManagementException(ex);
        }finally {
            entityManager.getTransaction().commit();
        }
    }

    @Override
    public Long getNextSequenceValue(String sequenceName) {
//        fdsa
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
