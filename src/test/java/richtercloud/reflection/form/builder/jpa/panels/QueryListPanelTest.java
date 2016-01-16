/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package richtercloud.reflection.form.builder.jpa.panels;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.ManyToMany;
import javax.persistence.metamodel.Metamodel;
import javax.swing.JFrame;
import org.junit.Test;
import static org.mockito.Mockito.*;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.jpa.JPACachedFieldRetriever;

/**
 *
 * @author richter
 */
public class QueryListPanelTest {

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

    @Test
    public void testInit() throws IllegalArgumentException, IllegalAccessException {
        EntityManager entityManager = mock(EntityManager.class);
        Metamodel metamodel = mock(Metamodel.class);
        when(entityManager.getMetamodel()).thenReturn(metamodel);
        ReflectionFormBuilder reflectionFormBuilder = mock(ReflectionFormBuilder.class);
        FieldRetriever fieldRetriever = new JPACachedFieldRetriever();
        when(reflectionFormBuilder.getFieldRetriever()).thenReturn(fieldRetriever);
        Class entityClass = A.class;
        List<Object> initialValues = null;
        String bidirectionalHelpDialogTitle = "test";
        QueryListPanel instance = new QueryListPanel(entityManager, reflectionFormBuilder, entityClass, initialValues, bidirectionalHelpDialogTitle);
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

}
