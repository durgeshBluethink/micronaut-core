package org.particleframework.inject.field

import org.particleframework.context.Context
import org.particleframework.context.DefaultContext
import spock.lang.Specification

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by graemerocher on 12/05/2017.
 */
class FieldInjectionSpec extends Specification {


    void "test injection via setter with interface"() {
        given:
        Context context = new DefaultContext()
        context.start()

        when:"A bean is obtained that has a setter with @Inject"
        B b =  context.getBean(B)

        then:"The implementation is injected"
        b.a != null
    }

    static interface A {

    }

    @Singleton
    static class AImpl implements A {

    }

    static class B {
        @Inject
        private A a

        A getA() {
            return this.a
        }
    }

}

