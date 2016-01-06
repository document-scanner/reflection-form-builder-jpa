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

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import richtercloud.reflection.form.builder.typehandler.TypeHandler;
import static richtercloud.reflection.form.builder.fieldhandler.factory.MappingFieldHandlerFactory.createStringTypeToken;
import richtercloud.reflection.form.builder.typehandler.factory.MappingTypeHandlerFactory;
import richtercloud.reflection.form.builder.jpa.typehandler.JPAStringTypeHandler;
import richtercloud.reflection.form.builder.message.MessageHandler;

/**
 *
 * @author richter
 */
public class JPAAmountMoneyMappingTypeHandlerFactory extends MappingTypeHandlerFactory {
    private final EntityManager entityManager;
    private final int initialQueryLimit;

    public JPAAmountMoneyMappingTypeHandlerFactory(EntityManager entityManager, int initialQueryLimit, MessageHandler messageHandler) {
        super(messageHandler);
        this.entityManager = entityManager;
        this.initialQueryLimit = initialQueryLimit;
    }

    @Override
    public Map<Type, TypeHandler<?, ?,?>> generateTypeHandlerMapping() {
        Map<Type, TypeHandler<?, ?,?>> classMapping0 = new HashMap<>(super.generateTypeHandlerMapping());
        //overwrite specification for String fields
        classMapping0.put(createStringTypeToken(),
                new JPAStringTypeHandler(entityManager,
                        initialQueryLimit,
                        getMessageHandler()));
        return classMapping0;
    }
}