/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squareup.eventbus.outside;

import com.squareup.eventbus.EventBus;
import com.squareup.eventbus.Subscribe;
import junit.framework.TestCase;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test that EventBus finds the correct handlers.
 *
 * This test must be outside the c.g.c.eventbus package to test correctly.
 *
 * @author Louis Wasserman
 */
@RunWith(Enclosed.class)
@SuppressWarnings("UnusedDeclaration")
public class AnnotatedHandlerFinderTest {

  private static final Object EVENT = new Object();

  abstract static class AbstractEventBusTest<H> extends TestCase {
    abstract H createHandler();

    private H handler;

    H getHandler() {
      return handler;
    }

    @Override
    protected void setUp() throws Exception {
      handler = createHandler();
      EventBus bus = new EventBus();
      bus.register(handler);
      bus.post(EVENT);
    }

    @Override
    protected void tearDown() throws Exception {
      handler = null;
    }
  }

  /*
   * We break the tests up based on whether they are annotated or abstract in the superclass.
   */
  public static class BaseHandlerFinderTest
      extends AbstractEventBusTest<BaseHandlerFinderTest.Handler> {
    static class Handler {
      final List<Object> nonSubscriberEvents = new ArrayList<Object>();
      final List<Object> subscriberEvents = new ArrayList<Object>();

      public void notASubscriber(Object o) {
        nonSubscriberEvents.add(o);
      }

      @Subscribe
      public void subscriber(Object o) {
        subscriberEvents.add(o);
      }
    }

    public void testNonSubscriber() {
      assertThat(getHandler().nonSubscriberEvents).isEmpty();
    }

    public void testSubscriber() {
      assertThat(getHandler().subscriberEvents).containsExactly(EVENT);
    }

    @Override Handler createHandler() {
      return new Handler();
    }
  }

  public static class AbstractNotAnnotatedInSuperclassTest
      extends AbstractEventBusTest<AbstractNotAnnotatedInSuperclassTest.SubClass> {
    abstract static class SuperClass {
      public abstract void overriddenInSubclassNowhereAnnotated(Object o);

      public abstract void overriddenAndAnnotatedInSubclass(Object o);
    }

    static class SubClass extends SuperClass {
      final List<Object> overriddenInSubclassNowhereAnnotatedEvents = new ArrayList<Object>();
      final List<Object> overriddenAndAnnotatedInSubclassEvents = new ArrayList<Object>();

      @Override
      public void overriddenInSubclassNowhereAnnotated(Object o) {
        overriddenInSubclassNowhereAnnotatedEvents.add(o);
      }

      @Subscribe @Override
      public void overriddenAndAnnotatedInSubclass(Object o) {
        overriddenAndAnnotatedInSubclassEvents.add(o);
      }
    }

    public void testOverriddenAndAnnotatedInSubclass() {
      assertThat(getHandler().overriddenAndAnnotatedInSubclassEvents).containsExactly(EVENT);
    }

    public void testOverriddenInSubclassNowhereAnnotated() {
      assertThat(getHandler().overriddenInSubclassNowhereAnnotatedEvents).isEmpty();
    }

    @Override SubClass createHandler() {
      return new SubClass();
    }
  }

  public static class NeitherAbstractNorAnnotatedInSuperclassTest
      extends AbstractEventBusTest<NeitherAbstractNorAnnotatedInSuperclassTest.SubClass> {
    static class SuperClass {
      final List<Object> neitherOverriddenNorAnnotatedEvents = new ArrayList<Object>();
      final List<Object> overriddenInSubclassNowhereAnnotatedEvents = new ArrayList<Object>();
      final List<Object> overriddenAndAnnotatedInSubclassEvents = new ArrayList<Object>();

      public void neitherOverriddenNorAnnotated(Object o) {
        neitherOverriddenNorAnnotatedEvents.add(o);
      }

      public void overriddenInSubclassNowhereAnnotated(Object o) {
        overriddenInSubclassNowhereAnnotatedEvents.add(o);
      }

      public void overriddenAndAnnotatedInSubclass(Object o) {
        overriddenAndAnnotatedInSubclassEvents.add(o);
      }
    }

    static class SubClass extends SuperClass {
      @Override
      public void overriddenInSubclassNowhereAnnotated(Object o) {
        super.overriddenInSubclassNowhereAnnotated(o);
      }

      @Subscribe @Override
      public void overriddenAndAnnotatedInSubclass(Object o) {
        super.overriddenAndAnnotatedInSubclass(o);
      }
    }

    public void testNeitherOverriddenNorAnnotated() {
      assertThat(getHandler().neitherOverriddenNorAnnotatedEvents).isEmpty();
    }

    public void testOverriddenInSubclassNowhereAnnotated() {
      assertThat(getHandler().overriddenInSubclassNowhereAnnotatedEvents).isEmpty();
    }

    public void testOverriddenAndAnnotatedInSubclass() {
      assertThat(getHandler().overriddenAndAnnotatedInSubclassEvents).containsExactly(EVENT);
    }

    @Override SubClass createHandler() {
      return new SubClass();
    }
  }

  public static class FailsOnInterfaceSubscription extends TestCase {

    static class InterfaceSubscriber {
      @Subscribe public void whatever(Comparable thingy) {
        // Do nothing.
      }
    }

    public void testSubscribingToInterfacesFails() {
      try {
        new EventBus().register(new InterfaceSubscriber());
        fail("Annotation finder allowed subscription to illegal interface type.");
      } catch (IllegalArgumentException expected) {
        // Do nothing.
      }
    }
  }

}
