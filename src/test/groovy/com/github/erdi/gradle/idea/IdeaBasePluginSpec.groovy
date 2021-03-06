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

import groovy.xml.XmlUtil
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input

import static org.xmlunit.builder.Input.fromStream

class IdeaBasePluginSpec extends PluginSpec {

    final String pluginId = 'com.github.erdi.idea-base'

    def "applying plugin sets up vcs"() {
        given:
        applyPlugin()

        when:
        def iprXml = generateAndParseIdeaProjectConf()

        then:
        def vcsMapping = iprXml.component.find { it.@name == 'VcsDirectoryMappings' }.mapping.first()
        vcsMapping.@directory == ''
        vcsMapping.@vcs == 'Git'
    }

    def "applying plugin disables unlinked gradle project notification"() {
        given:
        applyPlugin()

        when:
        def iwsXml = generateAndParseIdeaWorkspaceConf()

        then:
        propertyValue(iwsXml, 'show.inlinked.gradle.project.popup') == 'false'
        propertyValue(iwsXml, 'show.unlinked.gradle.project.popup') == 'false'
    }

    def "applying plugin sets up a debug run configuration"() {
        given:
        applyPlugin()

        when:
        def runManager = generateAndParseRunManagerConf()

        then:
        !DiffBuilder.compare(debugRunConfigurationResourceInput)
                .withTest(nodeInput(runManager.configuration.find { it.@name == 'Debug' }))
                .ignoreWhitespace()
                .build()
                .hasDifferences()
    }

    def "applying plugin disables code and todo analysis upon committing"() {
        given:
        applyPlugin()

        when:
        def iwsXml = generateAndParseIdeaWorkspaceConf()

        then:
        !DiffBuilder.compare(vcsManagerConfigurationResourceInput)
                .withTest(nodeInput(iwsXml.component.find { it.@name == 'VcsManagerConfiguration' }))
                .ignoreWhitespace()
                .build()
                .hasDifferences()
    }

    def "applying plugin to subprojects does not cause errors"() {
        given:
        def subprojectBuildScript = new File(testProjectDir.newFolder(subprojectName), 'build.gradle')

        and:
        applyPlugin(subprojectBuildScript)
        settingsFile << """
            include ':$subprojectName'
        """

        when:
        runTask('idea')

        then:
        noExceptionThrown()

        where:
        subprojectName = 'subproject'
    }

    private Input.Builder nodeInput(Node node) {
        Input.fromString(XmlUtil.serialize(node))
    }

    private Input.Builder getDebugRunConfigurationResourceInput() {
        fromStream(getClass().getResourceAsStream('debug-run-configuration.xml'))
    }

    private Input.Builder getVcsManagerConfigurationResourceInput() {
        fromStream(getClass().getResourceAsStream('vcs-manager-configuration.xml'))
    }

    private void runIdeaProjectTask() {
        runTask('ideaProject')
    }

    private Node generateAndParseIdeaProjectConf() {
        runIdeaProjectTask()
        new XmlParser().parse(new File(testProjectDir.root, "${TEST_PROJECT_NAME}.ipr"))
    }

}
