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

import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import richtercloud.message.handler.IssueHandler;

/**
 * Storage implemented through map XML serialization to file.
 *
 * @author richter
 */
public class XMLFileQueryHistoryEntryStorage extends AbstractFileQueryHistoryEntryStorage {

    public XMLFileQueryHistoryEntryStorage(File file,
            IssueHandler issueHandler) throws ClassNotFoundException, IOException {
        super(file,
                issueHandler);
    }

    @Override
    protected void store(Map<Class<?>, List<QueryHistoryEntry>> head) throws IOException {
        XStream xStream = new XStream();
        try(FileOutputStream fileOutputStream = new FileOutputStream(getFile())) {
            xStream.toXML(head, fileOutputStream);
            fileOutputStream.flush();
        }
    }

    @Override
    protected Map<Class<?>, List<QueryHistoryEntry>> init() throws IOException, ClassNotFoundException {
        Map<Class<?>, List<QueryHistoryEntry>> retValue;
        XStream xStream = new XStream();
        try(FileInputStream fileInputStream = new FileInputStream(getFile())) {
            retValue = (Map<Class<?>, List<QueryHistoryEntry>>) xStream.fromXML(fileInputStream);
        }catch(com.thoughtworks.xstream.io.StreamException ex) {
            //if file is empty or not readable
            retValue = new HashMap<>();
        }
        return retValue;
    }
}
