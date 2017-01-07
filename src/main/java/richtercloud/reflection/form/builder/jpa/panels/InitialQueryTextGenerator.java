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
package richtercloud.reflection.form.builder.jpa.panels;

import java.util.List;

/**
 * Since the user ought to have maximal flexibility to set the initial query
 * displayed in a {@link QueryComponent} and it's potentially annoying to do
 * that with a template, this interface is used.
 *
 * @author richter
 */
public interface InitialQueryTextGenerator {

    /**
     * Creates interesting querys for {@code entityClass}. What interesting
     * means is up to implementations.
     *
     * The first element in the return value ought to be the query which will be
     * initially selected in {@link QueryComponent}. {@code forbidSubtypes}
     * determines whether this initially selected query is of the exact type of
     * {@code entityClass} or can include subtypes.
     *
     * @param entityClass
     * @param forbidSubtypes
     * @return the created query texts
     */
    List<String> generateInitialQueryTexts(Class<?> entityClass,
            boolean forbidSubtypes);

    public static String generateEntityClassQueryIdentifier(Class<?> entityClass) {
        String retValue = String.valueOf(Character.toLowerCase(entityClass.getSimpleName().charAt(0)));
        return retValue;
    }
}
