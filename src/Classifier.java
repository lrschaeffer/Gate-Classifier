import java.util.BitSet;
import java.util.regex.*;
import java.io.*;

public class Classifier {
    /* A data structure to hold relevant information about the class.
     * Designed so that the meet/join of two classes could be computed
     * relatively easily, although that feature is not yet implemented.
     */
    public static final int AFFINE      = 0x1;
    public static final int LINEAR      = 0x2;
    // affine & f[0] = 0
    public static final int ORTHO       = 0x4;
    // linear part is orthogonal
    public static final int INF0        = 0x8;
    // matrix columns are unit vectors
    public static final int INF4        = 0x10;
    // matrix columns are 1 (mod 4)
    public static final int INF2        = 0x20;
    // matrix columns are 1 (mod 2), or similar in non-affine case

    public static final int even        = 0x55555555;
    public int flags;
    public int mod;

    /* Classify a gate of bit size n, and truthtable G. */
    public Classifier(int n, int [] G) {
        int hw_vec = hw_diffs(n, G);
        mod = modClass(hw_vec);

        if (affine(n, G)) {
            flags |= AFFINE;

            if (G[0] == 0) flags |= LINEAR;

            BitSet [] matrix = make_matrix(n, G);
            int inf_vec = inf_diffs(n, matrix);
            int inf = modClass(inf_vec);
            if (inf     == 0) flags |= INF0;
            if (inf % 4 == 0) flags |= INF4;
            if (inf % 2 == 0) flags |= INF2;

            if (matrix_orthogonal(n, matrix) &&
                (flags & INF2) != 0) {
                flags |= ORTHO;
            }
        } else {
            if ((hw_vec & even) == 0) flags |= INF2;
        }
    }

    /* Main logic for naming the class. The way we store the class is
     * not by name, but by the set of invariants it satisfies.
     * Hence, we need to go through this complicated logic to figure
     * out what the name is.
     */
    public String toString() {
        if ((AFFINE & flags) != 0) {
            if ((flags & INF0) != 0) {
                switch (mod) {
                    case 0: return "EMPTY";
                    case 1: return "NOT";
                    default: return "NOTNOT"; /* mod = 2 */
                }
            } else if ((flags & INF4) != 0) {
                switch (mod) {
                    case 1: return "T6+NOT";
                    case 2: return "T6+NOTNOT";
                    default: return "T6"; /* mod = 4 */
                }
            } else if ((flags & ORTHO) != 0) {
                if ((flags & LINEAR) != 0) return "T4";
                switch (mod) {
                    case 1: return "F4+NOT";
                    case 2: return "F4+NOTNOT";
                    default: return "F4"; /* mod = 4 */
                }
            } else if ((flags & INF2) != 0) {
                switch (mod) {
                    case 1: return "CNOTNOT+NOT";
                    default: return "CNOTNOT"; /* mod = 2 */
                }
            } else {
                return "CNOT";
            }
        } else {
            if (mod == 0) {
                return "FREDKIN";
            } else if (mod == 1) {
                if ((flags & INF2) != 0) {
                    return "FREDKIN+NOT";
                } else {
                    return "ALL";
                }
            } else {
                return "MOD" + Integer.toString(mod);
            }
        }
    }

    /* True if the gate is affine */
    public static boolean affine(int n, int [] G) {
        int N = 1 << n;
        for (int x = 0; x < N; ++x) {
            int z = Integer.lowestOneBit(x);
            if ((G[x] ^ G[z] ^ G[x ^ z]) != G[0]) return false;
        }
        return true;
    }

    /* Computes a bit vector (int) representing the set
     * W(G) = { |G(x)| - |x| : x \in {0,1}^n }
     */
    public static int hw_diffs(int n, int [] G) {
        int N = 1 << n;
        int ret = 0;
        for (int x = 0; x < N; ++x) {
            int diff = Math.abs(Integer.bitCount(x) -
                                Integer.bitCount(G[x]));
            ret |= 1 << diff;
        }
        return ret;
    }

    /* Computes a bit vector (int) representing the set
     * { |Ax| - |x| : unit vectors x }
     */
    public static int inf_diffs(int n, BitSet [] matrix) {
        int ret = 0;
        for (int i = 0; i < n; ++i) {
            int diff = matrix[i].cardinality() - 1;
            ret |= 1 << diff;
        }
        return ret;
    }

    /* Checks whether distinct columns of a matrix are orthogonal.
     * Does NOT check any property of individual columns, so this does
     * not necessarily place G in the class of orthogonal gates.
     */
    public static boolean matrix_orthogonal(int n, BitSet [] matrix) {
        for (int i = 1; i < n; ++i) {
            for (int j = 0; j < i; ++j) {
                BitSet temp = (BitSet)matrix[i].clone();
                temp.and(matrix[j]);
                if (temp.cardinality() % 2 == 1) return false;
            }
        }
        return true;
    }

    /* No built-in int GCD. Seriously, Java?
     * Oh well, we'll implement it ourselves.
     * Note that gcd(x, 0) = x
     */
    public static int gcd(int a, int b) {
        if (b == 0) return a;
        return gcd(b, a % b);
    }

    /* GCD of the bits set in an int bit vector */
    int modClass(int v) {
        int ret = 0;
        while (v != 0) {
            int i = Integer.numberOfTrailingZeros(v);
            v ^= (1 << i);
            ret = gcd(i, ret);
        }
        return ret;
    }

    /* Given an affine gate, compute the matrix of the linear part */
    public static BitSet [] make_matrix(int n, int [] G) {
        BitSet [] ret = new BitSet[n];
        for (int i = 0; i < n; ++i) {
            int column = G[1 << i] ^ G[0];
            ret[i] = new BitSet();
            for (int j = 0; j < n; ++j) {
                ret[i].set(j, ((1 << j) & column) != 0);
            }
        }
        return ret;
    }

    /* Check that G is surjective, and hence bijective. */
    public static boolean invertible(int n, int [] G) {
        int N = 1 << n;
        BitSet hit = new BitSet(N);
        for (int i = 0; i < N; ++i) {
            hit.set(G[i]);
        }
        return hit.cardinality() == N;
    }

    public static void main (String [] args) throws IOException {
        BufferedReader br = new BufferedReader(
                            new InputStreamReader(System.in));

        int n = 0, count = 0;
        int [] G = null;

        Pattern tableEntry = Pattern.compile("[^01]*([01]+)[^01]+([01]+)[^01]*");
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            Matcher m = tableEntry.matcher(line);
            if (!m.matches()) continue;
            String x = m.group(1);
            String y = m.group(2);
            if (n == 0) { // new gate
                n = x.length();
                G = new int[1 << n];
            }
            count++;
            if (x.length() != n) {
                System.out.println("Error: Input length does not match gate size (" + x.length() + " != " + n + ").");
                System.exit(1);
            }
            if (y.length() != n) {
                System.out.println("Error: Output length does not match gate size (" + y.length() + " != " + n + ").");
                System.exit(1);
            }
            int i = Integer.parseInt(x, 2);
            int j = Integer.parseInt(y, 2);
            G[i] = j;
            if (count == (1 << n)) {
                if (!invertible(n, G)) {
                    System.out.println("Error: Non-reversible gate.");
                    System.exit(1);
                }
                Classifier c = new Classifier(n, G);
                System.out.println(c);
                n = count = 0; G = null;
            }
        }
        if (n != 0) {
            System.out.println("Error: End of file in the middle of a gate.");
            System.exit(1);
        }
    }
}
