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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import richtercloud.reflection.form.builder.ReflectionFormBuilder;
import richtercloud.reflection.form.builder.ReflectionFormPanel;
import richtercloud.reflection.form.builder.panels.ListPanelTableModel;

/**
 *
 * @author richter
 */
public class EmbeddableListPanelTableModel extends DefaultTableModel implements ListPanelTableModel<Object> {
    private static final long serialVersionUID = 1L;
    private List<Object> embeddables = new ArrayList<>();
    private Constructor<?> embeddableClassConstructor;
    private List<Field> embeddableClassFields;

    public EmbeddableListPanelTableModel(Class<?> embeddableClass, ReflectionFormBuilder reflectionFormBuilder) throws NoSuchMethodException {
        if(embeddableClass == null) {
            throw new IllegalArgumentException("embeddableClass mustn't be null");
        }
        this.embeddableClassConstructor = embeddableClass.getDeclaredConstructor();
        this.embeddableClassConstructor.setAccessible(true);
        this.embeddableClassFields = reflectionFormBuilder.retrieveRelevantFields(embeddableClass);
        for (Field embeddableClassField : embeddableClassFields) {
            this.addColumn(embeddableClassField.getName());
        }
    }

    @Override
    public List<Object> getData() {
        return Collections.unmodifiableList(embeddables);
    }

    @Override
    public void addColumn(String columnName) {
        super.addColumn(columnName);//should fire data change event
    }

    @Override
    public void removeRow(int row) {
        embeddables.remove(row);
        this.fireTableDataChanged();
    }

    @Override
    public void addElement(Object element) {
        this.embeddables.add(element);
        this.fireTableDataChanged();
    }

    @Override
    public void removeElement(Object element) {
        this.embeddables.remove(element);
        this.fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        if(this.embeddables == null) {
            //during initialization (this is inefficient, but due to the bad design of DefaultTableModel
            return 0;
        }
        return embeddables.size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return this.embeddableClassFields.get(columnIndex).getType();
    }

    /**
     *
     * @param rowIndex
     * @param columnIndex
     * @return always {@code false} because editing takes place with
     * {@link ReflectionFormPanel} in dialog
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            return this.embeddableClassFields.get(columnIndex).get(this.embeddables.get(rowIndex));
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        Object embeddable;
        if(rowIndex > this.embeddables.size()) {
            throw new IllegalArgumentException(String.format("rowIndex (%d) must not be larger than the size of embeddables (%d)", rowIndex, this.embeddables.size())); //if this becomes a problem introduce null padding
        }else if(rowIndex == this.embeddables.size()) {
            try {
                embeddable = embeddableClassConstructor.newInstance();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            this.embeddables.add(embeddable);
        }else {
            embeddable = this.embeddables.get(rowIndex);
        }
        try {
            this.embeddableClassFields.get(columnIndex).set(embeddable, aValue);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

}
