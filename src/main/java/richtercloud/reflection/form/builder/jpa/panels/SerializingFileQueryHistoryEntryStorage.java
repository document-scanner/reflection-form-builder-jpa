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
package richtercloud.reflection.form.builder.jpa.panels;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import richtercloud.message.handler.MessageHandler;

/**
 * Storage implemented through map serialization to file using Java
 * (de-)serialization with {@link ObjectOutputStream} and
 * {@link ObjectInputStream}. Existing data is loaded at creation.
 *
 * @author richter
 */
public class SerializingFileQueryHistoryEntryStorage extends AbstractFileQueryHistoryEntryStorage {

    public SerializingFileQueryHistoryEntryStorage(File file,
            MessageHandler messageHandler) throws ClassNotFoundException, IOException {
        super(file,
                messageHandler);
    }

    @Override
    protected void store(Map<Class<?>, List<QueryHistoryEntry>> head) {
        try(ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(getFile()))) {
            objectOutputStream.writeObject(head);
            objectOutputStream.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected Map<Class<?>, List<QueryHistoryEntry>> init() throws IOException, ClassNotFoundException {
        Map<Class<?>, List<QueryHistoryEntry>> retValue;
        try(ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(getFile()))) {
            retValue = (Map<Class<?>, List<QueryHistoryEntry>>) objectInputStream.readObject();
        }catch(EOFException ex) {
            //if file is empty
            retValue = new HashMap<>();
        }
        return retValue;
    }
}
