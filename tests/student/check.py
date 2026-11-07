"""Small reference solutions, witness checks and mixed JSONL requests."""
import argparse
import itertools
import json
import random
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[1] / "tools"))
from public_validate import check
from run import build, execute, select_language


def subsequences(text):
    return {"".join(part) for k in range(len(text) + 1)
            for part in itertools.combinations(text, k)}


def best_merge(blocks):
    if len(blocks) < 2:
        return 0, []
    best = None
    for i in range(len(blocks) - 1):
        left, split, a = blocks[i]
        _, right, b = blocks[i + 1]
        combined = (left, right, a + b)
        rest, plan = best_merge(blocks[:i] + [combined] + blocks[i + 2:])
        candidate = a + b + rest
        if best is None or candidate < best[0]:
            best = candidate, [[left, split, right]] + plan
    return best


def positions(text, pattern):
    return [i for i in range(len(text) + 1) if text.startswith(pattern, i)]


def reference(x):
    task = x["task"]
    if task == "lcs":
        common = subsequences(x["a"]) & subsequences(x["b"])
        answer = max(common, key=lambda s: (len(s), s))
        return {"length": len(answer), "sequence": answer}
    if task == "merge":
        cost, plan = best_merge([(i, i, v) for i, v in enumerate(x["sizes"])])
        return {"cost": cost, "plan": plan}
    if task == "tree":
        weight, selected = 0, []
        for mask in range(1 << x["n"]):
            if any(mask & (1 << u) and mask & (1 << v) for u, v in x["edges"]):
                continue
            chosen = [u for u in range(x["n"]) if mask & (1 << u)]
            total = sum(x["weights"][u] for u in chosen)
            if total > weight:
                weight, selected = total, chosen
        return {"weight": weight, "selected": selected}
    if task in ("naive", "kmp", "rk"):
        return {"positions": positions(x["text"], x["pattern"])}
    if task == "suffix":
        text = x["text"]
        sa = sorted(range(len(text)), key=lambda i: text[i:])
        lcp = [0] * len(sa)
        repeated = ""
        for k in range(1, len(sa)):
            a, b = text[sa[k - 1]:], text[sa[k]:]
            for first, second in zip(a, b):
                if first != second:
                    break
                lcp[k] += 1
            if lcp[k] > len(repeated):
                repeated = a[:lcp[k]]
        return {"sa": sa, "lcp": lcp, "repeated": repeated}
    if task == "aho":
        return {"counts": [[len(positions(s, p)) for p in x["patterns"]]
                           for s in x["texts"]]}
    raise ValueError(task)


def named_cases():
    cases = []

    def add(task, purpose, **fields):
        cases.append((purpose, {"task": task, **fields}))

    for a, b in [("", ""), ("", "abc"), ("abc", "")]:
        add("lcs", "Empty prefix base case", a=a, b=b)
    add("lcs", "Separated and repeated characters", a="aa!b!a", b="aba")
    add("lcs", "Two optimal subsequences", a="ab", b="ba")
    for sizes, purpose in [
        ([], "No blocks"), ([9], "One block"), ([0, 0, 0], "Zero-cost splits"),
        ([1, 100, 1], "Smallest blocks are not adjacent; tied legal plans"),
        ([4, 1, 5, 3], "Replay merges in chronological order"),
        ([10**9] * 4, "Total cost exceeds a signed 32-bit integer"),
    ]:
        add("merge", purpose, sizes=sizes)
    for weights, edges, purpose in [
        ([], [], "Empty tree"), ([0], [], "Zero-weight singleton"),
        ([7], [], "Positive singleton"), ([-4, -2, -8], [[2, 1], [1, 0]], "All negative"),
        ([4, 4], [[0, 1]], "Two optimal one-vertex choices"),
        ([4, 0], [[0, 1]], "An adjacent zero-weight vertex is still forbidden"),
        ([-1, 10**9, 10**9, 10**9], [[1, 0], [2, 0], [0, 3]], "64-bit star optimum"),
    ]:
        add("tree", purpose, n=len(weights), weights=weights, edges=edges)
    for text, pattern, purpose in [
        ("", "", "Empty text and pattern"), ("hi", "", "Include the final boundary"),
        ("", "X", "Empty text with nonempty pattern"), ("ab", "abc", "Pattern too long"),
        ("Aa! a! A!", "A!", "Case and punctuation at the final start"),
        ('" \\!" \\!', '" \\!', "Quotes, spaces and backslashes"),
        ("aaaaa", "aaa", "Overlapping matches"), ("xaba", "ab", "Negative rolling remainder"),
        ("abdj", "ab", "Reject the dj collision with ab"),
    ]:
        for task in ("naive", "kmp", "rk"):
            add(task, purpose, text=text, pattern=pattern)
    for text, purpose in [
        ("", "Empty suffix array"), ("$", "One printable suffix"),
        ("aaaaa", "Overlapping longest repeat"), ("~ $!", "Distinct ASCII suffixes"),
        ("abXabYcdZcd", "Two equally long repeats"),
    ]:
        add("suffix", purpose, text=text)
    for patterns, texts, purpose in [
        ([], [], "Empty dictionary and texts"), ([], ["abc", ""], "One empty row per text"),
        (["ab"], [], "Empty texts array"),
        (["a", "aa", "aaa", "aa"], ["aaaa", "", "aaaa"], "Overlaps, duplicates and reset"),
        (["he", "she", "hers"], ["ushers"], "Count suffix-pattern matches"),
        (["ab"], ["a", "b"], "No match across a text boundary"),
    ]:
        add("aho", purpose, patterns=patterns, texts=texts)
    return cases


