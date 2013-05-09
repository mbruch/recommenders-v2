package org.eclipse.recommenders.examples.models;

import java.io.IOException;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.recommenders.models.ModelArchiveCoordinate;
import org.eclipse.recommenders.models.ModelRepository;
import org.eclipse.recommenders.models.ModelRepositoryEvents;
import org.eclipse.recommenders.models.ProjectCoordinate;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.eventbus.Subscribe;

@SuppressWarnings("unused")
public class UsingModelArchiveCache {

    void downloadModelArchive(ModelArchiveCoordinate model, ModelRepository repository) throws IOException {
        repository.resolve(model, newMonitor());
    }

    void findLocalModelArchive(ModelArchiveCoordinate model, ModelRepository repository) throws IOException {
        if (!repository.getLocation(model).isPresent()) {
            repository.resolve(model, newMonitor());
        }
    }

    void deleteCachedModelArchive(ModelArchiveCoordinate model, ModelRepository repository) throws IOException {
        repository.delete(model, newMonitor());
    }

    void deleteIndex(ModelRepository repository) throws IOException {
        repository.delete(ModelRepository.INDEX, newMonitor());
    }

    void findAllModelArtifacts(ProjectCoordinate[] gavs, ModelRepository cache,
            IModelArchiveCoordinateProvider[] modelProviders) {

        Table<ProjectCoordinate, String, Pair<ModelArchiveCoordinate, Boolean>> mappings = HashBasedTable.create();
        for (ProjectCoordinate projectCoord : gavs) {
            for (IModelArchiveCoordinateProvider modelProvider : modelProviders) {
                ModelArchiveCoordinate modelCoord = modelProvider.get(projectCoord).orNull();
                if (modelCoord != null) {
                    boolean cached = cache.isCached(modelCoord);
                    mappings.put(projectCoord, modelProvider.getType(), Pair.of(modelCoord, cached));
                }
            }
        }
        // update ui...
    }

    @Subscribe
    void onEvent(ModelRepositoryEvents.ModelArchiveCacheOpenedEvent e) {
        // TODO check if a new index is available and download it

    }

    @Subscribe
    void onEvent(ModelRepositoryEvents.ModelArchiveCacheClosedEvent e) {
        // TODO persists what needs to be persisted
    }

    @Subscribe
    void onEvent(ModelRepositoryEvents.ModelArchiveInstalledEvent e) {
        // TODO delete old cached keys, and reload the models currently required
    }

    private IProgressMonitor newMonitor() {
        // TODO Auto-generated method stub
        return null;
    }
}
