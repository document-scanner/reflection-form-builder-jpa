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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.SystemUtils;
import richtercloud.reflection.form.builder.storage.StorageConfValidationException;

/**
 *
 * @author richter
 */
public class MySQLAutoPersistenceStorageConf extends AbstractNetworkPersistenceStorageConf {
    private static final long serialVersionUID = 1L;
    public final static String DATABASE_DRIVER = com.mysql.jdbc.Driver.class.getName();
    public final static int PORT_DEFAULT = 3306;
    public final static String MY_CNF_FILE_NAME_DEFAULT = "my.cnf";
    private String databaseDir;
    /**
     * Since some OS prevent {@code mysqld} from being executed as non-root user
     * the server controller requires a local MySQL installation.
     * {@code baseDir} points to it.
     */
    private String baseDir;
    /**
     * The {@code mysqld} binary. {@code null} or an empty value indicates to
     * resolve relatively to {@code baseDir}.
     */
    /*
    internal implementation notes:
    - including an empty value to indicate resolution relative to baseDir makes
    retrieving values from GUI components much more easy. In case this causes
    trouble, it can be adjusted with some programming effort.
    */
    private String mysqld = null;
    /**
     * The {@code mysqladmin} binary. {@code null} or an empty value indicates to
     * resolve relatively to {@code baseDir}.
     */
    private String mysqladmin = null;
    /**
     * The {@code mysql} binary. {@code null} or an empty value indicates to
     * resolve relatively to {@code baseDir}.
     */
    private String mysql = null;
    /**
     * The path to the {@code my.cnf} file which is used to initialize the
     * database directory and start the server. {@code null} indicates that the
     * path ought to be resolved relative to {@code baseDir}.
     */
    private String myCnfFilePath = null;

    public MySQLAutoPersistenceStorageConf(Set<Class<?>> entityClasses,
            String hostname,
            String username,
            String databaseName,
            String databaseDir,
            File schemeChecksumFile) throws FileNotFoundException, IOException {
        super(DATABASE_DRIVER,
                hostname,
                PORT_DEFAULT,
                entityClasses,
                username,
                null, //password
                databaseName,
                schemeChecksumFile);
        this.databaseDir = databaseDir;
    }

    public MySQLAutoPersistenceStorageConf(String databaseDir,
            String baseDir,
            String hostname,
            int port,
            Set<Class<?>> entityClasses,
            String username,
            String password,
            String databaseName,
            File schemeChecksumFile) throws FileNotFoundException, IOException {
        this(databaseDir,
                baseDir,
                hostname,
                port,
                DATABASE_DRIVER,
                entityClasses,
                username,
                password,
                databaseName,
                schemeChecksumFile);
    }

    /**
     * Copy constructor.
     * @param databaseDir
     * @param baseDir
     * @param port
     * @param databaseDriver
     * @param entityClasses
     * @param username
     * @param password
     * @param databaseName
     * @param schemeChecksumFile
     * @throws FileNotFoundException
     * @throws IOException
     */
    public MySQLAutoPersistenceStorageConf(String databaseDir,
            String baseDir,
            String hostname,
            int port,
            String databaseDriver,
            Set<Class<?>> entityClasses,
            String username,
            String password,
            String databaseName,
            File schemeChecksumFile) throws FileNotFoundException, IOException {
        super(hostname,
                port,
                databaseDriver,
                entityClasses,
                username,
                password,
                databaseName,
                schemeChecksumFile);
        this.databaseDir = databaseDir;
        this.baseDir = baseDir;
    }