def random_cases():
    rng = random.Random(41107)

    def word(limit):
        return "".join(rng.choices("aaAb !~", k=rng.randrange(limit + 1)))

    cases = []
    for _ in range(48):
        weights = [rng.randrange(-5, 9) for _ in range(rng.randrange(10))]
        edges = [[i, rng.randrange(i)] for i in range(1, len(weights))]
        rng.shuffle(edges)
        text, pattern = word(14), word(5)
        requests = [
            {"task": "lcs", "a": word(8), "b": word(8)},
            {"task": "merge", "sizes": [rng.randrange(8) for _ in range(rng.randrange(8))]},
            {"task": "tree", "n": len(weights), "weights": weights, "edges": edges},
            {"task": "suffix", "text": word(15)},
            {"task": "aho", "patterns": [word(4) or "a" for _ in range(rng.randrange(6))],
             "texts": [word(14) for _ in range(rng.randrange(5))]},
        ]
        requests.extend({"task": t, "text": text, "pattern": pattern}
                        for t in ("naive", "kmp", "rk"))
        cases.extend(requests)
    rng.shuffle(cases)
    return cases


def check_witnesses():
    cases = [
        ({"task": "lcs", "a": "ab", "b": "ba"}, {"sequence": "a"}, {"sequence": "x"}),
        ({"task": "merge", "sizes": [1, 100, 1]},
         {"plan": [[1, 1, 2], [0, 0, 2]]}, {"plan": [[0, 0, 2], [1, 1, 2]]}),
        ({"task": "tree", "n": 2, "weights": [4, 4], "edges": [[0, 1]]},
         {"selected": [1]}, {"selected": [1, 1]}),
        ({"task": "tree", "n": 2, "weights": [4, 0], "edges": [[0, 1]]},
         {"selected": [0]}, {"selected": [0, 1]}),
        ({"task": "suffix", "text": "abXabYcdZcd"}, {"repeated": "cd"}, {"repeated": "ZZ"}),
    ]
    for x, valid, invalid in cases:
        expected = reference(x)
        if not check(x, expected | valid, expected) or check(x, expected | invalid, expected):
            raise AssertionError(f"Witness validation failed for {x}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--record", action="store_true", help="save complete named examples")
    args = parser.parse_args()
    named = named_cases()
    random_requests = random_cases()
    requests = [x for _, x in named] + random_requests
    expected = [reference(x) for x in requests]
    check_witnesses()
    command = build(select_language(None))
    results = execute(command, requests + requests[::-1])
    for i, actual in enumerate(results):
        j = i if i < len(requests) else 2 * len(requests) - 1 - i
        if not check(requests[j], actual, expected[j]):
            raise AssertionError(f"Request {requests[j]}\nExpected {expected[j]}\nActual {actual}")

    if args.record:
        with (HERE / "cases.jsonl").open("w", encoding="utf-8", newline="\n") as f:
            for i, (purpose, x) in enumerate(named):
                record = {"checks": purpose, "input": x, "expected": expected[i], "actual": results[i]}
                print(json.dumps(record, separators=(",", ":")), file=f)
    print(f"PASS {len(named)} named and {len(random_requests)} seeded cases; "
          f"{len(results)} requests in one JVM; 5 valid/invalid witness pairs")


if __name__ == "__main__":
    main()
