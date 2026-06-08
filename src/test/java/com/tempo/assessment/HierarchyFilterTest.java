package com.tempo.assessment;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HierarchyFilterTest {

    private interface HierarchyFactory {
        Hierarchy create(int[] nodeIds, int[] depths);
    }

    // Parameterize over both hierarchy types so every case runs against both code paths.
    static Stream<HierarchyFactory> hierarchyConstructors() {
        return Stream.of(ArrayBasedHierarchy::new, ArrayBasedIndexedHierarchy::new);
    }

    // -------------------------------------------------------------------------
    // Original test case from the assessment
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("hierarchyConstructors")
    void originalExample(HierarchyFactory factory) {
        Hierarchy input = factory.create(
                new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11},
                new int[] {0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2}
        );
        Hierarchy actual = HierarchyFilter.filter(input, id -> id % 3 != 0);
        assertEquals("[1:0, 2:1, 5:1, 8:0, 10:1, 11:2]", actual.formatString());
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("hierarchyConstructors")
    void emptyHierarchy(HierarchyFactory factory) {
        Hierarchy input = factory.create(new int[]{}, new int[]{});
        Hierarchy actual = HierarchyFilter.filter(input, id -> true);
        assertEquals("[]", actual.formatString());
    }

    @ParameterizedTest
    @MethodSource("hierarchyConstructors")
    void allNodesPass(HierarchyFactory factory) {
        Hierarchy input = factory.create(
            new int[]{1, 2, 3, 4},
            new int[]{0, 1, 2, 1}
        );
        Hierarchy actual = HierarchyFilter.filter(input, id -> true);
        assertEquals("[1:0, 2:1, 3:2, 4:1]", actual.formatString());
    }

    @ParameterizedTest
    @MethodSource("hierarchyConstructors")
    void allNodesFail(HierarchyFactory factory) {
        Hierarchy input = factory.create(
            new int[]{1, 2, 3},
            new int[]{0, 1, 2}
        );
        Hierarchy actual = HierarchyFilter.filter(input, id -> false);
        assertEquals("[]", actual.formatString());
    }

    @ParameterizedTest
    @MethodSource("hierarchyConstructors")
    void singleNodePasses(HierarchyFactory factory) {
        Hierarchy input = factory.create(new int[]{42}, new int[]{0});
        Hierarchy actual = HierarchyFilter.filter(input, id -> true);
        assertEquals("[42:0]", actual.formatString());
    }

    @ParameterizedTest
    @MethodSource("hierarchyConstructors")
    void singleNodeFails(HierarchyFactory factory) {
        Hierarchy input = factory.create(new int[]{42}, new int[]{0});
        Hierarchy actual = HierarchyFilter.filter(input, id -> false);
        assertEquals("[]", actual.formatString());
    }

    // -------------------------------------------------------------------------
    // Subtree pruning
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("hierarchyConstructors")
    void failingRootRemovesEntireTree(HierarchyFactory factory) {
        // Root 1 fails — its entire subtree (2, 3, 4) must go too.
        // Root 5 passes and keeps its child 6.
        Hierarchy input = factory.create(
            new int[]{1, 2, 3, 4, 5, 6},
            new int[]{0, 1, 1, 2, 0, 1}
        );
        Hierarchy actual = HierarchyFilter.filter(input, id -> id != 1);
        assertEquals("[5:0, 6:1]", actual.formatString());
    }

    @ParameterizedTest
    @MethodSource("hierarchyConstructors")
    void failingIntermediateNodeRemovesItsSubtreeOnly(HierarchyFactory factory) {
        // Node 3 (depth 1) fails — its child 4 must be excluded even though 4 would pass.
        // Sibling 5 (depth 1) and root 1 are unaffected.
        Hierarchy input = factory.create(
            new int[]{1, 3, 4, 5},
            new int[]{0, 1, 2, 1}
        );
        Hierarchy actual = HierarchyFilter.filter(input, id -> id != 3);
        assertEquals("[1:0, 5:1]", actual.formatString());
    }

    @ParameterizedTest
    @MethodSource("hierarchyConstructors")
    void passingLeafWithFailingAncestorIsExcluded(HierarchyFactory factory) {
        // 2 fails; 4 passes but its grandparent chain contains 2 → excluded.
        Hierarchy input = factory.create(
            new int[]{1, 2, 3, 4},
            new int[]{0, 1, 2, 3}
        );
        Hierarchy actual = HierarchyFilter.filter(input, id -> id != 2);
        assertEquals("[1:0]", actual.formatString());
    }

    // -------------------------------------------------------------------------
    // Multiple roots / forest
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("hierarchyConstructors")
    void multipleRootsIndependentlyPruned(HierarchyFactory factory) {
        // Three roots: 10 passes, 20 fails, 30 passes.
        Hierarchy input = factory.create(
            new int[]{10, 11, 20, 21, 30, 31},
            new int[]{0,  1,  0,  1,  0,  1}
        );
        Hierarchy actual = HierarchyFilter.filter(input, id -> id != 20);
        assertEquals("[10:0, 11:1, 30:0, 31:1]", actual.formatString());
    }

    // -------------------------------------------------------------------------
    // Predicate invocation contract
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("hierarchyConstructors")
    void predicateNotInvokedForDescendantsOfFailingNode(HierarchyFactory factory) {
        Hierarchy input = factory.create(
            new int[]{1, 2, 3, 4, 5},
            new int[]{0, 1, 2, 2, 0}
        );
        Set<Integer> seen = new HashSet<>();
        HierarchyFilter.filter(input, id -> {
            seen.add(id);
            return id != 1; // root 1 fails → 2, 3, 4 must never be tested
        });
        assertEquals(Set.of(1, 5), seen);
    }

    @ParameterizedTest
    @MethodSource("hierarchyConstructors")
    void predicateInvokedExactlyOncePerCandidate(HierarchyFactory factory) {
        Hierarchy input = factory.create(
            new int[]{1, 2, 3},
            new int[]{0, 1, 1}
        );
        Map<Integer, Integer> counts = new HashMap<>();
        HierarchyFilter.filter(input, id -> {
            counts.merge(id, 1, Integer::sum);
            return true;
        });
        assertEquals(Map.of(1, 1, 2, 1, 3, 1), counts);
    }
}
