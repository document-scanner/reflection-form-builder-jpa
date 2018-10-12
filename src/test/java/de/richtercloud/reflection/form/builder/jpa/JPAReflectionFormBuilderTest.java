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
package de.richtercloud.reflection.form.builder.jpa;

import de.richtercloud.message.handler.ConfirmMessageHandler;
import de.richtercloud.message.handler.IssueHandler;
import de.richtercloud.reflection.form.builder.FieldUpdateException;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import de.richtercloud.reflection.form.builder.fieldhandler.MappedFieldUpdateEvent;
import de.richtercloud.reflection.form.builder.jpa.entities.EntityA;
import de.richtercloud.reflection.form.builder.jpa.entities.EntityB;
import de.richtercloud.reflection.form.builder.jpa.entities.EntityC;
import de.richtercloud.reflection.form.builder.jpa.entities.EntityD;
import de.richtercloud.reflection.form.builder.jpa.entities.EntityE;
import de.richtercloud.reflection.form.builder.jpa.entities.EntityF;
import de.richtercloud.reflection.form.builder.jpa.idapplier.IdApplier;
import de.richtercloud.reflection.form.builder.jpa.retriever.JPAOrderedCachedFieldRetriever;
import de.richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import de.richtercloud.reflection.form.builder.retriever.FieldOrderValidationException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.mock;

/**
 *
 * @author richter
 */
public class JPAReflectionFormBuilderTest {
    private static final String TITLE = "title";

