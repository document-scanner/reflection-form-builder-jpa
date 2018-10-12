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
package de.richtercloud.reflection.form.builder.jpa.sequence;

import de.richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;

/**
 *
 * @author richter
 */
public class DerbySequenceManager extends HibernateWrapperSequenceManager {

    public DerbySequenceManager(PersistenceStorage<Long> storage) {
        super(storage,
                0 //initialValue
        );
    }

    @Override
    public void createSequence(String sequenceName) throws SequenceManagementException {
        String sequenceName0 = escapeSequenceName(sequenceName,
                "\"",
                "\"");
        super.createSequence(sequenceName0);
    }

    @Override
    public Long getNextSequenceValue(String sequenceName) throws SequenceManagementException {
        String sequenceName0 = escapeSequenceName(sequenceName,
                "\"",
                "\"");
        return super.getNextSequenceValue(sequenceName0);
    }
}
