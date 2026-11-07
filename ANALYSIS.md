# Assignment 4: Analysis

**Student:** Amir Kunanbayev, SE-2514  
**Laptop:** Intel Core i5-12450H, 16 GB RAM.

Space bounds below separate auxiliary space from output. The supplied
input uses `O(n+m+1)` for two strings, `O(n+1)` for a tree, block
array or single text, and `O(L+T+p+q+1)` for a dictionary request
with L pattern characters, T text characters, p patterns and q texts.

## 1. Longest common subsequence

`dp[i][j]` is the LCS length for the first `i` and `j` characters.
An empty prefix gives zero. Equal last characters can be paired, giving
`1 + dp[i-1][j-1]`. If they differ, at least one is omitted, so take
the larger of the cells above and to the left. Row order computes each
dependency first.

Trace back from the last cell. Record each diagonal match. On a mismatch, follow a
neighbour with the same value, preferring up. Reverse the result. For `ab` and `ba`, Java
returned `a`; the exhaustive reference found `b`. Both have length 1.
For input lengths `n,m`, time and auxiliary space are
`O((n+1)(m+1))`. The returned sequence uses `O(L+1)` space for L matches.

## 2. Ordered merging

`cost[l][r]` is the minimum cost for blocks `l..r`. A singleton costs
zero. The final merge must split at some `k` between `l` and `r-1`.
Try each split and minimize the two child costs plus the interval sum,
`prefix[r+1] - prefix[l]`. Shorter intervals are computed first.

Keep the first minimizing split and emit both child plans before their
parent. This guarantees the two blocks exist when merged. For
`[1,100,1]`, either legal first merge costs 101 and the total is 203.
Zero or one block gives an empty plan. For n blocks, time is `O(n³)`,
auxiliary space is `O(n²)` and the plan uses `O(n)` space. Empty input
takes constant time and space. All costs use `long`.

## 3. Independent set on a tree

Root the tree at 0 and visit children before parents in the DP pass.
For a vertex `u`, `take[u]` is its weight plus each child's skip value.
`skip[u]` sums the larger of take and skip for each child. Children have
no edges between their subtrees, so their choices can be combined.
A leaf starts with its own weight and a skip value of zero.

Reconstruct in parent-first order. A chosen parent forces its child
to skip; otherwise choose the vertex when `take > skip`. Ties go to
skip, and an empty or all-negative tree can return weight zero.
Both traversals are iterative. Time and auxiliary space are `O(n+1)`;
the output uses `O(k+1)` for k selected vertices. Weights sum in `long`.

## 4. Exact string search

Let n be the text length, m the pattern length and z the number of
matches. All three methods return increasing positions in `O(z+1)`
output space. An empty pattern matches all n+1 boundaries. A longer
pattern has no valid start.

**Naive.** At each start from 0 through n−m, compare characters until
a mismatch or a complete match. Moving one position preserves overlaps:
`aaa` in `aaaaa` gives `[0,1,2]`. Time is `O(nm+n+1)`; auxiliary
space is `O(1)`.

**KMP.** Build the prefix table from left to right.
`prefix[i]` stores the length of the longest proper prefix of
`pattern[0..i]` that is also its suffix. During the scan, the matched length records the pattern prefix
ending at the previous text position. On mismatch, try the next shorter
border `prefix[matched-1]`. After a full match use `prefix[m-1]`,
giving starts 0, 2 and 4 for `aba` in `abababa`. Each extension pays for a later
decrease, giving `O(n+m+z+1)` time and `O(m+1)` auxiliary space.

**Rabin–Karp.** Use ASCII codes, base 31 and modulus 101. Remove the
outgoing character times `31^(m-1)`, shift by 31 and add the incoming
character. Normalize the remainder, then compare characters on every
equal hash. Both `ab` and `dj` hash to 75; only position 0 matches in
`abdj` for pattern `ab`. For `1 ≤ m ≤ n` and c equal-hash windows,
time is `O(n+m+cm)`; `O(nm+n+m+1)` covers all inputs. Auxiliary
space is `O(1)`.

## 5. Suffix array and Kasai LCP

Initial ranks are ASCII codes. At width w, sort starts by the two ranks
at i and i+w, using −1 beyond the end. These pairs order prefixes of
length 2w. Assign equal pairs equal ranks and double w until the whole
suffixes are ordered. Java comparison sorting takes `O(n log²(n+1))`
time for nonempty input.

Kasai scans starts in text order. The inverse array locates the previous
suffix in sorted order. Dropping one character from a shared prefix of
length h leaves h−1 shared characters. Suffixes sharing a prefix are
consecutive, so the new predecessor shares at least that many. Reusing
this length gives `O(n)` time. A maximum adjacent LCP is a longest
repeat; keep the first maximum. In `aaaaa`, `aaaa` occurs at 0 and 1.
Empty text returns two empty arrays and an empty string. Auxiliary and
output space are each `O(n+1)`.

## 6. Aho–Corasick

Insert the patterns into a trie and save a terminal state per dictionary
entry. Build failure links breadth-first using the transition from the
parent's failure state. Root children link to the root. Missing
transitions reuse the shorter state's transition.

