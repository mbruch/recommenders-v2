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
package org.eclipse.recommenders.models.dependencies.rcp;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.recommenders.models.dependencies.DependencyInfo;
import org.eclipse.recommenders.models.dependencies.DependencyType;
import org.eclipse.recommenders.models.dependencies.IDependencyListener;
import org.eclipse.recommenders.rcp.events.JavaModelEvents.JarPackageFragmentRootAdded;
import org.eclipse.recommenders.rcp.events.JavaModelEvents.JarPackageFragmentRootRemoved;
import org.eclipse.recommenders.rcp.events.JavaModelEvents.JavaProjectClosed;
import org.eclipse.recommenders.rcp.events.JavaModelEvents.JavaProjectOpened;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@SuppressWarnings("restriction")
public class EclipseDependencyListener implements IDependencyListener<IJavaProject> {

    private final Map<IJavaProject, Set<DependencyInfo>> workspaceDependenciesByProject = Maps.newHashMap();
    private final Map<IJavaProject, Set<IPackageFragmentRoot>> jrePackageFragmentRoots = Maps.newHashMap();

    public EclipseDependencyListener(EventBus bus) {
        bus.register(this);
        parseWorkspaceForDependencies();
    }

    private void parseWorkspaceForDependencies() {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject project : projects) {
            parseProjectForDependencies(project);
        }
    }

    private void parseProjectForDependencies(IProject project) {
        try {
            if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
                IJavaProject javaProject = JavaCore.create(project);
                registerDependenciesForJavaProject(javaProject);
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onEvent(final JavaProjectOpened e) {
        registerDependenciesForJavaProject(e.project);
    }

    @Subscribe
    public void onEvent(final JavaProjectClosed e) {
        deregisterDependenciesForJavaProject(e.project);
    }

    @Subscribe
    public void onEvent(final JarPackageFragmentRootAdded e) {
        registerDependencyForJAR(e.root);
    }

    @Subscribe
    public void onEvent(final JarPackageFragmentRootRemoved e) {
        deregisterDependencyForJAR(e.root);
    }

    private void registerDependenciesForJavaProject(IJavaProject javaProject) {
        Set<DependencyInfo> newDependencies = Sets.newHashSet();
        newDependencies.add(createDependencyInfoForProject(javaProject));
        Optional<DependencyInfo> optionalJREDependencyInfo = createJREDependencyInfo(javaProject);
        if (optionalJREDependencyInfo.isPresent()) {
            newDependencies.add(optionalJREDependencyInfo.get());
        }
        Set<IPackageFragmentRoot> jreRoots = detectJREPackageFragementRoots(javaProject);
        jrePackageFragmentRoots.put(javaProject, jreRoots);
        newDependencies.addAll(searchForAllDependencyiesOfProject(javaProject));
        storeDependencyInfos(newDependencies, javaProject);
    }

    private Set<DependencyInfo> searchForAllDependencyiesOfProject(IJavaProject javaProject) {
        Set<DependencyInfo> dependencies = Sets.newHashSet();
        Set<IPackageFragmentRoot> jreRoots = jrePackageFragmentRoots.get(javaProject);
        try {

            for (final IPackageFragmentRoot packageFragmetRoot : javaProject.getAllPackageFragmentRoots()) {
                if (jreRoots == null || !jreRoots.contains(packageFragmetRoot)) {
                    if (packageFragmetRoot instanceof JarPackageFragmentRoot) {
                        DependencyInfo dependencyInfo = createDependencyInfoForJAR((JarPackageFragmentRoot) packageFragmetRoot);
                        dependencies.add(dependencyInfo);
                    }
                }
            }
        } catch (JavaModelException e1) {
            e1.printStackTrace();
        }
        return dependencies;
    }

    private Set<IPackageFragmentRoot> detectJREPackageFragementRoots(IJavaProject javaProject) {
        // Please notice that this is a heuristic to detect if a Jar is part of the JRE or not.
        // All Jars in the JRE_Container which are not located in the ext folder are defined as part of the JRE
        Set<IPackageFragmentRoot> jreRoots = new HashSet<IPackageFragmentRoot>();
        try {
            for (IClasspathEntry entry : javaProject.getRawClasspath()) {
                if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                    if (entry.getPath().toString().contains("org.eclipse.jdt.launching.JRE_CONTAINER")) {
                        for (IPackageFragmentRoot packageFragmentRoot : javaProject.findPackageFragmentRoots(entry)) {
                            if (!packageFragmentRoot.getPath().toFile().getParentFile().getName().equals("ext")) {
                                jreRoots.add(packageFragmentRoot);
                            }
                        }
                    }
                }
            }
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
        return jreRoots;
    }

    private DependencyInfo createDependencyInfoForProject(final IJavaProject project) {
        File file = project.getPath().toFile();
        DependencyInfo dependencyInfo = new DependencyInfo(file, DependencyType.PROJECT);
        return dependencyInfo;
    }

    private Optional<DependencyInfo> createJREDependencyInfo(IJavaProject javaProject) {
        try {
            IVMInstall vmInstall = JavaRuntime.getVMInstall(javaProject);
            if (vmInstall instanceof IVMInstall2) {
                IVMInstall2 vmInstall2 = (IVMInstall2) vmInstall;
                String version = vmInstall2.getJavaVersion();
                File javaHome = vmInstall.getInstallLocation();
                Map<String, String> attributes = Maps.newHashMap();
                attributes.put(DependencyInfo.JRE_VERSION_IDE, version);
                DependencyInfo jreDependencyInfo = new DependencyInfo(javaHome, DependencyType.JRE, attributes);
                return fromNullable(jreDependencyInfo);
            }
        } catch (CoreException e) {
            return absent();
        }
        return absent();
    }

    private void deregisterDependenciesForJavaProject(IJavaProject javaProject) {
        workspaceDependenciesByProject.remove(javaProject);
    }

    private void registerDependencyForJAR(final JarPackageFragmentRoot root) {
        Optional<IJavaProject> optionalJavaProject = getIJavaProjectForPackageFragmentRoot(root);
        if (!optionalJavaProject.isPresent()) {
            return;
        }

        IJavaProject javaProject = optionalJavaProject.get();
        if (!isJREOfProjectIsKnown(javaProject)) {
            workspaceDependenciesByProject.remove(javaProject);
            registerDependenciesForJavaProject(javaProject);
        } else {
            if (!isPartOfTheJRE(root)) {
                DependencyInfo dependencyInfo = createDependencyInfoForJAR(root);
                storeDependencyInfo(dependencyInfo, optionalJavaProject.get());
            }
        }
    }

    private boolean isJREOfProjectIsKnown(IJavaProject JavaProject) {
        for (DependencyInfo dependencyInfo : workspaceDependenciesByProject.get(JavaProject)) {
            if (dependencyInfo.getType() == DependencyType.JRE) {
                return true;
            }
        }
        return false;
    }

    private DependencyInfo createDependencyInfoForJAR(JarPackageFragmentRoot root) {
        File file = root.getPath().toFile();
        DependencyInfo dependencyInfo = new DependencyInfo(file, DependencyType.JAR);
        return dependencyInfo;
    }

    private void deregisterDependencyForJAR(final JarPackageFragmentRoot root) {
        Optional<IJavaProject> optionalJavaProject = getIJavaProjectForPackageFragmentRoot(root);
        if (!optionalJavaProject.isPresent()) {
            return;
        }
        IJavaProject javaProject = optionalJavaProject.get();
        if (isPartOfTheJRE(root)) {
            deregisterJREDependenciesForProject(javaProject);
        } else {
            DependencyInfo dependencyInfo = createDependencyInfoForJAR(root);
            removeDependencyInfo(dependencyInfo, javaProject);
        }
    }

    private boolean isPartOfTheJRE(IPackageFragmentRoot packageFragmentRoot) {
        Optional<IJavaProject> optionalJavaProject = getIJavaProjectForPackageFragmentRoot(packageFragmentRoot);
        if (optionalJavaProject.isPresent()) {
            Set<IPackageFragmentRoot> set = jrePackageFragmentRoots.get(optionalJavaProject.get());
            if (set.contains(packageFragmentRoot)) {
                return true;
            }
        }
        return false;
    }

    private void deregisterJREDependenciesForProject(IJavaProject javaProject) {
        Set<DependencyInfo> newDependencies = Sets.newHashSet();
        Set<DependencyInfo> oldDependencies = workspaceDependenciesByProject.get(javaProject);
        if (oldDependencies == null){
            return;
        }
        for (DependencyInfo dependencyInfo : oldDependencies) {
            if (dependencyInfo.getType() != DependencyType.JRE) {
                newDependencies.add(dependencyInfo);
            }
        }
        workspaceDependenciesByProject.put(javaProject, newDependencies);
    }

    private Optional<IJavaProject> getIJavaProjectForPackageFragmentRoot(IPackageFragmentRoot root) {
        IJavaElement parent = root.getParent();
        if (parent instanceof IJavaProject) {
            return fromNullable((IJavaProject) parent);
        }
        return absent();
    }

    private void storeDependencyInfo(DependencyInfo dependencyInfo, IJavaProject javaProject) {
        Set<DependencyInfo> dependencies = Sets.newHashSet();
        dependencies.add(dependencyInfo);
        storeDependencyInfos(dependencies, javaProject);
    }

    private void storeDependencyInfos(Set<DependencyInfo> dependencyInfos, IJavaProject javaProject) {
        Set<DependencyInfo> projectDependencies = workspaceDependenciesByProject.get(javaProject);
        if (projectDependencies == null) {
            projectDependencies = Sets.newHashSet();
        }
        projectDependencies.addAll(dependencyInfos);
        workspaceDependenciesByProject.put(javaProject, projectDependencies);
    }

    private void removeDependencyInfo(DependencyInfo dependencyInfo, IJavaProject javaProject) {
        Set<DependencyInfo> projectDependencies = workspaceDependenciesByProject.get(javaProject);
        if (projectDependencies != null) {
            projectDependencies.remove(dependencyInfo);
        }
        if (projectDependencies.isEmpty()) {
            workspaceDependenciesByProject.remove(javaProject);
            jrePackageFragmentRoots.remove(javaProject);
        } else {
            workspaceDependenciesByProject.put(javaProject, projectDependencies);
        }
    }

    @Override
    public Set<DependencyInfo> getDependencies() {
        Set<DependencyInfo> dependencies = Sets.newHashSet();
        for (IJavaProject javaProjects : workspaceDependenciesByProject.keySet()) {
            Set<DependencyInfo> dependenciesForProject = workspaceDependenciesByProject.get(javaProjects);
            dependencies.addAll(dependenciesForProject);
        }
        return ImmutableSet.copyOf(dependencies);
    }

    @Override
    public Set<DependencyInfo> getDependenciesForProject(IJavaProject project) {
        Set<DependencyInfo> projectDependencies = workspaceDependenciesByProject.get(project);
        if (projectDependencies != null) {
            return ImmutableSet.copyOf(projectDependencies);
        }
        return ImmutableSet.of();
    }

}
