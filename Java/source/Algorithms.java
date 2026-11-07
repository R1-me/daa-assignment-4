import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class Algorithms {
    static Map<String, Object> lcs(Map<String, Object> input) {
        String a = (String) input.get("a");
        String b = (String) input.get("b");
        int n = a.length();
        int m = b.length();
        int[][] dp = new int[n + 1][m + 1];

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = 1 + dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        StringBuilder sequence = new StringBuilder();
        int i = n;
        int j = m;
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                sequence.append(a.charAt(i - 1));
                i--;
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        return Map.of("length", dp[n][m], "sequence", sequence.reverse().toString());
    }

    static Map<String, Object> merge(Map<String, Object> input) {
        List<?> sizes = (List<?>) input.get("sizes");
        int n = sizes.size();
        List<int[]> plan = new ArrayList<>();
        if (n <= 1) {
            return Map.of("cost", 0L, "plan", plan);
        }

        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) {
            prefix[i + 1] = prefix[i] + ((Number) sizes.get(i)).longValue();
        }

        long[][] cost = new long[n][n];
        int[][] split = new int[n][n];
        for (int length = 2; length <= n; length++) {
            for (int left = 0; left + length <= n; left++) {
                int right = left + length - 1;
                long sum = prefix[right + 1] - prefix[left];
                cost[left][right] = Long.MAX_VALUE;
                for (int k = left; k < right; k++) {
                    long candidate = cost[left][k] + cost[k + 1][right] + sum;
                    if (candidate < cost[left][right]) {
                        cost[left][right] = candidate;
                        split[left][right] = k;
                    }
                }
            }
        }

        appendMergePlan(0, n - 1, split, plan);
        return Map.of("cost", cost[0][n - 1], "plan", plan);
    }

    private static void appendMergePlan(int left, int right, int[][] split, List<int[]> plan) {
        if (left == right) {
            return;
        }
        int k = split[left][right];
        appendMergePlan(left, k, split, plan);
        appendMergePlan(k + 1, right, split, plan);
        plan.add(new int[]{left, k, right});
    }

    static Map<String, Object> tree(Map<String, Object> input) {
        int n = ((Number) input.get("n")).intValue();
        List<Integer> selected = new ArrayList<>();
        if (n == 0) {
            return Map.of("weight", 0L, "selected", selected);
        }

        List<?> weights = (List<?>) input.get("weights");
        List<List<Integer>> graph = new ArrayList<>(n);
        for (int u = 0; u < n; u++) {
            graph.add(new ArrayList<>());
        }
        for (Object edge : (List<?>) input.get("edges")) {
            List<?> pair = (List<?>) edge;
            int u = ((Number) pair.get(0)).intValue();
            int v = ((Number) pair.get(1)).intValue();
            graph.get(u).add(v);
            graph.get(v).add(u);
        }

        int[] parent = new int[n];
        int[] order = new int[n];
        order[0] = 0;
        parent[0] = -1;
        int count = 1;
        for (int i = 0; i < count; i++) {
            int u = order[i];
            for (int v : graph.get(u)) {
                if (v != parent[u]) {
                    parent[v] = u;
                    order[count++] = v;
                }
            }
        }

        long[] take = new long[n];
        long[] skip = new long[n];
        for (int i = n - 1; i >= 0; i--) {
            int u = order[i];
            take[u] = ((Number) weights.get(u)).longValue();
            for (int v : graph.get(u)) {
                if (parent[v] == u) {
                    take[u] += skip[v];
                    skip[u] += Math.max(take[v], skip[v]);
                }
            }
        }

        boolean[] chosen = new boolean[n];
        for (int u : order) {
            if ((parent[u] == -1 || !chosen[parent[u]]) && take[u] > skip[u]) {
                chosen[u] = true;
                selected.add(u);
            }
        }
        return Map.of("weight", Math.max(take[0], skip[0]), "selected", selected);
    }

    static Map<String, Object> naive(Map<String, Object> input) {
        return Map.of("positions", naiveSearch((String) input.get("text"), (String) input.get("pattern")));
    }

    static Map<String, Object> kmp(Map<String, Object> input) {
        return Map.of("positions", kmpSearch((String) input.get("text"), (String) input.get("pattern")));
    }

    static Map<String, Object> rk(Map<String, Object> input) {
        return Map.of("positions", rkSearch((String) input.get("text"), (String) input.get("pattern")));
    }

    private static List<Integer> allBoundaries(int length) {
        List<Integer> positions = new ArrayList<>(length + 1);
        for (int i = 0; i <= length; i++) {
            positions.add(i);
        }
        return positions;
    }

    private static boolean matchesAt(String text, String pattern, int start) {
        for (int j = 0; j < pattern.length(); j++) {
            if (text.charAt(start + j) != pattern.charAt(j)) {
                return false;
            }
        }
        return true;
    }

    static List<Integer> naiveSearch(String text, String pattern) {
        int n = text.length();
        int m = pattern.length();
        if (m == 0) {
            return allBoundaries(n);
        }
        List<Integer> positions = new ArrayList<>();
        for (int start = 0; start <= n - m; start++) {
            if (matchesAt(text, pattern, start)) {
                positions.add(start);
            }
        }
        return positions;
    }

    static List<Integer> kmpSearch(String text, String pattern) {
        int n = text.length();
        int m = pattern.length();
        if (m == 0) {
            return allBoundaries(n);
        }
        List<Integer> positions = new ArrayList<>();
        if (m > n) {
            return positions;
        }
        int[] prefix = new int[m];
        for (int i = 1; i < m; i++) {
            int j = prefix[i - 1];
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) {
                j = prefix[j - 1];
            }
            if (pattern.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            prefix[i] = j;
        }

        int matched = 0;
        for (int i = 0; i < n; i++) {
            while (matched > 0 && text.charAt(i) != pattern.charAt(matched)) {
                matched = prefix[matched - 1];
            }
            if (text.charAt(i) == pattern.charAt(matched)) {
                matched++;
            }
            if (matched == m) {
                positions.add(i - m + 1);
                matched = prefix[m - 1];
            }
        }
        return positions;
    }

    static List<Integer> rkSearch(String text, String pattern) {
        int n = text.length();
        int m = pattern.length();
        if (m == 0) {
            return allBoundaries(n);
        }
        List<Integer> positions = new ArrayList<>();
        if (m > n) {
            return positions;
        }

        final int base = 31;
        final int mod = 101;
        int highestPower = 1;
        int patternHash = 0;
        int windowHash = 0;
        for (int i = 0; i < m; i++) {
            if (i > 0) {
                highestPower = highestPower * base % mod;
            }
            patternHash = (patternHash * base + pattern.charAt(i)) % mod;
            windowHash = (windowHash * base + text.charAt(i)) % mod;
        }
        for (int start = 0; start <= n - m; start++) {
            if (windowHash == patternHash && matchesAt(text, pattern, start)) {
                positions.add(start);
            }
            if (start + m < n) {
                int rest = windowHash - text.charAt(start) * highestPower;
                windowHash = (rest * base + text.charAt(start + m)) % mod;
                if (windowHash < 0) {
                    windowHash += mod;
                }
            }
        }
        return positions;
    }

    private static int compareRankPairs(int i, int j, int[] rank, int width) {
        if (rank[i] != rank[j]) {
            return Integer.compare(rank[i], rank[j]);
        }
        int secondI = i + width < rank.length ? rank[i + width] : -1;
        int secondJ = j + width < rank.length ? rank[j + width] : -1;
        return Integer.compare(secondI, secondJ);
    }

    static Map<String, Object> suffix(Map<String, Object> input) {
        String text = (String) input.get("text");
        int n = text.length();
        Integer[] sa = new Integer[n];
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) {
            sa[i] = i;
            rank[i] = text.charAt(i);
        }

        for (int width = 1; width < n; width *= 2) {
            final int half = width;
            final int[] previous = rank;
            Arrays.sort(sa, (i, j) -> compareRankPairs(i, j, previous, half));
            int[] newRank = new int[n];
            for (int k = 1; k < n; k++) {
                int left = sa[k - 1], right = sa[k];
                newRank[right] = newRank[left];
                if (compareRankPairs(left, right, previous, half) != 0) {
                    newRank[right]++;
                }
            }
            rank = newRank;
            if (rank[sa[n - 1]] == n - 1) {
                break;
            }
        }

        int[] position = new int[n];
        for (int k = 0; k < n; k++) {
            position[sa[k]] = k;
        }
        int[] lcp = new int[n];
        int common = 0, bestStart = 0, bestLength = 0;
        for (int i = 0; i < n; i++) {
            int k = position[i];
            if (k == 0) {
                common = 0;
                continue;
            }
            int j = sa[k - 1];
            while (i + common < n && j + common < n
                    && text.charAt(i + common) == text.charAt(j + common)) {
                common++;
            }
            lcp[k] = common;
            if (common > bestLength) {
                bestLength = common;
                bestStart = i;
            }
            if (common > 0) {
                common--;
            }
        }
        return Map.of("sa", sa, "lcp", lcp,
                "repeated", text.substring(bestStart, bestStart + bestLength));
    }

    static Map<String, Object> aho(Map<String, Object> input) {
        List<?> patterns = (List<?>) input.get("patterns");
        List<?> texts = (List<?>) input.get("texts");
        int alphabetSize = 95;
        List<int[]> next = new ArrayList<>();
        next.add(new int[alphabetSize]);
        int[] terminal = new int[patterns.size()];
        for (int p = 0; p < patterns.size(); p++) {
            String pattern = (String) patterns.get(p);
            int state = 0;
            for (int i = 0; i < pattern.length(); i++) {
                int c = pattern.charAt(i) - ' ';
                int[] row = next.get(state);
                if (row[c] == 0) {
                    row[c] = next.size();
                    next.add(new int[alphabetSize]);
                }
                state = row[c];
            }
            terminal[p] = state;
        }

        int[] failure = new int[next.size()];
        int[] order = new int[next.size()];
        int count = 0;
        for (int child : next.get(0)) {
            if (child != 0) {
                order[count++] = child;
            }
        }
        for (int i = 0; i < count; i++) {
            int state = order[i];
            int[] row = next.get(state);
            int[] fallback = next.get(failure[state]);
            for (int c = 0; c < alphabetSize; c++) {
                int child = row[c];
                if (child == 0) {
                    row[c] = fallback[c];
                } else {
                    failure[child] = fallback[c];
                    order[count++] = child;
                }
            }
        }

        long[][] counts = new long[texts.size()][patterns.size()];
        for (int t = 0; t < texts.size(); t++) {
            String text = (String) texts.get(t);
            int state = 0;
            long[] visits = new long[next.size()];
            for (int i = 0; i < text.length(); i++) {
                state = next.get(state)[text.charAt(i) - ' '];
                visits[state]++;
            }
            for (int i = count - 1; i >= 0; i--) {
                int node = order[i];
                visits[failure[node]] += visits[node];
            }
            for (int p = 0; p < patterns.size(); p++) {
                counts[t][p] = visits[terminal[p]];
            }
        }
        return Map.of("counts", counts);
    }
}
