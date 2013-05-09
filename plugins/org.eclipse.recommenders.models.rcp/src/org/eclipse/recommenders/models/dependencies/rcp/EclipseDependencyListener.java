package org.eclipse.recommenders.models.dependencies.rcp;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.recommenders.models.dependencies.DependencyType;
import org.eclipse.recommenders.models.dependencies.IDependencyListener;
import org.eclipse.recommenders.models.dependencies.DependencyInfo;
import org.eclipse.recommenders.rcp.events.JavaModelEvents.JarPackageFragmentRootAdded;
import org.eclipse.recommenders.rcp.events.JavaModelEvents.JarPackageFragmentRootRemoved;
import org.eclipse.recommenders.rcp.events.JavaModelEvents.JavaProjectClosed;
import org.eclipse.recommenders.rcp.events.JavaModelEvents.JavaProjectOpened;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class EclipseDependencyListener implements IDependencyListener {

    private final Set<DependencyInfo> dependencies = Collections.emptySet();
    private final EventBus bus;
    
    public EclipseDependencyListener(EventBus bus){
        bus.register(this);
        this.bus = bus;
    }

    public EventBus getBus(){
        return bus;
    }
    
    @Override
    public Set<DependencyInfo> getDependencies() {
        return dependencies;
    }

    @Subscribe
    public void onEvent(final JavaProjectOpened e) {
        System.out.println("JavaProjectOpend");
        DependencyInfo dependencyInfo = createDependencyInfoForProject(e.project);
        boolean added = dependencies.add(dependencyInfo);
        //check for subelements
    }

    private DependencyInfo createDependencyInfoForProject(final IJavaProject project) {
        File file = project.getPath().toFile();
        DependencyInfo dependencyInfo = new DependencyInfo(file, DependencyType.PROJECT);
        return dependencyInfo;
    }
    
    @Subscribe
    public void onEvent(final JavaProjectClosed e) {
        System.out.println("JavaProjectClosed");
        DependencyInfo dependencyInfo = createDependencyInfoForProject(e.project);
        boolean removed = dependencies.remove(dependencyInfo);
        //check for subelements
    }
    
    @Subscribe
    public void onEvent(final JarPackageFragmentRootAdded e) {
        System.out.println("JarPackageFragmentRootAdded");
        // Check what happens with JRE Jars
        DependencyInfo dependencyInfo = createDependencyInfoForJar(e.root);
        boolean added = dependencies.add(dependencyInfo);
    }
    
    private DependencyInfo createDependencyInfoForJar(JarPackageFragmentRoot root) {
        File file = root.getPath().toFile();
        DependencyInfo dependencyInfo = new DependencyInfo(file, DependencyType.PROJECT);
        return dependencyInfo;
    }

    @Subscribe
    public void onEvent(final JarPackageFragmentRootRemoved e) {
        System.out.println("JarPackageFragmentRootRemoved");
        DependencyInfo dependencyInfo = createDependencyInfoForJar(e.root);
        boolean removed = dependencies.remove(dependencyInfo);
    }

}
