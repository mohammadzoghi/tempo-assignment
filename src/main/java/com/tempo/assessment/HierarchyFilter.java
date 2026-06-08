package com.tempo.assessment;

import java.util.function.IntPredicate;
import java.util.stream.IntStream;

/**
 * A node is present in the filtered hierarchy iff its node ID passes the predicate
 * and all of its ancestors pass it as well.
 */
public class HierarchyFilter {
    /**
     * Returns a filtered view of the given hierarchy.
     *
     * <p>If the hierarchy is an {@link IndexedHierarchy}, filtering uses the precomputed
     * jump index to skip pruned subtrees in O(1) per rejected root. Otherwise, a linear
     * scan is used, which is O(n).
     *
     * @param hierarchy the source hierarchy to filter
     * @param nodeIdPredicate the predicate that each node ID must satisfy
     * @return a new {@code Hierarchy} containing only nodes whose IDs pass the predicate
     *         and whose entire ancestor chain also passes the predicate
     */
    public static Hierarchy filter(Hierarchy hierarchy, IntPredicate nodeIdPredicate) {
        if (hierarchy instanceof IndexedHierarchy indexed) {
            return filterWithJumps(indexed, nodeIdPredicate);
        }
        return filterLinear(hierarchy, nodeIdPredicate);
    }

    /**
     * O(n) linear scan. Tracks the depth at which we entered a pruned subtree and skips
     * all nodes until we rise back above it.
     */
    private static Hierarchy filterLinear(Hierarchy h, IntPredicate predicate) {
        Skipper skipper = new Skipper();
        IntStream.Builder nodeIds = IntStream.builder();
        IntStream.Builder depths = IntStream.builder();
        IntStream.range(0, h.size())
            .forEach(i -> {
                int depth = h.depth(i);
                if (skipper.shouldProcess(depth)) {
                    if (predicate.test(h.nodeId(i))) {
                        nodeIds.accept(h.nodeId(i));
                        depths.accept(depth);
                        skipper.proceed();
                    } else {
                        skipper.skip(depth);
                    }
                }
            });

        return new ArrayBasedHierarchy(nodeIds.build().toArray(), depths.build().toArray());
    }

    /**
     * Better average-case using the precomputed jump index on {@link IndexedHierarchy}.
     * A failing node jumps directly to its first non-descendant, costing O(k + f) where
     * k = nodes kept and f = nodes that fail outside an already-pruned subtree.
     */
    private static Hierarchy filterWithJumps(IndexedHierarchy h, IntPredicate predicate) {
        IntStream.Builder nodeIds = IntStream.builder();
        IntStream.Builder depths = IntStream.builder();

        for (int i = 0; i < h.size(); ) {
            if (predicate.test(h.nodeId(i))) {
                nodeIds.accept(h.nodeId(i));
                depths.accept(h.depth(i));
                i++;
            } else {
                i = h.nextNonDescendant(i);
            }

        }

        return new ArrayBasedHierarchy(nodeIds.build().toArray(), depths.build().toArray());
    }

    /**
     * Tracks whether the linear scan is currently inside a pruned subtree.
     *
     * <p>When a node fails the predicate, its entire subtree must be skipped. The
     * {@code Skipper} remembers the depth of the failing node; every subsequent node
     * whose depth is strictly greater is a descendant and is skipped. As soon as a
     * node appears at the same depth or shallower, the scan has exited the pruned
     * subtree and normal processing resumes.
     */
    private static class Skipper {
        /**
         * Depth of the root of the currently-skipped subtree, or {@code -1} when no
         * subtree is being skipped. Nodes with {@code depth > skipBelowDepth} are
         * descendants of the pruned root and must be ignored.
         */
        private int skipBelowDepth;

        /**
         * Creates a {@code Skipper} in the non-skipping state.
         */
        Skipper() {
            this.skipBelowDepth = -1;
        }

        /**
         * Clears any active skip state, indicating the scan should process subsequent
         * nodes normally. Called after a node passes the predicate.
         */
        void proceed() {
            this.skipBelowDepth = -1;
        }

        /**
         * Marks the start of a pruned subtree rooted at the given depth. All nodes
         * deeper than {@code depth} encountered next are descendants and will be
         * skipped until a node at depth {@code <= depth} is reached.
         *
         * @param depth the depth of the failing node whose subtree must be skipped
         */
        void skip(int depth) {
            this.skipBelowDepth = depth;
        }

        /**
         * Returns whether a node at the given depth should be processed.
         *
         * @param depth the depth of the candidate node
         * @return {@code true} if no skip is active, or the node has risen back to
         *         (or above) the pruned root's depth; {@code false} if the node lies
         *         strictly inside the pruned subtree
         */
        boolean shouldProcess(int depth) {
            return this.skipBelowDepth == -1 || depth <= this.skipBelowDepth;
        }
    }
}
