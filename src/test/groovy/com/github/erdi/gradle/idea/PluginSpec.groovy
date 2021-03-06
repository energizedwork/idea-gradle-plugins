/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.erdi.gradle.idea

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

abstract class PluginSpec extends Specification {

    protected static final String TEST_PROJECT_NAME = 'idea-test'

    @Rule
    protected TemporaryFolder testProjectDir
    protected File buildScript
    protected File settingsFile

    abstract String getPluginId()

    void setup() {
        buildScript = testProjectDir.newFile('build.gradle')
        settingsFile = testProjectDir.newFile('settings.gradle')
        projectName = TEST_PROJECT_NAME
    }

    protected void setProjectName(String name) {
        settingsFile << """
            rootProject.name = '$name'
        """
    }

    protected BuildResult runTask(String taskName) {
        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(taskName)
                .withPluginClasspath()
                .build()
    }

    protected void applyPlugin(File buildScript = buildScript) {
        buildScript << """
            plugins {
                id '$pluginId'
            }
         """
    }

    protected BuildResult runIdeaWorkspaceTask() {
        runTask('ideaWorkspace')
    }

    protected Node generateAndParseIdeaWorkspaceConf() {
        runIdeaWorkspaceTask()
        new XmlParser().parse(new File(testProjectDir.root, "${TEST_PROJECT_NAME}.iws"))
    }

    protected Node generateAndParseRunManagerConf() {
        def node = generateAndParseIdeaWorkspaceConf()
        node.component.find { it.@name == 'RunManager' }
    }

    protected String propertyValue(Node xml, String name) {
        xml.component.find { it.@name == 'PropertiesComponent' }.property.find { it.@name == name }.@value
    }

}
