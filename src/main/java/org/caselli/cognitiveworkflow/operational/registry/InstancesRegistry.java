package org.caselli.cognitiveworkflow.operational.registry;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for managing instances in the operational layer.
 * @author niccolocaselli
 */
public abstract class InstancesRegistry<T> {
    protected final Map<String, T> runningInstances = new ConcurrentHashMap<>();

    public Optional<T> get(String id) {
        return Optional.ofNullable(runningInstances.get(id));
    }

    public void register(String id, T instance) {
        if(id == null || id.isEmpty()){
            throw new IllegalArgumentException("Id cannot be empty.");
        }

        if(runningInstances.containsKey(id)) {
            throw new IllegalArgumentException("Instance with id " + id + " already exists.");
        }

        if(instance == null) {
            throw new IllegalArgumentException("Instance cannot be null.");
        }

        runningInstances.put(id, instance);
    }

    public void remove(String id) {
        runningInstances.remove(id);
    }


    public void clear() {
        runningInstances.clear();
    }

    public Collection<T> list() {
        return runningInstances.values();
    }
}