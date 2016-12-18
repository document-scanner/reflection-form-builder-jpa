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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.storage.StorageConfInitializationException;
import richtercloud.reflection.form.builder.storage.StorageCreationException;

/**
 * The {@link #shutdown() } routine doesn't shutdown the database server since
 * it's expected to be managed outside the application JVM.
 *
 * @author richter
 */
public class DerbyNetworkPersistenceStorage extends AbstractPersistenceStorage<DerbyNetworkPersistenceStorageConf> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DerbyNetworkPersistenceStorage.class);

    public DerbyNetworkPersistenceStorage(DerbyNetworkPersistenceStorageConf storageConf,
            String persistenceUnitName,
            int parallelQueryCount) throws StorageConfInitializationException, StorageCreationException {
        super(storageConf,
                persistenceUnitName,
                parallelQueryCount);
    }

    @Override
    protected void init() {
        //do nothing
    }
}