    @Test(expected = FieldUpdateException.class)
    public void testOnFieldUpdateMappedFieldNotContained() throws NoSuchFieldException,
            IllegalAccessException,
            InvocationTargetException,
            FieldOrderValidationException,
            FieldUpdateException {
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
        JPAReflectionFormBuilder instance = new JPAReflectionFormBuilder(storage,
                TITLE, //fieldDescriptionDialogTitle
                issueHandler,
                confirmMesserHandler,
                fieldRetriever,
                idApplier,
                new HashMap<>() //warningHandlers
        );
        //test IllegalArgumentException if mappedField which is contained in
        //field's class
        instance.onFieldUpdate(event, field, instance);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts")
    public void testOnFieldUpdateManyToMany() throws NoSuchFieldException,
            IllegalAccessException,
            InvocationTargetException,
            FieldOrderValidationException,
            FieldUpdateException {
        EntityA entityA1 = new EntityA(1L);
        EntityB entityB1 = new EntityB(2L);
        EntityB entityB2 = new EntityB(3L);
        Set<Class<?>> entityClasses = new HashSet<>(Arrays.asList(EntityA.class,
                EntityB.class));
        PersistenceStorage storage = mock(PersistenceStorage.class);
        IssueHandler issueHandler = mock(IssueHandler.class);
        ConfirmMessageHandler confirmMesserHandler = mock(ConfirmMessageHandler.class);
        JPAFieldRetriever fieldRetriever = new JPAOrderedCachedFieldRetriever(entityClasses);
        IdApplier<?> idApplier = mock(IdApplier.class);
        JPAReflectionFormBuilder instance = new JPAReflectionFormBuilder(storage,
                TITLE, //fieldDescriptionDialogTitle
                issueHandler,
                confirmMesserHandler,
                fieldRetriever,
                idApplier,
                new HashMap<>() //warningHandlers
        );
        //test many-to-many
        Field mappedField = EntityB.class.getDeclaredField("as");
        FieldUpdateEvent<?> event = new MappedFieldUpdateEvent<>(new LinkedList<>(Arrays.asList(entityB1, entityB2)),
                mappedField //mappedField
        );
            //only MappedFieldUpdateEvents are interesting
        Field field = EntityA.class.getDeclaredField("bs");
        instance.onFieldUpdate(event, field, entityA1);
        assertTrue(entityA1.getBs().contains(entityB1));
        assertTrue(entityA1.getBs().contains(entityB2));
        assertTrue(entityB1.getAs().contains(entityA1));
        assertTrue(entityB2.getAs().contains(entityA1));
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts")
    public void testOnFieldUpdateOneToMany() throws NoSuchFieldException,
            IllegalAccessException,
            InvocationTargetException,
            FieldOrderValidationException,
            FieldUpdateException {
        Set<Class<?>> entityClasses = new HashSet<>(Arrays.asList(EntityA.class,
                EntityB.class));
        PersistenceStorage storage = mock(PersistenceStorage.class);
        IssueHandler issueHandler = mock(IssueHandler.class);
        ConfirmMessageHandler confirmMesserHandler = mock(ConfirmMessageHandler.class);
        JPAFieldRetriever fieldRetriever = new JPAOrderedCachedFieldRetriever(entityClasses);
        IdApplier<?> idApplier = mock(IdApplier.class);
        JPAReflectionFormBuilder instance = new JPAReflectionFormBuilder(storage,
                TITLE, //fieldDescriptionDialogTitle
                issueHandler,
                confirmMesserHandler,
                fieldRetriever,
                idApplier,
                new HashMap<>() //warningHandlers
        );
        //test one-to-many
        EntityC entityC1 = new EntityC(10L);
        EntityD entityD1 = new EntityD(11L);
        EntityD entityD2 = new EntityD(12L);
        Field field = EntityC.class.getDeclaredField("ds");
        Field mappedField = EntityD.class.getDeclaredField("c");
        FieldUpdateEvent<?> event = new MappedFieldUpdateEvent<>(new LinkedList<>(Arrays.asList(entityD1, entityD2)),
                mappedField //mappedField
        );
        instance.onFieldUpdate(event, field, entityC1);
        assertTrue(entityC1.getDs().contains(entityD1));
        assertTrue(entityC1.getDs().contains(entityD2));
        assertTrue(entityD1.getC().equals(entityC1));
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts")
    public void testOnFieldUpdateOneToOne() throws NoSuchFieldException,
            IllegalAccessException,
            InvocationTargetException,
            FieldOrderValidationException,
            FieldUpdateException {
        Set<Class<?>> entityClasses = new HashSet<>(Arrays.asList(EntityA.class,
                EntityB.class));
        PersistenceStorage storage = mock(PersistenceStorage.class);
        IssueHandler issueHandler = mock(IssueHandler.class);
        ConfirmMessageHandler confirmMesserHandler = mock(ConfirmMessageHandler.class);
        JPAFieldRetriever fieldRetriever = new JPAOrderedCachedFieldRetriever(entityClasses);
        IdApplier<?> idApplier = mock(IdApplier.class);
        JPAReflectionFormBuilder instance = new JPAReflectionFormBuilder(storage,
                TITLE, //fieldDescriptionDialogTitle
                issueHandler,
                confirmMesserHandler,
                fieldRetriever,
                idApplier,
                new HashMap<>() //warningHandlers
        );
        //test one-to-one
        EntityE entityE1 = new EntityE(20L);
        EntityF entityF1 = new EntityF(21L);
        Field field = EntityE.class.getDeclaredField("f");
        Field mappedField = EntityF.class.getDeclaredField("e");
        FieldUpdateEvent<?> event = new MappedFieldUpdateEvent<>(entityF1,
                mappedField //mappedField
        );
        instance.onFieldUpdate(event, field, entityE1);
        assertTrue(entityE1.getF().equals(entityF1));
        assertTrue(entityF1.getE().equals(entityE1));
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts")
    public void testOnFieldUpdateManyToOneReuseInverse() throws NoSuchFieldException,
            IllegalAccessException,
            InvocationTargetException,
            FieldOrderValidationException,
            FieldUpdateException {
        Set<Class<?>> entityClasses = new HashSet<>(Arrays.asList(EntityA.class,
                EntityB.class));
        PersistenceStorage storage = mock(PersistenceStorage.class);
        IssueHandler issueHandler = mock(IssueHandler.class);
        ConfirmMessageHandler confirmMesserHandler = mock(ConfirmMessageHandler.class);
        JPAFieldRetriever fieldRetriever = new JPAOrderedCachedFieldRetriever(entityClasses);
        IdApplier<?> idApplier = mock(IdApplier.class);
        JPAReflectionFormBuilder instance = new JPAReflectionFormBuilder(storage,
                TITLE, //fieldDescriptionDialogTitle
                issueHandler,
                confirmMesserHandler,
                fieldRetriever,
                idApplier,
                new HashMap<>() //warningHandlers
        );
        //test many-to-one (reuse EntityC and EntityD, but inverse)
        EntityD entityD10 = new EntityD(30L);
        EntityC entityC10 = new EntityC(31L);
        Field field = EntityD.class.getDeclaredField("c");
        Field mappedField = EntityC.class.getDeclaredField("ds");
        FieldUpdateEvent<?> event = new MappedFieldUpdateEvent<>(entityC10,
                mappedField);
        instance.onFieldUpdate(event, field, entityD10);
        assertTrue(entityD10.getC().equals(entityC10));
        assertTrue(entityC10.getDs().contains(entityD10));
    }

    @Test(expected = FieldUpdateException.class)
    public void testOnFieldUpdateManyToManyNull() throws NoSuchFieldException,
            IllegalAccessException,
            InvocationTargetException,
            FieldOrderValidationException,
            FieldUpdateException {
        EntityA entityA1 = new EntityA(1L);
        Set<Class<?>> entityClasses = new HashSet<>(Arrays.asList(EntityA.class,
                EntityB.class));
        PersistenceStorage storage = mock(PersistenceStorage.class);
        IssueHandler issueHandler = mock(IssueHandler.class);
        ConfirmMessageHandler confirmMesserHandler = mock(ConfirmMessageHandler.class);
        JPAFieldRetriever fieldRetriever = new JPAOrderedCachedFieldRetriever(entityClasses);
        IdApplier<?> idApplier = mock(IdApplier.class);
        JPAReflectionFormBuilder instance = new JPAReflectionFormBuilder(storage,
                TITLE, //fieldDescriptionDialogTitle
                issueHandler,
                confirmMesserHandler,
                fieldRetriever,
                idApplier,
                new HashMap<>() //warningHandlers
        );
        //test IllegalArgumentException on null (many-to-many)
        Field field = EntityB.class.getDeclaredField("as");
        Field mappedField = EntityA.class.getDeclaredField("bs");
        FieldUpdateEvent<?> event = new MappedFieldUpdateEvent<>(null, //newValue
                mappedField //mappedField
        );
        instance.onFieldUpdate(event,
                field,
                entityA1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOnFieldUpdateOneToManyNull() throws NoSuchFieldException,
            IllegalAccessException,
            InvocationTargetException,
            FieldOrderValidationException,
            FieldUpdateException {
        EntityC entityC1 = new EntityC(10L);
        Set<Class<?>> entityClasses = new HashSet<>(Arrays.asList(EntityA.class,
                EntityB.class));
        PersistenceStorage storage = mock(PersistenceStorage.class);
        IssueHandler issueHandler = mock(IssueHandler.class);
        ConfirmMessageHandler confirmMesserHandler = mock(ConfirmMessageHandler.class);
        JPAFieldRetriever fieldRetriever = new JPAOrderedCachedFieldRetriever(entityClasses);
        IdApplier<?> idApplier = mock(IdApplier.class);
        JPAReflectionFormBuilder instance = new JPAReflectionFormBuilder(storage,
                TITLE, //fieldDescriptionDialogTitle
                issueHandler,
                confirmMesserHandler,
                fieldRetriever,
                idApplier,
                new HashMap<>() //warningHandlers
        );
        //test IllegalArgumentException on null (one-to-many)
        Field field = EntityC.class.getDeclaredField("ds");
        Field mappedField = EntityD.class.getDeclaredField("c");
        FieldUpdateEvent<?> event = new MappedFieldUpdateEvent<>(null, //newValue
                mappedField);
        instance.onFieldUpdate(event,
                field,
                entityC1);
    }

    @Test
    public void testOnFieldUpdateOneToOneNull() throws NoSuchFieldException,
            IllegalAccessException,
            InvocationTargetException,
            FieldOrderValidationException,
            FieldUpdateException {
        EntityE entityE1 = new EntityE(20L);
        EntityF entityF1 = new EntityF(21L);
        Set<Class<?>> entityClasses = new HashSet<>(Arrays.asList(EntityA.class,
                EntityB.class));
        PersistenceStorage storage = mock(PersistenceStorage.class);
        IssueHandler issueHandler = mock(IssueHandler.class);
        ConfirmMessageHandler confirmMesserHandler = mock(ConfirmMessageHandler.class);
        JPAFieldRetriever fieldRetriever = new JPAOrderedCachedFieldRetriever(entityClasses);
        IdApplier<?> idApplier = mock(IdApplier.class);
        JPAReflectionFormBuilder instance = new JPAReflectionFormBuilder(storage,
                TITLE, //fieldDescriptionDialogTitle
                issueHandler,
                confirmMesserHandler,
                fieldRetriever,
                idApplier,
                new HashMap<>() //warningHandlers
        );
        //test null in one-to-one relationship
        entityE1.setF(entityF1);
        Field field = EntityE.class.getDeclaredField("f");
        Field mappedField = EntityF.class.getDeclaredField("e");
        FieldUpdateEvent<?> event = new MappedFieldUpdateEvent<>(null, //newValue
                mappedField);
        instance.onFieldUpdate(event,
                field,
                entityE1);
        assertNull(entityE1.getF());
    }

    @Test
    public void testOnFieldUpdateManyToOneNull() throws NoSuchFieldException,
            IllegalAccessException,
            InvocationTargetException,
            FieldOrderValidationException,
            FieldUpdateException {
        EntityC entityC1 = new EntityC(10L);
        EntityD entityD1 = new EntityD(11L);
        Set<Class<?>> entityClasses = new HashSet<>(Arrays.asList(EntityA.class,
                EntityB.class));
        PersistenceStorage storage = mock(PersistenceStorage.class);
        IssueHandler issueHandler = mock(IssueHandler.class);
        ConfirmMessageHandler confirmMesserHandler = mock(ConfirmMessageHandler.class);
        JPAFieldRetriever fieldRetriever = new JPAOrderedCachedFieldRetriever(entityClasses);
        IdApplier<?> idApplier = mock(IdApplier.class);
        JPAReflectionFormBuilder instance = new JPAReflectionFormBuilder(storage,
                TITLE, //fieldDescriptionDialogTitle
                issueHandler,
                confirmMesserHandler,
                fieldRetriever,
                idApplier,
                new HashMap<>() //warningHandlers
        );
        //test null in many-to-one relationship
        entityD1.setC(entityC1);
        Field field = EntityD.class.getDeclaredField("c");
        Field mappedField = EntityC.class.getDeclaredField("ds");
        FieldUpdateEvent<?> event = new MappedFieldUpdateEvent<>(null, //newValue
                mappedField);
        instance.onFieldUpdate(event,
                field,
                entityD1);
        assertFalse(entityC1.getDs().contains(entityD1));
    }
}
