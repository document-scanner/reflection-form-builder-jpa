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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.storage.StorageConfValidationException;
import richtercloud.reflection.form.builder.storage.StorageCreationException;

/**
 *
 * @author richter
 */
public abstract class AbstractProcessPersistenceStorage<C extends AbstractPersistenceStorageConf> extends AbstractPersistenceStorage<C> {
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

    public AbstractProcessPersistenceStorage(C storageConf,
            String persistenceUnitName,
            int parallelQueryCount,
            FieldRetriever fieldRetriever) throws StorageConfValidationException, StorageCreationException {
        super(storageConf,
                persistenceUnitName,
                parallelQueryCount,
                fieldRetriever);
    }

    public Lock getShutdownLock() {
        return shutdownLock;
    }

    public boolean isServerRunning() {
        return serverRunning;
    }

    public void setServerRunning(boolean serverRunning) {
        this.serverRunning = serverRunning;
    }

    protected abstract void shutdown0();

    @Override
    public final void shutdown() {
        super.shutdown();
        shutdown0();
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
}
