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

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.swing.JFrame;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.message.handler.LoggerMessageHandler;
import richtercloud.message.handler.MessageHandler;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.jpa.JPACachedFieldRetriever;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.reflection.form.builder.jpa.storage.FieldInitializer;

/**
 *
 * @author richter
 */
public class QueryListPanelTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(QueryListPanelTest.class);


    @Test
    public void testInit() throws IllegalArgumentException, IllegalAccessException {
        PersistenceStorage storage = mock(PersistenceStorage.class);
        when(storage.isManaged(any())).thenReturn(true);
        ReflectionFormBuilder reflectionFormBuilder = mock(ReflectionFormBuilder.class);
        FieldRetriever fieldRetriever = new JPACachedFieldRetriever();
        when(reflectionFormBuilder.getFieldRetriever()).thenReturn(fieldRetriever);
        Class entityClass = A.class;
        List<Object> initialValues = null;
        String bidirectionalHelpDialogTitle = "test";
        MessageHandler messageHandler = new LoggerMessageHandler(LOGGER);
        FieldInitializer fieldInitializer = mock(FieldInitializer.class);
        QueryListPanel instance = new QueryListPanel(storage,
                reflectionFormBuilder,
                entityClass,
                messageHandler,
                initialValues,
                bidirectionalHelpDialogTitle,
                fieldInitializer);
        JFrame x = new JFrame();
        x.getContentPane().add(instance);
        x.setBounds(0, 0, 200, 200);
        x.pack();
        x.setVisible(true);
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
