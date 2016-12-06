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

import richtercloud.reflection.form.builder.storage.StorageConf;
import richtercloud.reflection.form.builder.storage.StorageCreationException;
import richtercloud.reflection.form.builder.storage.StorageFactory;
import richtercloud.reflection.form.builder.storage.XMLStorageFactory;

/**
 *
 * @author richter
 */
public class DelegatingPersistenceStorageFactory implements StorageFactory<PersistenceStorage, StorageConf> {
    private final DerbyEmbeddedPersistenceStorageFactory derbyEmbeddedPersistenceStorageFactory;
    private final DerbyNetworkPersistenceStorageFactory derbyNetworkPersistenceStorageFactory;

    public DelegatingPersistenceStorageFactory() {
        this.derbyEmbeddedPersistenceStorageFactory = new DerbyEmbeddedPersistenceStorageFactory();
        this.derbyNetworkPersistenceStorageFactory = new DerbyNetworkPersistenceStorageFactory();
    }

    public DelegatingPersistenceStorageFactory(DerbyEmbeddedPersistenceStorageFactory derbyEmbeddedPersistenceStorageFactory, DerbyNetworkPersistenceStorageFactory derbyNetworkPersistenceStorageFactory, XMLStorageFactory xMLStorageFactory) {
        this.derbyEmbeddedPersistenceStorageFactory = derbyEmbeddedPersistenceStorageFactory;
        this.derbyNetworkPersistenceStorageFactory = derbyNetworkPersistenceStorageFactory;
    }

    @Override
    public PersistenceStorage create(StorageConf storageConf) throws StorageCreationException {
        PersistenceStorage retValue;
        if(storageConf instanceof DerbyEmbeddedPersistenceStorageConf) {
            retValue = derbyEmbeddedPersistenceStorageFactory.create((DerbyEmbeddedPersistenceStorageConf) storageConf);
        }else if(storageConf instanceof DerbyNetworkPersistenceStorageConf) {
            retValue = derbyNetworkPersistenceStorageFactory.create((DerbyNetworkPersistenceStorageConf) storageConf);
        }else {
            throw new IllegalArgumentException(String.format("Storage configurations of type '%s' aren't supported by this storage factory",
                    storageConf.getClass()));
        }
        return retValue;
    }
}
