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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.jpa.sequence.SequenceManagementException;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;

/**
 *
 * @author richter
 */
public class SequentialIdGenerator implements IdGenerator<Long> {
    private final static Logger LOGGER = LoggerFactory.getLogger(SequentialIdGenerator.class);
    public final static String SEQUENCE_NAME_DEFAULT = "sequential-id";
    private final String sequenceName = SEQUENCE_NAME_DEFAULT;
    private final PersistenceStorage<Long> sequenceManager;

    /**
     * Creates a new {@code SequentialIdGenerator}.
     * @param sequenceManager it's recommended to use the sequence manager of
     * the storage used in the application which can be retrieved with
     * {@link PersistenceStorage#getSequenceManager() }
     * @throws IdGenerationException
     */
    public SequentialIdGenerator(PersistenceStorage<Long> sequenceManager) throws IdGenerationException {
        this.sequenceManager = sequenceManager;
        init();
    }

    private void init() throws IdGenerationException {
        try {
            if(!sequenceManager.checkSequenceExists(sequenceName)) {
                sequenceManager.createSequence(sequenceName);
            }
        } catch (SequenceManagementException ex) {
            throw new IdGenerationException(ex);
        }
    }

    @Override
    public Long getNextId(Object instance) throws IdGenerationException {
        try {
            return sequenceManager.getNextSequenceValue(sequenceName);
        } catch (SequenceManagementException ex) {
            throw new IdGenerationException(ex);
        }
    }
}
