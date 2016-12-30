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

/**
 * Since the user ought to have maximal flexibility to set the initial query
 * displayed in a {@link QueryComponent} and it's potentially annoying to do
 * that with a template, this interface is used.
 *
 * @author richter
 */
public interface InitialQueryTextGenerator {

    /**
     * Creates a query of {@code entityClass} eventually restricting it to the
     * exact type of it or including subtypes dependening on whether
     * {@code forbidSubtypes} if {@code true} or {@code false}.
     *
     * @param entityClass
     * @param forbidSubtypes
     * @return the created query text
     */
    String generateInitialQueryText(Class<?> entityClass,
            boolean forbidSubtypes);

    public static String generateEntityClassQueryIdentifier(Class<?> entityClass) {
        String retValue = String.valueOf(Character.toLowerCase(entityClass.getSimpleName().charAt(0)));
        return retValue;
    }
}
