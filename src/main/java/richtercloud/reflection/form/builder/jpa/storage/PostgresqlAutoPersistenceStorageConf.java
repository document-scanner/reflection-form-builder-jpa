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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import richtercloud.reflection.form.builder.storage.StorageConfValidationException;

/**
 *
 * @author richter
 */
public class PostgresqlAutoPersistenceStorageConf extends PostgresqlPersistenceStorageConf {
    private static final long serialVersionUID = 1L;

    /**
     * Searches {@code /usr/lib/postgresql} which is the typical PostgreSQL
     * installation location on Debian-based systems for verison directories
     * containing {@code initdb} and {@code postgres} binaries. Chooses the
     * highest version. Returns {@code null} if no installation is found which
     * should trigger the storage configuration dialog to enforce a value being
     * set by the user when the configuration is chosen.
     *
     * @return the pathes to the found {@code initdb} and {@code postgres}binary
     * or {@code null} if none are found
     */
    public static Pair<String, String> findBestInitialPostgresqlBasePath() {
        File postgresqlDir = new File("/usr/lib/postgresql");
        if(!postgresqlDir.exists()) {
            return null;
        }
        File highestVersionDir = null;
        int versionMajorMax = -1;
        int versionMinorMax = -1;
        for(File postgresqlVersionDir : postgresqlDir.listFiles()) {
            if(!postgresqlVersionDir.getName().matches("[0-9]\\.[0-9]")) {
                continue;
            }
            if(!new File(postgresqlVersionDir, String.join(File.separator, "bin", "initdb")).exists()) {
                continue;
            }
            if(!new File(postgresqlVersionDir, String.join(File.separator, "bin", "postgres")).exists()) {
                continue;
            }
            String[] versionSplit = postgresqlVersionDir.getName().split("\\.");
            assert versionSplit.length == 2;
            int versionMajor = Integer.valueOf(versionSplit[0]);
            int versionMinor = Integer.valueOf(versionSplit[1]);
            if(versionMajor > versionMajorMax) {
                highestVersionDir = postgresqlVersionDir;
            }else if(versionMajor == versionMajorMax) {
                if(versionMinor > versionMinorMax) {
                    highestVersionDir = postgresqlVersionDir;
                }
            }
        }
        if(highestVersionDir == null) {
            return null;
        }
        Pair<String, String> retValue = new ImmutablePair<>(new File(highestVersionDir, String.join(File.separator, "bin", "initdb")).getAbsolutePath(),
                new File(highestVersionDir, String.join(File.separator, "bin", "postgres")).getAbsolutePath());
        return retValue;
    }

    /**
     * If a PostgreSQL server is started we need both a database directory and
     * name.
     */
    private String databaseDir;
    private String initdbBinaryPath;
    private String postgresBinaryPath;


    public PostgresqlAutoPersistenceStorageConf(Set<Class<?>> entityClasses,
            String username,
            File schemeChecksumFile,
            String databaseDir,
            String initdbBinaryPath,
            String postgresBinaryPath) throws FileNotFoundException, IOException {
        super(entityClasses,
                username,
                schemeChecksumFile);
        this.databaseDir = databaseDir;
        this.initdbBinaryPath = initdbBinaryPath;
        this.postgresBinaryPath = postgresBinaryPath;
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
            File schemeChecksumFile,
            String initdbBinaryPath,
            String postgresBinaryPath) throws FileNotFoundException, IOException {
        super(port,
                databaseDriver,
                entityClasses,
                username,
                password,
                databaseName,
                schemeChecksumFile);
        this.databaseDir = databaseDir;
        this.initdbBinaryPath = initdbBinaryPath;
        this.postgresBinaryPath = postgresBinaryPath;
    }

    public String getDatabaseDir() {
        return databaseDir;
    }

    public void setDatabaseDir(String databaseDir) {
        this.databaseDir = databaseDir;
    }

    public String getInitdbBinaryPath() {
        return initdbBinaryPath;
    }

    public void setInitdbBinaryPath(String initdbBinaryPath) {
        this.initdbBinaryPath = initdbBinaryPath;
    }

    public String getPostgresBinaryPath() {
        return postgresBinaryPath;
    }

    public void setPostgresBinaryPath(String postgresBinaryPath) {
        this.postgresBinaryPath = postgresBinaryPath;
    }

    /**
     * Requires {@code postgresBinaryPath} and {@code initdbBinaryPath} to be
     * not {@code null} and an existing file.
     *
     * @throws StorageConfValidationException if one of the above conditions
     * isn't met (with a message pointing to the specific condition)
     */
    @Override
    public void validate() throws StorageConfValidationException {
        super.validate();
        if(this.postgresBinaryPath == null) {
            throw new StorageConfValidationException("postgres binary path is null");
        }
        if(this.initdbBinaryPath == null) {
            throw new StorageConfValidationException("initdb binary path is null");
        }
        if(!new File(this.postgresBinaryPath).exists()) {
            throw new StorageConfValidationException("postgres binary path points to an inexisting location");
        }
        if(!new File(this.initdbBinaryPath).exists()) {
            throw new StorageConfValidationException("initdb binary path points to an inexisting location");
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(this.databaseDir);
        hash = 67 * hash + Objects.hashCode(this.initdbBinaryPath);
        hash = 67 * hash + Objects.hashCode(this.postgresBinaryPath);
        return hash;
    }

    protected boolean equalsTransitive(PostgresqlAutoPersistenceStorageConf other) {
        if(!super.equalsTransitive(other)) {
            return false;
        }
        if (!Objects.equals(this.databaseDir, other.databaseDir)) {
            return false;
        }
        if(!Objects.equals(this.initdbBinaryPath, other.initdbBinaryPath)) {
            return false;
        }
        if(!Objects.equals(this.postgresBinaryPath, other.postgresBinaryPath)) {
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
