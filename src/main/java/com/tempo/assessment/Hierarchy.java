package com.tempo.assessment;

/**
 * A {@code Hierarchy} stores an arbitrary <i>forest</i> (an ordered collection of ordered trees)
 * as an array of node IDs in the order of DFS traversal, combined with a parallel array of node depths.
 *
 * <p>Parent-child relationships are identified by the position in the array and the associated depth.
 * Each tree root has depth 0, its children have depth 1 and follow it in the array, their children
 * have depth 2 and follow them, etc.
 *
 * <p>Invariants on the depths array:
 * <ul>
 *   <li>Depth of the first element is 0.</li>
 *   <li>If the depth of a node is {@code D}, the depth of the next node can be:
 *     <ul>
 *       <li>{@code D + 1} — next node is a child;</li>
 *       <li>{@code D} — next node is a sibling;</li>
 *       <li>{@code d < D} — next node is not related.</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public interface Hierarchy {
    /**
     * Returns the number of nodes in the hierarchy.
     *
     * @return the total node count
     */
    int size();

    /**
     * Returns the unique ID of the node at the given index.
     * @param index must be non-negative and less than {@link #size()}
     * @return the unique ID of the node at {@code index}
     */
    int nodeId(int index);

    /**
     * Returns the depth of the node at the given index.
     * @param index must be non-negative and less than {@link #size()}
     * @return the depth of the node at {@code index}
     */
    int depth(int index);

    /**
     * Returns a compact debug string listing each nodeId:depth pair.
     *
     * @return a string of the form {@code [id1:depth1, id2:depth2, ...]}
     */
    default String formatString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(nodeId(i)).append(":").append(depth(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
