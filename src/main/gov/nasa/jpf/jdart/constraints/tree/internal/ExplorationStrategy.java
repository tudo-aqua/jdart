package gov.nasa.jpf.jdart.constraints.tree.internal;

interface ExplorationStrategy {

    LeafNode nextOpenNode();

    void newOpen(LeafNode n);

    boolean hasMoreNodes();
}
