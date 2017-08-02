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
package richtercloud.reflection.form.builder.jpa;

import richtercloud.reflection.form.builder.jpa.sequence.SequenceManagementException;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;

/**
 * An ID generator which delegates sequence retrieval to a
 * {@link PersistenceStorage}.
 *
 * @author richter
 */
public class SequentialIdGenerator implements IdGenerator<Long> {
    public final static String SEQUENCE_NAME_DEFAULT = "sequential-id";
    private final String sequenceName = SEQUENCE_NAME_DEFAULT;
    private final PersistenceStorage<Long> storage;

    /**
     * Creates a new {@code SequentialIdGenerator}.
     * @param storage the persistence storage providing the sequence
     * @throws IdGenerationException
     */
    public SequentialIdGenerator(PersistenceStorage<Long> storage) throws IdGenerationException {
        this.storage = storage;
        init();
    }

    private void init() throws IdGenerationException {
        try {
            if(!storage.checkSequenceExists(sequenceName)) {
                storage.createSequence(sequenceName);
            }
        } catch (SequenceManagementException ex) {
            throw new IdGenerationException(ex);
        }
    }

    @Override
    public Long getNextId(Object instance) throws IdGenerationException {
        try {
            return storage.getNextSequenceValue(sequenceName);
        } catch (SequenceManagementException ex) {
            throw new IdGenerationException(ex);
        }
    }
}
