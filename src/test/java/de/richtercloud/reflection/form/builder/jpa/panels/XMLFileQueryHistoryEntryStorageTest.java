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

import com.thoughtworks.xstream.XStream;
import de.richtercloud.message.handler.IssueHandler;
import de.richtercloud.reflection.form.builder.jpa.entities.EntityA;
import de.richtercloud.reflection.form.builder.jpa.entities.EntityB;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.mock;

/**
 *
 * @author richter
 */
public class XMLFileQueryHistoryEntryStorageTest {

    @Test
    @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts")
    public void testStore() throws IOException,
            ClassNotFoundException,
            QueryHistoryEntryStorageException {
        Class<?> clazz0 = EntityA.class;
        String entryText0 = "a";
        String entryText1 = "b";
        QueryHistoryEntry entry0 = new QueryHistoryEntry(entryText0);
        QueryHistoryEntry entry1 = new QueryHistoryEntry(entryText1);
        File file = File.createTempFile(XMLFileQueryHistoryEntryStorageTest.class.getSimpleName(), null);
        IssueHandler issueHandler = mock(IssueHandler.class);
        XMLFileQueryHistoryEntryStorage instance = new XMLFileQueryHistoryEntryStorage(file,
                issueHandler);
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

    @Test
    public void testInit() throws IOException,
            ClassNotFoundException {
        File file = File.createTempFile(XMLFileQueryHistoryEntryStorageTest.class.getSimpleName(), null);
        IssueHandler issueHandler = mock(IssueHandler.class);
        Class<?> clazz = EntityA.class;
        QueryHistoryEntry entry0 = new QueryHistoryEntry("a");
        Map<Class<?>, List<QueryHistoryEntry>> expResult = new HashMap<>();
        expResult.put(clazz, new LinkedList<>(Arrays.asList(entry0)));
        XStream xStream = new XStream();
        xStream.toXML(expResult,
                Files.newOutputStream(file.toPath()));
        XMLFileQueryHistoryEntryStorage instance = new XMLFileQueryHistoryEntryStorage(file,
                issueHandler);
        Map<Class<?>, List<QueryHistoryEntry>> result = instance.init();
        assertEquals(expResult, result);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts")
    public void testRetrieve() throws IOException,
            ClassNotFoundException,
            QueryHistoryEntryStorageException {
        Class<?> clazz0 = EntityA.class;
        String entryText0 = "a";
        String entryText1 = "b";
        QueryHistoryEntry entry0 = new QueryHistoryEntry(entryText0);
        QueryHistoryEntry entry1 = new QueryHistoryEntry(entryText1);
        File file = File.createTempFile(XMLFileQueryHistoryEntryStorageTest.class.getSimpleName(), null);
        IssueHandler issueHandler = mock(IssueHandler.class);
        XMLFileQueryHistoryEntryStorage instance = new XMLFileQueryHistoryEntryStorage(file,
                issueHandler);
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
