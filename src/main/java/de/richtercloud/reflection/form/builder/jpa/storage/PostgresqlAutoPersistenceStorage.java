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

import de.richtercloud.message.handler.ExceptionMessage;
import de.richtercloud.message.handler.IssueHandler;
import de.richtercloud.reflection.form.builder.jpa.sequence.PostgresqlSequenceManager;
import de.richtercloud.reflection.form.builder.jpa.sequence.SequenceManager;
import de.richtercloud.reflection.form.builder.storage.StorageConfValidationException;
import de.richtercloud.reflection.form.builder.storage.StorageCreationException;
import de.richtercloud.validation.tools.FieldRetriever;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages start of a PostgreSQL instance with system processes.
 *
 * Working with the build-in Java Process API is painful because it doesn't
 * guarantee that a certain signal is send. Postgres 9.6 has 3 different
 * shutdown modes (see
 * <a href="https://www.postgresql.org/docs/9.6/static/server-shutdown.html">PostgreSQL documentation</a>
 * for details) of which the "smart" one might wait for connections to be closed
 * on the client side. That means that any issue in JPA provider or JDBC driver
 * implementation with connections remaining open causes a process deadlock
 * which one can work around by specifying a timeout to wait for the process,
 * but that's a last resort. This last resort doesn't have to be used if the
 * {@code pg_ctl} binary is used which has a {@code stop} subcommand which
 * allows to choose the shutdown mode (see <a href="https://www.postgresql.org/docs/9.6/static/app-pg-ctl.html">PostgreSQL documentation</a>
 * for details).
 *
 * @author richter
 */
public class PostgresqlAutoPersistenceStorage extends AbstractProcessPersistenceStorage<PostgresqlAutoPersistenceStorageConf> {
    private static final long serialVersionUID = 1L;
    private final static Logger LOGGER = LoggerFactory.getLogger(PostgresqlAutoPersistenceStorage.class);

    public PostgresqlAutoPersistenceStorage(PostgresqlAutoPersistenceStorageConf storageConf,
            String persistenceUnitName,
            int parallelQueryCount,
            FieldRetriever fieldRetriever,
            IssueHandler issueHandler) throws IOException, InterruptedException, StorageConfValidationException, StorageCreationException {
        super(storageConf,
                persistenceUnitName,
                parallelQueryCount,
                fieldRetriever,
                issueHandler,
                String.format("PostgreSQL server at %s:%d",
                        storageConf.getHostname(),
                        storageConf.getPort()));
    }

    @Override
    protected SequenceManager<Long> createSequenceManager() {
        return new PostgresqlSequenceManager(this);
    }

    @Override
    protected void preCreation() throws IOException {
        //do nothing
    }

    @Override
    protected boolean needToCreate() {
        return !new File(getStorageConf().getDatabaseDir()).exists();
    }

    @Override
    protected void createDatabase() throws IOException, StorageCreationException, InterruptedException {
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
    }

    @Override
    protected Process createProcess() throws IOException {
        //postgres process (necessary for both createdb to work and for the
        //storage to run (in case createdb isn't necessary)
        ProcessBuilder postgresProcessBuilder = new ProcessBuilder(getStorageConf().getPostgresBinaryPath(),
                "-D", getStorageConf().getDatabaseDir(),
                "-h", getStorageConf().getHostname(),
                "-p", String.valueOf(getStorageConf().getPort()));
        LOGGER.debug(String.format("running command '%s'", postgresProcessBuilder.command().toString()));
        return postgresProcessBuilder.start();
            //Process.destroy causes process stdout and stderr to be
            //closed which is very unfortunate and can be seen as a JDK
            //design error -> read from stdout and stderr in separate
            //threads
    }

    @Override
    protected void setupDatabase() throws IOException,
            StorageCreationException,
            InterruptedException {
        try {
            //createdb @TODO: check whether database exists rather than
            //just assume that it needs to be created if the directory
            //doesn't exist
            waitForServerToBeUp(() -> {
                ProcessBuilder createdbProcessBuilder = new ProcessBuilder(getStorageConf().getCreatedbBinaryPath(),
                        "-h", getStorageConf().getHostname(),
                        //using the short options improves escaping of user
                        //and database names dramatically
                        "-p", String.valueOf(getStorageConf().getPort()),
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
                    return new Exception(String.format("createdb process returned with non-zero code %d",
                            createdbProcess.exitValue()));
                }
                LOGGER.debug("createdb succeeded");
                return null;
            },
                    "PostgreSQL's createdb command");
        } catch (ServerStartTimeoutException ex) {
            shutdown();
            throw new StorageCreationException(ex);
        }
    }

    @Override
    protected void shutdown0() {
        if(getProcess() != null) {
            try {
                Process pgCtlProcess = new ProcessBuilder(getStorageConf().getPgCtlBinaryPath(),
                        "stop",
                        "-D", getStorageConf().getDatabaseDir())
                        .start();
                pgCtlProcess.waitFor();
            } catch (InterruptedException | IOException ex) {
                LOGGER.error("unexpected exception during waiting for pg_ctl process",
                        ex);
                getIssueHandler().handleUnexpectedException(new ExceptionMessage(ex));
            }
            //Joining stdout and stderr output threads before waiting for
            //postgres process to terminate causes waiting forever, unclear why
            LOGGER.info("waiting for postgres process to terminate");
            try {
                getProcessThread().join();
                LOGGER.info("postgres process returned expectedly");
            } catch (InterruptedException ex) {
                LOGGER.error("unexpected exception during joining of process watch thread",
                        ex);
                getIssueHandler().handleUnexpectedException(new ExceptionMessage(ex));
            }
        }
        if(getProcessStdoutReaderThread() != null) {
            try {
                LOGGER.info("waiting for postgres process stdout reader thread to terminate");
                getProcessStdoutReaderThread().join();
                LOGGER.debug("postgres stdout reader thread joined");
            }catch(InterruptedException ex) {
                LOGGER.error("unexpected exception during joining of process stdout reader thread",
                        ex);
                getIssueHandler().handleUnexpectedException(new ExceptionMessage(ex));
            }
        }
        if(getProcessStderrReaderThread() != null) {
            try {
                LOGGER.info("waiting for postgres process stderr reader thread to terminate");
                getProcessStderrReaderThread().join();
                LOGGER.debug("postgres stderr reader thread joined");
                //should handle writing to stdout and stderr
            } catch (InterruptedException ex) {
                LOGGER.error("unexpected exception during joining of process stderr reader thread",
                        ex);
                getIssueHandler().handleUnexpectedException(new ExceptionMessage(ex));
            }
        }
    }
}
