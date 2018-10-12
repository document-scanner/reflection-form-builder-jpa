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
package de.richtercloud.reflection.form.builder.jpa.storage;

/**
 * An exception indicating that a storage start routine timed out <i>and</i>
 * that the storage process thus needs to be shutdown (other routines might not
 * need that). It should be caught, the shutdown be performed an be wrapped in a
 * {@link richtercloud.reflection.form.builder.storage.StorageCreationException}
 * which should then be thrown.
 *
 * @author richter
 */
public class ServerStartTimeoutException extends Exception {
    private static final long serialVersionUID = 1L;

    public ServerStartTimeoutException(String message) {
        super(message);
    }

    public ServerStartTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerStartTimeoutException(Throwable cause) {
        super(cause);
    }
}
