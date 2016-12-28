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
import java.util.Objects;
import java.util.Set;
import richtercloud.reflection.form.builder.storage.StorageConfValidationException;

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
    public AbstractNetworkPersistenceStorageConf(int port,
            String databaseDriver,
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
    public void validate() throws StorageConfValidationException {
        super.validate();
        if(getPassword() == null || getPassword().isEmpty()) {
            throw new StorageConfValidationException("Password mustn't be empty");
        }
        if(getUsername() == null || getUsername().isEmpty()) {
            throw new StorageConfValidationException("Password mustn't be empty");
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 61 * hash + Objects.hashCode(this.hostname);
        hash = 61 * hash + this.port;
        return hash;
    }

    protected boolean equalsTransitive(AbstractNetworkPersistenceStorageConf other) {
        if(!super.equalsTransitive(other)) {
            return false;
        }
        if (this.port != other.port) {
            return false;
        }
        if (!Objects.equals(this.hostname, other.hostname)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractNetworkPersistenceStorageConf other = (AbstractNetworkPersistenceStorageConf) obj;
        return equalsTransitive(other);
    }
}