    @Override
    public String getConnectionURL() {
        String retValue = String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC",
                    //additional properties necessary in order to avoid
                    //`java.sql.SQLException: The server time zone value 'CEST' is unrecognized or represents more than one time zone. You must configure either the server or JDBC driver (via the serverTimezone configuration property) to use a more specifc time zone value if you want to utilize time zone support.`
                    //see https://stackoverflow.com/questions/26515700/mysql-jdbc-driver-5-1-33-time-zone-issue
                    //for details
                getHostname(),
                getPort(),
                getDatabaseName());
        return retValue;
    }

    public String getDatabaseDir() {
        return databaseDir;
    }

    public void setDatabaseDir(String databaseDir) {
        this.databaseDir = databaseDir;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getMyCnfFilePath() {
        return myCnfFilePath != null ? myCnfFilePath : new File(baseDir, MY_CNF_FILE_NAME_DEFAULT).getAbsolutePath();
    }

    public void setMyCnfFilePath(String myCnfFilePath) {
        this.myCnfFilePath = myCnfFilePath;
    }

    @Override
    public String getShortDescription() {
        return "MySQL managed server network connection";
    }

    @Override
    public String getLongDescription() {
        return "MySQL is a powerful, performant database implementation";
    }

    @Override
    public void validate() throws StorageConfValidationException {
        super.validate();
        if(getDatabaseDir() == null || getDatabaseDir().isEmpty()) {
            throw new StorageConfValidationException("database directory path mustn't be null or empty");
        }
        if(getBaseDir() == null || getBaseDir().isEmpty()) {
            throw new StorageConfValidationException("base directory path mustn't be null or empty");
        }
    }

    /**
     * @return the mysqld
     */
    public String getMysqld() {
        String retValue;
        if(mysqld != null && !mysqld.isEmpty()) {
            retValue = mysqld;
        }else {
            if(SystemUtils.IS_OS_WINDOWS) {
                retValue = new File(baseDir, "bin/mysqld.exe").getAbsolutePath();
            }else {
                retValue = new File(baseDir, "bin/mysqld").getAbsolutePath();
            }
        }
        return retValue;
    }

    /**
     * @param mysqld the mysqld to set
     */
    public void setMysqld(String mysqld) {
        this.mysqld = mysqld;
    }

    /**
     * @return the mysqladmin
     */
    public String getMysqladmin() {
        String retValue;
        if(mysqladmin != null && !mysqladmin.isEmpty()) {
            retValue = mysqladmin;
        }else {
            if(SystemUtils.IS_OS_WINDOWS) {
                retValue = new File(baseDir, "bin/mysqladmin.exe").getAbsolutePath();
            }else {
                retValue = new File(baseDir, "bin/mysqladmin").getAbsolutePath();
            }
        }
        return retValue;
    }

    /**
     * @param mysqladmin the mysqladmin to set
     */
    public void setMysqladmin(String mysqladmin) {
        this.mysqladmin = mysqladmin;
    }

    /**
     * @return the mysql
     */
    public String getMysql() {
        String retValue;
        if(mysql != null && !mysql.isEmpty()) {
            retValue = mysql;
        }else {
            if(SystemUtils.IS_OS_WINDOWS) {
                retValue = new File(baseDir, "bin/mysql.exe").getAbsolutePath();
            }else {
                retValue = new File(baseDir, "bin/mysql").getAbsolutePath();
            }
        }
        return retValue;
    }

    /**
     * @param mysql the mysql to set
     */
    public void setMysql(String mysql) {
        this.mysql = mysql;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.databaseDir);
        hash = 59 * hash + Objects.hashCode(this.baseDir);
        hash = 59 * hash + Objects.hashCode(this.mysqld);
        hash = 59 * hash + Objects.hashCode(this.mysqladmin);
        hash = 59 * hash + Objects.hashCode(this.mysql);
        return hash;
    }

    protected boolean equalsTransitive(MySQLAutoPersistenceStorageConf other) {
        if(!super.equalsTransitive(other)) {
            return false;
        }
        if (!Objects.equals(this.databaseDir, other.databaseDir)) {
            return false;
        }
        if (!Objects.equals(this.baseDir, other.baseDir)) {
            return false;
        }
        if (!Objects.equals(this.mysqld, other.mysqld)) {
            return false;
        }
        if (!Objects.equals(this.mysqladmin, other.mysqladmin)) {
            return false;
        }
        if (!Objects.equals(this.mysql, other.mysql)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MySQLAutoPersistenceStorageConf other = (MySQLAutoPersistenceStorageConf) obj;
        return equalsTransitive(other);
    }
}
