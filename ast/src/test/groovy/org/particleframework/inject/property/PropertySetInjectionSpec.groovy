package org.particleframework.inject.property

import org.particleframework.context.Context
import org.particleframework.context.DefaultContext
import spock.lang.Specification

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by graemerocher on 12/05/2017.
 */
class PropertySetInjectionSpec extends Specification {
    void "test injection via setter that takes a set"() {
        given:
        Context context = new DefaultContext()
        context.start()

        when:
        B b =  context.getBean(B)

        then:
        b.all != null
        b.all.size() == 2
        b.all.contains(context.getBean(AImpl))
    }

    static interface A {

    }

    @Singleton
    static class AImpl implements A {

    }

    @Singleton
    static class AnotherImpl implements A {

    }

    static class B {
        @Inject
        Set<A> all
    }
}

