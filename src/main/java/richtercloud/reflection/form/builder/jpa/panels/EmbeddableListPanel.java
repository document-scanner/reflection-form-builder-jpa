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
package richtercloud.reflection.form.builder.jpa.panels;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import richtercloud.message.handler.MessageHandler;
import richtercloud.reflection.form.builder.FieldInfo;
import richtercloud.validation.tools.FieldRetriever;
import richtercloud.reflection.form.builder.ReflectionFormPanel;
import richtercloud.reflection.form.builder.TransformationException;
import richtercloud.reflection.form.builder.fieldhandler.FieldHandler;
import richtercloud.reflection.form.builder.jpa.JPAReflectionFormBuilder;
import richtercloud.reflection.form.builder.panels.AbstractListPanel;
import richtercloud.reflection.form.builder.panels.ListPanelItemListener;
import richtercloud.reflection.form.builder.panels.RightHeightTableHeader;

/**
 *
 * @author richter
 */
public class EmbeddableListPanel extends AbstractListPanel<Object, ListPanelItemListener<Object>, EmbeddableListPanelTableModel, JPAReflectionFormBuilder> {
    private static final long serialVersionUID = 1L;

    /*
    internal implementation notes:
    - no need to pass renderers because editing takes place in dialog
     */
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
    private final FieldHandler embeddableFieldHandler;
    private Class<?> embeddableClass;

    public EmbeddableListPanel(JPAReflectionFormBuilder reflectionFormBuilder,
            Class<?> embeddableClass,
            List<Object> initialValues,
            MessageHandler messageHandler,
            FieldHandler embeddableFieldHandler,
            FieldRetriever fieldRetriever) {
        super(reflectionFormBuilder,
                new EmbeddableListPanelCellEditor(),
                new EmbeddableListPanelCellRenderer(),
                new EmbeddableListPanelTableModel(embeddableClass,
                        fieldRetriever),
                initialValues,
                messageHandler,
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
            Object retValue;
            Constructor<?> embeddableClassConstructor = this.embeddableClass.getDeclaredConstructor();
            embeddableClassConstructor.setAccessible(true);
            retValue = embeddableClassConstructor.newInstance();
            return retValue;
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new IllegalArgumentException(String.format("embeddableClass %s doesn't have a zero-argument contructor", this.embeddableClass));
        }
    }

    @Override
    protected void editRow() throws TransformationException {
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
