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

import de.richtercloud.execution.tools.OutputReaderThread;
import de.richtercloud.execution.tools.OutputReaderThreadMode;
import de.richtercloud.jhbuild.java.wrapper.BinaryUtils;
import de.richtercloud.jhbuild.java.wrapper.BinaryValidationException;
import de.richtercloud.reflection.form.builder.storage.StorageConfValidationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richter
 */
public class PostgresqlAutoPersistenceStorageConf extends PostgresqlPersistenceStorageConf {
    private static final long serialVersionUID = 1L;
    private final static Logger LOGGER = LoggerFactory.getLogger(PostgresqlAutoPersistenceStorageConf.class);
    private final static String BIN_TEMPLATE = "bin";
    /**
     * If a PostgreSQL server is started we need both a database directory and
     * name.
     */
    private String databaseDir;
    private String initdbBinaryPath;
    private String postgresBinaryPath;
    private String createdbBinaryPath;
    private String pgCtlBinaryPath;

    /**
     * Searches {@code /usr/lib/postgresql} which is the typical PostgreSQL
     * installation location on Debian-based systems for verison directories
     * containing {@code initdb} and {@code postgres} binaries. Chooses the
     * highest version. Returns {@code null} if no installation is found which
     * should trigger the storage configuration dialog to enforce a value being
     * set by the user when the configuration is chosen.
     *
     * @return the pathes to the found {@code initdb} and {@code postgres}binary
     *     or {@code null} if none are found
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public static Triple<String, String, String> findBestInitialPostgresqlBasePath() {
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
            if(!new File(postgresqlVersionDir, String.join(File.separator, BIN_TEMPLATE, "initdb")).exists()) {
                continue;
            }
            if(!new File(postgresqlVersionDir, String.join(File.separator, BIN_TEMPLATE, "postgres")).exists()) {
                continue;
            }
            String[] versionSplit = postgresqlVersionDir.getName().split("\\.");
            assert versionSplit.length == 2;
            int versionMajor = Integer.valueOf(versionSplit[0]);
            int versionMinor = Integer.valueOf(versionSplit[1]);
            if(versionMajor > versionMajorMax) {
                highestVersionDir = postgresqlVersionDir;
            }else if(versionMajor == versionMajorMax && versionMinor > versionMinorMax) {
                highestVersionDir = postgresqlVersionDir;
            }
        }
        if(highestVersionDir == null) {
            return null;
        }
        Triple<String, String, String> retValue = new ImmutableTriple<>(new File(highestVersionDir, String.join(File.separator, BIN_TEMPLATE, "initdb")).getAbsolutePath(),
                new File(highestVersionDir, String.join(File.separator, BIN_TEMPLATE, "postgres")).getAbsolutePath(),
                new File(highestVersionDir, String.join(File.separator, BIN_TEMPLATE, "pg_ctl")).getAbsolutePath());
        return retValue;
    }

    public PostgresqlAutoPersistenceStorageConf(Set<Class<?>> entityClasses,
            String hostname,
            String username,
            String password,
            String databaseName,
            File schemeChecksumFile,
            String databaseDir,
            String initdbBinaryPath,
            String postgresBinaryPath,
            String createdbBinaryPath,
            String pgCtlBinaryPath) throws FileNotFoundException, IOException {
        this(entityClasses,
                hostname,
                username,
                password,
                databaseName,
                schemeChecksumFile,
                databaseDir,
                initdbBinaryPath,
                postgresBinaryPath,
                createdbBinaryPath,
                pgCtlBinaryPath,
                PostgresqlPersistenceStorageConf.PORT_DEFAULT);
    }

    public PostgresqlAutoPersistenceStorageConf(Set<Class<?>> entityClasses,
            String hostname,
            String username,
            String password,
            String databaseName,
            File schemeChecksumFile,
            String databaseDir,
            String initdbBinaryPath,
            String postgresBinaryPath,
            String createdbBinaryPath,
            String pgCtlBinaryPath,
            int port) throws FileNotFoundException, IOException {
        this(entityClasses,
                hostname,
                username,
                password,
                databaseName,
                schemeChecksumFile,
                databaseDir,
                initdbBinaryPath,
                postgresBinaryPath,
                createdbBinaryPath,
                pgCtlBinaryPath,
                port,
                PostgresqlPersistenceStorageConf.DATABASE_DRIVER_DEFAULT);
    }

    /**
     * Copy constructor.
     * @param databaseDir the database directory
     * @param hostname the base directory
     * @param port the port
     * @param databaseDriver the database driver
     * @param entityClasses the entity classes to manage
     * @param username the username
     * @param password the password
     * @param initdbBinaryPath the path to the initdb binary
     * @param postgresBinaryPath the path to the postgres binary
     * @param createdbBinaryPath the path to the createdb binary
     * @param pgCtlBinaryPath the path to the pg_ctl binary
     * @param databaseName the database name
     * @param schemeChecksumFile the scheme checksum file
     * @throws FileNotFoundException in case the scheme checksum file can't be
     *     found
     * @throws IOException in case an I/O exception occurs while reading from or
     *     writing to the scheme checksum file
     */
    public PostgresqlAutoPersistenceStorageConf(Set<Class<?>> entityClasses,
            String hostname,
            String username,
            String password,
            String databaseName,
            File schemeChecksumFile,
            String databaseDir,
            String initdbBinaryPath,
            String postgresBinaryPath,
            String createdbBinaryPath,
            String pgCtlBinaryPath,
            int port,
            String databaseDriver) throws FileNotFoundException, IOException {
        super(entityClasses,
                hostname,
                username,
                password,
                databaseName,
                schemeChecksumFile,
                port,
                databaseDriver);
        this.databaseDir = databaseDir;
        this.initdbBinaryPath = initdbBinaryPath;
        this.postgresBinaryPath = postgresBinaryPath;
        this.createdbBinaryPath = createdbBinaryPath;
        this.pgCtlBinaryPath = pgCtlBinaryPath;
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

    public String getCreatedbBinaryPath() {
        return createdbBinaryPath;
    }

    public void setCreatedbBinaryPath(String createdbBinaryPath) {
        this.createdbBinaryPath = createdbBinaryPath;
    }

    public String getPgCtlBinaryPath() {
        return pgCtlBinaryPath;
    }

    public void setPgCtlBinaryPath(String pgCtlBinaryPath) {
        this.pgCtlBinaryPath = pgCtlBinaryPath;
    }

    /**
     * Requires {@code postgresBinaryPath} and {@code initdbBinaryPath} to be
     * not {@code null} and an existing file.
     *
     * @throws StorageConfValidationException if one of the above conditions
     *     isn't met (with a message pointing to the specific condition)
     */
    @Override
    public void validate() throws StorageConfValidationException {
        super.validate();
        //validate binaries
        for(Pair<String, String> binaryNamePair : Arrays.asList(new ImmutablePair<>(postgresBinaryPath, "postgres"),
            new ImmutablePair<>(initdbBinaryPath, "initdb"),
            new ImmutablePair<>(createdbBinaryPath, "createdb"),
            new ImmutablePair<>(pgCtlBinaryPath, "pg_ctl"))) {
            try {
                BinaryUtils.validateBinary(binaryNamePair.getKey(),
                        binaryNamePair.getValue());
            }catch(BinaryValidationException ex) {
                throw new StorageConfValidationException(ex);
            }
        }
        try {
            //validate version
            Process postgresProcess = new ProcessBuilder(postgresBinaryPath, "--version")
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            OutputReaderThread postgresProcessStdoutThread = new OutputReaderThread(postgresProcess.getInputStream(),
                    OutputReaderThreadMode.PIPED);
            postgresProcessStdoutThread.start();
            postgresProcess.waitFor();
            postgresProcessStdoutThread.join();
            String postgresProcessStdout = IOUtils.toString(postgresProcessStdoutThread.getProcessOutputStream()).trim();
            if(postgresProcess.exitValue() != 0) {
                throw new StorageConfValidationException(String.format(
                        "running %s --version during configuration validation "
                        + "failed with return code %d (stdout was '%s' and "
                        + "stderr was redirected to the JVMs output",
                        postgresBinaryPath,
                        postgresProcess.exitValue(),
                        postgresProcessStdout));
            }
            LOGGER.trace(String.format("postgres version process output: %s",
                    postgresProcessStdout));
            Matcher versionMatcher = Pattern.compile(".*?(?<version>(\\d)+\\.(\\d)+(\\.(\\d)+)?).*").matcher(postgresProcessStdout);
                //- should parse both versions like 9.6.3 and 10.5
                //- need to make `.*` to be reluctant, see
                //https://stackoverflow.com/questions/5319840/greedy-vs-reluctant-vs-possessive-quantifiers for an
                //explanation
            if(!versionMatcher.find()) {
                throw new StorageConfValidationException(String.format(
                        "postgres process version output '%s' couldn't be parsed",
                        postgresProcessStdout));
            }
            String version = versionMatcher.group("version");
            LOGGER.trace(String.format("version: %s",
                    version));
            String[] versionSplit = version.split("\\.");
            if(Integer.valueOf(versionSplit[0]) < 9) {
                throw new StorageConfValidationException(String.format(
                        "postgres binary '%s' has a version %s < 9.x which "
                        + "isn't supported",
                        postgresBinaryPath,
                        version));
            }
            if(Integer.valueOf(versionSplit[1]) <= 2) {
                //9.2 doesn't work, 9.6 does, others not tested
                throw new StorageConfValidationException(String.format(
                        "postgres binary '%s' has a version %s < 9.2.x which "
                        + "isn't supported",
                        postgresBinaryPath,
                        version));
            }
        } catch (IOException | InterruptedException ex) {
            throw new StorageConfValidationException(ex);
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
        return Objects.equals(this.postgresBinaryPath, other.postgresBinaryPath);
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
