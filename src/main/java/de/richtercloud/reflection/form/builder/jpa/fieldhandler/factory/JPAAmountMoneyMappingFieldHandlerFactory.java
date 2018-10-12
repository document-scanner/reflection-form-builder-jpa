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
package de.richtercloud.reflection.form.builder.jpa.fieldhandler.factory;

import de.richtercloud.message.handler.IssueHandler;
import de.richtercloud.reflection.form.builder.components.money.AmountMoneyCurrencyStorage;
import de.richtercloud.reflection.form.builder.components.money.AmountMoneyExchangeRateRetriever;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import de.richtercloud.reflection.form.builder.fieldhandler.factory.AmountMoneyMappingFieldHandlerFactory;
import de.richtercloud.reflection.form.builder.jpa.JPAStringFieldHandler;
import de.richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import de.richtercloud.validation.tools.FieldRetriever;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author richter
 */
public class JPAAmountMoneyMappingFieldHandlerFactory extends AmountMoneyMappingFieldHandlerFactory {
    private final PersistenceStorage storage;
    private final int initialQueryLimit;
    private final FieldRetriever readOnlyFieldRetriever;

    public static JPAAmountMoneyMappingFieldHandlerFactory create(PersistenceStorage storage,
            int initialQueryLimit,
            IssueHandler issueHandler,
            AmountMoneyCurrencyStorage amountMoneyCurrencyStorage,
            AmountMoneyExchangeRateRetriever amountMoneyConversionRateRetriever,
            FieldRetriever readOnlyFieldRetriever) {
        return new JPAAmountMoneyMappingFieldHandlerFactory(storage,
                initialQueryLimit,
                issueHandler,
                amountMoneyCurrencyStorage,
                amountMoneyConversionRateRetriever,
                readOnlyFieldRetriever);
    }

    public JPAAmountMoneyMappingFieldHandlerFactory(PersistenceStorage storage,
            int initialQueryLimit,
            IssueHandler issueHandler,
            AmountMoneyCurrencyStorage amountMoneyCurrencyStorage,
            AmountMoneyExchangeRateRetriever amountMoneyConversionRateRetriever,
            FieldRetriever readOnlyFieldRetriever) {
        super(amountMoneyCurrencyStorage,
                amountMoneyConversionRateRetriever,
                issueHandler);
        this.storage = storage;
        this.initialQueryLimit = initialQueryLimit;
        this.readOnlyFieldRetriever = readOnlyFieldRetriever;
    }

    /*
    internal implementation notes:
    - return a modifiable view of the collections because protecting against
    writes doesn't increase any security or enforce any interface
    */
    /**
     * Generates the mapping.
     * @return the generated mapping
     */
    @Override
    public Map<Type, FieldHandler<?, ?,?, ?>> generateClassMapping() {
        Map<Type, FieldHandler<?, ?,?, ?>> classMapping0 = new HashMap<>();
        classMapping0.putAll(super.generateClassMapping());
        //overwrite specification for String fields
        classMapping0.put(createStringTypeToken(),
                new JPAStringFieldHandler(storage,
                        initialQueryLimit,
                        getIssueHandler(),
                        readOnlyFieldRetriever));
        return classMapping0;
    }
}
