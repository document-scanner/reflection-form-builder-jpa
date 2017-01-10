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

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 *
 * @author richter
 */
public class QueryHistoryEntry implements Serializable, Comparable<QueryHistoryEntry>{
    private static final long serialVersionUID = 1L;
    private String text;
    private int usageCount;
    private Date lastUsage;

    protected QueryHistoryEntry() {
    }

    public QueryHistoryEntry(String text) {
        this(text,
                1,
                new Date());
    }

    public QueryHistoryEntry(String text, int usageCount, Date lastUsage) {
        this.text = text;
        this.usageCount = usageCount;
        this.lastUsage = lastUsage;
    }

    /**
     * @return the text
     */
    public String getText() {
        return this.text;
    }

    /**
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the usageCount
     */
    public int getUsageCount() {
        return this.usageCount;
    }

    /**
     * @param usageCount the usageCount to set
     */
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    /**
     * @return the lastUsage
     */
    public Date getLastUsage() {
        return this.lastUsage;
    }

    /**
     * @param lastUsage the lastUsage to set
     */
    public void setLastUsage(Date lastUsage) {
        this.lastUsage = lastUsage;
    }

    @Override
    public int compareTo(QueryHistoryEntry o) {
        return Integer.compare(this.getUsageCount(), o.getUsageCount());
    }

    @Override
    public String toString() {
        return this.text;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.text);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final QueryHistoryEntry other = (QueryHistoryEntry) obj;
        if (!Objects.equals(this.text, other.text)) {
            return false;
        }
        return true;
    }
}
