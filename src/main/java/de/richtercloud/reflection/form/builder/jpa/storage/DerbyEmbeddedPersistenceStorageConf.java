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
import org.apache.derby.jdbc.EmbeddedDriver;

/**
 *
 * @author richter
 */
public class DerbyEmbeddedPersistenceStorageConf extends AbstractPersistenceStorageConf {
    private static final long serialVersionUID = 1L;
    private final static String DRIVER_NAME = EmbeddedDriver.class.getName();
    public final static String USERNAME_DEFAULT = "";
    static {
        try {
            Class.forName(DRIVER_NAME);
        } catch (ClassNotFoundException ex) {
            //dependencies not provided properly
            throw new ExceptionInInitializerError(ex);
        }
    }

    public DerbyEmbeddedPersistenceStorageConf(Set<Class<?>> entityClasses,
            String databaseName,
            File schemeChecksumFile) throws IOException {
        super(DRIVER_NAME, //databaseDriver
                entityClasses,
                USERNAME_DEFAULT,
                databaseName,
                schemeChecksumFile);
    }

    public DerbyEmbeddedPersistenceStorageConf(String databaseDriver,
            Set<Class<?>> entityClasses,
            String username,
            String password,
            String databaseName,
            File schemeChecksumFile) throws FileNotFoundException, IOException {
        super(databaseDriver,
                entityClasses,
                username,
                password,
                databaseName,
                schemeChecksumFile);
    }

    @Override
    public String getConnectionURL() {
        return String.format("jdbc:derby:%s;create=%s",
                getDatabaseName(),
                String.valueOf(!new File(getDatabaseName()).exists()));
    }

    @Override
    public String getShortDescription() {
        return "Derby embedded database connection";
    }

    @Override
    public String getLongDescription() {
        return "Can consume quite a lot of memory, but doesn't require other "
                + "resources to be set up.";
    }
}
