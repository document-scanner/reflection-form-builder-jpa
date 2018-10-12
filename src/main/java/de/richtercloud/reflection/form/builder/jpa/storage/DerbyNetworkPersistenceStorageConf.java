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
import org.apache.derby.jdbc.ClientDriver;

/*
internal implementation notes:
- There's no reason to set initial values to null in the internal constructor
call hierachy in order to enforce setting by user because callers can explicitly
set values to null at initialization and reaction on validation failures.
*/
/**
 * A {@link StorageConf} for a Derby network connection (as opposed to other
 * Derby connection types, like embedded or memory instances). Uses the
 * {@code databaseDir} property to specify the database name or path (both are
 * valid) which is expected to exist in the database server the connection is
 * established to.
 *
 * @author richter
 */
public class DerbyNetworkPersistenceStorageConf extends AbstractNetworkPersistenceStorageConf {
    private static final long serialVersionUID = 1L;
    private final static String DRIVER_NAME = ClientDriver.class.getName();
    public final static String USERNAME_DEFAULT = "sa";
    public final static int PORT_DEFAULT = 1527;
    static {
        try {
            Class.forName(DRIVER_NAME);
        }catch(ClassNotFoundException ex) {
            //dependencies not provided properly
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Constructs a new {@link DerbyNetworkPersistenceStorageConf} with
     * {@code password} and {@code databaseName} set to {@code null} in order to
     * enforce them being set by callers/users.
     *
     * @param entityClasses the entity classes to store
     * @param hostname the hostname
     * @param schemeChecksumFile the scheme checksum file
     * @throws FileNotFoundException in case the scheme checksum file can't be
     *     found
     * @throws IOException in case an I/O exception occurs while reading from or
     *     writing to the scheme checksum file
     */
    public DerbyNetworkPersistenceStorageConf(Set<Class<?>> entityClasses,
            String hostname,
            File schemeChecksumFile) throws FileNotFoundException, IOException {
        super("org.apache.derby.jdbc.ClientDriver", //databaseDriver
                hostname,
                PORT_DEFAULT,
                entityClasses,
                USERNAME_DEFAULT,
                null, //password (see constructor comment)
                null, //databaseDir (see constructor comment)
                schemeChecksumFile);
    }

    /**
     * Copy constructor.
     * @param hostname the hostname
     * @param port the port
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
    public DerbyNetworkPersistenceStorageConf(String hostname,
            int port,
            String databaseDriver,
            Set<Class<?>> entityClasses,
            String username,
            String password,
            String databaseName,
            File schemeChecksumFile) throws FileNotFoundException, IOException {
        super(hostname,
                port,
                databaseDriver,
                entityClasses,
                username,
                password,
                databaseName,
                schemeChecksumFile);
    }

    @Override
    public String getShortDescription() {
        return "Derby network database connection";
    }

    @Override
    public String getLongDescription() {
        return "Requires a Derby network server to be running outside the "
                + "application.";
    }
}