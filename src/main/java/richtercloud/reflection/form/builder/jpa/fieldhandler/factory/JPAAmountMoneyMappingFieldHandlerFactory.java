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
import richtercloud.reflection.form.builder.components.AmountMoneyCurrencyStorage;
import richtercloud.reflection.form.builder.components.AmountMoneyUsageStatisticsStorage;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import richtercloud.reflection.form.builder.fieldhandler.factory.AmountMoneyMappingFieldHandlerFactory;
import richtercloud.reflection.form.builder.jpa.JPAStringFieldHandler;
import richtercloud.reflection.form.builder.message.MessageHandler;

/**
 *
 * @author richter
 */
public class JPAAmountMoneyMappingFieldHandlerFactory extends AmountMoneyMappingFieldHandlerFactory {
    private final EntityManager entityManager;
    private final int initialQueryLimit;
    private final String bidirectionalHelpDialogTitle;

    public static JPAAmountMoneyMappingFieldHandlerFactory create(EntityManager entityManager,
            int initialQueryLimit,
            MessageHandler messageHandler,
            AmountMoneyUsageStatisticsStorage amountMoneyUsageStatisticsStorage,
            AmountMoneyCurrencyStorage amountMoneyCurrencyStorage,
            String bidirectionalHelpDialogTitle) {
        return new JPAAmountMoneyMappingFieldHandlerFactory(entityManager,
                initialQueryLimit,
                messageHandler,
                amountMoneyUsageStatisticsStorage,
                amountMoneyCurrencyStorage,
                bidirectionalHelpDialogTitle);
    }

    public JPAAmountMoneyMappingFieldHandlerFactory(EntityManager entityManager,
            int initialQueryLimit,
            MessageHandler messageHandler,
            AmountMoneyUsageStatisticsStorage amountMoneyUsageStatisticsStorage,
            AmountMoneyCurrencyStorage amountMoneyCurrencyStorage,
            String bidirectionalHelpDialogTitle) {
        super(amountMoneyUsageStatisticsStorage, amountMoneyCurrencyStorage, messageHandler);
        this.entityManager = entityManager;
        this.initialQueryLimit = initialQueryLimit;
        this.bidirectionalHelpDialogTitle = bidirectionalHelpDialogTitle;
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
                new JPAStringFieldHandler(entityManager,
                        initialQueryLimit,
                        getMessageHandler(),
                        bidirectionalHelpDialogTitle));
        return classMapping0;
    }
}
