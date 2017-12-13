/*
 * Copyright (C) 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.swift.generator.template;

import com.diffblue.deeptestutils.Reflector;

import org.testng.annotations.Test;
import org.testng.Assert;

import java.lang.reflect.Method;

public class TemplateContextGeneratorTest {

  /*
   * This test case covers:
   * conditional line 267 branch to line 268
   */

  @Test
  public void com_facebook_swift_generator_template_TemplateContextGenerator_nameStartsWithAcronym_6b660d252b2b90ac_000() throws Throwable {

    boolean retval;
    {
      /* Arrange */
      String name = "";

      /* Act */
      Class<?> c = Reflector.forName("com.facebook.swift.generator.template.TemplateContextGenerator");
      Method m = c.getDeclaredMethod("nameStartsWithAcronym", Reflector.forName("java.lang.String"));
      m.setAccessible(true);
      retval = (boolean) m.invoke(null, name);
    }
    {
      /* Assert */
      Assert.assertEquals(false, retval);
    }
  }

  /*
   * This test case covers:
   * conditional line 267 branch to line 270
   * conditional line 270 branch to line 270
   * conditional line 270 branch to line 271
   */

  @Test
	  public void com_facebook_swift_generator_template_TemplateContextGenerator_nameStartsWithAcronym_6b660d252b2b90ac_001() throws Throwable {

    boolean retval;
    {
      /* Arrange */
      String param_1 = "BA";
      String name = param_1;

      /* Act */
      Class<?> c = Reflector.forName("com.facebook.swift.generator.template.TemplateContextGenerator");
      Method m = c.getDeclaredMethod("nameStartsWithAcronym", Reflector.forName("java.lang.String"));
      m.setAccessible(true);
      retval = (boolean) m.invoke(null, name);
    }
    {
      /* Assert */
      Assert.assertEquals(true, retval);
    }
  }

  /*
   * This test case covers:
   * conditional line 267 branch to line 270
   * conditional line 270 branch to line 270
   * conditional line 270 branch to line 273
   */

  @Test
  public void com_facebook_swift_generator_template_TemplateContextGenerator_nameStartsWithAcronym_6b660d252b2b90ac_002() throws Throwable {

    boolean retval;
    {
      /* Arrange */
      String param_1 = "bA";
      String name = param_1;

      /* Act */
      Class<?> c = Reflector.forName("com.facebook.swift.generator.template.TemplateContextGenerator");
      Method m = c.getDeclaredMethod("nameStartsWithAcronym", Reflector.forName("java.lang.String"));
      m.setAccessible(true);
      retval = (boolean) m.invoke(null, name);
    }
    {
      /* Assert */
      Assert.assertEquals(false, retval);
    }
  }
}
