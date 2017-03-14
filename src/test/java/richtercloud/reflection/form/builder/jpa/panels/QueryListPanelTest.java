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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.message.handler.IssueHandler;
import richtercloud.message.handler.LoggerIssueHandler;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.jpa.JPACachedFieldRetriever;
import richtercloud.reflection.form.builder.jpa.storage.FieldInitializer;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;

/**
 *
 * @author richter
 */
public class QueryListPanelTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(QueryListPanelTest.class);


    @Test
    public void testInit() throws IllegalArgumentException, IllegalAccessException, IOException, QueryHistoryEntryStorageCreationException {
        PersistenceStorage storage = mock(PersistenceStorage.class);
        when(storage.isClassSupported(any())).thenReturn(true);
        ReflectionFormBuilder reflectionFormBuilder = mock(ReflectionFormBuilder.class);
        FieldRetriever fieldRetriever = new JPACachedFieldRetriever();
        Class entityClass = A.class;
        List<Object> initialValues = null;
        String bidirectionalHelpDialogTitle = "test";
        IssueHandler issueHandler = new LoggerIssueHandler(LOGGER);
        FieldInitializer fieldInitializer = mock(FieldInitializer.class);
        File entryStorageFile = File.createTempFile(QueryListPanelTest.class.getSimpleName(), null);
        Set<Class<?>> entityClasses = new HashSet<>(Arrays.asList(A.class));
        QueryHistoryEntryStorageFactory entryStorageFactory = new XMLFileQueryHistoryEntryStorageFactory(entryStorageFile,
                entityClasses,
                true,
                issueHandler);
        QueryHistoryEntryStorage initialQueryTextGenerator = entryStorageFactory.create();
        QueryListPanel instance = new QueryListPanel(storage,
                fieldRetriever,
                entityClass,
                issueHandler,
                initialValues,
                bidirectionalHelpDialogTitle,
                fieldInitializer,
                initialQueryTextGenerator);
    }

    /**
     * Test of reset method, of class QueryListPanel.
     */
    @org.junit.Test
    public void testReset() {
        //@TODO
    }

    @Entity
    private class B {
        @ManyToMany(mappedBy = "bs")
        private List<A> as;

        public void setAs(List<A> as) {
            this.as = as;
        }

        public List<A> getAs() {
            return as;
        }
    }
    @Entity
    private class A {
        @ManyToMany
        private List<B> bs;

        public void setBs(List<B> bs) {
            this.bs = bs;
        }

        public List<B> getBs() {
            return bs;
        }
    }

}
