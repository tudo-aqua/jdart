package gov.nasa.jpf.jdart.constraints.tree;

interface ExplorationStrategy {

    LeafNode nextOpenNode();

    void newOpen(LeafNode n);

    boolean hasMoreNodes();
}
