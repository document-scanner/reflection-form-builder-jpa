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

import richtercloud.message.handler.MessageHandler;
import richtercloud.validation.tools.FieldRetriever;
import richtercloud.reflection.form.builder.storage.StorageConfValidationException;
import richtercloud.reflection.form.builder.storage.StorageCreationException;

/**
 *
 * @author richter
 */
public class MySQLAutoPersistenceStorageFactory extends AbstractPersistenceStorageFactory<MySQLAutoPersistenceStorage, MySQLAutoPersistenceStorageConf> {
    private final MessageHandler messageHandler;

    public MySQLAutoPersistenceStorageFactory(String persistenceUnitName,
            int parallelQueryCount,
            MessageHandler messageHandler,
            FieldRetriever fieldRetriever) {
        super(persistenceUnitName,
                parallelQueryCount,
                fieldRetriever);
        this.messageHandler = messageHandler;
    }

    @Override
    public MySQLAutoPersistenceStorage create0(MySQLAutoPersistenceStorageConf storageConf) throws StorageCreationException {
        MySQLAutoPersistenceStorage retValue;
        try {
            retValue = new MySQLAutoPersistenceStorage(storageConf,
                    getPersistenceUnitName(),
                    getParallelQueryCount(),
                    messageHandler,
                    getFieldRetriever());
        } catch (StorageConfValidationException ex) {
            throw new StorageCreationException(ex);
        }
        return retValue;
    }
}
