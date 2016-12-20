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

import richtercloud.reflection.form.builder.storage.Storage;
import richtercloud.reflection.form.builder.storage.StorageConf;
import richtercloud.reflection.form.builder.storage.StorageCreationException;
import richtercloud.reflection.form.builder.storage.StorageFactory;

/**
 *
 * @author richter
 */
public abstract class AbstractPersistenceStorageFactory<S extends Storage, C extends StorageConf> implements StorageFactory<S, C> {
    private final String persistenceUnitName;
    private final int parallelQueryCount;

    public AbstractPersistenceStorageFactory(String persistenceUnitName,
            int parallelQueryCount) {
        this.persistenceUnitName = persistenceUnitName;
        this.parallelQueryCount = parallelQueryCount;
    }

    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public int getParallelQueryCount() {
        return parallelQueryCount;
    }

    @Override
    public final S create(C storageConf) throws StorageCreationException {
        S retValue = create0(storageConf);
        retValue.start();
        return retValue;
    }

    protected abstract S create0(C storageConf) throws StorageCreationException;
}
