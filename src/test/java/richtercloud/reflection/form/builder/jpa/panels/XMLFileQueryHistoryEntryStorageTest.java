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
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import richtercloud.message.handler.MessageHandler;
import richtercloud.reflection.form.builder.jpa.entities.EntityA;
import richtercloud.reflection.form.builder.jpa.entities.EntityB;

/**
 *
 * @author richter
 */
public class XMLFileQueryHistoryEntryStorageTest {

    /**
     * Test of store method, of class XMLFileQueryHistoryEntryStorage.
     */
    @Test
    public void testStore() throws Exception {
        Class<?> clazz0 = EntityA.class;
        String entryText0 = "a";
        String entryText1 = "b";
        QueryHistoryEntry entry0 = new QueryHistoryEntry(entryText0);
        QueryHistoryEntry entry1 = new QueryHistoryEntry(entryText1);
        File file = File.createTempFile(XMLFileQueryHistoryEntryStorageTest.class.getSimpleName(), null);
        MessageHandler messageHandler = mock(MessageHandler.class);
        XMLFileQueryHistoryEntryStorage instance = new XMLFileQueryHistoryEntryStorage(file, messageHandler);
        instance.store(clazz0,
                entry0);
        List<QueryHistoryEntry> result = instance.retrieve(clazz0);
        List<QueryHistoryEntry> expResult = new LinkedList<>(Arrays.asList(entry0));
        assertEquals(expResult, result);
        instance.store(clazz0, entry1);
        result = instance.retrieve(clazz0);
        expResult = new LinkedList<>(Arrays.asList(entry0, entry1));
        assertEquals(expResult, result);
        Class<?> clazz1 = EntityB.class;
        QueryHistoryEntry entry3 = new QueryHistoryEntry(entryText1);
        instance.store(clazz1, entry3);
        result = instance.retrieve(clazz0);
        assertEquals(expResult, result);
    }

    /**
     * Test of init method, of class XMLFileQueryHistoryEntryStorage.
     */
    @Test
    public void testInit() throws Exception {
        File file = File.createTempFile(XMLFileQueryHistoryEntryStorageTest.class.getSimpleName(), null);
        MessageHandler messageHandler = mock(MessageHandler.class);
        Class<?> clazz = EntityA.class;
        QueryHistoryEntry entry0 = new QueryHistoryEntry("a");
        Map<Class<?>, List<QueryHistoryEntry>> expResult = new HashMap<>();
        expResult.put(clazz, new LinkedList<>(Arrays.asList(entry0)));
        XStream xStream = new XStream();
        xStream.toXML(expResult, new FileOutputStream(file));
        XMLFileQueryHistoryEntryStorage instance = new XMLFileQueryHistoryEntryStorage(file, messageHandler);
        Map<Class<?>, List<QueryHistoryEntry>> result = instance.init();
        assertEquals(expResult, result);
    }

    /**
     * Test of store method, of class XMLFileQueryHistoryEntryStorage.
     */
    @Test
    public void testRetrieve() throws Exception {
        Class<?> clazz0 = EntityA.class;
        String entryText0 = "a";
        String entryText1 = "b";
        QueryHistoryEntry entry0 = new QueryHistoryEntry(entryText0);
        QueryHistoryEntry entry1 = new QueryHistoryEntry(entryText1);
        File file = File.createTempFile(XMLFileQueryHistoryEntryStorageTest.class.getSimpleName(), null);
        MessageHandler messageHandler = mock(MessageHandler.class);
        XMLFileQueryHistoryEntryStorage instance = new XMLFileQueryHistoryEntryStorage(file, messageHandler);
        instance.store(clazz0,
                entry0);
        List<QueryHistoryEntry> result = instance.retrieve(clazz0);
        List<QueryHistoryEntry> expResult = new LinkedList<>(Arrays.asList(entry0));
        assertEquals(expResult, result);
        instance.store(clazz0, entry1);
        result = instance.retrieve(clazz0);
        expResult = new LinkedList<>(Arrays.asList(entry0, entry1));
        assertEquals(expResult, result);
        Class<?> clazz1 = EntityB.class;
        QueryHistoryEntry entry3 = new QueryHistoryEntry(entryText1);
        instance.store(clazz1, entry3);
        result = instance.retrieve(clazz0);
        assertEquals(expResult, result);
        //test that multiple calls to retrieve cause the same result
        result = instance.retrieve(clazz0);
        assertEquals(expResult, result);
    }
}
