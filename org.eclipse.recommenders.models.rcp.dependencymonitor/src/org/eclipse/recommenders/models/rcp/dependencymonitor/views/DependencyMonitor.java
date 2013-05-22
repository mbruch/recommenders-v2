package org.eclipse.recommenders.models.rcp.dependencymonitor.views;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.recommenders.injection.InjectionService;
import org.eclipse.recommenders.models.ProjectCoordinate;
import org.eclipse.recommenders.models.dependencies.DependencyInfo;
import org.eclipse.recommenders.models.dependencies.IDependencyListener;
import org.eclipse.recommenders.models.dependencies.impl.JREIDEVersionStrategy;
import org.eclipse.recommenders.models.dependencies.impl.JREReleaseFileStrategy;
import org.eclipse.recommenders.models.dependencies.impl.MappingProvider;
import org.eclipse.recommenders.models.dependencies.impl.MavenPomPropertiesStrategy;
import org.eclipse.recommenders.models.dependencies.rcp.EclipseDependencyListener;
import org.eclipse.recommenders.rcp.events.JavaModelEvents.JarPackageFragmentRootAdded;
import org.eclipse.recommenders.rcp.events.JavaModelEvents.JarPackageFragmentRootRemoved;
import org.eclipse.recommenders.rcp.events.JavaModelEvents.JavaProjectClosed;
import org.eclipse.recommenders.rcp.events.JavaModelEvents.JavaProjectOpened;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class DependencyMonitor extends ViewPart {
    private TableViewer viewer;
    private ViewContentProvider viewContentProvider;
    private final EclipseDependencyListener eclipseDependencyListener;
    private Composite parent;
    private final MappingProvider mp;

    class ViewContentProvider implements IStructuredContentProvider {
        private DependencyInfo[] data = new DependencyInfo[0];

        public void setData(Set<DependencyInfo> data) {
            this.data = data.toArray(new DependencyInfo[0]);
        }

        @Override
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public Object[] getElements(Object parent) {
            return data;
        }
    }

    class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public String getColumnText(Object obj, int index) {
            return getText(obj);
        }

        @Override
        public Image getColumnImage(Object obj, int index) {
            return getImage(obj);
        }

        @Override
        public Image getImage(Object obj) {
            return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
        }
    }

    public DependencyMonitor() {
        
        mp = new MappingProvider();
        mp.addStrategy(new MavenPomPropertiesStrategy());
        mp.addStrategy(new JREReleaseFileStrategy());
        mp.addStrategy(new JREIDEVersionStrategy());
        
        EventBus eventBus = InjectionService.getInstance().requestInstance(EventBus.class);
        eventBus.register(this);
        
        @SuppressWarnings("unchecked")
        IDependencyListener<IJavaProject> requestInstance = InjectionService.getInstance().requestInstance(IDependencyListener.class);
        eclipseDependencyListener = (EclipseDependencyListener) requestInstance;

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
                viewContentProvider.setData(eclipseDependencyListener.getDependencies());
                viewer.refresh();
            }
        });
    }

    @Override
    public void createPartControl(Composite parent) {
        this.parent = parent;
        viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);
        viewContentProvider = new ViewContentProvider();
        viewContentProvider.setData(new HashSet<DependencyInfo>());
        viewer.setContentProvider(viewContentProvider);
        viewer.setLabelProvider(new ViewLabelProvider());
        viewer.setInput(getViewSite());

        TableViewerColumn dependencyInfo = new TableViewerColumn(viewer, SWT.NONE);
        dependencyInfo.getColumn().setWidth(200);
        dependencyInfo.getColumn().setText("DependencyInfo:");
        dependencyInfo.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return element.toString();
            }
        });

        TableViewerColumn projectCoordinate = new TableViewerColumn(viewer, SWT.NONE);
        projectCoordinate.getColumn().setWidth(200);
        projectCoordinate.getColumn().setText("ProjectCoordinate:");
        projectCoordinate.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof DependencyInfo){
                    DependencyInfo di = (DependencyInfo) element;
                    Optional<ProjectCoordinate> searchForProjectCoordinate = mp.searchForProjectCoordinate(di);
                    if (searchForProjectCoordinate.isPresent()){
                        return searchForProjectCoordinate.get().toString();
                    } 
                }
                return "------";
            }
        });
        
        checkForDependencyUpdates();

    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }
}