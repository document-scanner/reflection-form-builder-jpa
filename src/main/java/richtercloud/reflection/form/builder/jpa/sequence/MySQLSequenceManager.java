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
import java.sql.ResultSet;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;

/**
 * MySQL doesn't support sequences, so using a table with a single column is the
 * only option.
 *
 * @author richter
 */
/*
internal implementation notes:
- Mysql5Dialect doesn't support queries. Apparently MySQL doesn't in general.
*/
public class MySQLSequenceManager extends AbstractSequenceManager<Long> {
    private final static Logger LOGGER = LoggerFactory.getLogger(MySQLSequenceManager.class);
    private final String onlyColumnName = "counter";

    public MySQLSequenceManager(PersistenceStorage storage) {
        super(storage);
    }

    @Override
    public boolean checkSequenceExists(String sequenceName) throws SequenceManagementException {
        return doSQLTask((Connection connection) -> {
            String query = String.format("SHOW TABLES LIKE '%s';",
                    sequenceName);
                //quote with '' because LIKE expects a string (quoting with ``
                //doesn't make sense)
            LOGGER.debug(String.format("running query '%s'",
                    query));
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement(query);
                resultSet = statement.executeQuery();
                if(!resultSet.next()) {
                    //need to create sequence
                    return false;
                }else {
                    //need to validate existing sequence
                    do {
                        String resultSequenceName = resultSet.getString(1 //only column index (1-based)
                        );
                        if(resultSequenceName.equals(sequenceName)) {
                            return true;
                        }
                    }while(resultSet.next());
                }
                return false;
            }finally {
                if(statement != null) {
                    statement.close();
                }
                if(resultSet != null) {
                    resultSet.close();
                }
            }
        });
    }

    @Override
    public void createSequence(String sequenceName) throws SequenceManagementException {
        doSQLTask((Connection connection) -> {
            String query0 = String.format("CREATE TABLE `%s` (%s INT NOT NULL);",
                    sequenceName,
                    onlyColumnName);
            String query1 = String.format("INSERT INTO `%s` VALUES (0);",
                    sequenceName);
            LOGGER.debug(String.format("running query '%s\n%s'",
                    query0,
                    query1));
            try (Statement statement = connection.createStatement()) {
                statement.addBatch(query0);
                statement.addBatch(query1);
                    //following http://stackoverflow.com/questions/26578313/how-do-i-create-a-sequence-in-mysql
                statement.executeBatch();
            }
            return null;
        });
    }

    /**
     *
     * @param sequenceName
     * @return
     * @throws SequenceManagementException
     */
    /*
    internal implementation notes:
    - `UPDATE child_codes SET counter_field = LAST_INSERT_ID(counter_field + 1);
    SELECT LAST_INSERT_ID();` fails due to
    `java.sql.SQLException: Column 'counter' not found.` which doesn't make any
    sense
    */
    @Override
    public Long getNextSequenceValue(String sequenceName) throws SequenceManagementException {
        return doSQLTask((Connection connection) -> {
            String query0 = String.format("UPDATE `%s` SET %s = (@next := %s + 1);",
                    sequenceName,
                    onlyColumnName,
                    onlyColumnName);
            try (PreparedStatement statement = connection.prepareStatement(query0)) {
                LOGGER.debug(String.format("running query '%s'",
                        query0));
                statement.execute();
                    //PreparedStatement.executeQuery causes `java.sql.SQLException: Can not issue data manipulation statements with executeQuery().`
            }
            String query1 = String.format("SELECT @next;");
                //- following http://stackoverflow.com/questions/26578313/how-do-i-create-a-sequence-in-mysql
                //- using Connection.createStatement.addBatch causes
                //`java.sql.SQLException: Can not issue SELECT via executeUpdate() or executeLargeUpdate().`
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement(query1);
                LOGGER.debug(String.format("running query '%s'",
                        query1));
                resultSet = statement.executeQuery();
                if(!resultSet.next()) {
                    throw new SequenceManagementException("sequence query result is empty");
                }
                Long retValue = resultSet.getLong("@next");
                return retValue;
            }finally {
                if(statement != null) {
                    statement.close();
                }
                if(resultSet != null) {
                    resultSet.close();
                }
            }
        });
    }
}
