/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.core.annotation;

import io.micronaut.core.naming.Named;

/**
 * An annotated element is some point in the code, whether it be a constructor, field,
 * method etc. which provides annotation metadata.
 *
 * @author graemerocher
 * @since 1.2
 */
public interface AnnotatedElement extends AnnotationMetadataProvider, Named {
    /**
     * @return Whether the element is nullable.
     */
    default boolean isDeclaredNullable() {
        return getAnnotationMetadata().hasDeclaredStereotype(AnnotationUtil.NULLABLE) || getAnnotationMetadata().hasDeclaredStereotype(AnnotationUtil.JAVAX_NULLABLE);
    }

    /**
     * @return Whether the element is nullable.
     */
    default boolean isNullable() {
        return getAnnotationMetadata().hasStereotype(AnnotationUtil.NULLABLE) || getAnnotationMetadata().hasStereotype(AnnotationUtil.JAVAX_NULLABLE);
    }

    /**
     * @return Whether the element is declared as not being null
     */
    default boolean isNonNull() {
        return getAnnotationMetadata().hasStereotype(AnnotationUtil.NON_NULL) || getAnnotationMetadata().hasStereotype(AnnotationUtil.JAVAX_NON_NULL);
    }

    /**
     * @return Whether the element is declared as not being null
     */
    default boolean isDeclaredNonNull() {
        return getAnnotationMetadata().hasDeclaredStereotype(AnnotationUtil.NON_NULL) || getAnnotationMetadata().hasDeclaredStereotype(AnnotationUtil.JAVAX_NON_NULL);
    }
}
