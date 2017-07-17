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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.jpa.sequence.PostgresqlSequenceManager;
import richtercloud.reflection.form.builder.jpa.sequence.SequenceManagementException;
import richtercloud.reflection.form.builder.jpa.sequence.SequenceManager;
import richtercloud.reflection.form.builder.storage.StorageConfValidationException;
import richtercloud.reflection.form.builder.storage.StorageCreationException;
import richtercloud.validation.tools.FieldRetriever;

/**
 * Manages start of a PostgreSQL instance with system processes.
 *
 * @author richter
 */
public class PostgresqlAutoPersistenceStorage extends AbstractProcessPersistenceStorage<PostgresqlAutoPersistenceStorageConf> {
    private static final long serialVersionUID = 1L;
    private final static Logger LOGGER = LoggerFactory.getLogger(PostgresqlAutoPersistenceStorage.class);
    private Process postgresProcess;
    private Thread postgresThread;
    private final SequenceManager<Long> sequenceManager;
    /**
     * The maximum wait in ms until the server is up when initializing the
     * server.
     */
    private int waitServerUpMaxMillis = 10000;
    /**
     * The wait time between checks whether the server is up in ms.
     */
    private int waitServerUpIntervalMillis = 1000;

    public PostgresqlAutoPersistenceStorage(PostgresqlAutoPersistenceStorageConf storageConf,
            String persistenceUnitName,
            int parallelQueryCount,
            FieldRetriever fieldRetriever) throws IOException, InterruptedException, StorageConfValidationException, StorageCreationException {
        super(storageConf,
                persistenceUnitName,
                parallelQueryCount,
                fieldRetriever);
        this.sequenceManager = new PostgresqlSequenceManager(this);
    }

