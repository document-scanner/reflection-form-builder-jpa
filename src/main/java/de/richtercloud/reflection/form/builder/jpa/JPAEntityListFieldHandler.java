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

import de.richtercloud.message.handler.IssueHandler;
import de.richtercloud.message.handler.MessageHandler;
import de.richtercloud.reflection.form.builder.fieldhandler.AbstractListFieldHandler;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import de.richtercloud.reflection.form.builder.jpa.panels.QueryHistoryEntryStorage;
import de.richtercloud.reflection.form.builder.jpa.storage.FieldInitializer;
import de.richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import de.richtercloud.reflection.form.builder.jpa.typehandler.JPAEntityListTypeHandler;
import de.richtercloud.reflection.form.builder.panels.AbstractListPanel;
import de.richtercloud.reflection.form.builder.typehandler.TypeHandler;
import de.richtercloud.validation.tools.FieldRetriever;
import java.util.List;

/**
 *
 * @author richter
 */
public class JPAEntityListFieldHandler extends AbstractListFieldHandler<List<Object>, FieldUpdateEvent<List<Object>>, JPAReflectionFormBuilder> implements FieldHandler<List<Object>,FieldUpdateEvent<List<Object>>, JPAReflectionFormBuilder, AbstractListPanel> {

    public JPAEntityListFieldHandler(PersistenceStorage storage,
            IssueHandler issueHandler,
            String bidirectionalHelpDialogTitle,
            FieldInitializer fieldInitializer,
            QueryHistoryEntryStorage entryStorage,
            FieldRetriever readOnlyFieldRetriever) {
        super(issueHandler,
                new JPAEntityListTypeHandler(storage,
                        issueHandler,
                        bidirectionalHelpDialogTitle,
                        fieldInitializer,
                        entryStorage,
                        readOnlyFieldRetriever));
    }

    public JPAEntityListFieldHandler(MessageHandler messageHandler,
            TypeHandler<List<Object>, FieldUpdateEvent<List<Object>>,JPAReflectionFormBuilder, AbstractListPanel> typeHandler) {
        super(messageHandler, typeHandler);
    }
}

