import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class Benchmark {
    private static final String[] METHODS = {"naive", "kmp", "rk"};
    private static final int CALLS = 16;
    private static final int SAMPLES = 5;
    private static final int WARMUP = 6;

    static String randomString(Random random, int length) {
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return result.toString();
    }

    static List<Integer> expectedPositions(String text, String pattern) {
        List<Integer> expected = new ArrayList<>();
        for (int start = 0; start <= text.length() - pattern.length(); start++) {
            if (text.startsWith(pattern, start)) {
                expected.add(start);
            }
        }
        return expected;
    }

    static List<Integer> search(int method, String text, String pattern) {
        switch (method) {
            case 0: return Algorithms.naiveSearch(text, pattern);
            case 1: return Algorithms.kmpSearch(text, pattern);
            case 2: return Algorithms.rkSearch(text, pattern);
            default: throw new IllegalArgumentException("Unknown method");
        }
    }

    static long runBatch(int method, String text, String pattern, List<Integer> expected) {
        List<?>[] answers = new List<?>[CALLS];
        long start = System.nanoTime();
        for (int i = 0; i < CALLS; i++) {
            answers[i] = search(method, text, pattern);
        }
        long elapsed = System.nanoTime() - start;
        for (List<?> answer : answers) {
            if (!expected.equals(answer)) {
                throw new AssertionError("Wrong positions from " + METHODS[method]);
            }
        }
        return elapsed;
    }

    static void compare(String type, String text, String pattern, String seed) {
        List<Integer> expected = expectedPositions(text, pattern);
        for (int method = 0; method < METHODS.length; method++) {
            if (!expected.equals(search(method, text, pattern))) {
                throw new AssertionError("Wrong positions from " + METHODS[method]);
            }
        }

        long[][] times = new long[METHODS.length][SAMPLES];
        for (int round = 0; round < WARMUP + SAMPLES; round++) {
            for (int offset = 0; offset < METHODS.length; offset++) {
                int method = (round + offset) % METHODS.length;
                long elapsed = runBatch(method, text, pattern, expected);
                if (round >= WARMUP) {
                    times[method][round - WARMUP] = elapsed;
                }
            }
        }

        for (int method = 0; method < METHODS.length; method++) {
            long[] ordered = times[method].clone();
            Arrays.sort(ordered);
            long median = ordered[SAMPLES / 2];
            System.out.printf(Locale.ROOT, "%s,%d,%d,%s,%d,%s,%d", type, text.length(),
                    pattern.length(), seed, expected.size(), METHODS[method], CALLS);
            for (long sample : times[method]) {
                System.out.print("," + sample);
            }
            System.out.printf(Locale.ROOT, ",%d,%.6f%n", median, median / (1000.0 * CALLS));
        }
    }

    static void warmUpJvm() {
        String randomText = randomString(new Random(4), 3000);
        String[] texts = {randomText, "a".repeat(3000)};
        String[] patterns = {randomText.substring(500, 628), "a".repeat(128)};
        for (int kind = 0; kind < texts.length; kind++) {
            List<Integer> expected = expectedPositions(texts[kind], patterns[kind]);
            for (int round = 0; round < 64; round++) {
                for (int method = 0; method < METHODS.length; method++) {
                    runBatch(method, texts[kind], patterns[kind], expected);
                }
            }
        }
    }

    public static void main(String[] args) {
        warmUpJvm();
        int[][] sizes = {{500, 16}, {1500, 64}, {3000, 128}};
        System.out.println("case,n,m,seed,matches,method,calls_per_batch,"
                + "t1_ns,t2_ns,t3_ns,t4_ns,t5_ns,median_batch_ns,median_us_per_call");
        for (int[] size : sizes) {
            int n = size[0];
            int m = size[1];
            long seed = 41000L + n;
            Random random = new Random(seed);
            StringBuilder text = new StringBuilder(randomString(random, n));
            String pattern = randomString(random, m);
            int start = random.nextInt(n - m + 1);
            text.replace(start, start + m, pattern);
            compare("random", text.toString(), pattern, Long.toString(seed));
            compare("repetitive", "a".repeat(n), "a".repeat(m), "");
        }
    }
}