Each text starts at the root with zero counts. After a character, the
state is the longest dictionary prefix ending there. Record a visit,
then propagate visits to failure parents in reverse breadth-first order:
each suffix pattern receives every occurrence. Duplicate entries read
the same terminal. For `he, she, he` in `she` the counts are `[1,1,1]`.
An empty text gives zeros; an empty dictionary gives one empty row per text.

For L pattern characters, S trie states and p patterns, construction
takes `O(L+95S+p)` time and `O(95S+p)` auxiliary space. Each text of
length t takes `O(t+S+p)`. For q texts the output uses `O(q(p+1)+1)`.

## 7. Tests

[Named cases](tests/student/cases.jsonl) give the inputs, expected and
actual answers, and the defect each case checks. The references enumerate
subsequences, adjacent merge orders and vertex subsets. For strings they
compare every start, sort short suffixes directly, or count each pattern
separately. Five valid/invalid witness pairs check ties and broken answers.

All 44 public cases and their repeated batch passed. The student suite
ran 56 named and 384 seeded cases (seed 41107) forwards and backwards in
one JVM. All 880 responses and 13 scale cases passed. The scale checks
include a 100,000-vertex chain with optimum 50,000,000,000,000 and a
50,000-character suffix array checked entry by entry. [Test log](tests/student/checks.txt)

## 8. Search benchmark

**Benchmark environment.** Ubuntu 24.04.3, Intel Xeon Platinum 8573C, 8 GiB container
limit, OpenJDK 17.0.20 (`-Xmx384m`), Python 3.12.14.

Each method searched the same inputs. For random cases, Java
`Random(41000+n)` draws lowercase text and pattern independently, then
inserts the pattern at a random valid start. Each random case had one
match. Repetitive cases use `a^n` and `a^m` with n−m+1 matches.

The full positions lists were checked against `String.startsWith` at
every start before timing. The JVM first warmed up with 1,024 calls per
method on each of two 3,000-character inputs. Each test pair then had
six warm-up batches and five measured batches, rotating method order.
`System.nanoTime()` covered 16 calls per batch, including preprocessing
and building the output. Compilation, JVM startup, input generation,
JSON and validation were outside the timer. Each result was checked.

Medians in microseconds per call (five batches per method):

| n / m | Input | Naive | KMP | RK |
|---|---|---:|---:|---:|
| 500 / 16 | random | 1.27 | 1.54 | 2.79 |
| 500 / 16 | repetitive | 18.18 | 3.99 | 19.06 |
| 1500 / 64 | random | 4.09 | 3.87 | 8.44 |
| 1500 / 64 | repetitive | 181.21 | 12.47 | 196.94 |
| 3000 / 128 | random | 7.43 | 9.03 | 17.02 |
| 3000 / 128 | repetitive | 766.09 | 30.82 | 884.86 |

[Five raw timings per method, in nanoseconds per batch](tests/student/benchmark.csv).

<details>
<summary>Raw timings (ns per batch of 16 calls)</summary>

```text
input      n/m       method   five measured batches
random     500/16    naive   20127 20689 20258 20408 20278
random     500/16    kmp     20769 20943 24864 24762 24578
random     500/16    rk      45044 71222 44545 44588 44562
repetitive 500/16    naive   372246 288097 290932 291353 288127
repetitive 500/16    kmp     67638 63802 59602 60567 65446
repetitive 500/16    rk      304927 295512 307588 309471 295741
random     1500/64   naive   59852 109759 60112 65471 65541
random     1500/64   kmp     61845 61980 80872 61778 61779
random     1500/64   rk      135145 135081 134923 135217 135013
repetitive 1500/64   naive   2925151 2899439 2855711 3416881 2838517
repetitive 1500/64   kmp     191197 179230 210868 206837 199546
repetitive 1500/64   rk      3070968 3034245 3151089 3183271 3373257
random     3000/128  naive   118815 118820 118900 118929 123381
random     3000/128  kmp     143136 124930 144827 144438 417636
random     3000/128  rk      272072 271642 283214 272370 293815
repetitive 3000/128  naive   11555224 12733180 12257367 12447938 11607452
repetitive 3000/128  kmp     404758 666300 521255 493138 401523
repetitive 3000/128  rk      14157797 14111338 14896200 12942045 14297151
```

</details>

At 3,000/128, all 2,873 repetitive windows match. Naive and RK compare
128 characters at every start; KMP uses the prefix it has already
matched. Its median was 30.82 µs per call. On the random input the
naive median was 7.43 µs. These measurements depend on the inputs and
runtime. Modulus 101 increases the chance of RK collisions; other hash
choices change how often character verification runs.

## 9. Build and run

Run with JDK 17 and Python 3.12+ from the repository root.

```sh
python tools/run.py --setup
python tools/run.py --input examples/lcs.jsonl
python tools/run.py --check
python tests/student/check.py
python tests/student/scale.py
javac --release 17 -cp Java/build -d Java/build tests/student/Benchmark.java
java -Xmx384m -cp Java/build Benchmark
```

## 10. Sources and assistance

The [course template](https://github.com/RZhaniya/daa-assignment-4-archive/tree/4686cc60334aa189c2ac168e10a82d882b32858c)
supplied the contracts, course notes, Java transport and public checks.

ChatGPT was used as a supplementary tool to help debug Java syntax, test the Java algorithms, 
format the Markdown report, and generate boilerplate code for the test suite