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
import richtercloud.reflection.form.builder.panels.ListPanelTableCellRenderer;
import richtercloud.reflection.form.builder.panels.ListPanelTableModel;

/**
 *
 * @author richter
 */
public class EmbeddableListPanelCellRenderer extends ListPanelTableCellRenderer<JLabel> {

    public EmbeddableListPanelCellRenderer() {
        super(new JLabel());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        ListPanelTableModel<Object> tableModel = (ListPanelTableModel<Object>) table.getModel();
        Object valueValue = tableModel.getValueAt(row, column);
        String displayValue = "";
        if(valueValue != null) {
            displayValue = valueValue.toString();
        }
        this.getComponent().setText(displayValue);
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

}
