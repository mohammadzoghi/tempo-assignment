package com.tempo.assessment;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.IntStream;

/**
 * An {@link IndexedHierarchy} backed by parallel arrays with a precomputed jump index.
 *
 * <p>The jump index is built in a single O(n) pass using a monotone stack.
 * A failing node during filtering can skip its entire subtree in O(1) via
 * {@link #nextNonDescendant(int)}.
 */
class ArrayBasedIndexedHierarchy extends ArrayBasedHierarchy implements IndexedHierarchy {
    private final int[] nextNonDescendantIndex;

    ArrayBasedIndexedHierarchy(int[] nodeIds, int[] depths) {
        super(nodeIds, depths);
        this.nextNonDescendantIndex = buildJumpIndex(depths);
    }

    @Override
    public int nextNonDescendant(int index) {
        return nextNonDescendantIndex[index];
    }

    /**
     * For each index i, computes the first j > i, where depth[j] &le; depth[i].
     * Uses a monotone stack: when we reach index i, every stacked index with depth
     * &ge; depth[i] has found its answer.
     */
    private static int[] buildJumpIndex(int[] depths) {
        int n = depths.length;
        int[] index = new int[n];
        Deque<Integer> stack = new ArrayDeque<>();

        IntStream.range(0, n).forEach(i -> {
            while (!stack.isEmpty() && depths[stack.peek()] >= depths[i]) {
                index[stack.pop()] = i;
            }
            stack.push(i);
        });
        stack.forEach(j -> index[j] = n);
        return index;
    }
}
