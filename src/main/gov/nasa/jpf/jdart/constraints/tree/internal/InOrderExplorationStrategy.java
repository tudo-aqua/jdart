package gov.nasa.jpf.jdart.constraints.tree.internal;

import java.util.LinkedList;

class InOrderExplorationStrategy implements ExplorationStrategy {

    private LinkedList<LeafNode> nodes = new LinkedList<>();

    @Override
    public LeafNode nextOpenNode() {
        return nodes.poll();
    }

    @Override
    public void newOpen(LeafNode n) {
        nodes.offer(n);
    }

    @Override
    public boolean hasMoreNodes() {
        return !nodes.isEmpty();
    }
}
