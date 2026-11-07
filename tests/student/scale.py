"""Check the Java solver on the published input limits."""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
from public_validate import check, ints
from run import build, execute


def cases():
    # The two character groups appear in opposite orders. A witness uses one group.
    a = "A" * 500 + "B" * 500
    b = "B" * 500 + "A" * 500
    yield "lcs: 1000 x 1000, optimum 500", {"task": "lcs", "a": a, "b": b}, {"length": 500}

    # With 200 equal leaves, the cheapest full merge tree has 56 leaves at depth 7
    # and 144 at depth 8. Ordering those leaves from left to right preserves adjacency.
    unit = 10**9
    cost = (56 * 7 + 144 * 8) * unit
    yield f"merge: 200 blocks, cost {cost}", {"task": "merge", "sizes": [unit] * 200}, {"cost": cost}

    # Each adjacent pair (0,1), (2,3), ... contributes at most one vertex.
    n = 100_000
    chain = {"task": "tree", "n": n, "weights": [unit] * n,
             "edges": [[v - 1, v] for v in range(1, n)]}
    yield "tree: 100000-vertex chain, weight 50000000000000", chain, {"weight": n // 2 * unit}

    # The unique 'b' allows a single match, ending at the last text character.
    for task, n, m in [("naive", 3000, 3000), ("naive", 3000, 1500),
                       ("kmp", 200_000, 100_000)]:
        request = {"task": task, "text": "a" * (n - 1) + "b", "pattern": "a" * (m - 1) + "b"}
        yield f"{task}: lengths {n}/{m}, start {n - m}", request, {"positions": [n - m]}

    request = {"task": "kmp", "text": "a" * 200_000, "pattern": ""}
    yield "kmp: all 200001 empty-pattern boundaries", request, {"positions": list(range(200_001))}

    request = {"task": "rk", "text": "a" * 10_000, "pattern": "a" * 2000}
    yield "rk: lengths 10000/2000, 8001 overlapping matches", request, {"positions": list(range(8001))}
    request = {"task": "rk", "text": "a" * 2000, "pattern": "a" * 10_000}
    yield "rk: lengths 2000/10000, zero matches", request, {"positions": []}

    # 'a'-suffixes precede 'b'-suffixes. Within each group the shortest comes first.
    n = 50_000
    text = "ab" * (n // 2)
    sa = list(range(n - 2, -1, -2)) + list(range(n - 1, 0, -2))
    lcp = list(range(0, n, 2)) + [0] + list(range(1, n - 2, 2))
    yield "suffix: 50000 characters, longest repeat 49998", {"task": "suffix", "text": text}, {
        "sa": sa, "lcp": lcp, "repeated": text[:-2]}

    # The long pattern creates 100001 trie states. Sliding it one step preserves a match.
    request = {"task": "aho", "patterns": ["a" * 100_000], "texts": ["a" * 200_000]}
    yield "aho: 100001 states, 100001 matches", request, {"counts": [[100_001]]}

    # Each suffix pattern matches at every position where it fits.
    patterns = ["a" * length for length in range(1, 447)]
    request = {"task": "aho", "patterns": patterns, "texts": ["a" * 200_000]}
    yield "aho: 446 nested patterns, counts 200000 down to 199555", request, {
        "counts": [[200_000 - len(pattern) + 1 for pattern in patterns]]}

    # All dictionary columns share one terminal. Alternate two text lengths to test reset.
    patterns = ["a" * 10] * 10_000
    texts = ["a" * 1999, "a" * 2001] * 50
    counts = [[1990] * len(patterns), [1992] * len(patterns)] * 50
    yield "aho: 100 texts x 10000 patterns, counts 1990/1992", {
        "task": "aho", "patterns": patterns, "texts": texts}, {"counts": counts}


def main():
    command = build("Java")
    for label, request, expected in cases():
        actual, = execute(command, [request])
        if request["task"] == "suffix":
            # The generated text has a unique longest repeat. Compare the full arrays
            # directly; rescanning every suffix for a 49998-character match is unnecessary.
            valid = (ints(actual.get("sa")) and ints(actual.get("lcp")) and
                     all(actual.get(key) == value for key, value in expected.items()))
        else:
            valid = check(request, actual, expected)
        if not valid:
            raise SystemExit("FAIL " + label)
        print("PASS " + label)


if __name__ == "__main__":
    main()
