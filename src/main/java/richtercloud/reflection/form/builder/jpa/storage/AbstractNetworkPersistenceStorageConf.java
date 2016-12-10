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
import richtercloud.reflection.form.builder.storage.StorageConfInitializationException;

/**
 *
 * @author richter
 */
public abstract class AbstractNetworkPersistenceStorageConf extends AbstractPersistenceStorageConf {
    private static final long serialVersionUID = 1L;
    public final static String HOSTNAME_DEFAULT = "localhost";
    private String hostname = HOSTNAME_DEFAULT;
    private int port;

    public AbstractNetworkPersistenceStorageConf(String databaseDriver,
            int port,
            Set<Class<?>> entityClasses,
            String username,
            String databaseName,
            File schemeChecksumFile) throws FileNotFoundException, IOException {
        super(databaseDriver,
                entityClasses,
                username,
                databaseName,
                schemeChecksumFile);
        this.port = port;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getConnectionURL() {
        String retValue = String.format("jdbc:derby://%s:%d/%s",
                hostname,
                port,
                getDatabaseName());
        return retValue;
    }

    @Override
    public void validate() throws StorageConfInitializationException {
        super.validate();
        if(getPassword().isEmpty()) {
            throw new StorageConfInitializationException("Password mustn't be empty");
        }
        if(getUsername().isEmpty()) {
            throw new StorageConfInitializationException("Password mustn't be empty");
        }
    }
}
