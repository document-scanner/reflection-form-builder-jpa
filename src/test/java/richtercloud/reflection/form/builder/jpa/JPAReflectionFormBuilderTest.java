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
package richtercloud.reflection.form.builder.jpa;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.junit.Assert;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import richtercloud.message.handler.ConfirmMessageHandler;
import richtercloud.message.handler.IssueHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.MappedFieldUpdateEvent;
import richtercloud.reflection.form.builder.jpa.entities.EntityA;
import richtercloud.reflection.form.builder.jpa.entities.EntityB;
import richtercloud.reflection.form.builder.jpa.entities.EntityC;
import richtercloud.reflection.form.builder.jpa.entities.EntityD;
import richtercloud.reflection.form.builder.jpa.entities.EntityE;
import richtercloud.reflection.form.builder.jpa.entities.EntityF;
import richtercloud.reflection.form.builder.jpa.idapplier.IdApplier;
import richtercloud.reflection.form.builder.jpa.retriever.JPAOrderedCachedFieldRetriever;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.reflection.form.builder.retriever.FieldOrderValidationException;

/**
 *
 * @author richter
 */
public class JPAReflectionFormBuilderTest {

    /**
     * Test of onFieldUpdate method, of class JPAReflectionFormBuilder.
     * @throws java.lang.NoSuchFieldException
     * @throws java.lang.IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     */
    @Test
    public void testOnFieldUpdate() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, FieldOrderValidationException {
        EntityA entityA1 = new EntityA(1L);
        EntityB entityB1 = new EntityB(2L);
        EntityB entityB2 = new EntityB(3L);
        Set<Class<?>> entityClasses = new HashSet<>(Arrays.asList(EntityA.class,
                EntityB.class));
        Field mappedField = EntityA.class.getDeclaredField("bs");
        FieldUpdateEvent<?> event = new MappedFieldUpdateEvent<>(new LinkedList<>(Arrays.asList(entityB1, entityB2)),
                mappedField //mappedField
        );
            //only MappedFieldUpdateEvents are interesting
        Field field = EntityA.class.getDeclaredField("bs");
        PersistenceStorage storage = mock(PersistenceStorage.class);
        IssueHandler issueHandler = mock(IssueHandler.class);
        ConfirmMessageHandler confirmMesserHandler = mock(ConfirmMessageHandler.class);
        JPAFieldRetriever fieldRetriever = new JPAOrderedCachedFieldRetriever(entityClasses);
        IdApplier<?> idApplier = mock(IdApplier.class);
        JPAReflectionFormBuilder instance = new JPAReflectionFormBuilder(storage, "title", //fieldDescriptionDialogTitle
                issueHandler,
                confirmMesserHandler,
                fieldRetriever,
                idApplier,
                new HashMap<>() //warningHandlers
        );

        //test IllegalArgumentException if mappedField which is contained in
        //field's class
        try {
            instance.onFieldUpdate(event, field, instance);
            Assert.fail("IllegalArgumentException expected");
        }catch(IllegalArgumentException ex) {
            //expected
        }

        //test many-to-many
        mappedField = EntityB.class.getDeclaredField("as");
        event = new MappedFieldUpdateEvent<>(new LinkedList<>(Arrays.asList(entityB1, entityB2)),
                mappedField //mappedField
        );
            //only MappedFieldUpdateEvents are interesting
        field = EntityA.class.getDeclaredField("bs");
        instance.onFieldUpdate(event, field, entityA1);
        assertTrue(entityA1.getBs().contains(entityB1));
        assertTrue(entityA1.getBs().contains(entityB2));
        assertTrue(entityB1.getAs().contains(entityA1));
        assertTrue(entityB2.getAs().contains(entityA1));
        //test one-to-many
        EntityC entityC1 = new EntityC(10L);
        EntityD entityD1 = new EntityD(11L);
        EntityD entityD2 = new EntityD(12L);
        field = EntityC.class.getDeclaredField("ds");
        mappedField = EntityD.class.getDeclaredField("c");
        event = new MappedFieldUpdateEvent<>(new LinkedList<>(Arrays.asList(entityD1, entityD2)),
                mappedField //mappedField
        );
        instance.onFieldUpdate(event, field, entityC1);
        assertTrue(entityC1.getDs().contains(entityD1));
        assertTrue(entityC1.getDs().contains(entityD2));
        assertTrue(entityD1.getC().equals(entityC1));
        //test one-to-one
        EntityE entityE1 = new EntityE(20L);
        EntityF entityF1 = new EntityF(21L);
        field = EntityE.class.getDeclaredField("f");
        mappedField = EntityF.class.getDeclaredField("e");
        event = new MappedFieldUpdateEvent<>(entityF1,
                mappedField //mappedField
        );
        instance.onFieldUpdate(event, field, entityE1);
        assertTrue(entityE1.getF().equals(entityF1));
        assertTrue(entityF1.getE().equals(entityE1));
        //test many-to-one (reuse EntityC and EntityD, but inverse)
        EntityD entityD10 = new EntityD(30L);
        EntityC entityC10 = new EntityC(31L);
        field = EntityD.class.getDeclaredField("c");
        mappedField = EntityC.class.getDeclaredField("ds");
        event = new MappedFieldUpdateEvent<>(entityC10,
                mappedField);
        instance.onFieldUpdate(event, field, entityD10);
        assertTrue(entityD10.getC().equals(entityC10));
        assertTrue(entityC10.getDs().contains(entityD10));

        //test IllegalArgumentException on null (many-to-many)
        field = EntityB.class.getDeclaredField("as");
        mappedField = EntityA.class.getDeclaredField("bs");
        event = new MappedFieldUpdateEvent<>(null, //newValue
                mappedField //mappedField
        );
        try {
            instance.onFieldUpdate(event,
                    field,
                    entityA1);
            Assert.fail("IllegalArgumentException expected");
        }catch(IllegalArgumentException ex) {
            //expected
        }
        //test IllegalArgumentException on null (one-to-many)
        field = EntityC.class.getDeclaredField("ds");
        mappedField = EntityD.class.getDeclaredField("c");
        event = new MappedFieldUpdateEvent<>(null, //newValue
                mappedField);
        try {
            instance.onFieldUpdate(event,
                    field,
                    entityC1);
            Assert.fail("IllegalArgumentException expected");
        }catch(IllegalArgumentException ex) {
            //expected
        }
        //test null in one-to-one relationship
        entityE1.setF(entityF1);
        field = EntityE.class.getDeclaredField("f");
        mappedField = EntityF.class.getDeclaredField("e");
        event = new MappedFieldUpdateEvent<>(null, //newValue
                mappedField);
        instance.onFieldUpdate(event,
                field,
                entityE1);
        assertTrue(entityE1.getF() == null);
        //test null in many-to-one relationship
        entityD1.setC(entityC1);
        field = EntityD.class.getDeclaredField("c");
        mappedField = EntityC.class.getDeclaredField("ds");
        event = new MappedFieldUpdateEvent<>(null, //newValue
                mappedField);
        instance.onFieldUpdate(event,
                field,
                entityD1);
        assertTrue(!entityC1.getDs().contains(entityD1));
    }
}
