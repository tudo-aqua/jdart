package gov.nasa.jpf.jdart.constraints.tree.internal;

import java.util.Comparator;
import java.util.PriorityQueue;

class BFSExplorationStrategy implements ExplorationStrategy {

    private PriorityQueue<LeafNode> queue = new PriorityQueue<>(100, new Comparator<LeafNode>() {
        // Note: this comparator imposes orderings that are inconsistent with equals.
        @Override
        public int compare(LeafNode o1, LeafNode o2) {
            return o1.depth() - o2.depth();
        }
    });

    @Override
    public LeafNode nextOpenNode() {
        return queue.poll();
    }

    @Override
    public void newOpen(LeafNode n) {
        queue.offer(n);
    }

    @Override
    public boolean hasMoreNodes() {
        return !queue.isEmpty();
    }
}
