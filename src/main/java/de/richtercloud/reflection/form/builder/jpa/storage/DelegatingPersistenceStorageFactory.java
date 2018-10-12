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

import de.richtercloud.message.handler.IssueHandler;
import de.richtercloud.reflection.form.builder.storage.StorageConf;
import de.richtercloud.reflection.form.builder.storage.StorageCreationException;
import de.richtercloud.reflection.form.builder.storage.StorageFactory;
import de.richtercloud.validation.tools.FieldRetriever;

/**
 *
 * @author richter
 */
public class DelegatingPersistenceStorageFactory implements StorageFactory<PersistenceStorage, StorageConf> {
    private final DerbyEmbeddedPersistenceStorageFactory derbyEmbeddedPersistenceStorageFactory;
    private final DerbyNetworkPersistenceStorageFactory derbyNetworkPersistenceStorageFactory;
    private final PostgresqlPersistenceStorageFactory postgresqlPersistenceStorageFactory;
    private final PostgresqlAutoPersistenceStorageFactory postgresqlAutoPersistenceStorageFactory;
    private final MySQLAutoPersistenceStorageFactory mySQLAutoPersistenceStorageFactory;

    public DelegatingPersistenceStorageFactory(String persistenceUnitName,
            int parallelQueryCount,
            IssueHandler issueHandler,
            FieldRetriever fieldRetriever) {
        this.derbyEmbeddedPersistenceStorageFactory = new DerbyEmbeddedPersistenceStorageFactory(persistenceUnitName,
                parallelQueryCount,
                fieldRetriever);
        this.derbyNetworkPersistenceStorageFactory = new DerbyNetworkPersistenceStorageFactory(persistenceUnitName,
                parallelQueryCount,
                fieldRetriever);
        this.postgresqlPersistenceStorageFactory = new PostgresqlPersistenceStorageFactory(persistenceUnitName,
                parallelQueryCount,
                fieldRetriever);
        this.postgresqlAutoPersistenceStorageFactory = new PostgresqlAutoPersistenceStorageFactory(persistenceUnitName,
                parallelQueryCount,
                fieldRetriever,
                issueHandler);
        this.mySQLAutoPersistenceStorageFactory = new MySQLAutoPersistenceStorageFactory(persistenceUnitName,
                parallelQueryCount,
                issueHandler,
                fieldRetriever);
    }

    public DelegatingPersistenceStorageFactory(DerbyEmbeddedPersistenceStorageFactory derbyEmbeddedPersistenceStorageFactory,
            DerbyNetworkPersistenceStorageFactory derbyNetworkPersistenceStorageFactory,
            PostgresqlPersistenceStorageFactory postgresqlNetworkPersistenceStorageFactory,
            PostgresqlAutoPersistenceStorageFactory postgresqlAutoPersistenceStorageFactory,
            MySQLAutoPersistenceStorageFactory mySQLAutoPersistenceStorageFactory) {
        this.derbyEmbeddedPersistenceStorageFactory = derbyEmbeddedPersistenceStorageFactory;
        this.derbyNetworkPersistenceStorageFactory = derbyNetworkPersistenceStorageFactory;
        this.postgresqlPersistenceStorageFactory = postgresqlNetworkPersistenceStorageFactory;
        this.postgresqlAutoPersistenceStorageFactory = postgresqlAutoPersistenceStorageFactory;
        this.mySQLAutoPersistenceStorageFactory = mySQLAutoPersistenceStorageFactory;
    }

    @Override
    public PersistenceStorage create(StorageConf storageConf) throws StorageCreationException {
        PersistenceStorage retValue;
        if(storageConf instanceof DerbyEmbeddedPersistenceStorageConf) {
            retValue = derbyEmbeddedPersistenceStorageFactory.create((DerbyEmbeddedPersistenceStorageConf) storageConf);
        }else if(storageConf instanceof DerbyNetworkPersistenceStorageConf) {
            retValue = derbyNetworkPersistenceStorageFactory.create((DerbyNetworkPersistenceStorageConf) storageConf);
        }else if(storageConf instanceof PostgresqlAutoPersistenceStorageConf) {
            retValue = postgresqlAutoPersistenceStorageFactory.create((PostgresqlAutoPersistenceStorageConf) storageConf);
        }else if(storageConf instanceof PostgresqlPersistenceStorageConf) {
            retValue = postgresqlPersistenceStorageFactory.create((PostgresqlPersistenceStorageConf) storageConf);
        }else if(storageConf instanceof MySQLAutoPersistenceStorageConf) {
            retValue = mySQLAutoPersistenceStorageFactory.create((MySQLAutoPersistenceStorageConf) storageConf);
        }else {
            throw new IllegalArgumentException(String.format("Storage configurations of type '%s' aren't supported by this storage factory",
                    storageConf.getClass()));
        }
        return retValue;
    }
}
