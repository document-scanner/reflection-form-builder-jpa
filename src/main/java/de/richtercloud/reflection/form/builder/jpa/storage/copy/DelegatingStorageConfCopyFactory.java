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
package de.richtercloud.reflection.form.builder.jpa.storage.copy;

import de.richtercloud.reflection.form.builder.jpa.storage.DerbyEmbeddedPersistenceStorageConf;
import de.richtercloud.reflection.form.builder.jpa.storage.DerbyNetworkPersistenceStorageConf;
import de.richtercloud.reflection.form.builder.jpa.storage.MySQLAutoPersistenceStorageConf;
import de.richtercloud.reflection.form.builder.jpa.storage.PostgresqlAutoPersistenceStorageConf;
import de.richtercloud.reflection.form.builder.jpa.storage.PostgresqlPersistenceStorageConf;
import de.richtercloud.reflection.form.builder.storage.StorageConf;
import de.richtercloud.reflection.form.builder.storage.copy.StorageConfCopyException;
import de.richtercloud.reflection.form.builder.storage.copy.StorageConfCopyFactory;
import java.io.IOException;

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
                retValue = new DerbyNetworkPersistenceStorageConf(storageConfCast.getHostname(),
                        storageConfCast.getPort(),
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
                        storageConfCast.getHostname(),
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
                retValue = new PostgresqlAutoPersistenceStorageConf(storageConfCast.getEntityClasses(),
                        storageConfCast.getHostname(),
                        storageConfCast.getUsername(),
                        storageConfCast.getPassword(),
                        storageConfCast.getDatabaseName(),
                        storageConfCast.getSchemeChecksumFile(),
                        storageConfCast.getDatabaseDir(),
                        storageConfCast.getInitdbBinaryPath(),
                        storageConfCast.getPostgresBinaryPath(),
                        storageConfCast.getCreatedbBinaryPath(),
                        storageConfCast.getPgCtlBinaryPath(),
                        storageConfCast.getPort(),
                        storageConfCast.getDatabaseDriver());
            } catch (IOException ex) {
                throw new StorageConfCopyException(ex);
            }
        }else if(storageConf instanceof PostgresqlPersistenceStorageConf) {
            PostgresqlPersistenceStorageConf storageConfCast = (PostgresqlPersistenceStorageConf) storageConf;
            try {
                retValue = new PostgresqlPersistenceStorageConf(storageConfCast.getEntityClasses(),
                        storageConfCast.getHostname(),
                        storageConfCast.getUsername(),
                        storageConfCast.getPassword(),
                        storageConfCast.getDatabaseName(),
                        storageConfCast.getSchemeChecksumFile(),
                        storageConfCast.getPort(),
                        storageConfCast.getDatabaseDriver());
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
