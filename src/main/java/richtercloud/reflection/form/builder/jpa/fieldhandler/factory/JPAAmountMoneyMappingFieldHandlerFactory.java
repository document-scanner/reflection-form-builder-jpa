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
import richtercloud.message.handler.IssueHandler;
import richtercloud.reflection.form.builder.components.money.AmountMoneyCurrencyStorage;
import richtercloud.reflection.form.builder.components.money.AmountMoneyExchangeRateRetriever;
import richtercloud.reflection.form.builder.components.money.AmountMoneyUsageStatisticsStorage;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.factory.AmountMoneyMappingFieldHandlerFactory;
import richtercloud.reflection.form.builder.jpa.JPAStringFieldHandler;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.validation.tools.FieldRetriever;

/**
 *
 * @author richter
 */
public class JPAAmountMoneyMappingFieldHandlerFactory extends AmountMoneyMappingFieldHandlerFactory {
    public static JPAAmountMoneyMappingFieldHandlerFactory create(PersistenceStorage storage,
            int initialQueryLimit,
            IssueHandler issueHandler,
            AmountMoneyUsageStatisticsStorage amountMoneyUsageStatisticsStorage,
            AmountMoneyCurrencyStorage amountMoneyCurrencyStorage,
            AmountMoneyExchangeRateRetriever amountMoneyConversionRateRetriever,
            String bidirectionalHelpDialogTitle,
            FieldRetriever readOnlyFieldRetriever) {
        return new JPAAmountMoneyMappingFieldHandlerFactory(storage,
                initialQueryLimit,
                issueHandler,
                amountMoneyUsageStatisticsStorage,
                amountMoneyCurrencyStorage,
                amountMoneyConversionRateRetriever,
                bidirectionalHelpDialogTitle,
                readOnlyFieldRetriever);
    }
    private final PersistenceStorage storage;
    private final int initialQueryLimit;
    private final String bidirectionalHelpDialogTitle;
    private final FieldRetriever readOnlyFieldRetriever;

    public JPAAmountMoneyMappingFieldHandlerFactory(PersistenceStorage storage,
            int initialQueryLimit,
            IssueHandler issueHandler,
            AmountMoneyUsageStatisticsStorage amountMoneyUsageStatisticsStorage,
            AmountMoneyCurrencyStorage amountMoneyCurrencyStorage,
            AmountMoneyExchangeRateRetriever amountMoneyConversionRateRetriever,
            String bidirectionalHelpDialogTitle,
            FieldRetriever readOnlyFieldRetriever) {
        super(amountMoneyUsageStatisticsStorage,
                amountMoneyCurrencyStorage,
                amountMoneyConversionRateRetriever,
                issueHandler);
        this.storage = storage;
        this.initialQueryLimit = initialQueryLimit;
        this.bidirectionalHelpDialogTitle = bidirectionalHelpDialogTitle;
        this.readOnlyFieldRetriever = readOnlyFieldRetriever;
    }

    /**
     *
     * @return
     */
    /*
    internal implementation notes:
    - return a modifiable view of the collections because protecting against
    writes doesn't increase any security or enforce any interface
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
                        bidirectionalHelpDialogTitle,
                        readOnlyFieldRetriever));
        return classMapping0;
    }
}
