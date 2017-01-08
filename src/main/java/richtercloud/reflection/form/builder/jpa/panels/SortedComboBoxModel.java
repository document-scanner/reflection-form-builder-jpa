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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.DefaultComboBoxModel;

/*
internal implementation notes:
- due to the fact that the interface defines index based methods, a
PriorityQueue can't be used for item storage -> use a List and List.sort at
every model change
 */
public class SortedComboBoxModel<E> extends DefaultComboBoxModel<E> {

    private static final long serialVersionUID = 1L;
    private final List<E> items;
    private final Comparator<E> comparator;

    /*
    internal implementation notes:
    - comparator can only be assigned at instantiation of PriorityQueue, so
    it has to be set here
     */
    public SortedComboBoxModel(Comparator<E> comparator, List<E> items) {
        this.comparator = comparator;
        this.items = items;
        //don't just intialize with reference, but do what addElement would
        //do, but only sort items once
        Collections.sort(this.items, this.comparator);
        for (E item : items) {
            super.addElement(item);
        }
    }

    @Override
    public void addElement(E item) {
        this.items.add(item);
        sort();
        super.addElement(item);
    }

    @Override
    public void removeElement(Object obj) {
        this.items.remove(obj);
        super.removeElement(obj);
    }

    @Override
    public void insertElementAt(E item, int index) {
        this.items.add(index, item);
        sort();
        super.insertElementAt(item, index);
    }

    @Override
    public void removeElementAt(int index) {
        this.items.remove(index);
        super.removeElementAt(index);
    }

    @Override
    public int getSize() {
        return this.items.size();
    }

    @Override
    public E getElementAt(int index) {
        return this.items.get(index);
    }

    /*
    internal implementation notes:
    - must not be unmodifiable because Hibernate will invoke clear on it
     */
    public List<E> getItems() {
        return this.items;
    }

    public boolean contains(E element) {
        return this.items.contains(element);
    }

    public void sort() {
        Collections.sort(this.items, this.comparator);
    }
}
