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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 *
 * @author richter
 */
public class AbstractFileQueryHistoryEntryStorageTest {

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void testShutdown() throws IOException,
            ClassNotFoundException {
        File file = File.createTempFile(AbstractFileQueryHistoryEntryStorageTest.class.getSimpleName(), null);
        AbstractFileQueryHistoryEntryStorage instance = new AbstractFileQueryHistoryEntryStorageImpl(file);
        instance.shutdown();
    }

    private class AbstractFileQueryHistoryEntryStorageImpl extends AbstractFileQueryHistoryEntryStorage {

        protected AbstractFileQueryHistoryEntryStorageImpl(File file) throws ClassNotFoundException,
                IOException {
            super(file,
                    null //messageHandler
            );
        }

        @Override
        public void store(Map<Class<?>, List<QueryHistoryEntry>> head) throws IOException {
            //do nothing
        }

        @Override
        public Map<Class<?>, List<QueryHistoryEntry>> init() throws IOException, ClassNotFoundException {
            return null;
        }
    }
}
