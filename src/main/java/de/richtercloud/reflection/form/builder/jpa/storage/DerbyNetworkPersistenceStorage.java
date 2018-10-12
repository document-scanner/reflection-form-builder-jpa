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

import de.richtercloud.reflection.form.builder.jpa.sequence.DerbySequenceManager;
import de.richtercloud.reflection.form.builder.jpa.sequence.SequenceManagementException;
import de.richtercloud.reflection.form.builder.jpa.sequence.SequenceManager;
import de.richtercloud.reflection.form.builder.storage.StorageConfValidationException;
import de.richtercloud.reflection.form.builder.storage.StorageCreationException;
import de.richtercloud.validation.tools.FieldRetriever;

/**
 * The {@link #shutdown() } routine doesn't shutdown the database server since
 * it's expected to be managed outside the application JVM.
 *
 * @author richter
 */
public class DerbyNetworkPersistenceStorage extends AbstractPersistenceStorage<DerbyNetworkPersistenceStorageConf> {
    private final SequenceManager<Long> sequenceManager;

    public DerbyNetworkPersistenceStorage(DerbyNetworkPersistenceStorageConf storageConf,
            String persistenceUnitName,
            int parallelQueryCount,
            FieldRetriever fieldRetriever) throws StorageConfValidationException, StorageCreationException {
        super(storageConf,
                persistenceUnitName,
                parallelQueryCount,
                fieldRetriever);
        this.sequenceManager = new DerbySequenceManager(this);
    }

    @Override
    protected void init() {
        //do nothing
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
