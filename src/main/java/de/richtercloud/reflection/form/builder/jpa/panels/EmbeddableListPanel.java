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
package de.richtercloud.reflection.form.builder.jpa.panels;

import de.richtercloud.message.handler.IssueHandler;
import de.richtercloud.reflection.form.builder.FieldInfo;
import de.richtercloud.reflection.form.builder.ReflectionFormPanel;
import de.richtercloud.reflection.form.builder.ResetException;
import de.richtercloud.reflection.form.builder.TransformationException;
import de.richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import de.richtercloud.reflection.form.builder.jpa.JPAReflectionFormBuilder;
import de.richtercloud.reflection.form.builder.panels.AbstractListPanel;
import de.richtercloud.reflection.form.builder.panels.ListPanelItemListener;
import de.richtercloud.reflection.form.builder.panels.RightHeightTableHeader;
import de.richtercloud.validation.tools.FieldRetriever;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

/**
 *
 * @author richter
 */
public class EmbeddableListPanel extends AbstractListPanel<Object, ListPanelItemListener<Object>, EmbeddableListPanelTableModel, JPAReflectionFormBuilder> {
    private static final long serialVersionUID = 1L;
    private final FieldHandler embeddableFieldHandler;
    private final Class<?> embeddableClass;

    /*
    internal implementation notes:
    - no need to pass renderers because editing takes place in dialog
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private static DefaultTableColumnModel createMainListColumnModel(Class<?> embeddableClass,
            FieldRetriever fieldRetriever) {
        DefaultTableColumnModel mainListColumnModel = new DefaultTableColumnModel();
        List<Field> embeddableClassFields = fieldRetriever.retrieveRelevantFields(embeddableClass);
        int i=0;
        for(Field embeddableClassField : embeddableClassFields) {
            TableColumn tableColumn = new TableColumn(i, 100);
            FieldInfo fieldInfo = embeddableClassField.getAnnotation(FieldInfo.class);
            if(fieldInfo != null) {
                tableColumn.setHeaderValue(String.format("%s (%s)",
                        fieldInfo.name(),
                        embeddableClassField.getName()));
            }else {
                tableColumn.setHeaderValue(embeddableClassField.getName());
            }
            mainListColumnModel.addColumn(tableColumn);
            i += 1;
        }
        return mainListColumnModel;
    }

    public EmbeddableListPanel(JPAReflectionFormBuilder reflectionFormBuilder,
            Class<?> embeddableClass,
            List<Object> initialValues,
            IssueHandler issueHandler,
            FieldHandler embeddableFieldHandler,
            FieldRetriever fieldRetriever) {
        super(reflectionFormBuilder,
                new EmbeddableListPanelCellEditor(),
                new EmbeddableListPanelCellRenderer(),
                new EmbeddableListPanelTableModel(embeddableClass,
                        fieldRetriever,
                        issueHandler),
                initialValues,
                issueHandler,
                new RightHeightTableHeader(createMainListColumnModel(embeddableClass, fieldRetriever), 16));
        if(embeddableClass == null) {
            throw new IllegalArgumentException("embeddableClass mustn't be null");
        }
        if(embeddableFieldHandler == null) {
            throw new IllegalArgumentException("embeddableFieldHandler mustn't be null");
        }
        this.embeddableClass = embeddableClass;
        this.embeddableFieldHandler = embeddableFieldHandler;
        reset();
    }

    @Override
    protected Object createNewElement() {
        try {
            Constructor<?> embeddableClassConstructor = this.embeddableClass.getDeclaredConstructor();
            embeddableClassConstructor.setAccessible(true);
            return embeddableClassConstructor.newInstance();
        } catch (NoSuchMethodException
                | SecurityException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException ex) {
            throw new IllegalArgumentException(String.format("embeddableClass "
                    + "%s doesn't have a zero-argument contructor",
                    this.embeddableClass),
                    ex);
        }
    }

    @Override
    protected void editRow() throws TransformationException, NoSuchFieldException, ResetException {
        ReflectionFormPanel reflectionFormPanel = this.getReflectionFormBuilder().transformEmbeddable(
                embeddableClass, //entityClass
                this.getMainListModel().getData().get(this.getMainList().getSelectedRow()), //entityToUpdate
                embeddableFieldHandler
        );
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        EmbeddableListPanelEditDialog dialog = new EmbeddableListPanelEditDialog(topFrame, //parent
                reflectionFormPanel //reflectionFormPanel
        );
        dialog.setVisible(true); //since EmbeddableListPanelEditDialog is
            //always model this will block until the dialog is closed
        this.getMainList().updateUI(); //reflect changes done by field
                //updates
    }
}
