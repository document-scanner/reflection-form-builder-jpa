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

import java.io.IOException;
import java.net.Socket;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.jhbuild.java.wrapper.OutputReaderThread;
import richtercloud.message.handler.ExceptionMessage;
import richtercloud.message.handler.IssueHandler;
import richtercloud.message.handler.Message;
import richtercloud.reflection.form.builder.jpa.sequence.SequenceManagementException;
import richtercloud.reflection.form.builder.jpa.sequence.SequenceManager;
import richtercloud.reflection.form.builder.storage.StorageConfValidationException;
import richtercloud.reflection.form.builder.storage.StorageCreationException;
import richtercloud.validation.tools.FieldRetriever;

/**
 *
 * @author richter
 */
/*
internal implementation notes:
- Uses AbstractNetworkPersistenceStorageConf rather than
AbstractPersistenceStorageConf since no database which is managed as external
process and is not communicating via network comes to mind.
- In order to properly wait for a process to terminate, it's necessary to read
its stdout and stderr output completely. This has to happen on another thread.
*/
public abstract class AbstractProcessPersistenceStorage<C extends AbstractNetworkPersistenceStorageConf> extends AbstractPersistenceStorage<C> {
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractProcessPersistenceStorage.class);
    /**
     * Used to prevent messing up shutdown routine(s) when run by more
     * than one thread and to check whether shutdown has been requested after
     * the process returned.
     */
    private final Lock shutdownLock = new ReentrantLock();
    /**
     * Whether or not the server is running.
     */
    private boolean serverRunning;
    private Thread processThread;
    private Process process;
    private OutputReaderThread processStdoutReaderThread;
    private OutputReaderThread processStderrReaderThread;
    private final IssueHandler issueHandler;
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
    /**
     * A short description used in success or failure notifications, like
     * '... process crashed' where ... should be MySQL server or PostgreSQL
     * management application or something similar.
     */
    private final String shortDescription;

    public AbstractProcessPersistenceStorage(C storageConf,
            String persistenceUnitName,
            int parallelQueryCount,
            FieldRetriever fieldRetriever,
            IssueHandler issueHandler,
            String shortDescription) throws StorageConfValidationException, StorageCreationException {
        super(storageConf,
                persistenceUnitName,
                parallelQueryCount,
                fieldRetriever);
        this.issueHandler = issueHandler;
        this.shortDescription = shortDescription;
        this.sequenceManager = createSequenceManager();
    }

    /**
     * Allows to enforce the creation of a {@link SequenceManager} in
     * constructor with a reference to {@code this}.
     *
     * @return the {@link SequenceManager} for this storage instance
     */
    protected abstract SequenceManager<Long> createSequenceManager();

    public Lock getShutdownLock() {
        return shutdownLock;
    }

    public boolean isServerRunning() {
        return serverRunning;
    }

    public void setServerRunning(boolean serverRunning) {
        this.serverRunning = serverRunning;
    }

    public Process getProcess() {
        return process;
    }

    public Thread getProcessThread() {
        return processThread;
    }

    public OutputReaderThread getProcessStdoutReaderThread() {
        return processStdoutReaderThread;
    }

    public OutputReaderThread getProcessStderrReaderThread() {
        return processStderrReaderThread;
    }

    public IssueHandler getIssueHandler() {
        return issueHandler;
    }

    /**
     * Invoked in {@link #shutdown() } inside a block locking and unlocking
     * {@code shutdownLock}. Has to set the {@code serverRunning} state.
     */
    protected abstract void shutdown0();

    /**
     * Invokes {@link #shutdown0() } in a block locking and unlocking
     * {@code shutdownLock} which has to set the {@code serverRunning} state.
     */
    @Override
    public final void shutdown() {
        super.shutdown();
        getShutdownLock().lock();
        try {
            if(!isServerRunning()) {
                return;
            }
            shutdown0();
        }finally{
            getShutdownLock().unlock();
        }
    }

    @Override
    public void start() throws StorageCreationException {
        super.start();
        try {
            Thread.sleep(2000);
            //@TODO: find more elegant way to do this (check whether server
            //is available
        } catch (InterruptedException ex) {
            throw new StorageCreationException(ex);
        }
        recreateEntityManager();
    }

    protected abstract void preCreation() throws IOException;

    protected abstract boolean needToCreate();

    protected abstract void createDatabase() throws IOException,
            StorageCreationException,
            InterruptedException;

    /**
     * Creates and starts the storage process.
     *
     * @return a reference to the started process
     * @throws IOException
     */
    protected abstract Process createProcess() throws IOException;

    protected abstract void setupDatabase() throws IOException,
            StorageCreationException,
            InterruptedException;

    /**
     * Starts the database server and if necessary initializes the database
     * directories.
     * @throws StorageCreationException
     */
    /*
    internal implementation notes:
    - There's no sense in putting this into recreateEntityManager since it
    messes up setting of serverRunning flag (showing that using overridable
    methods in constructor (of AbstractPersistenceStorage) is discouraged for a
    reason).
    */
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
            preCreation();
            boolean needToCreate = needToCreate();
            if(needToCreate) {
                createDatabase();
            }else {
                LOGGER.info("the database is expected to exist and be operational");
            }
            this.process = createProcess();
            this.processThread = createProcessWatchThread();
            this.processThread.start();
            if(needToCreate) {
                setupDatabase();
            }
            try {
                //wait for the server to be up and running
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
                },
                        shortDescription);
                setServerRunning(true);
            } catch (ServerStartTimeoutException ex) {
                LOGGER.error("a server start timeout exception occured",
                        ex);
                    //extra logging (might be covered by caller which is
                    //catching the created StorageCreationException below) in
                    //order to figure out what might be wrong if shutdown0 hangs
                    //or fails
                setServerRunning(true);
                    //shutdown only works if serverRunning is true and we're
                    //sure that the server will be shutdown in the next
                    //statement anyway, so it doesn't matter
                shutdown();
                    //don't call shutdown0 here because it might want to join
                    //processThread on EDT and processThread might want to
                    //handle issue with a GUI based IssueHandler implementation
                    //which results in deadlock. The deadlock can be avoided if
                    //processThread tries to acquire shutdownLock and doesn't do
                    //anything on EDT if that's refused
                throw new StorageCreationException(ex);
            }
        }catch(IOException | InterruptedException ex) {
            throw new StorageCreationException(ex);
                //@TODO: this StorageCreationException is ignored by the JVM
                //and is not caught in DocumentScanner.main where it should end
                //up -> investigate through reproduction and eventually file
                //a bug against OpenJDK
        }
    }

    /**
     * Since {@code createdb} needs to be performed on a running server, but no
     * DBC test connection can be obtained before {@code createdb} has been run
     * the wait routine is needed at two place inside {@link #init() }.
     *
     * @throws StorageCreationException if the maximum of connection attempts
     * has been exceeded
     * @throws InterruptedException if it occurs in {@link Thread#sleep(long) }
     * @return {@code true} if the setup was successful, {@code false} otherwise
     */
    protected void waitForServerToBeUp(WaitForServerToBeUpLambda waitForServerToBeUpLambda,
            String processName) throws StorageCreationException,
            InterruptedException,
            IOException,
            ServerStartTimeoutException {
        int waitServerUpMillis = 0;
        while(true) {
            if(!process.isAlive()) {
                //fail fast check (doesn't make sense to run the loop again if
                //postgres is dead)
                LOGGER.debug("waiting for process stdout reader thread to join");
                processStdoutReaderThread.join();
                LOGGER.debug("process stdout reader thread joined");
                LOGGER.debug("waiting for process stderr reader thread to join");
                processStderrReaderThread.join();
                LOGGER.debug("process stderr reader thread joined");
                throw new StorageCreationException(String.format("%s process "
                        + "is no longer running (stdout was '%s' and stderr "
                        + "was '%s'",
                        shortDescription,
                        processStdoutReaderThread.getOutputBuilder().toString(),
                        processStderrReaderThread.getOutputBuilder().toString()));
            }
            if(waitServerUpMillis > waitServerUpMaxMillis) {
                throw new ServerStartTimeoutException(String.format("waiting for "
                        + "the %s process to start up timed out and has "
                        + "been tried too many times (stdout was '%s' and stderr was '%s')",
                        shortDescription,
                        processStdoutReaderThread.getOutputBuilder().toString(),
                        processStderrReaderThread.getOutputBuilder().toString()));
            }
            if(waitForServerToBeUpLambda.run()) {
                break;
            }
            waitServerUpMillis += waitServerUpIntervalMillis;
            if(waitServerUpMillis < waitServerUpMaxMillis) {
                LOGGER.debug(String.format("%s failed "
                        + "(server might not be up yet, next check "
                        + "in %d ms",
                        processName,
                        waitServerUpIntervalMillis));
                Thread.sleep(waitServerUpIntervalMillis);
            }else {
                LOGGER.warn(String.format("%s failed all "
                        + "connection attempts, aborting in order "
                        + "to avoid to wait for ever (consider "
                        + "adusting waitServerUpMaxMillis (was "
                        + "%d))",
                        processName,
                        waitServerUpMaxMillis));
            }
        }
    }

    protected Thread createProcessWatchThread() {
        this.processStdoutReaderThread = new OutputReaderThread(process.getInputStream(),
                process,
                "storage-process-stdout-thread");
        this.processStderrReaderThread = new OutputReaderThread(process.getErrorStream(),
                process,
                "storage-process-stderr-thread");
        this.processStdoutReaderThread.start();
        this.processStderrReaderThread.start();
        Thread retValue = new Thread(() -> {
            try {
                LOGGER.trace("waiting for process to finish");
                process.waitFor();
                if(getShutdownLock().tryLock()) {
                    LOGGER.trace("locked shutdown lock");
                    try {
                        issueHandler.handle(new Message(String.format("%s process crashed or was shutdown from outside the application. Restart the application in order to avoid data loss.",
                                shortDescription),
                                JOptionPane.ERROR_MESSAGE,
                                String.format("%s process crashed",
                                        shortDescription)));
                        setServerRunning(false);
                    }finally{
                        getShutdownLock().unlock();
                        LOGGER.trace("released shutdown lock");
                    }
                }else {
                    //must not do anything on EDT here because shutdown might be
                    //in progress (e.g. after start attempts timed out and
                    //ServerStartTimeoutException is caught in init
                    LOGGER.trace("attempt to lock shutdown lock unsuccessful, assuming that shutdown is in progress and that process successfully finished");
                    LOGGER.info(String.format("%s returned expectedly during shutdown process with stdout '%s' and stderr '%s'",
                            shortDescription,
                            processStdoutReaderThread.getOutputBuilder().toString(),
                            processStderrReaderThread.getOutputBuilder().toString()));
                }
            } catch (InterruptedException ex) {
                LOGGER.error(String.format("unexpected exception during watching of %s process",
                                shortDescription),
                        ex);
                issueHandler.handleUnexpectedException(new ExceptionMessage(ex));
            }
        },
                "storage-process-thread");
        return retValue;
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
