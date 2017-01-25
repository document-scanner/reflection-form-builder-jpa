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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;

/**
 * Uses {@link Dialect} API to provide {@link SequenceManager} functionality.
 *
 * Inspired by http://stackoverflow.com/questions/20948536/any-way-to-check-for-the-existence-of-a-sequence-using-jdbc.
 *
 * @author richter
 */
/*
internal implementation notes:
- made abstract in order to ease overview and testing and eventually different
escape methods
*/
public abstract class HibernateWrapperSequenceManager extends AbstractSequenceManager<Long> {
    /**
     * {@code true} as long as https://hibernate.atlassian.net/browse/HHH-11403
     * is not fixed.
     */
    protected final static boolean HIBERNATE_NEED_TO_WORKAROUND_ESCAPE = true;
    /**
     * Some databases support only a certain minimum for the initial value of
     * the sequence.
     */
    private final int initialValue;

    public HibernateWrapperSequenceManager(PersistenceStorage<Long> storage,
            int initialValue) {
        super(storage);
        this.initialValue = initialValue;
    }

    @Override
    public boolean checkSequenceExists(String sequenceName) throws SequenceManagementException {
        return doHibernateSQLTask((dialect, connection) -> {
            if ( !dialect.supportsSequences() ) {
                throw new RuntimeException();
            }
            String sql = dialect.getQuerySequencesString();
            if (sql == null) {
                throw new RuntimeException(); //@TODO
            }
            Statement statement = null;
            ResultSet rs = null;
            try {
                statement = connection.createStatement();
                rs = statement.executeQuery(sql);
                while ( rs.next() ) {
                    if(sequenceName.equals(rs.getString(1))) {
                        return true;
                    }
                }
            } finally {
                if (rs!=null) {
                    rs.close();
                }
                if (statement!=null) {
                    statement.close();
                }
            }
            return false;
        });
    }

    @Override
    public void createSequence(String sequenceName) throws SequenceManagementException {
        doHibernateSQLTask((dialect, connection) -> {
            if ( !dialect.supportsSequences() ) {
                throw new RuntimeException(); //@TODO
            }
            String[] sqls = dialect.getCreateSequenceStrings(sequenceName,
                    initialValue, //initialValue
                    1 //incrementSize
            );
            if (sqls == null) {
                throw new RuntimeException(); //@TODO
            }
            for(String sql : sqls) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
            return null;
        });
    }

    @Override
    public Long getNextSequenceValue(String sequenceName) throws SequenceManagementException {
        return doHibernateSQLTask((dialect, connection) -> {
            if ( !dialect.supportsSequences() ) {
                throw new RuntimeException(); //@TODO
            }
            String sql = dialect.getSequenceNextValString(sequenceName);
            if (sql == null) {
                throw new RuntimeException(); //@TODO
            }
            Statement statement = null;
            ResultSet rs = null;
            try {
                statement = connection.createStatement();
                rs = statement.executeQuery(sql);
                if(!rs.next()) {
                    throw new RuntimeException();
                }
                return rs.getLong(1 //columnIndex (1-based; using sequenceName
                        //as columnName fails due to column label being unknown;
                        //it's fine to assume that there's only one column)
                );
            } finally {
                if (rs!=null) {
                    rs.close();
                }
                if (statement!=null) {
                    statement.close();
                }
            }
        });
    }

    private <T> T doHibernateSQLTask(HibernateSQLTask<T> task) throws SequenceManagementException {
        return doSQLTask((connection) -> {
            DialectResolver dialectResolver = new StandardDialectResolver();
            DialectResolutionInfo dialectResolutionInfo = new DatabaseMetaDataDialectResolutionInfoAdapter(connection.getMetaData());
            Dialect dialect =  dialectResolver.resolveDialect(dialectResolutionInfo);
            return task.run(dialect,
                    connection);
        });
    }

    @FunctionalInterface
    private interface HibernateSQLTask<T> {

        T run(Dialect dialect, Connection connection) throws SQLException;
    }

    protected String escapeSequenceName(String sequenceName,
            String escapeBegin,
            String escapeEnd) {
        String sequenceName0;
        if(!HIBERNATE_NEED_TO_WORKAROUND_ESCAPE) {
            sequenceName0 = sequenceName;
        }else {
            sequenceName0 = String.format("%s%s%s", escapeBegin,
                    sequenceName,
                    escapeEnd);
        }
        return sequenceName0;
    }
}
