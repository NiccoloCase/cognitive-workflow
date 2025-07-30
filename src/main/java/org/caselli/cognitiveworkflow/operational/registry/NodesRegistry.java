package org.caselli.cognitiveworkflow.operational.registry;

import org.caselli.cognitiveworkflow.operational.instances.NodeInstance;
import org.springframework.stereotype.Component;

/**
 * Registry for managing node instances.
 * @author niccolocaselli
 */
@Component
public class NodesRegistry extends InstancesRegistry<NodeInstance> {
}