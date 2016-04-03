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

import java.util.List;

/**
 *
 * @author richter
 */
public class QueryComponentEvent<E> {
    /*
    internal implementation notes:
    - Has generic type `? extends E` rather than `E` in order to allow passing
    return value of Query.getResultList directly. This is for convenience only
    and can be changed back in case of trouble.
    */
    private final List<? extends E> queryResults;

    public QueryComponentEvent(List<? extends E> queryResults) {
        this.queryResults = queryResults;
    }

    public List<? extends E> getQueryResults() {
        return queryResults;
    }
}
