package com.dremioplatform.workload.k8s;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Scales the K8s Deployment (or StatefulSet — see {@link #scale}'s dual attempt)
 * backing an {@link com.dremioplatform.workload.domain.Engine} to a target replica
 * count (plan Section 5.4: "Talks to Kubernetes API ... to scale executor pods").
 *
 * `workload.k8s.enabled=false` (the local-dev default — see application.properties)
 * makes every call a logged no-op instead of a real API call, so the service layer
 * and REST API are fully exercisable (including in tests) without a real cluster.
 * This is a deliberate safety default, not just a dev convenience: accidentally
 * scaling a real cluster's executor pool from a locally-run instance pointed at the
 * wrong kubeconfig would be a bad time. Production deployments must explicitly
 * opt in.
 */
@ApplicationScoped
public class K8sScalingClient {

    private static final Logger LOG = Logger.getLogger(K8sScalingClient.class);

    @Inject KubernetesClient kubernetesClient;

    @ConfigProperty(name = "workload.k8s.enabled")
    boolean enabled;

    /**
     * Attempts a Deployment scale first, falling back to StatefulSet — Dremio
     * executor pools are plausibly either depending on deployment topology (plan
     * Section 5.4 mentions both interchangeably: "scale executor StatefulSet/
     * Deployment replicas"). Throws {@link K8sScalingException} on failure so the
     * service layer can decide whether to roll back the Engine row's status.
     */
    public void scale(String namespace, String deploymentName, int targetReplicas) {
        if (!enabled) {
            LOG.infof("[no-op: workload.k8s.enabled=false] Would scale %s/%s to %d replicas",
                    namespace, deploymentName, targetReplicas);
            return;
        }
        try {
            boolean scaledAsDeployment = kubernetesClient.apps().deployments()
                    .inNamespace(namespace).withName(deploymentName).scale(targetReplicas) != null;
            if (!scaledAsDeployment) {
                kubernetesClient.apps().statefulSets()
                        .inNamespace(namespace).withName(deploymentName).scale(targetReplicas);
            }
        } catch (KubernetesClientException e) {
            throw new K8sScalingException(
                    "Failed to scale " + namespace + "/" + deploymentName + " to " + targetReplicas + " replicas", e);
        }
    }

    /** Current ready replica count, used to reconcile Engine.currentReplicas after
     *  a scale operation or on periodic drift-correction (not yet scheduled — see
     *  module README "Known gaps"). Returns -1 (unknown) in no-op mode. */
    public int currentReadyReplicas(String namespace, String deploymentName) {
        if (!enabled) {
            return -1;
        }
        try {
            var deployment = kubernetesClient.apps().deployments().inNamespace(namespace).withName(deploymentName).get();
            if (deployment != null && deployment.getStatus() != null) {
                Integer ready = deployment.getStatus().getReadyReplicas();
                return ready == null ? 0 : ready;
            }
            var statefulSet = kubernetesClient.apps().statefulSets().inNamespace(namespace).withName(deploymentName).get();
            if (statefulSet != null && statefulSet.getStatus() != null) {
                Integer ready = statefulSet.getStatus().getReadyReplicas();
                return ready == null ? 0 : ready;
            }
            return 0;
        } catch (KubernetesClientException e) {
            throw new K8sScalingException(
                    "Failed to read replica status for " + namespace + "/" + deploymentName, e);
        }
    }

    public static class K8sScalingException extends RuntimeException {
        public K8sScalingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
