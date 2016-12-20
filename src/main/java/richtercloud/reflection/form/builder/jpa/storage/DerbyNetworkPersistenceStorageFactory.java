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

import richtercloud.reflection.form.builder.storage.StorageConfValidationException;
import richtercloud.reflection.form.builder.storage.StorageCreationException;

/**
 *
 * @author richter
 */
public class DerbyNetworkPersistenceStorageFactory extends AbstractPersistenceStorageFactory<DerbyNetworkPersistenceStorage, DerbyNetworkPersistenceStorageConf>{

    public DerbyNetworkPersistenceStorageFactory(String persistenceUnitName,
            int parallelQueryCount) {
        super(persistenceUnitName,
                parallelQueryCount);
    }

    @Override
    public DerbyNetworkPersistenceStorage create0(DerbyNetworkPersistenceStorageConf storageConf) throws StorageCreationException {
        DerbyNetworkPersistenceStorage retValue;
        try {
            retValue = new DerbyNetworkPersistenceStorage(storageConf,
                    getPersistenceUnitName(),
                    getParallelQueryCount());
        } catch (StorageConfValidationException ex) {
            throw new StorageCreationException(ex);
        }
        return retValue;
    }
}
