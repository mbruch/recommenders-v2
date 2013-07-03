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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.recommenders.models.ProjectCoordinate;
import org.eclipse.recommenders.models.dependencies.DependencyInfo;
import org.eclipse.recommenders.models.dependencies.DependencyType;
import org.eclipse.recommenders.models.dependencies.IMappingStrategy;
import org.eclipse.recommenders.models.dependencies.impl.JREExecutionEnvironmentStrategy;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.ViewPart;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class DependencyMonitor extends ViewPart {

	private static final int COLUMN_LOCATION = 0;
	private static final int COLUMN_COORDINATE = 1;

	private Composite parent;
	private TableViewer tableViewer;
	private ContentProvider contentProvider;

	private EclipseDependencyListener eclipseDependencyListener;
	private MappingProvider mappingProvider;
	private EventBus eventBus;

	private TableViewerColumn locationColumn;
	private TableViewerColumn coordinateColumn;
	private TableComparator comparator;

	public DependencyMonitor() {
		eventBus = new EventBus("org.eclipse.recommenders.models.rcp.eventbus");
		eclipseDependencyListener = new EclipseDependencyListener(eventBus);
		eventBus.register(this);
		JavaModelEventsProvider javaModelEventsProvider = new JavaModelEventsProvider(
				eventBus, ResourcesPlugin.getWorkspace().getRoot());
		JavaCore.addElementChangedListener(javaModelEventsProvider);

		mappingProvider = new MappingProvider();
		mappingProvider.addStrategy(new MavenPomPropertiesStrategy());
		mappingProvider.addStrategy(new JREExecutionEnvironmentStrategy());
		mappingProvider.addStrategy(new JREReleaseFileStrategy());
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
		if (parent != null){
			parent.getDisplay().syncExec(new Runnable() {
				
				@Override
				public void run() {
					contentProvider.setData(eclipseDependencyListener
							.getDependencies());
					refreshTable();
				}
				
			});
		}
	}

	protected void setLabelProviderForTooltips() {
		locationColumn.setLabelProvider(new LocationTooltip());
	}

	private void refreshTable() {
		tableViewer.setLabelProvider(new ViewLabelProvider());
		locationColumn.setLabelProvider(new LocationTooltip());
		coordinateColumn.setLabelProvider(new CoordinateTooltip());
		tableViewer.refresh();
	}
	
	@Override
	public void createPartControl(Composite parent) {
		this.parent = parent;
		tableViewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION);
		tableViewer.setLabelProvider(new ViewLabelProvider());
		contentProvider = new ContentProvider();
		tableViewer.setContentProvider(contentProvider);
		tableViewer.setInput(getViewSite());
		comparator = new TableComparator();
		tableViewer.setComparator(comparator);

		ColumnViewerToolTipSupport.enableFor(tableViewer, ToolTip.NO_RECREATE);

		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().setLinesVisible(true);

		locationColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tableColumn = locationColumn.getColumn();
		tableColumn.setText("Location");
		tableColumn.setWidth(200);
		tableColumn.addSelectionListener(new SelectionListener(tableColumn, 0));

		coordinateColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		tableColumn = coordinateColumn.getColumn();
		tableColumn.setText("Coordinate");
		tableColumn.setWidth(450);
		tableColumn.addSelectionListener(new SelectionListener(tableColumn, 1));

		tableViewer.getTable().setSortDirection(SWT.UP);
		tableViewer.getTable().setSortColumn(locationColumn.getColumn());
		
		checkForDependencyUpdates();
	}

	@Override
	public void setFocus() {
		tableViewer.getControl().setFocus();
	}

	class ViewLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (obj instanceof DependencyInfo) {
				DependencyInfo dependencyInfo = (DependencyInfo) obj;
				switch (index) {
				case COLUMN_LOCATION:
					if (dependencyInfo.getType() == DependencyType.JRE){
						Optional<String> executionEnvironment = dependencyInfo.getAttribute(DependencyInfo.EXECUTION_ENVIRONMENT);
						if (executionEnvironment.isPresent()){
							return executionEnvironment.get();
						}
					}
					return dependencyInfo.getFile().getName();
				case COLUMN_COORDINATE:
					Optional<ProjectCoordinate> optionalProjectCoordinate = mappingProvider
							.searchForProjectCoordinate(dependencyInfo);
					if (optionalProjectCoordinate.isPresent()) {
						return optionalProjectCoordinate.get().toString();
					}
				default:
					return "";
				}
			}
			return "";
		}

		public Image getColumnImage(Object obj, int index) {
			if (obj instanceof DependencyInfo) {
				DependencyInfo dependencyInfo = (DependencyInfo) obj;
				switch (index) {
				case COLUMN_LOCATION:
					return getImageForDependencyTyp(dependencyInfo);
				default:
					return null;
				}
			}
			return null;
		}

		private Image getImageForDependencyTyp(DependencyInfo dependencyInfo) {
			switch (dependencyInfo.getType()) {
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

		private Image loadImage(String name) {
			ImageDescriptor imageDescriptor = Activator
					.getImageDescriptor(name);
			if (imageDescriptor != null) {
				Image image = imageDescriptor.createImage();
				return image;
			}
			return null;
		}
	}

	class ContentProvider implements IStructuredContentProvider {

		private List<DependencyInfo> data = new ArrayList<DependencyInfo>();

		public void setData(Set<DependencyInfo> dependencyInfos) {
			data.clear();
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

	abstract class ToolTipProvider extends CellLabelProvider {
		@Override
		public void update(ViewerCell cell) {
			cell.setText(cell.getText());
		}

		@Override
		public String getToolTipText(Object element) {
			if (element instanceof DependencyInfo) {
				DependencyInfo dependencyInfo = (DependencyInfo) element;
				return generateTooltip(dependencyInfo);
			}
			return "";
		}

		protected abstract String generateTooltip(DependencyInfo dependencyInfo);

		@Override
		public Point getToolTipShift(Object object) {
			return new Point(5, 5);
		}

		@Override
		public int getToolTipDisplayDelayTime(Object object) {
			return 100;
		}

		@Override
		public int getToolTipTimeDisplayed(Object object) {
			return 10000;
		}

	}

	class LocationTooltip extends ToolTipProvider {

		@Override
		protected String generateTooltip(DependencyInfo dependencyInfo) {
			StringBuilder sb = new StringBuilder();
			sb.append("Location: ");
			if (dependencyInfo.getType() == DependencyType.PROJECT){
				sb.append(dependencyInfo.getFile().getPath());				
			}else{
				sb.append(dependencyInfo.getFile().getAbsolutePath());
			}
			sb.append(System.getProperty("line.separator"));

			sb.append("Type: ");
			sb.append(dependencyInfo.getType().toString());

			Map<String, String> attributeMap = dependencyInfo.getAttributeMap();
			if ((attributeMap != null) && (!attributeMap.isEmpty())) {
				sb.append(System.getProperty("line.separator"));
				sb.append("Attributes: ");
				for (Entry<String, String> entry : attributeMap.entrySet()) {
					sb.append(System.getProperty("line.separator"));
					sb.append("  ");
					sb.append(entry.getKey());
					sb.append(": ");
					sb.append(entry.getValue());
				}
			}

			return sb.toString();
		}

	}

	class CoordinateTooltip extends ToolTipProvider {

		@Override
		protected String generateTooltip(DependencyInfo dependencyInfo) {
			StringBuilder sb = new StringBuilder();
			List<IMappingStrategy> strategies = mappingProvider.getStrategies();

			for (IMappingStrategy strategy : strategies) {
				if (strategies.indexOf(strategy) != 0) {
					sb.append(System.getProperty("line.separator"));
				}
				sb.append(strategy.getClass().getSimpleName());
				sb.append(": ");
				if (!strategy.isApplicable(dependencyInfo.getType())){
					sb.append("n/a");
				}else{
					Optional<ProjectCoordinate> optionalCoordinate = strategy
							.searchForProjectCoordinate(dependencyInfo);
					if (optionalCoordinate.isPresent()) {
						sb.append(optionalCoordinate.get().toString());
					}else{
						sb.append("unknown");
					}
				}
				
			}
			return sb.toString();
		}

	}

	public class TableComparator extends ViewerComparator {
		private int column = 0;
		private int direction = SWT.UP;

		public int getDirection() {
			return direction;
		}

		public void setColumn(int column) {
			if (column == this.column) {
				switch (direction) {
				case SWT.NONE:
					direction = SWT.UP;
					break;
				case SWT.UP:
					direction = SWT.DOWN;
					break;
				default:
					direction = SWT.NONE;
					break;
				}
			} else {
				this.column = column;
				direction = SWT.UP;
			}
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int result = 0;
			if (direction == SWT.NONE){
				return 0;
			}
			if ((e1 instanceof DependencyInfo)
					&& (e2 instanceof DependencyInfo)) {
				DependencyInfo firstElement = (DependencyInfo) e1;
				DependencyInfo secondElement = (DependencyInfo) e2;

				switch (column) {
				case COLUMN_LOCATION:
					result = compareLocation(firstElement, secondElement);
					break;
				case COLUMN_COORDINATE:
					result = compareCoordinate(firstElement, secondElement);
					break;
				default:
					result = 0;
					break;
				}
			}
			if (direction == SWT.DOWN) {
				return -result;
			}
			return result;
		}

		private int compareCoordinate(DependencyInfo firstElement,
				DependencyInfo secondElement) {
			Optional<ProjectCoordinate> optionalCoordinateFirstElement = mappingProvider.searchForProjectCoordinate(firstElement);
			Optional<ProjectCoordinate> optionalCoordinateSecondElement = mappingProvider.searchForProjectCoordinate(secondElement);
			
			if (optionalCoordinateFirstElement.isPresent()){
				if (optionalCoordinateSecondElement.isPresent()){
					return optionalCoordinateFirstElement.get().toString().compareTo(optionalCoordinateSecondElement.get().toString());
				}else{
					return -1;
				}
			}else{
				if (optionalCoordinateSecondElement.isPresent()){
					return 1;					
				}else{
					return 0;
				}
			}
		}

		private int compareLocation(DependencyInfo firstElement,
				DependencyInfo secondElement) {
			int compareScore = -firstElement.getType().compareTo(secondElement.getType());
			if (compareScore == 0){
				return firstElement.getFile().getName().compareToIgnoreCase(secondElement.getFile().getName());
			}
			return compareScore;
		}

	}

	class SelectionListener extends SelectionAdapter {

		private TableColumn tableColumn;
		private int index;

		public SelectionListener(final TableColumn tableColumn, int index) {
			this.tableColumn = tableColumn;
			this.index = index;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			comparator.setColumn(index);
			int direction = comparator.getDirection();
			tableViewer.getTable().setSortDirection(direction);
			tableViewer.getTable().setSortColumn(tableColumn);
			refreshTable();
		}
	};

}
