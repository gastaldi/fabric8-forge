/**
 *  Copyright 2005-2015 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.forge.devops.setup;

import io.fabric8.forge.addon.utils.MavenHelpers;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.maven.plugins.MavenPlugin;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.fabric8.forge.devops.setup.DockerSetupHelper.hasSpringBoot;
import static io.fabric8.forge.devops.setup.DockerSetupHelper.hasWildlySwarm;
import static io.fabric8.forge.devops.setup.Fabric8SetupStep.setupFabricMavenPlugin;

public class SetupProjectHelper {

    public static Set<Dependency> findCamelArtifacts(Project project) {
        Set<Dependency> answer = new LinkedHashSet<Dependency>();

        List<Dependency> dependencies = getDependencies(project);
        for (Dependency d : dependencies) {
            if ("org.apache.camel".equals(d.getCoordinate().getGroupId())) {
                answer.add(d);
            }
        }
        return answer;
    }


    public static boolean isFunktionParentPom(Project project) {
        MavenFacet mavenFacet = project.getFacet(MavenFacet.class);
        if (mavenFacet != null) {
            Model model = mavenFacet.getModel();
            if (model != null) {
                Parent parent = model.getParent();
                if (parent != null) {
                    String groupId = parent.getGroupId();
                    if (groupId != null && groupId.startsWith("io.fabric8.funktion")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isFabric8MavenPlugin3OrGreater(Project project) {
        MavenPlugin plugin = MavenHelpers.findPlugin(project, "io.fabric8", "fabric8-maven-plugin");
        if (plugin != null) {
            Coordinate coordinate = plugin.getCoordinate();
            if (coordinate != null) {
                String version = coordinate.getVersion();
                if (version != null) {
                    return !version.startsWith("1.") && !version.startsWith("2.");
                }
            }
        }
        return false;
    }

    public static boolean fabric8ProjectSetupCorrectly(Project project) {
        if (isFunktionParentPom(project) || isFabric8MavenPlugin3OrGreater(project)) {
            return true;
        }

        MavenPlugin plugin = MavenHelpers.findPlugin(project, "io.fabric8", "fabric8-maven-plugin");
        if (plugin != null) {
            return DockerSetupHelper.verifyDocker(project);
        }
        // For things like Spring Boot and Wildfly Swarm we should just default things!
        boolean springBoot = hasSpringBoot(project);
        boolean wildlySwarm = hasWildlySwarm(project);
        if (springBoot || wildlySwarm) {
            String fromImage = null;
            String main = null;
            DockerSetupHelper.setupDocker(project, fromImage, main);

            setupFabricMavenPlugin(project);
            plugin = MavenHelpers.findPlugin(project, "io.fabric8", "fabric8-maven-plugin");
            if (plugin != null) {
                return DockerSetupHelper.verifyDocker(project);
            }
        }
        return false;
    }


    public static List<Dependency> getDependencies(Project project) {
        DependencyFacet facet = project.getFacet(DependencyFacet.class);
        if (facet != null) {
            List<Dependency> answer = facet.getEffectiveDependencies();
            if (answer != null) {
                return answer;
            }
        }
        return Collections.EMPTY_LIST;
    }


}
