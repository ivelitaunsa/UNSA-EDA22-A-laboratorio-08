public class BTree<T extends Comparable<T>> {
  
  BNodeGeneric<T> root;
  int MinDeg;

  public BTree(int deg) {
    this.root = null;
    this.MinDeg = deg;
  }

  public void add(T value) {
    if (root == null){
  
      root = new BNodeGeneric<T>(MinDeg,true);
      root.keys.set(0, value);
      root.num = 1;
    }
    else {
      // When the root node is full, the tree will grow high
      if (root.num == 2*MinDeg-1){
        BNodeGeneric<T> s = new BNodeGeneric<T>(MinDeg,false);
        // The old root node becomes a child of the new root node
        s.children.set(0, root);
        // Separate the old root node and give a key to the new node
        s.splitChild(0,root);
        // The new root node has 2 child nodes. Move the old one over there
        int i = 0;
        if (s.keys.get(0).compareTo(value)==-1)
          i++;

        // REVISION EN CASO DE ERROR
        s.children.get(i).insertNotFull(value);
  
        root = s;
      }
      else
        root.insertNotFull(value);
    }
  }
  
  public void remove(T value) {
    if (root == null){
      System.out.println("The tree is empty");
      return;
    }

    root.remove(value);

    if (root.num == 0){ // If the root node has 0 keys
      // If it has a child, its first child is taken as the new root,
      // Otherwise, set the root node to null
      if (root.isLeaf)
        root = null;
      else
        root = root.children.get(0);
    }
  }
  
  public BNodeGeneric<T> search(T value) {
    return root == null ? null : root.search(value);
  }
  
}
