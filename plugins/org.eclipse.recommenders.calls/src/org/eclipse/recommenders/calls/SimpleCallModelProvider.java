package org.eclipse.recommenders.calls;

import static com.google.common.base.Optional.*;
import static org.eclipse.recommenders.utils.IOUtils.closeQuietly;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.recommenders.commons.bayesnet.BayesianNetwork;
import org.eclipse.recommenders.models.BasedTypeName;
import org.eclipse.recommenders.models.IModelProvider;
import org.eclipse.recommenders.models.ModelRepository;
import org.eclipse.recommenders.models.SimpleModelProvider;
import org.eclipse.recommenders.utils.Zips;
import org.eclipse.recommenders.utils.names.ITypeName;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

/**
 * A non-thread-safe implementation of {@link IModelProvider} for call models that keeps references on the model
 * archives.
 * <p>
 * Note that models should not be shared between several recommenders.
 */
@Beta
public class SimpleCallModelProvider extends SimpleModelProvider<BasedTypeName, ICallModel> implements
ICallModelProvider {

    public SimpleCallModelProvider(ModelRepository cache, String modelType) {
        super(cache, modelType);
    }

    @Override
    protected Optional<ICallModel> loadModel(ZipFile zip, BasedTypeName key) throws Exception {
        ITypeName type = key.getName();
        String path = Zips.path(type, ".data");
        ZipEntry entry = zip.getEntry(path);
        if (entry == null) {
            return absent();
        }
        InputStream is = zip.getInputStream(entry);
        BayesianNetwork bayesNet = BayesianNetwork.read(is);
        ICallModel net = new JayesCallModel(type, bayesNet);
        closeQuietly(is);
        return of(net);
    }

}
