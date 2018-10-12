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
package de.richtercloud.reflection.form.builder.jpa.typehandler.factory;

import de.richtercloud.message.handler.IssueHandler;
import static de.richtercloud.reflection.form.builder.fieldhandler.factory.MappingFieldHandlerFactory.createStringTypeToken;
import de.richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import de.richtercloud.reflection.form.builder.jpa.typehandler.JPAStringTypeHandler;
import de.richtercloud.reflection.form.builder.typehandler.TypeHandler;
import de.richtercloud.reflection.form.builder.typehandler.factory.MappingTypeHandlerFactory;
import de.richtercloud.validation.tools.FieldRetriever;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author richter
 */
public class JPAAmountMoneyMappingTypeHandlerFactory extends MappingTypeHandlerFactory {
    private final PersistenceStorage storage;
    private final int initialQueryLimit;
    private final FieldRetriever readOnlyFieldRetriever;

    public JPAAmountMoneyMappingTypeHandlerFactory(PersistenceStorage storage,
            int initialQueryLimit,
            IssueHandler issueHandler,
            FieldRetriever readOnlyFieldRetriever) {
        super(issueHandler);
        this.storage = storage;
        this.initialQueryLimit = initialQueryLimit;
        this.readOnlyFieldRetriever = readOnlyFieldRetriever;
    }

    @Override
    public Map<Type, TypeHandler<?, ?,?, ?>> generateTypeHandlerMapping() {
        Map<Type, TypeHandler<?, ?,?, ?>> classMapping0 = new HashMap<>(super.generateTypeHandlerMapping());
        //overwrite specification for String fields
        classMapping0.put(createStringTypeToken(),
                new JPAStringTypeHandler(storage,
                        initialQueryLimit,
                        getIssueHandler(),
                        readOnlyFieldRetriever));
        return classMapping0;
    }
}
