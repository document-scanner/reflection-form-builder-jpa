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
package de.richtercloud.reflection.form.builder.jpa.panels;

import de.richtercloud.message.handler.IssueHandler;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Storage implemented through map serialization to file using Java
 * (de-)serialization with {@link ObjectOutputStream} and
 * {@link ObjectInputStream}. Existing data is loaded at creation.
 *
 * @author richter
 */
public class SerializingFileQueryHistoryEntryStorage extends AbstractFileQueryHistoryEntryStorage {

    public SerializingFileQueryHistoryEntryStorage(File file,
            IssueHandler issueHandler) throws ClassNotFoundException, IOException {
        super(file,
                issueHandler);
    }

    @Override
    protected void store(Map<Class<?>, List<QueryHistoryEntry>> head) throws IOException {
        try(ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(getFile().toPath()))) {
            objectOutputStream.writeObject(head);
            objectOutputStream.flush();
        }
    }

    @Override
    protected Map<Class<?>, List<QueryHistoryEntry>> init() throws IOException, ClassNotFoundException {
        Map<Class<?>, List<QueryHistoryEntry>> retValue;
        try(ObjectInputStream objectInputStream = new ObjectInputStream(Files.newInputStream(getFile().toPath()))) {
            retValue = (Map<Class<?>, List<QueryHistoryEntry>>) objectInputStream.readObject();
        }catch(EOFException ex) {
            //if file is empty
            retValue = new HashMap<>();
        }
        return retValue;
    }
}
