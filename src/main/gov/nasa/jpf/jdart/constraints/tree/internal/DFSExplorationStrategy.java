package gov.nasa.jpf.jdart.constraints.tree.internal;

import java.util.LinkedList;

class DFSExplorationStrategy implements ExplorationStrategy {

    private LinkedList<LeafNode> nodes = new LinkedList<>();

    @Override
    public LeafNode nextOpenNode() {
        return nodes.pop();
    }

    @Override
    public void newOpen(LeafNode n) {
        nodes.push(n);
    }

    @Override
    public boolean hasMoreNodes() {
        return !nodes.isEmpty();
    }
}
