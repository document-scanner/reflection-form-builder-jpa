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

import richtercloud.reflection.form.builder.jpa.entities.EntityD;
import richtercloud.reflection.form.builder.jpa.entities.EntityE;
import richtercloud.reflection.form.builder.jpa.entities.EntityF;
import richtercloud.reflection.form.builder.jpa.entities.EntityB;
import richtercloud.reflection.form.builder.jpa.entities.EntityA;
import richtercloud.reflection.form.builder.jpa.entities.EntityC;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import richtercloud.message.handler.ConfirmMessageHandler;
import richtercloud.message.handler.MessageHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.MappedFieldUpdateEvent;
import richtercloud.reflection.form.builder.jpa.idapplier.IdApplier;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;

/**
 *
 * @author richter
 */
public class JPAReflectionFormBuilderTest {

    /**
     * Test of onFieldUpdate method, of class JPAReflectionFormBuilder.
     */
    @Test
    public void testOnFieldUpdate() throws Exception {
        EntityA entityA1 = new EntityA(1L);
        EntityB entityB1 = new EntityB(2L);
        EntityB entityB2 = new EntityB(3L);
        FieldUpdateEvent event = new MappedFieldUpdateEvent(new LinkedList(Arrays.asList(entityB1, entityB2)),
                EntityB.class.getDeclaredField("as") //mappedField
        );
            //only MappedFieldUpdateEvents are interesting
        Field field = EntityA.class.getDeclaredField("bs");
        PersistenceStorage storage = mock(PersistenceStorage.class);
        MessageHandler messageHandler = mock(MessageHandler.class);
        ConfirmMessageHandler confirmMesserHandler = mock(ConfirmMessageHandler.class);
        JPAFieldRetriever fieldRetriever = new JPACachedFieldRetriever();
        IdApplier idApplier = mock(IdApplier.class);
        IdGenerator idGenerator = new MemorySequentialIdGenerator();
        JPAReflectionFormBuilder instance = new JPAReflectionFormBuilder(storage, "title", //fieldDescriptionDialogTitle
                messageHandler,
                confirmMesserHandler,
                fieldRetriever,
                idApplier,
                idGenerator,
                new HashMap<>() //warningHandlers
        );
        //test many-to-many
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
        event = new MappedFieldUpdateEvent(new LinkedList(Arrays.asList(entityD1, entityD2)),
                EntityD.class.getDeclaredField("c") //mappedField
        );
        instance.onFieldUpdate(event, field, entityC1);
        assertTrue(entityC1.getDs().contains(entityD1));
        assertTrue(entityC1.getDs().contains(entityD2));
        assertTrue(entityD1.getC().equals(entityC1));
        //test one-to-one
        EntityE entityE1 = new EntityE(20L);
        EntityF entityF1 = new EntityF(21L);
        field = EntityE.class.getDeclaredField("f");
        event = new MappedFieldUpdateEvent(entityF1,
                EntityF.class.getDeclaredField("e") //mappedField
        );
        instance.onFieldUpdate(event, field, entityE1);
        assertTrue(entityE1.getF().equals(entityF1));
        assertTrue(entityF1.getE().equals(entityE1));
        //test many-to-one (reuse EntityC and EntityD, but inverse)
        EntityD entityD10 = new EntityD(30L);
        EntityC entityC10 = new EntityC(31L);
        field = EntityD.class.getDeclaredField("c");
        event = new MappedFieldUpdateEvent(entityC10,
                EntityC.class.getDeclaredField("ds"));
        instance.onFieldUpdate(event, field, entityD10);
        assertTrue(entityD10.getC().equals(entityC10));
        assertTrue(entityC10.getDs().contains(entityD10));
    }
}
