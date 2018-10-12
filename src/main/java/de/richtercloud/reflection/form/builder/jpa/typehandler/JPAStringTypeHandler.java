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
package de.richtercloud.reflection.form.builder.jpa.typehandler;

import de.richtercloud.message.handler.IssueHandler;
import de.richtercloud.reflection.form.builder.ComponentHandler;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldHandlingException;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldUpdateListener;
import de.richtercloud.reflection.form.builder.jpa.JPAReflectionFormBuilder;
import de.richtercloud.reflection.form.builder.jpa.panels.StringAutoCompletePanel;
import de.richtercloud.reflection.form.builder.jpa.panels.StringPanelUpdateEvent;
import de.richtercloud.reflection.form.builder.jpa.panels.StringPanelUpdateListener;
import de.richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import de.richtercloud.reflection.form.builder.typehandler.TypeHandler;
import de.richtercloud.validation.tools.FieldRetriever;
import java.lang.reflect.Type;
import javax.swing.JComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 * @author richter
 */
public class JPAStringTypeHandler implements TypeHandler<String, FieldUpdateEvent<String>,JPAReflectionFormBuilder, StringAutoCompletePanel> {
    private final PersistenceStorage storage;
    private final int initialQueryLimit;
    private final IssueHandler issueHandler;
    private final FieldRetriever fieldRetriever;

    public JPAStringTypeHandler(PersistenceStorage storage,
            int initialQueryLimit,
            IssueHandler issueHandler,
            FieldRetriever fieldRetriever) {
        this.storage = storage;
        this.initialQueryLimit = initialQueryLimit;
        this.issueHandler = issueHandler;
        this.fieldRetriever = fieldRetriever;
    }

    @Override
    public Pair<JComponent, ComponentHandler<?>> handle(Type type,
            String fieldValue,
            String fieldName,
            Class<?> declaringClass,
            final FieldUpdateListener<FieldUpdateEvent<String>> updateListener,
            JPAReflectionFormBuilder reflectionFormBuilder) throws FieldHandlingException {
        StringAutoCompletePanel retValue = new StringAutoCompletePanel(storage,
                fieldValue, //initialValue
                declaringClass,
                fieldName,
                initialQueryLimit,
                fieldRetriever,
                issueHandler);
        retValue.addUpdateListener(new StringPanelUpdateListener() {
            @Override
            public void onUpdate(StringPanelUpdateEvent event) {
                updateListener.onUpdate(new FieldUpdateEvent<>(event.getNewValue()));
            }
        });
        return new ImmutablePair<JComponent, ComponentHandler<?>>(retValue, this);
    }

    @Override
    public void reset(StringAutoCompletePanel component) {
        component.reset();
    }
}
