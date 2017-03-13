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
 * Stores {@link QueryHistoryEntry}s unreliably, i.e. implementations must not
 * guarantee that the a once stored entry can be retrieved which avoids
 * cluttering up the storage.
 *
 * If previous queries ought to be deleted for privacy reasons or because the
 * initial look of the application ought to be the same, provide an empty
 * storage or create a new one and add default entries programmatically.
 *
 * @author richter
 */
public interface QueryHistoryEntryStorage {

    /**
     * Stores {@code entry}. What that means is up to implementations. If an
     * entry already exist which has the same query text as {@code entry} its
     * properties {@code usageCount} and {@code lastUsage} ought to be updated
     * instead of storing a copy of the entry.
     * @param clazz the class to store for
     * @param entry the entry to store
     * @throws richtercloud.reflection.form.builder.jpa.panels.QueryHistoryEntryStorageException
     */
    void store(Class<?> clazz,
            QueryHistoryEntry entry) throws QueryHistoryEntryStorageException;

    /**
     * Retrieves the list of stored entries for {@code clazz}.
     * @param clazz the class to retrieve for
     * @return the list of stored entries
     */
    List<QueryHistoryEntry> retrieve(Class<?> clazz);

    /**
     * Get the entry which ought to be selected initially in
     * {@link QueryComponent}.
     * @param clazz the class to get the entry for
     * @return the entry to be initially selected or {@code null} if the initial
     * selection doesn't matter
     */
    QueryHistoryEntry getInitialEntry(Class<?> clazz);

    void shutdown();
}
