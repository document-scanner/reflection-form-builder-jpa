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
package de.richtercloud.reflection.form.builder.jpa.storage;

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
    public final static String DATABASE_DRIVER_DEFAULT = Driver.class.getName();
    public final static String USERNAME_DEFAULT = "sa";
    public final static int PORT_DEFAULT = 5432;
    static {
        try {
            Class.forName(DATABASE_DRIVER_DEFAULT);
        }catch(ClassNotFoundException ex) {
            //dependencies not provided properly
            throw new ExceptionInInitializerError(ex);
        }
    }

    public PostgresqlPersistenceStorageConf(Set<Class<?>> entityClasses,
            String hostname,
            String username,
            String password,
            String databaseName,
            File schemeChecksumFile) throws FileNotFoundException, IOException {
        super(DATABASE_DRIVER_DEFAULT, //databaseDriver
                hostname,
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
     * @param port the port
     * @param hostname the hostname
     * @param databaseDriver the database driver
     * @param entityClasses the entity classes to manage
     * @param username the username
     * @param password the password
     * @param databaseName the database name
     * @param schemeChecksumFile the scheme checksum file
     * @throws FileNotFoundException in case the scheme checksum file can't be
     *     found
     * @throws IOException in case an I/O exception occurs while reading from or
     *     writing to the scheme checksum file
     */
    public PostgresqlPersistenceStorageConf(Set<Class<?>> entityClasses,
            String hostname,
            String username,
            String password,
            String databaseName,
            File schemeChecksumFile,
            int port,
            String databaseDriver) throws FileNotFoundException, IOException {
        super(hostname,
                port, //port
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
        return String.format("jdbc:postgresql://%s:%d/%s",
                getHostname(),
                getPort(),
                getDatabaseName());
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
