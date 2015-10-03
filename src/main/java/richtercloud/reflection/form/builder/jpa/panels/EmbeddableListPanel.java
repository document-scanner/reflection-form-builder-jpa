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
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.panels.AbstractListPanel;
import richtercloud.reflection.form.builder.panels.ListPanelItemListener;
import richtercloud.reflection.form.builder.panels.ListPanelTableCellEditor;
import richtercloud.reflection.form.builder.panels.ListPanelTableCellRenderer;
import richtercloud.reflection.form.builder.panels.ListPanelTableModel;

/**
 *
 * @author richter
 * @param <T> the type of the managed values
 */
public class EmbeddableListPanel extends AbstractListPanel<Object, ListPanelItemListener<Object>, EmbeddableListPanelTableModel> {
    private static final long serialVersionUID = 1L;
    private Class<?> embeddableClass;

    public EmbeddableListPanel(ReflectionFormBuilder reflectionFormBuilder,
            ListPanelTableCellEditor mainListCellEditor,
            ListPanelTableCellRenderer mainListCellRenderer,
            Class<?> embeddableClass) throws NoSuchMethodException {
        super(reflectionFormBuilder,
                mainListCellEditor,
                mainListCellRenderer,
                new EmbeddableListPanelTableModel(embeddableClass,
                        reflectionFormBuilder),
                createMainListColumnModel(embeddableClass, reflectionFormBuilder));
        if(embeddableClass == null) {
            throw new IllegalArgumentException("embeddableClass mustn't be null");
        }
        this.embeddableClass = embeddableClass;
    }

    /*
    internal implementation notes:
    - no need to pass renderers because editing takes place in dialog
    */
    private static DefaultTableColumnModel createMainListColumnModel(Class<?> embeddableClass, ReflectionFormBuilder reflectionFormBuilder) {
        DefaultTableColumnModel mainListColumnModel = new DefaultTableColumnModel();
        List<Field> embeddableClassFields = reflectionFormBuilder.retrieveRelevantFields(embeddableClass);
        for(int i=0; i<embeddableClassFields.size(); i++) {
            mainListColumnModel.addColumn(new TableColumn(i, 100));
        }
        return mainListColumnModel;
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
}
