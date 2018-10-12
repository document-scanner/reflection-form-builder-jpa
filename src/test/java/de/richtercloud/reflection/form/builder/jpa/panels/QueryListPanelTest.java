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
import de.richtercloud.message.handler.LoggerIssueHandler;
import de.richtercloud.reflection.form.builder.ResetException;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import de.richtercloud.reflection.form.builder.jpa.retriever.JPAOrderedCachedFieldRetriever;
import de.richtercloud.reflection.form.builder.jpa.storage.FieldInitializer;
import de.richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import de.richtercloud.reflection.form.builder.retriever.FieldOrderValidationException;
import de.richtercloud.validation.tools.FieldRetriever;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author richter
 */
public class QueryListPanelTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(QueryListPanelTest.class);

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void testInit() throws IllegalArgumentException,
            IllegalAccessException,
            IOException,
            QueryHistoryEntryStorageCreationException,
            NoSuchFieldException,
            ResetException,
            FieldOrderValidationException,
            FieldHandlingException {
        PersistenceStorage storage = mock(PersistenceStorage.class);
        when(storage.isClassSupported(any())).thenReturn(true);
        Class entityClass = ClassA.class;
        Set<Class<?>> entityClasses = new HashSet<>(Arrays.asList(ClassA.class,
                ClassB.class));
        FieldRetriever fieldRetriever = new JPAOrderedCachedFieldRetriever(entityClasses);
        List<Object> initialValues = new LinkedList<>();
        String bidirectionalHelpDialogTitle = "test";
        IssueHandler issueHandler = new LoggerIssueHandler(LOGGER);
        FieldInitializer fieldInitializer = mock(FieldInitializer.class);
        File entryStorageFile = File.createTempFile(QueryListPanelTest.class.getSimpleName(), null);
        QueryHistoryEntryStorageFactory entryStorageFactory = new XMLFileQueryHistoryEntryStorageFactory(entryStorageFile,
                entityClasses,
                true,
                issueHandler);
        QueryHistoryEntryStorage initialQueryTextGenerator = entryStorageFactory.create();
        new QueryListPanel(storage,
                fieldRetriever,
                entityClass,
                issueHandler,
                initialValues,
                bidirectionalHelpDialogTitle,
                fieldInitializer,
                initialQueryTextGenerator);
    }

    @Entity
    private class ClassB {
        @ManyToMany(mappedBy = "bs")
        private List<ClassA> as;

        public void setAs(List<ClassA> as) {
            this.as = as;
        }

        public List<ClassA> getAs() {
            return as;
        }
    }
    @Entity
    private class ClassA {
        @ManyToMany
        private List<ClassB> bs;

        public void setBs(List<ClassB> bs) {
            this.bs = bs;
        }

        public List<ClassB> getBs() {
            return bs;
        }
    }

}
