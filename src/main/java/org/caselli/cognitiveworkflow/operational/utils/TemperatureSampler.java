package org.caselli.cognitiveworkflow.operational.utils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for temperature-based sampling from list of candidates.
 * Higher-ranked (higher-scored) candidates are more likely to be selected,
 * with temperature controlling the randomness of the selection.
 * @author niccolocaselli
 */
public final class TemperatureSampler {
    private TemperatureSampler() {}

    /**
     * Selects an item from a SORTED list using temperature-based sampling.
     *
     * @param <T>          The type of items to sample
     * @param candidates   List of items sorted by score in descending order (highest first)
     * @param temperature  Controls sampling randomness:
     *                     <ul>
     *                       <li>0 = always select first item</li>
     *                       <li>1 = standard softmax sampling (weight ∝ exp(-rank))</li>
     *                       <li>∞ = exact uniform sampling (use {@code Double.POSITIVE_INFINITY})</li>
     *                     </ul>
     * @return The selected item
     * @throws IllegalArgumentException if candidates is null/empty or temperature is negative
     */
    public static <T> T sapleSortedList(List<T> candidates, double temperature) {
        // Edge Cases:
        if (candidates == null || candidates.isEmpty())  throw new IllegalArgumentException("Candidates list cannot be null or empty");
        if (temperature < 0) throw new IllegalArgumentException("Temperature must be ≥ 0");
        if (candidates.size() == 1 || temperature == 0.0)  return candidates.get(0);
        if (Double.isInfinite(temperature)) {
            int idx = ThreadLocalRandom.current().nextInt(candidates.size());
            return candidates.get(idx);
        }

        // Compute probabilities via softmax over negative ranks
        double[] probabilities = computeProbabilities(candidates.size(), temperature);
        return selectItem(candidates, probabilities);
    }

    /**
     * Computes normalized selection probabilities for each candidate index
     * using a numerically-stable softmax over -rank/temperature.
     */
    private static double[] computeProbabilities(int numCandidates, double temperature) {
        double[] logProbs = new double[numCandidates];
        double maxLog = Double.NEGATIVE_INFINITY;

        // logP_i = -i / T
        for (int i = 0; i < numCandidates; i++) {
            double lp = -((double) i) / temperature;
            logProbs[i] = lp;

            if (lp > maxLog) maxLog = lp;

        }

        double sum = 0.0;
        double[] probs = new double[numCandidates];

        for (int i = 0; i < numCandidates; i++) {
            double exp = Math.exp(logProbs[i] - maxLog);
            probs[i] = exp;
            sum += exp;
        }

        // normalize to sum to 1
        for (int i = 0; i < numCandidates; i++)
            probs[i] /= sum;

        return probs;
    }

    /**
     * Picks an index according to the given probability distribution
     * and returns the corresponding item from the candidate list.
     */
    private static <T> T selectItem(List<T> candidates, double[] probabilities) {
        double r = ThreadLocalRandom.current().nextDouble();
        double cumulative = 0.0;

        for (int i = 0; i < probabilities.length; i++) {
            cumulative += probabilities[i];
            if (r <= cumulative) {
                return candidates.get(i);
            }
        }

        // Fallback
        return candidates.get(candidates.size() - 1);
    }
}
