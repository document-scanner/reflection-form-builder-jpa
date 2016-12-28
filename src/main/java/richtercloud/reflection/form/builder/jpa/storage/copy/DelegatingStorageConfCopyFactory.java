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
package richtercloud.reflection.form.builder.jpa.storage.copy;

import java.io.IOException;
import richtercloud.reflection.form.builder.jpa.storage.DerbyEmbeddedPersistenceStorageConf;
import richtercloud.reflection.form.builder.jpa.storage.DerbyNetworkPersistenceStorageConf;
import richtercloud.reflection.form.builder.jpa.storage.MySQLAutoPersistenceStorageConf;
import richtercloud.reflection.form.builder.jpa.storage.PostgresqlAutoPersistenceStorageConf;
import richtercloud.reflection.form.builder.jpa.storage.PostgresqlPersistenceStorageConf;
import richtercloud.reflection.form.builder.storage.StorageConf;
import richtercloud.reflection.form.builder.storage.copy.StorageConfCopyException;
import richtercloud.reflection.form.builder.storage.copy.StorageConfCopyFactory;

/**
 *
 * @author richter
 */
public class DelegatingStorageConfCopyFactory implements StorageConfCopyFactory<StorageConf> {

    @Override
    public StorageConf copy(StorageConf storageConf) throws StorageConfCopyException {
        StorageConf retValue;
        if(storageConf instanceof DerbyEmbeddedPersistenceStorageConf) {
            DerbyEmbeddedPersistenceStorageConf storageConfCast = (DerbyEmbeddedPersistenceStorageConf) storageConf;
            try {
                retValue = new DerbyEmbeddedPersistenceStorageConf(storageConfCast.getDatabaseDriver(),
                        storageConfCast.getEntityClasses(),
                        storageConfCast.getUsername(),
                        storageConfCast.getPassword(),
                        storageConfCast.getDatabaseName(),
                        storageConfCast.getSchemeChecksumFile());
            } catch (IOException ex) {
                throw new StorageConfCopyException(ex);
            }
        }else if(storageConf instanceof DerbyNetworkPersistenceStorageConf) {
            DerbyNetworkPersistenceStorageConf storageConfCast = (DerbyNetworkPersistenceStorageConf) storageConf;
            try {
                retValue = new DerbyNetworkPersistenceStorageConf(storageConfCast.getPort(),
                        storageConfCast.getDatabaseDriver(),
                        storageConfCast.getEntityClasses(),
                        storageConfCast.getUsername(),
                        storageConfCast.getHostname(),
                        storageConfCast.getDatabaseName(),
                        storageConfCast.getSchemeChecksumFile());
            } catch (IOException ex) {
                throw new StorageConfCopyException(ex);
            }
        }else if(storageConf instanceof MySQLAutoPersistenceStorageConf) {
            MySQLAutoPersistenceStorageConf storageConfCast = (MySQLAutoPersistenceStorageConf) storageConf;
            try {
                retValue = new MySQLAutoPersistenceStorageConf(storageConfCast.getDatabaseDir(),
                        storageConfCast.getBaseDir(),
                        storageConfCast.getPort(),
                        storageConfCast.getDatabaseDriver(),
                        storageConfCast.getEntityClasses(),
                        storageConfCast.getUsername(),
                        storageConfCast.getPassword(),
                        storageConfCast.getDatabaseName(),
                        storageConfCast.getSchemeChecksumFile());
            } catch (IOException ex) {
                throw new StorageConfCopyException(ex);
            }
        }else if(storageConf instanceof PostgresqlAutoPersistenceStorageConf) {
            PostgresqlAutoPersistenceStorageConf storageConfCast = (PostgresqlAutoPersistenceStorageConf) storageConf;
            try {
                retValue = new PostgresqlAutoPersistenceStorageConf(storageConfCast.getDatabaseDir(),
                        storageConfCast.getPort(),
                        storageConfCast.getDatabaseDriver(),
                        storageConfCast.getEntityClasses(),
                        storageConfCast.getUsername(),
                        storageConfCast.getPassword(),
                        storageConfCast.getDatabaseName(),
                        storageConfCast.getSchemeChecksumFile());
            } catch (IOException ex) {
                throw new StorageConfCopyException(ex);
            }
        }else if(storageConf instanceof PostgresqlPersistenceStorageConf) {
            PostgresqlPersistenceStorageConf storageConfCast = (PostgresqlPersistenceStorageConf) storageConf;
            try {
                retValue = new PostgresqlPersistenceStorageConf(storageConfCast.getPort(),
                        storageConfCast.getDatabaseDriver(),
                        storageConfCast.getEntityClasses(),
                        storageConfCast.getUsername(),
                        storageConfCast.getPassword(),
                        storageConfCast.getDatabaseName(),
                        storageConfCast.getSchemeChecksumFile());
            } catch (IOException ex) {
                throw new StorageConfCopyException(ex);
            }
        }else {
            throw new IllegalArgumentException(String.format("storage conf of "
                    + "type %s not supported",
                    storageConf.getClass()));
        }
        return retValue;
    }
}
