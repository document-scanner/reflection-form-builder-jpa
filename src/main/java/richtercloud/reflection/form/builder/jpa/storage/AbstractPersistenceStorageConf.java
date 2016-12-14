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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import richtercloud.reflection.form.builder.storage.StorageConf;
import richtercloud.reflection.form.builder.storage.StorageConfInitializationException;

/**
 * Holds configuration parameter for file-based database persistence storage as
 * well as scheme validation routines.
 *
 * Since database names in derby refer to base directories (and simple names
 * without path separator to the working directory of the database server (in
 * network mode) or the JVM (in embedded mode)), there's only a database
 * name option to configure (name is chosen instead of directory in order to
 * make the property reusable for other databases, like PostgreSQL).
 *
 * @author richter
 */
public abstract class AbstractPersistenceStorageConf implements StorageConf, Serializable {
    private static final long serialVersionUID = 1L;
    public final static String PASSWORD_DEFAULT = "";
    /**
     * Generates a checksum to track changes to {@code clazz} from the hash codes of declared fields and methods (tracking both might cause redundancies, but increases safety of getting all changes of database relevant properties).
     *
     * This could be used to generate {@code serialVersionUID}, but shouldn't be necessary.
     *
     * Doesn't care about constructors since they have no influence on database schemes.
     *
     * @param clazz
     * @return
     */
    public static long generateSchemeChecksum(Class<?> clazz) {
        long retValue = 0L;
        for(Field field : clazz.getDeclaredFields()) {
            retValue += field.hashCode();
        }
        for(Method method : clazz.getDeclaredMethods()) {
            retValue += method.hashCode();
        }
        return retValue;
    }
    private static Map<Class<?>, Long> generateSchemeChecksumMap(Set<Class<?>> classes) {
        Map<Class<?>, Long> retValue = new HashMap<>();
        for(Class<?> clazz: classes) {
            long checksum = generateSchemeChecksum(clazz);
            retValue.put(clazz, checksum);
        }
        return retValue;
    }
    /**
     * Embedded Derby doesn't need a non-empty username, but the network
     * connection does.
     */
    private String username = null;
    private String password = PASSWORD_DEFAULT;
    /**
     * Can refer to name or directory depending on the implementation (see class
     * comment for more info).
     */
    private String databaseName = null;
    private File schemeChecksumFile = null;
    private Set<Class<?>> entityClasses = null;
    /**
     * The name of the database driver. Not configurable because configuration
     * classes are bound to one connection type (represented by a driver).
     */
    private final String databaseDriver;

    public AbstractPersistenceStorageConf(String databaseDriver,
            Set<Class<?>> entityClasses,
            String username,
            String databaseDir,
            File schemeChecksumFile) throws FileNotFoundException, IOException {
        this.databaseDriver = databaseDriver;
        this.entityClasses = entityClasses;
        this.username = username;
        this.databaseName = databaseDir;
        this.schemeChecksumFile = schemeChecksumFile;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public abstract String getConnectionURL();

    public String getDatabaseDriver() {
        return databaseDriver;
    }

    /**
     * Retrieves a persisted version of the database scheme (stored in
     * {@code lastSchemeStorageFile} and fails the validation if it doesn't
     * match with the current set of classes.
     */
    /*
    internal implementation notes:
    - Metamodel implementations don't reliably implement `equals` (e.g.
    `org.hibernate.jpa.internal.metamodel.Metamodel` doesn't
    - java.lang.reflect.Field can't be serialized with `ObjectOutputStream`
    (fails with `java.io.NotSerializableException: java.lang.reflect.Field`) ->
    use version field, e.g. `serialVersionUID`
    obsolete internal implementation notes:
    - Metamodel can't be serialized with XMLEncoder because implementations
    don't guarantee to be persistable with it (needs a default constructor and
    also hibernate's MetamodelImpl doesn't provide one) -> ObjectOutputStream
    and ObjectInputStream
    */
    @Override
    public void validate() throws StorageConfInitializationException {
        if(this.databaseName == null) {
            throw new StorageConfInitializationException("Database name isn't specified");
        }
        if(!schemeChecksumFile.exists()) {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(schemeChecksumFile))) {
                Map<Class<?>, Long> checksumMap = generateSchemeChecksumMap(entityClasses);
                objectOutputStream.writeObject(checksumMap);
                objectOutputStream.flush();
            } catch (IOException ex) {
                throw new StorageConfInitializationException(ex);
            }
        }else {
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(schemeChecksumFile));
                Map<Class<?>, Long> checksumMapOld = (Map<Class<?>, Long>) objectInputStream.readObject();
                Map<Class<?>, Long> checksumMap = generateSchemeChecksumMap(entityClasses);
                if(!checksumMap.equals(checksumMapOld)) {
                    throw new StorageConfInitializationException(String.format(
                            "The sum of checksum of class fields and methods "
                                    + "doesn't match with the persisted map in "
                                    + "'%s'. The indicates a change to the "
                                    + "metamodel and the database scheme needs "
                                    + "to be adjusted externally. It might "
                                    + "help to store the entities in an XML "
                                    + "file, open the XML file and store the "
                                    + "entities in the new format. If you're "
                                    + "sure you know what you're doing, "
                                    + "consider removing the old scheme "
                                    + "checksum file '%s' and restart the "
                                    + "application.",
                            this.schemeChecksumFile.getAbsolutePath(),
                            this.schemeChecksumFile.getAbsolutePath()));
                }
            } catch (IOException | ClassNotFoundException ex) {
                throw new StorageConfInitializationException(ex);
            }
        }
    }
}
