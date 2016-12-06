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
import java.io.IOException;
import java.util.Set;

/**
 *
 * @author richter
 */
public class DerbyEmbeddedPersistenceStorageConf extends AbstractPersistenceStorageConf {
    private static final long serialVersionUID = 1L;
    private final static String DRIVER_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
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
            File databaseDir,
            File schemeChecksumFile) throws IOException {
        super(DRIVER_NAME, //databaseDriver
                entityClasses,
                USERNAME_DEFAULT,
                databaseDir,
                schemeChecksumFile);
    }

    @Override
    public String getConnectionURL() {
        String retValue = String.format("jdbc:derby:%s",
                getDatabaseDir().getAbsolutePath());
        return retValue;
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
