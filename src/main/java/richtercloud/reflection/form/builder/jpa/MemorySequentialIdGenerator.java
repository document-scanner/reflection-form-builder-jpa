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

/**
 * Generates IDs starting from 0 at every start of the JVM. This should be used
 * for testing purposes only which involve entities which are deleted after the
 * test(s) end.
 *
 * This class is a singleton and a reference ought to be retrieved through
 * {@link #getInstance() }.
 *
 * @author richter
 */
public class MemorySequentialIdGenerator implements IdGenerator<Long> {
    private final static MemorySequentialIdGenerator INSTANCE = new MemorySequentialIdGenerator();

    public static MemorySequentialIdGenerator getInstance() {
        return INSTANCE;
    }
    private long nextId = 0;

    protected MemorySequentialIdGenerator() {
    }

    /**
     * Returns the next id value, regardless which type is passed as
     * {@code clazz} argument.
     * @param instance
     * @return
     */
    @Override
    public Long getNextId(Object instance) {
        this.nextId += 1;
        return nextId;
    }
}
