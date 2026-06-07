package com.tempo.assessment;

import java.util.function.IntPredicate;

/**
 * A node is present in the filtered hierarchy iff its node ID passes the predicate
 * and all of its ancestors pass it as well.
 */
public class HierarchyFilter {
    /**
     * Returns a filtered view of the given hierarchy.
     *
     * @param hierarchy the source hierarchy to filter
     * @param nodeIdPredicate the predicate that each node ID must satisfy
     * @return a new {@code Hierarchy} containing only nodes whose IDs pass the predicate
     *         and whose entire ancestor chain also passes the predicate
     */
    public static Hierarchy filter(Hierarchy hierarchy, IntPredicate nodeIdPredicate) {
        // TODO implement
        return new ArrayBasedHierarchy(new int[0], new int[0]);
    }
}
