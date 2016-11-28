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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.swing.JPanel;

/**
 *
 * @author richter
 */
public abstract class AbstractStringPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final EntityManager entityManager;
    private final int initialQueryLimit;
    private final Class<?> entityClass;
    private final String fieldName;
    private final Set<StringPanelUpdateListener> updateListeners = new HashSet<>();

    public AbstractStringPanel(EntityManager entityManager,
            Class<?> entityClass,
            String fieldName,
            int initialQueryLimit) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
        this.fieldName = fieldName;
        this.initialQueryLimit = initialQueryLimit;
    }

    public void addUpdateListener(StringPanelUpdateListener updateListener) {
        this.updateListeners.add(updateListener);
    }

    public void removeUpdateListener(StringPanelUpdateListener updateListener) {
        this.updateListeners.remove(updateListener);
    }

    public Set<StringPanelUpdateListener> getUpdateListeners() {
        return Collections.unmodifiableSet(updateListeners);
    }

    /**
     * Queries the database to check if there's entities which match the current
     * value of the text field with a LIKE query.
     * @param textFieldText
     * @return
     */
    /*
    internal implementation notes:
    - retrieving Query.getResultList is the shortest, maybe only way of getting
    the size of the result set
    */
    protected List<?> check(String textFieldText) {
        TypedQuery<?> query = entityManager.createQuery(generateQueryText(textFieldText),
                entityClass);
        query.setMaxResults(this.initialQueryLimit);
        return query.getResultList();
    }

    /**
     * Since there's no converter between text and criteria API we need to use
     * text everywhere.
     *
     * @param textFieldText
     * @return
     */
    protected String generateQueryText(String textFieldText) {
        //if there ever is a way to convert to text use the following saver
        //routine:
//        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
//        CriteriaQuery<T> c = cb.createQuery(this.entityClass);
//        Root<T> emp = c.from(this.entityClass);
//        c.select(emp);
//        List<Predicate> criteria = new ArrayList<>();
//        ParameterExpression<String> p = cb.parameter(String.class, this.fieldName);
//        criteria.add(cb.like(emp.<String>get(this.fieldName), p));
//        c.where(criteria.get(0));
//        TypedQuery<T> q = this.entityManager.createQuery(c);

        String entityClassQueryIdentifier = QueryComponent.generateEntityClassQueryIdentifier(entityClass);
        String retValue = String.format("SELECT %s from %s %s WHERE %s.%s LIKE '%s'",
                entityClassQueryIdentifier,
                entityClass.getSimpleName(),
                entityClassQueryIdentifier,
                entityClassQueryIdentifier,
                this.fieldName,
                "%"+textFieldText.replaceAll("'", "\\'")+"%" //specify % here in order to
                    //keep them output of the format string
        );
        return retValue;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getFieldName() {
        return fieldName;
    }

    /**
     * Resets the component.
     */
    public abstract void reset();
}
