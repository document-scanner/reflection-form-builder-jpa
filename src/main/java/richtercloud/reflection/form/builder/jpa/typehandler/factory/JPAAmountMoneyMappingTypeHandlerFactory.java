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
package richtercloud.reflection.form.builder.jpa.typehandler.factory;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import richtercloud.message.handler.MessageHandler;
import static richtercloud.reflection.form.builder.fieldhandler.factory.MappingFieldHandlerFactory.createStringTypeToken;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.reflection.form.builder.jpa.typehandler.JPAStringTypeHandler;
import richtercloud.reflection.form.builder.typehandler.TypeHandler;
import richtercloud.reflection.form.builder.typehandler.factory.MappingTypeHandlerFactory;

/**
 *
 * @author richter
 */
public class JPAAmountMoneyMappingTypeHandlerFactory extends MappingTypeHandlerFactory {
    private final PersistenceStorage storage;
    private final int initialQueryLimit;
    private final String bidirectionalHelpDialogTitle;

    public JPAAmountMoneyMappingTypeHandlerFactory(PersistenceStorage storage,
            int initialQueryLimit,
            MessageHandler messageHandler,
            String bidirectionalHelpDialogTitle) {
        super(messageHandler);
        this.storage = storage;
        this.initialQueryLimit = initialQueryLimit;
        this.bidirectionalHelpDialogTitle = bidirectionalHelpDialogTitle;
    }

    @Override
    public Map<Type, TypeHandler<?, ?,?, ?>> generateTypeHandlerMapping() {
        Map<Type, TypeHandler<?, ?,?, ?>> classMapping0 = new HashMap<>(super.generateTypeHandlerMapping());
        //overwrite specification for String fields
        classMapping0.put(createStringTypeToken(),
                new JPAStringTypeHandler(storage,
                        initialQueryLimit,
                        getMessageHandler(),
                        bidirectionalHelpDialogTitle));
        return classMapping0;
    }
}
