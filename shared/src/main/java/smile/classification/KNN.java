package smile.classification;

import java.util.*;

public final class KNN<T> {
    private final double[][] x;
    private final int[] y;
    private final int k;

    private KNN(double[][] x, int[] y, int k) {
        this.x = x;
        this.y = y;
        this.k = k;
    }

    public static KNN<double[]> fit(double[][] x, int[] y, int k) {
        return new KNN<>(x, y, k);
    }

    public int predict(double[] sample) {
        int n = x.length;
        double[] dists = new double[n];
        for (int i = 0; i < n; i++) {
            dists[i] = euclidean(sample, x[i]);
        }
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, Comparator.comparingDouble(i -> dists[i]));
        Map<Integer, Integer> votes = new HashMap<>();
        for (int i = 0; i < Math.min(k, n); i++) {
            int label = y[idx[i]];
            votes.put(label, votes.getOrDefault(label, 0) + 1);
        }
        return votes.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
    }

    private static double euclidean(double[] a, double[] b) {
        double s = 0.0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            s += d * d;
        }
        return Math.sqrt(s);
    }
}