    @Override
    protected void init() throws StorageCreationException {
        try {
            String createdb = "createdb";
            boolean needToCreate = !new File(getStorageConf().getDatabaseDir()).exists();
            if(needToCreate) {
                File passwordFile = File.createTempFile("image-storage-it-postgres", "suffix");
                Files.write(Paths.get(passwordFile.getAbsolutePath()),
                        getStorageConf().getPassword().getBytes(),
                        StandardOpenOption.WRITE);
                ProcessBuilder initdbProcessBuilder = new ProcessBuilder(getStorageConf().getInitdbBinaryPath(),
                        String.format("--username=%s", getStorageConf().getUsername()),
                        String.format("--pwfile=%s", passwordFile.getAbsolutePath()),
                        getStorageConf().getDatabaseDir());
                Process initdbProcess = initdbProcessBuilder.start();
                initdbProcess.waitFor();
                passwordFile.delete(); //@TODO: insecure handling of password
                //fix `FATAL:  could not create lock file "/var/run/postgresql/.s.PGSQL.5432.lock": Keine Berechtigung`
                File postgresqlConfFile = new File(getStorageConf().getDatabaseDir(), "postgresql.conf");
                try {
                    Files.write(Paths.get(postgresqlConfFile.getAbsolutePath()), "\nunix_socket_directories = '/tmp'\n".getBytes(), StandardOpenOption.APPEND);
                }catch (IOException ex) {
                    LOGGER.error(String.format("unexpected exception during writing to PostgreSQL configuration file '%s', see nested exception for details", postgresqlConfFile.getAbsolutePath()), ex);
                }
                IOUtils.copy(initdbProcess.getInputStream(), System.out);
                IOUtils.copy(initdbProcess.getErrorStream(), System.err);
            }else {
                LOGGER.info(String.format("database directory '%s' exists, expecting valid PostgreSQL data directory which is used", getStorageConf().getDatabaseDir()));
            }
            ProcessBuilder postgresProcessBuilder = new ProcessBuilder(getStorageConf().getPostgresBinaryPath(),
                    "-D", getStorageConf().getDatabaseDir(),
                    "-h", getStorageConf().getHostname(),
                    "-p", String.valueOf(getStorageConf().getPort()));
            LOGGER.debug(String.format("running command '%s'", postgresProcessBuilder.command().toString()));
            postgresProcess = postgresProcessBuilder.start();
            postgresThread = new Thread(() -> {
                try {
                    postgresProcess.waitFor();
                    LOGGER.warn("Process 'postgres' returned. This shouldn't happen unless the JVM is shutting down.");
                    IOUtils.copy(postgresProcess.getInputStream(), System.out);
                    IOUtils.copy(postgresProcess.getErrorStream(), System.err);
                } catch (InterruptedException | IOException ex) {
                    LOGGER.error("unexpected exception, see nested exception for details", ex);
                }
            },
                    "postgres-thread");
            postgresThread.start();
            if(needToCreate) {
                //createdb @TODO: check whether database exists rather than
                //just assume that it needs to be created if the directory
                //doesn't exist
                boolean success = false;
                int waitServerUpMillis = 0;
                while(!success) {
                    if(waitServerUpMillis > waitServerUpMaxMillis) {
                        throw new StorageCreationException("createdb process "
                                + "failed (see preceeding process output for "
                                + "details and reasons)");
                    }
                    ProcessBuilder createdbProcessBuilder = new ProcessBuilder(createdb,
                            String.format("--host=%s", getStorageConf().getHostname()),
                            String.format("--username=%s", getStorageConf().getUsername()),
                            getStorageConf().getDatabaseName());
                    LOGGER.debug(String.format("running command '%s'", createdbProcessBuilder.command().toString()));
                    Process createdbProcess = createdbProcessBuilder.start();
                    createdbProcess.waitFor();
                    IOUtils.copy(createdbProcess.getInputStream(), System.out);
                    IOUtils.copy(createdbProcess.getErrorStream(), System.err);
                    if(createdbProcess.exitValue() == 0) {
                        LOGGER.debug("createdb succeeded");
                        success = true;
                    }else {
                        waitServerUpMillis += waitServerUpIntervalMillis;
                        if(waitServerUpMillis < waitServerUpMaxMillis) {
                            LOGGER.debug(String.format("createdb failed "
                                    + "(server might not be up yet, next check "
                                    + "in %d ms",
                                    waitServerUpIntervalMillis));
                            Thread.sleep(waitServerUpIntervalMillis);
                        }else {
                            LOGGER.warn(String.format("createdb failed all "
                                    + "connection attempts, aborting in order "
                                    + "to avoid to wait for ever (consider "
                                    + "adusting waitServerUpMaxMillis (was "
                                    + "%d))",
                                    waitServerUpMaxMillis));
                        }
                    }
                }
            }
            setServerRunning(true);
        }catch (IOException | InterruptedException ex) {
            throw new StorageCreationException(ex);
        }
    }

    @Override
    protected void shutdown0() {
        getShutdownLock().lock();
        if(!isServerRunning()) {
            getShutdownLock().unlock();
            return;
        }
        try {
            postgresProcess.destroy();
            try {
                LOGGER.info("waiting for postgres process to terminate");
                postgresProcess.waitFor();
            } catch (InterruptedException ex) {
                LOGGER.error("waiting for termination of postgres process failed, see nested exception for details", ex);
            }
            try {
                LOGGER.info("waiting for postgres process watch thread to terminate");
                postgresThread.join();
                //should handle writing to stdout and stderr
            } catch (InterruptedException ex) {
                LOGGER.error("unexpected exception, see nested exception for details", ex);
            }
        }finally{
            getShutdownLock().unlock();
        }
    }

    @Override
    public boolean checkSequenceExists(String sequenceName) throws SequenceManagementException {
        return this.sequenceManager.checkSequenceExists(sequenceName);
    }

    @Override
    public void createSequence(String sequenceName) throws SequenceManagementException {
        this.sequenceManager.createSequence(sequenceName);
    }

    @Override
    public Long getNextSequenceValue(String sequenceName) throws SequenceManagementException {
        return this.sequenceManager.getNextSequenceValue(sequenceName);
    }
}
