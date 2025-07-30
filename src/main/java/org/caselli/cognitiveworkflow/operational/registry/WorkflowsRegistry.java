package org.caselli.cognitiveworkflow.operational.registry;

import org.caselli.cognitiveworkflow.operational.instances.WorkflowInstance;
import org.springframework.stereotype.Component;

/**
 * Registry for managing workflow instances.
 * @author niccolocaselli
 */
@Component
public class WorkflowsRegistry extends InstancesRegistry<WorkflowInstance> {

}