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
package richtercloud.reflection.form.builder.jpa.fieldhandler.factory;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import richtercloud.reflection.form.builder.ClassAnnotationHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.factory.MappingClassAnnotationFactory;
import richtercloud.reflection.form.builder.jpa.EntityClassAnnotationHandler;

/**
 *
 * @author richter
 */
public class JPAAmountMoneyMappingClassAnnotationFactory extends MappingClassAnnotationFactory {
    private final EntityClassAnnotationHandler entityClassAnnotationHandler;

    public JPAAmountMoneyMappingClassAnnotationFactory(EntityClassAnnotationHandler entityClassAnnotationHandler) {
        this.entityClassAnnotationHandler = entityClassAnnotationHandler;
    }

    public static JPAAmountMoneyMappingClassAnnotationFactory create(EntityManager entityManager) {
        return new JPAAmountMoneyMappingClassAnnotationFactory(new EntityClassAnnotationHandler(entityManager));
    }

    @Override
    public List<Pair<Class<? extends Annotation>, ClassAnnotationHandler<Object, FieldUpdateEvent<Object>>>> generateClassAnnotationMapping() {
        List<Pair<Class<? extends Annotation>, ClassAnnotationHandler<Object,FieldUpdateEvent<Object>>>> retValue = new LinkedList<>();
        retValue.add(new ImmutablePair<Class<? extends Annotation>, ClassAnnotationHandler<Object,FieldUpdateEvent<Object>>>(Entity.class,
                entityClassAnnotationHandler));
        return retValue;
    }
}
