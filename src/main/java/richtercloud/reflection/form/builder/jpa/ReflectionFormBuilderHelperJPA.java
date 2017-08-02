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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import richtercloud.validation.tools.FieldRetriever;

/**
 *
 * @author richter
 */
public class ReflectionFormBuilderHelperJPA {

    /**
     * Checks every field of the type of every field of {@code entityClass} if
     * it's assignable from (i.e. a superclass) of {@code entityClass} so that
     * it can be assumed that a relationship can be defined (this avoids a
     * reference to a declaring class being passed to the constructor and thus
     * arbitrary nesting of type handling).
     *
     * Only JPA-annotated field are used whereas it'd be possbile to check based
     * on field types as well. The implementation naively assumes that generic
     * types and targetEntity attributes of annotations are correct and
     * validated by JPA providers.
     *
     * @param entityClass
     * @param entityClassFields
     * @param fieldRetriever
     * @return
     */
    public static Set<Field> retrieveMappedFieldCandidates(Class<?> entityClass,
            List<Field> entityClassFields,
            FieldRetriever fieldRetriever) {
        Set<Field> retValue = new HashSet<>();
        for(Field entityClassField : entityClassFields) {
            OneToMany entityClassFieldOneToMany = entityClassField.getAnnotation(OneToMany.class);
            ManyToMany entityClassFieldManyToMany = entityClassField.getAnnotation(ManyToMany.class);
            if(entityClassFieldOneToMany != null || entityClassFieldManyToMany != null) {
                Class<?> entityClassFieldType = null;
                if(entityClassFieldOneToMany != null) {
                    Class<?> targetEntity = entityClassFieldOneToMany.targetEntity();
                    if(targetEntity != null) {
                        if(!targetEntity.equals(void.class)) {
                            //if targetEntity isn't specified it is void for
                            //some reason
                            entityClassFieldType = targetEntity;
                        }
                    }
                }
                if(entityClassFieldManyToMany != null) {
                    Class<?> targetEntity = entityClassFieldManyToMany.targetEntity();
                    if(targetEntity != null) {
                        if(!targetEntity.equals(void.class)) {
                            //if targetEntity isn't specified it is void for
                            //some reason
                            entityClassFieldType = targetEntity;
                        }
                    }
                }
                if(List.class.isAssignableFrom(entityClassField.getType())) {
                    Type entityClassFieldListType = entityClassField.getGenericType();
                    if(!(entityClassFieldListType instanceof ParameterizedType)) {
                        throw new IllegalArgumentException(String.format("field %s isn't declared as parameterized type and doesn't have a target annotation, can't handle field", entityClassField));
                    }
                    ParameterizedType entityClassFieldListParameterizedType = (ParameterizedType) entityClassFieldListType;
                    Type[] entityClassFieldListParameterizedTypeArguments = entityClassFieldListParameterizedType.getActualTypeArguments();
                    if(entityClassFieldListParameterizedTypeArguments.length == 0) {
                        throw new IllegalArgumentException();
                    }
                    if(entityClassFieldListParameterizedTypeArguments.length > 1) {
                        throw new IllegalArgumentException();
                    }
                    if(!(entityClassFieldListParameterizedTypeArguments[0] instanceof Class)) {
                        throw new IllegalArgumentException();
                    }
                    Class<?> entityClassFieldFieldParameterizedTypeArgument = (Class<?>) entityClassFieldListParameterizedTypeArguments[0];
                    entityClassFieldType = entityClassFieldFieldParameterizedTypeArgument;
                }else {
                    throw new IllegalArgumentException(String.format("collection type %s of field %s not supported", entityClassField.getType(), entityClassField));
                }

                for(Field entityClassFieldField : fieldRetriever.retrieveRelevantFields(entityClassFieldType)) {
                    //OneToOne and OneToMany don't make sense
                    ManyToOne entityClassFieldFieldManyToOne = entityClassFieldField.getAnnotation(ManyToOne.class);
                    ManyToMany entityClassFieldFieldManyToMany = entityClassFieldField.getAnnotation(ManyToMany.class);
                    if(entityClassFieldFieldManyToOne != null || entityClassFieldFieldManyToMany != null) {
                        if(entityClassFieldFieldManyToOne != null) {
                            Class<?> targetEntity = entityClassFieldFieldManyToOne.targetEntity();
                            if(targetEntity != null) {
                                if(!targetEntity.equals(void.class)) {
                                    retValue.add(entityClassField);
                                    continue;
                                }
                            }
                            Class<?> entityClassFieldFieldType = entityClassFieldField.getType();
                            if(entityClassFieldType.isAssignableFrom(entityClassFieldFieldType)) {
                                retValue.add(entityClassField);
                            }
                        }
                        if(entityClassFieldFieldManyToMany != null) {
                            Class<?> targetEntity = entityClassFieldFieldManyToMany.targetEntity();
                            if(targetEntity != null) {
                                if(!targetEntity.equals(void.class)) {
                                    retValue.add(entityClassField);
                                    continue;
                                }
                            }
                            if(List.class.isAssignableFrom(entityClassField.getType())) {
                                Type entityClassFieldListType = entityClassField.getGenericType();
                                if(!(entityClassFieldListType instanceof ParameterizedType)) {
                                    throw new IllegalArgumentException(String.format("field %s isn't declared as parameterized type and doesn't have a target annotation, can't handle field", entityClassField));
                                }
                                ParameterizedType entityClassFieldListParameterizedType = (ParameterizedType) entityClassFieldListType;
                                Type[] entityClassFieldListParameterizedTypeArguments = entityClassFieldListParameterizedType.getActualTypeArguments();
                                if(entityClassFieldListParameterizedTypeArguments.length == 0) {
                                    throw new IllegalArgumentException();
                                }
                                if(entityClassFieldListParameterizedTypeArguments.length > 1) {
                                    throw new IllegalArgumentException();
                                }
                                if(!(entityClassFieldListParameterizedTypeArguments[0] instanceof Class)) {
                                    throw new IllegalArgumentException();
                                }
                                Class<?> entityClassFieldFieldParameterizedTypeArgument = (Class<?>) entityClassFieldListParameterizedTypeArguments[0];
                                Class<?> entityClassFieldFieldType = entityClassFieldFieldParameterizedTypeArgument;
                                if(entityClassFieldType.isAssignableFrom(entityClassFieldFieldType)) {
                                    retValue.add(entityClassField);
                                }
                            }else {
                                throw new IllegalArgumentException(String.format("collection type %s of field %s not supported", entityClassField.getType(), entityClassField));
                            }
                        }
                    }
                }
            }
        }
        return retValue;
    }

