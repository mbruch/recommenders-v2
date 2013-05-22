/**
 * Copyright (c) 2010, 2013 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Olav Lenz - initial API and implementation
 */
package org.eclipse.recommenders.tests.models;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.recommenders.models.dependencies.DependencyInfo;
import org.eclipse.recommenders.models.dependencies.DependencyType;
import org.eclipse.recommenders.models.dependencies.rcp.EclipseDependencyListener;
import org.eclipse.recommenders.rcp.events.JavaModelEvents.JavaProjectClosed;
import org.eclipse.recommenders.rcp.events.JavaModelEvents.JavaProjectOpened;
import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.EventBus;

public class EclipseDependencyListenerTest {

    private static String PROJECT_NAME = "TestProject";

    private EventBus eventBus;
    private EclipseDependencyListener sut;

    private static int projectNumber = 0;

    private static String generateProjectName() {
        projectNumber++;
        return PROJECT_NAME + projectNumber;
    }

    private IJavaProject createProject(String projectName) throws Exception {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(projectName);
        project.create(null);
        project.open(null);
        IProjectDescription description = project.getDescription();
        description.setNatureIds(new String[] { JavaCore.NATURE_ID });
        project.setDescription(description, null);
        return JavaCore.create(project);
    }
    
    @Before
    public void init() throws IOException {
        eventBus = new EventBus("org.eclipse.recommenders.tests.models.rcp");
        sut = new EclipseDependencyListener(eventBus);
    }

    @Test
    public void testInitialWorkspaceParsing() throws Exception {
        String projectName = generateProjectName();
        createProject(projectName);
        
        EclipseDependencyListener sut = new EclipseDependencyListener(new EventBus(""));
        DependencyInfo expected = new DependencyInfo(new File("", projectName), DependencyType.PROJECT);

        assertTrue(sut.getDependencies().contains(expected));
    }

    @Test
    public void testThatProjectIsAddedCorrect() throws Exception {
        String projectName = generateProjectName();
        IJavaProject javaProject = createProject(projectName);
        eventBus.post(new JavaProjectOpened(javaProject));

        DependencyInfo expected = new DependencyInfo(new File("", projectName), DependencyType.PROJECT);

        assertTrue(sut.getDependencies().contains(expected));
    }
    
    @Test
    public void testThatProjectIsRemovedCorrectAfterClosing() throws Exception {
        String projectName = generateProjectName();
        IJavaProject javaProject = createProject(projectName);
        eventBus.post(new JavaProjectOpened(javaProject));
        eventBus.post(new JavaProjectClosed(javaProject));
        
        DependencyInfo notExpected = new DependencyInfo(new File("", projectName), DependencyType.PROJECT);

        assertFalse(sut.getDependencies().contains(notExpected));
    }

    @Test
    public void testThatDependencyForSpecificProjectIsASubsetOfAllDependencies() throws Exception{
        String projectName = generateProjectName();
        IJavaProject javaProject = createProject(projectName);
        eventBus.post(new JavaProjectOpened(javaProject));

        assertTrue(sut.getDependencies().containsAll(sut.getDependenciesForProject(javaProject))); 
    }

}
