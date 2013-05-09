package org.eclipse.recommenders.calls.rcp.wiring;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.recommenders.calls.ICallModel;
import org.eclipse.recommenders.calls.ICallModelProvider;
import org.eclipse.recommenders.calls.OneZipCallModelProvider;
import org.eclipse.recommenders.models.BasedTypeName;
import org.osgi.framework.FrameworkUtil;

import com.google.common.base.Optional;

/**
 * Just for testing.
 */
final class NullCallModelProvider implements ICallModelProvider {

    private OneZipCallModelProvider models;

    @Override
    public void open() throws IOException {
        File bundle = FileLocator.getBundleFile(FrameworkUtil.getBundle(getClass()));
        File zip = new File(bundle, "jre-1.0.0-call.zip");
        models = new OneZipCallModelProvider(zip);
        models.open();
    }

    @Override
    public void close() throws IOException {
        models.close();
    }

    @Override
    public void releaseModel(ICallModel value) {
    }

    @Override
    public Optional<ICallModel> acquireModel(BasedTypeName key) {
        return models.acquireModel(key);
    }
}