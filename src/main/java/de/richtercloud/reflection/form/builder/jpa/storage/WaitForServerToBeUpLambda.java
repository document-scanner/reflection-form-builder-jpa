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

import java.io.IOException;

/**
 *
 * @author richter
 */
@FunctionalInterface
public interface WaitForServerToBeUpLambda {

    /*
    internal implementation notes:
    - in order to allow callers to get the last exception which caused the test
    to result in false and which probably contains very valuable information
    and allow tests which aren't based on an exception, it's a compomise to make
    the return value an Exception instead of a boolean because one can just
    throw Exception(String) because an explanation is needed anyway and this way
    the interface can be a functional interface
    */
    /**
     * Performs the repeatable check whether the server is up/reachable/running
     * which is e.g. performed in
     * {@link AbstractProcessPersistenceStorage#waitForServerToBeUp(richtercloud.reflection.form.builder.jpa.storage.WaitForServerToBeUpLambda, java.lang.String) }
     *
     * @return the exception which indicates which was the cause for the failure
     *     of the connection check or {@code null} if the check was successful
     *     and callers can assume that the check loop can be left
     * @throws InterruptedException if such an exception occurs
     * @throws IOException if an I/O exception occurs during communication with
     *     the process to wait for
     */
    Exception run() throws InterruptedException, IOException;
}
