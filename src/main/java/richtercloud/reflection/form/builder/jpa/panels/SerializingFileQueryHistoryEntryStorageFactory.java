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
package richtercloud.reflection.form.builder.jpa.panels;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import richtercloud.message.handler.IssueHandler;

/**
 * Creates a {@link FileQueryHistoryEntryStorage} with default queries.
 * @author richter
 */
public class SerializingFileQueryHistoryEntryStorageFactory extends AbstractQueryHistoryEntryStorageFactory<SerializingFileQueryHistoryEntryStorage> {
    private final File file;

    public SerializingFileQueryHistoryEntryStorageFactory(File file,
            IssueHandler issueHandler,
            Set<Class<?>> entityClasses,
            boolean forbidSubtypes) {
        super(entityClasses,
                forbidSubtypes,
                issueHandler);
        this.file = file;
    }

    @Override
    public final SerializingFileQueryHistoryEntryStorage create0() throws QueryHistoryEntryStorageCreationException {
        SerializingFileQueryHistoryEntryStorage entryStorage;
        try {
            entryStorage = new SerializingFileQueryHistoryEntryStorage(file,
                    getIssueHandler());
        } catch (ClassNotFoundException | IOException ex) {
            throw new QueryHistoryEntryStorageCreationException(ex);
        }
        return entryStorage;
    }
}
