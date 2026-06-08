package com.tempo.assessment;

/**
 * A {@link Hierarchy} that precomputes a jump index for efficient subtree skipping during filtering.
 *
 * <p>Use this when the hierarchy is built infrequently but filtered many times.
 */
interface IndexedHierarchy extends Hierarchy {
    /**
     * Returns the index of the first node after {@code index} that is not a descendant of it,
     * i.e. the first subsequent node whose depth is &le; {@code depth(index)}.
     * Returns {@link #size()} if no such node exists.
     */
    int nextNonDescendant(int index);
}
