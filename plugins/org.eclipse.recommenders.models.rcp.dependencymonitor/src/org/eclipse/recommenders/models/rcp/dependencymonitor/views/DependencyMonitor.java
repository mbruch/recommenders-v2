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
package org.eclipse.recommenders.models.rcp.dependencymonitor.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.recommenders.models.ProjectCoordinate;
import org.eclipse.recommenders.models.dependencies.DependencyInfo;
import org.eclipse.recommenders.models.dependencies.impl.JREIDEVersionStrategy;
import org.eclipse.recommenders.models.dependencies.impl.JREReleaseFileStrategy;
import org.eclipse.recommenders.models.dependencies.impl.MappingProvider;
import org.eclipse.recommenders.models.dependencies.impl.MavenPomPropertiesStrategy;
import org.eclipse.recommenders.models.dependencies.rcp.EclipseDependencyListener;
import org.eclipse.recommenders.models.dependencies.rcp.JavaModelEventsProvider;
import org.eclipse.recommenders.models.rcp.dependencymonitor.Activator;
import org.eclipse.recommenders.utils.rcp.events.JavaModelEvents.JarPackageFragmentRootAdded;
import org.eclipse.recommenders.utils.rcp.events.JavaModelEvents.JarPackageFragmentRootRemoved;
import org.eclipse.recommenders.utils.rcp.events.JavaModelEvents.JavaProjectClosed;
import org.eclipse.recommenders.utils.rcp.events.JavaModelEvents.JavaProjectOpened;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.ViewPart;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class DependencyMonitor extends ViewPart {
  
	private static final int COLOUMN_DEPENDENCYTYP = 0;
	private static final int COLOUMN_DEPENDENCYFILE = 1;
	private static final int COLOUMN_PROJECTCOORDINATE = 2;
	
	private Composite parent;
	private TableViewer tableViewer;
	private ContentProvider contentProvider;

	private EclipseDependencyListener eclipseDependencyListener;
    private MappingProvider mappingProvider;
	private EventBus eventBus;

    public DependencyMonitor() {
		eventBus = new EventBus("org.eclipse.recommenders.models.rcp.eventbus");
		eclipseDependencyListener = new EclipseDependencyListener(eventBus);
		eventBus.register(this);
		JavaModelEventsProvider javaModelEventsProvider = new JavaModelEventsProvider(
				eventBus, ResourcesPlugin.getWorkspace().getRoot());
		JavaCore.addElementChangedListener(javaModelEventsProvider);

		mappingProvider = new MappingProvider();
		mappingProvider.addStrategy(new MavenPomPropertiesStrategy());
		mappingProvider.addStrategy(new JREReleaseFileStrategy());
		mappingProvider.addStrategy(new JREIDEVersionStrategy());   
    }
    
    @Subscribe
    public void onEvent(final JavaProjectOpened e) {
        checkForDependencyUpdates();
    }

    @Subscribe
    public void onEvent(final JavaProjectClosed e) {
        checkForDependencyUpdates();
    }

    @Subscribe
    public void onEvent(final JarPackageFragmentRootAdded e) {
        checkForDependencyUpdates();
    }

    @Subscribe
    public void onEvent(final JarPackageFragmentRootRemoved e) {
        checkForDependencyUpdates();
    }

    protected void checkForDependencyUpdates() {
        parent.getDisplay().syncExec(new Runnable() {

			@Override
            public void run() {
				contentProvider.setData(eclipseDependencyListener.getDependencies());
				tableViewer.setLabelProvider(new ViewLabelProvider());
				tableViewer.refresh();
            }
        });
    }

    @Override
    public void createPartControl(Composite parent) {
        this.parent = parent;
		tableViewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		tableViewer.setLabelProvider(new ViewLabelProvider());
		contentProvider = new ContentProvider();
		tableViewer.setContentProvider(contentProvider);
		tableViewer.setInput(getViewSite());

		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);

		TableViewerColumn tableViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tableColumn = tableViewerColumn.getColumn();
		tableColumn.setText("Type");
		tableColumn.setWidth(75);

		tableViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		tableColumn = tableViewerColumn.getColumn();
		tableColumn.setText("File");
		tableColumn.setWidth(200);

		tableViewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		tableColumn = tableViewerColumn.getColumn();
		tableColumn.setText("ProjectCoordinate");
		tableColumn.setWidth(200);
        
        checkForDependencyUpdates();

    }

    @Override
    public void setFocus() {
    	tableViewer.getControl().setFocus();
    }
    
    class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (obj instanceof DependencyInfo){
				DependencyInfo dependencyInfo = (DependencyInfo) obj;
				switch(index){
				case COLOUMN_DEPENDENCYTYP:
					return dependencyInfo.getType().toString();
				case COLOUMN_DEPENDENCYFILE:
					return dependencyInfo.getFile().getName();
				case COLOUMN_PROJECTCOORDINATE:
					Optional<ProjectCoordinate> optionalProjectCoordinate = mappingProvider.searchForProjectCoordinate(dependencyInfo);
					if (optionalProjectCoordinate.isPresent()){
						return optionalProjectCoordinate.get().toString();						
					}
				default:
					return "";
				}
			}
			return "";
		}

		public Image getColumnImage(Object obj, int index) {
			if (obj instanceof DependencyInfo){
				DependencyInfo dependencyInfo = (DependencyInfo) obj;
				switch(index){
				case COLOUMN_DEPENDENCYTYP:
					return getImageForDependencyTyp(dependencyInfo);
				default:
					return null;
				}
			}
			return null;
		}

		private Image getImageForDependencyTyp(DependencyInfo dependencyInfo) {
			switch(dependencyInfo.getType()){
			case JRE:
				return loadImage("icons/cview16/classpath.gif");
			case JAR:
				return loadImage("icons/cview16/jar_obj.gif");				
			case PROJECT:
				return loadImage("icons/cview16/projects.gif");
			default:
				return null;
			}
		}
		
		private Image loadImage(String name){
			ImageDescriptor imageDescriptor = Activator.getImageDescriptor(name);
			if (imageDescriptor != null){
				Image image = imageDescriptor.createImage();
				return image;
			}
			return null;
		}
	}
    
	class ContentProvider implements IStructuredContentProvider{

		private List<DependencyInfo> data = new ArrayList<DependencyInfo>();

		public void setData(Set<DependencyInfo> dependencyInfos){
			data.addAll(dependencyInfos);
		}
				
		@Override
		public void dispose() {
			// unused in this case
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// unused in this case
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return data.toArray();
		}
		
	}

}
