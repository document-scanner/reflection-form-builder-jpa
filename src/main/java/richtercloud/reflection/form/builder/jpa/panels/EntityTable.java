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

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

/**
 *
 * @author richter
 * @param <E> the entity type
 */
public class EntityTable<E> extends JTable {

    private static final long serialVersionUID = 1L;

    public EntityTable(EntityTableModel<E> dm) {
        super(dm);
        this.setDefaultRenderer(Object.class,
                new EntityTableCellRenderer());
    }

    @Override
    public void setModel(TableModel dataModel) {
        if(!(dataModel instanceof EntityTableModel)) {
            throw new IllegalArgumentException(String.format("dataModel has to be a %s", EntityTableModel.class.getName()));
        }
        super.setModel(dataModel);
    }

    @Override
    public EntityTableModel<E> getModel() {
        return (EntityTableModel<E>) super.getModel();
    }

    private class EntityTableCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel retValue = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            assert retValue != null;
            if(retValue.getText() != null && !retValue.getText().isEmpty()) {
                retValue.setToolTipText(retValue.getText());
            }
            return retValue;
        }
    }
}
