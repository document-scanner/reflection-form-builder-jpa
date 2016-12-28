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

/**
 *
 * @author richter
 */
public class PostgresqlAutoPersistenceStorageConf extends PostgresqlPersistenceStorageConf {
    private static final long serialVersionUID = 1L;
    /**
     * If a PostgreSQL server is started we need both a database directory and
     * name.
     */
    private String databaseDir;

    public PostgresqlAutoPersistenceStorageConf(Set<Class<?>> entityClasses,
            String username,
            File schemeChecksumFile,
            String databaseDir) throws FileNotFoundException, IOException {
        super(entityClasses,
                username,
                schemeChecksumFile);
        this.databaseDir = databaseDir;
    }

    /**
     * Copy constructor.
     * @param databaseDir
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
    public PostgresqlAutoPersistenceStorageConf(String databaseDir,
            int port,
            String databaseDriver,
            Set<Class<?>> entityClasses,
            String username,
            String password,
            String databaseName,
            File schemeChecksumFile) throws FileNotFoundException, IOException {
        super(port,
                databaseDriver,
                entityClasses,
                username,
                password,
                databaseName,
                schemeChecksumFile);
        this.databaseDir = databaseDir;
    }

    public String getDatabaseDir() {
        return databaseDir;
    }

    public void setDatabaseDir(String databaseDir) {
        this.databaseDir = databaseDir;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(this.databaseDir);
        return hash;
    }

    protected boolean equalsTransitive(PostgresqlAutoPersistenceStorageConf other) {
        if(!super.equalsTransitive(other)) {
            return false;
        }
        if (!Objects.equals(this.databaseDir, other.databaseDir)) {
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
        final PostgresqlAutoPersistenceStorageConf other = (PostgresqlAutoPersistenceStorageConf) obj;
        return equalsTransitive(other);
    }
}
