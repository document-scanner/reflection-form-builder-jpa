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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
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
    private Thread postgresStdoutThread;
    private Thread postgresStderrThread;
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
            //check whether port is free
            try (Socket testSocket = new Socket(getStorageConf().getHostname(),
                            getStorageConf().getPort())) {
                throw new StorageCreationException(String.format("A service is "
                        + "already listening at the address %s on port %d",
                        getStorageConf().getHostname(),
                        getStorageConf().getPort()));
            }catch(IOException ex) {
                //expected if port is free
            }
            //initdb
            boolean needToCreate = !new File(getStorageConf().getDatabaseDir()).exists();
            if(needToCreate) {
                File passwordFile = File.createTempFile("image-storage-it-postgres", "suffix");
                Files.write(Paths.get(passwordFile.getAbsolutePath()),
                        getStorageConf().getPassword().getBytes(),
                        StandardOpenOption.WRITE);
                ProcessBuilder initdbProcessBuilder = new ProcessBuilder(getStorageConf().getInitdbBinaryPath(),
                        "-U", getStorageConf().getUsername(),
                            //using the short options improves escaping of user
                            //and database names dramatically
                        String.format("--pwfile=%s", passwordFile.getAbsolutePath()),
                        getStorageConf().getDatabaseDir())
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT);
                LOGGER.debug(String.format("running command '%s'", initdbProcessBuilder.command().toString()));
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
            }else {
                LOGGER.info(String.format("database directory '%s' exists, expecting valid PostgreSQL data directory which is used", getStorageConf().getDatabaseDir()));
            }
            ProcessBuilder postgresProcessBuilder = new ProcessBuilder(getStorageConf().getPostgresBinaryPath(),
                    "-D", getStorageConf().getDatabaseDir(),
                    "-h", getStorageConf().getHostname(),
                    "-p", String.valueOf(getStorageConf().getPort()));
            LOGGER.debug(String.format("running command '%s'", postgresProcessBuilder.command().toString()));
            postgresProcess = postgresProcessBuilder.start();
            //Process.destroy causes process stdout and stderr to be
            //closed which is very unfortunate and can be seen as a JDK
            //design error -> read from stdout and stderr in separate
            //threads
            postgresStdoutThread = new Thread(() -> {
                try {
                    BufferedReader postgresProcessStdoutReader = new BufferedReader(new InputStreamReader(postgresProcess.getInputStream()));
                    while(postgresProcessStdoutReader.ready()) {
                        String line = postgresProcessStdoutReader.readLine();
                        if(line == null) {
                            break;
                        }
                        LOGGER.info(String.format("[PostgreSQL stdout] %s",
                                line));
                    }
                    LOGGER.debug("postgres process stdout stream reader thread terminated");
                } catch (IOException ex) {
                    LOGGER.error("unexpected exception, see nested exception for details", ex);
                }
            },
                    "postgres-stdout-thread");
            postgresStderrThread = new Thread(() -> {
                try {
                    BufferedReader postgresProcessStderrReader = new BufferedReader(new InputStreamReader(postgresProcess.getErrorStream()));
                    while(postgresProcessStderrReader.ready()) {
                        String line = postgresProcessStderrReader.readLine();
                        if(line == null) {
                            break;
                        }
                        LOGGER.info(String.format("[PostgreSQL stderr] %s",
                                line));
                    }
                    LOGGER.debug("postgres process stderr stream reader thread terminated");
                } catch (IOException ex) {
                    LOGGER.error("unexpected exception, see nested exception for details", ex);
                }
            },
                    "postgres-stderr-thread");
            postgresStdoutThread.start();
            postgresStderrThread.start();
            //createdb needs the server to be up and running
            if(needToCreate) {
                //createdb @TODO: check whether database exists rather than
                //just assume that it needs to be created if the directory
                //doesn't exist
                waitForServerToBeUp(() -> {
                    ProcessBuilder createdbProcessBuilder = new ProcessBuilder(getStorageConf().getCreatedbBinaryPath(),
                            "-h", getStorageConf().getHostname(),
                                //using the short options improves escaping of user
                                //and database names dramatically
                            "-U", getStorageConf().getUsername(),
                            getStorageConf().getDatabaseName())
                            .redirectError(ProcessBuilder.Redirect.INHERIT)
                            .redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    LOGGER.debug(String.format("running command '%s'", createdbProcessBuilder.command().toString()));
                    Process createdbProcess = createdbProcessBuilder.start();
                    createdbProcess.waitFor();
                    if(createdbProcess.exitValue() != 0) {
                        LOGGER.debug(String.format("createdb failed with return code %s",
                                createdbProcess.exitValue()));
                        return false;
                    }
                    LOGGER.debug("createdb succeeded");
                    return true;
                });
            }
            //can only wait for server to be up and running after createdb
            waitForServerToBeUp(() -> {
                try {
                    Properties connectionProps = new Properties();
                    connectionProps.put("user", getStorageConf().getUsername());
                    connectionProps.put("password", getStorageConf().getPassword());
                    DriverManager.getConnection(getStorageConf().getConnectionURL(),
                            connectionProps);
                    return true;
                } catch (SQLException ex) {
                    return false;
                }
            });
            setServerRunning(true);
        }catch (IOException | InterruptedException ex) {
            throw new StorageCreationException(ex);
        }
    }


    /**
     * Since {@code createdb} needs to be performed on a running server, but not
     * no JDBC test connection can be obtained before {@code createdb} has been
     * run the wait routine is needed at two place inside {@link #init() }.
     *
     * @throws StorageCreationException if the maximum of connection attempts
     * has been exceeded
     * @throws InterruptedException if it occurs in {@link Thread#sleep(long) }
     */
    private void waitForServerToBeUp(WaitForServerToBeUpLambda waitForServerToBeUpLambda) throws StorageCreationException,
            InterruptedException,
            IOException {
        int waitServerUpMillis = 0;
        while(true) {
            if(waitServerUpMillis > waitServerUpMaxMillis) {
                throw new StorageCreationException("waiting for the "
                        + "PostgreSQL server to start up timed out and has "
                        + "been tried too many times (see preceeding "
                        + "process output for details and reasons)");
            }
            if(waitForServerToBeUpLambda.run()) {
                break;
            }
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
                LOGGER.info("waiting for postgres process reader thread to terminate");
                postgresStdoutThread.join();
                LOGGER.debug("postgres stdout reader thread joined");
                postgresStderrThread.join();
                LOGGER.debug("postgres stderr reader thread joined");
                //should handle writing to stdout and stderr
            } catch (InterruptedException ex) {
                LOGGER.error("unexpected exception, see nested exception for details", ex);
            }
            LOGGER.info("waiting for postgres process to terminate");
            //postgresProcess.waitFor(); blocks in integration tests which is
            //a common phenomenon of the very simple Java Process API although
            //only explained through remaining output in stdout or stderr; the
            //following doesn't block
            boolean returned = false;
            while(!returned) {
                try {
                    postgresProcess.exitValue();
                        //throws IllegalThreadStateException is the process
                        //hasn't terminated yet
                    returned = true;
                }catch(IllegalThreadStateException ex) {
                    //continue
                }
            }
            LOGGER.info("postgres process returned");
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
