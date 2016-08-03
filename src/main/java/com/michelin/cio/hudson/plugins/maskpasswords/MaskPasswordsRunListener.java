/*
 * The MIT License
 *
 * Copyright (c) 2010-2012, Manufacture Francaise des Pneumatiques Michelin,
 * Romain Seguy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.michelin.cio.hudson.plugins.maskpasswords;

//import com.michelin.cio.hudson.plugins.maskpasswords.MaskPasswordsBuildWrapper;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.listeners.RunListener;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.model.Run.RunnerAbortedException;
import hudson.util.DescribableList;
import hudson.tasks.BuildWrapper;
import hudson.model.Descriptor;
import hudson.model.Project;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension(ordinal = 1001) // JENKINS-12161
@SuppressWarnings("rawtypes")
public class MaskPasswordsRunListener extends RunListener<Run> {

  /**
   * If configured to do so, attempt to inject our build wrapper into ALL builds that don't
   * already have it. Note that this is intentionally fail-secure; i.e. exceptions are not
   * caught here, and if it fails for some reason, things will break.
   */
  @Override
  public Environment setUpEnvironment( AbstractBuild build, Launcher launcher, BuildListener listener ) throws IOException, InterruptedException, RunnerAbortedException {
    // check if we're configured to run globally
    MaskPasswordsConfig conf = MaskPasswordsConfig.getInstance();
    if (! conf.isEnabledGlobally()) {
      LOGGER.log(Level.FINE, "MaskPasswordsRunListener not enabled globally");
      return super.setUpEnvironment(build, launcher, listener);
    }
    // we don't want to actually fail builds because of bad code; try and log an exception if it fails...
    Project p = (Project)build.getProject();
    // find out if we already have the wrapper setup on this project
    boolean has_mask_passwords = false;
    DescribableList<BuildWrapper,Descriptor<BuildWrapper>> wrappers = p.getBuildWrappersList();
    for (BuildWrapper w: wrappers) {
      if (w.getClass() == MaskPasswordsBuildWrapper.class) {
        has_mask_passwords = true;
      }
    }
    // if we already have the wrapper, do nothing
    if (has_mask_passwords) {
      LOGGER.log(Level.FINE, "build {0} of project {1} already has MaskPasswordsBuildWrapper; doing nothing", new Object[]{build, p.getFullDisplayName()});
      return super.setUpEnvironment(build, launcher, listener);
    }
    // else inject it...
    LOGGER.log(Level.INFO, "build {0} of project {1} DOES NOT already have MaskPasswordsBuildWrapper; injecting it", new Object[]{build, p.getFullDisplayName()});
    wrappers.add(new MaskPasswordsBuildWrapper(null, null));
    p.save();
    return super.setUpEnvironment(build, launcher, listener);
  }

  private static final Logger LOGGER = Logger.getLogger(MaskPasswordsRunListener.class.getName());

}
