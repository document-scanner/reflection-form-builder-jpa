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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import org.postgresql.Driver;

/**
 *
 * @author richter
 */
public class PostgresqlPersistenceStorageConf extends AbstractNetworkPersistenceStorageConf {
    private static final long serialVersionUID = 1L;
    private final static String DRIVER_NAME = Driver.class.getName();
    public final static String USERNAME_DEFAULT = "sa";
    public final static String HOSTNAME_DEFAULT = "localhost";
    public final static int PORT_DEFAULT = 5432;
    static {
        try {
            Class.forName(DRIVER_NAME);
        }catch(ClassNotFoundException ex) {
            //dependencies not provided properly
            throw new ExceptionInInitializerError(ex);
        }
    }

    public PostgresqlPersistenceStorageConf(Set<Class<?>> entityClasses,
            String username,
            String password,
            String databaseName,
            File schemeChecksumFile) throws FileNotFoundException, IOException {
        super(DRIVER_NAME, //databaseDriver
                PORT_DEFAULT, //port
                entityClasses, //entityClasses
                username, //username
                password,
                databaseName, //databaseName
                schemeChecksumFile //schemeChecksumFile
        );
    }

    /**
     * Copy constructor.
     * @param port
     * @param databaseDriver
     * @param entityClasses
     * @param username
     * @param password
     * @param databaseName
     * @param schemeChecksumFile
     * @throws FileNotFoundException
     * @throws IOException
     */
    public PostgresqlPersistenceStorageConf(Set<Class<?>> entityClasses,
            String username,
            String password,
            String databaseName,
            File schemeChecksumFile,
            int port,
            String databaseDriver) throws FileNotFoundException, IOException {
        super(port, //port
                databaseDriver, //databaseDriver
                entityClasses, //entityClasses
                username, //username
                password, //password
                databaseName, //databaseName
                schemeChecksumFile //schemeChecksumFile
        );
    }

    @Override
    public String getConnectionURL() {
        String retValue = String.format("jdbc:postgresql://%s:%d/%s",
                getHostname(),
                getPort(),
                getDatabaseName());
        return retValue;
    }

    @Override
    public String getShortDescription() {
        return "Postgresql network connection";
    }

    @Override
    public String getLongDescription() {
        return "Postgresql is a powerful performant database which is "
                + "might be a slightly bit difficult to configure."
                + "Unfortunately the Java Database Connector (JDBC) "
                + "implementation is very memory consuming. Use with care.";
    }
}
