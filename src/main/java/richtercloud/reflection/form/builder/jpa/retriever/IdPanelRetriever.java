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
package richtercloud.reflection.form.builder.jpa.retriever;

import richtercloud.reflection.form.builder.jpa.IdPanel;
import richtercloud.reflection.form.builder.retriever.ValueRetriever;

/**
 *
 * @author richter
 */
public class IdPanelRetriever implements ValueRetriever<Long, IdPanel> {
    private final static IdPanelRetriever INSTANCE = new IdPanelRetriever();

    public static IdPanelRetriever getInstance() {
        return INSTANCE;
    }

    protected IdPanelRetriever() {
    }

    @Override
    public Long retrieve(IdPanel comp) {
        int retValueLong = (Integer)comp.getIdSpinner().getValue(); //@TODO: consider
            //development of parameterized OpenJDK SpinnerModel
        return (long)retValueLong;
    }

}