    /**
     * Checks both the {@code entityClass} fields' annotations and the mapped
     * field candidates annotations for XToMany annoations with {@code mappedBy}
     * attribute.
     *
     * @param entityClassFields
     * @param mappedFieldCandidates
     * @return
     */
    public static Field retrieveMappedByFieldListPanel(List<Field> entityClassFields,
            Set<Field> mappedFieldCandidates) {
        //check entityClass fields' annotations
        for(Field entityClassField : entityClassFields) {
            if(checkMappedByField(entityClassField)) {
                return entityClassField;
            }
        }
        //check mapped field candidates annotations
        for(Field mappedFieldCandidate : mappedFieldCandidates) {
            if(checkMappedByField(mappedFieldCandidate)) {
                return mappedFieldCandidate;
            }
        }
        return null;
    }

    /**
     * Checks {@code field} for specified {@code mappedBy} parameters of
     * x-to-many relationship annotations.
     * @param field
     * @return {@code true} if {@code field} contains a {@link OneToMany} or
     * {@link ManyToMany} annotation with a non-empty {@code mappedBy} parameter
     */
    private static boolean checkMappedByField(Field field) {
        OneToMany entityClassFieldOneToMany = field.getAnnotation(OneToMany.class);
        //ManyToOne doesn't have a mappedBy field, but it needs to be
        //checked to be offered for a user-defined mapping
        ManyToMany entityClassFieldManyToMany = field.getAnnotation(ManyToMany.class);
        if(entityClassFieldOneToMany != null) {
            String mappedBy = entityClassFieldOneToMany.mappedBy();
            if(mappedBy != null && !mappedBy.isEmpty()) {
                //if mappedBy is specified the user isn't given a choice
                return true;
            }
        }else if(entityClassFieldManyToMany != null) {
            String mappedBy = entityClassFieldManyToMany.mappedBy();
            if(mappedBy != null && !mappedBy.isEmpty()) {
                //if mappedBy is specified the user isn't given a choice
                return true;
            }
        }
        return false;
    }

    private ReflectionFormBuilderHelperJPA() {
    }
}
