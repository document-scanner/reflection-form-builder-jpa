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
 *
 * @author richter
 */
public class DefaultInitialQueryTextGenerator implements InitialQueryTextGenerator {

    /**
     * This doesn't work with Hibernate 5.1.0 as JPA provider due to bug
     * https://hibernate.atlassian.net/browse/HHH-10653!
     *
     * Since it's possible to use {@link Class#getSimpleName() } to identify
     * classes it's not necessary to use parameters which provides queries which
     * are much more readable if plain text and simple names are used. Note that
     * JPA 2.1 query API and CriteriaBuilder API are seriously incapable of
     * retrieving the text of the query (both Query and TypedQuery) after it has
     * been created with parameters so that it'd be necessary to store
     * parameters like {@code entityClass} in {@link HistoryEntry}s which is
     * quite unelegant or keep the parameter escape string (e.g.
     * {@code :entityClass} in the query).
     *
     * @param entityManager
     * @param entityClass
     * @return
     */
    @Override
    public String generateInitialQueryText(Class<?> entityClass,
            boolean forbidSubtypes) {
        //Criteria API doesn't allow retrieval of string/text from objects
        //created with CriteriaBuilder, but text should be the first entry in
        //the query combobox -> construct String instead of using
        //CriteriaBuilder
        String entityClassQueryIdentifier = InitialQueryTextGenerator.generateEntityClassQueryIdentifier(entityClass);
        String retValue = String.format("SELECT %s FROM %s %s%s",
                entityClassQueryIdentifier,
                entityClass.getSimpleName(),
                entityClassQueryIdentifier,
                forbidSubtypes
                        ? String.format(" WHERE TYPE(%s) = %s",
                                entityClassQueryIdentifier,
                                entityClass.getSimpleName())
                        : "");
        return retValue;
    }
}
