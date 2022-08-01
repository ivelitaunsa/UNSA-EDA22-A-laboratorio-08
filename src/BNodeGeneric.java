import java.util.Vector;

public class BNodeGeneric<T extends Comparable<T>> {

  Vector<T> keys;
  Vector<BNodeGeneric<T>> children;
  int MinDeg;
  int num;
  boolean isLeaf;

  public BNodeGeneric(int deg, boolean isLeaf) {
    this.MinDeg = deg;
    this.isLeaf = isLeaf;
    this.keys = new Vector<T>(2 * this.MinDeg - 1);
    this.children = new Vector<BNodeGeneric<T>>(2 * this.MinDeg);
  }

  public BNodeGeneric<T> search(T value) {
    int i = 0;
    while (i < num && (value.compareTo(keys.get(i)) == 1))
      i++;

    if (value.compareTo(keys.get(i)) == 0)
      return this;
    if (isLeaf)
      return null;
    return children.get(i).search(value);

  }

  // INSERTION MODE

  public void splitChild(int i, BNodeGeneric<T> y) {
    // First, create a node to hold the keys of MinDeg-1 of y
    BNodeGeneric<T> z = new BNodeGeneric<T>(y.MinDeg, y.isLeaf);
    z.num = MinDeg - 1;

    // Pass the properties of y to z
    for (int j = 0; j < MinDeg - 1; j++)
      z.keys.set(j, y.keys.get(j + MinDeg));
    if (!y.isLeaf) {
      for (int j = 0; j < MinDeg; j++)
        z.children.set(j, y.children.get(j + MinDeg));
    }
    y.num = MinDeg - 1;

    // Insert a new child into the child
    for (int j = num; j >= i + 1; j--)
      children.set(j + 1, children.get(j));
    children.set(i + 1, z);

    // Move a key in y to this node
    for (int j = num - 1; j >= i; j--)
      keys.set(j + 1, keys.get(j));
    keys.set(i, y.keys.get(MinDeg - 1));

    num = num + 1;
  }

  public void insertNotFull(T value) {

    int i = num - 1; // Initialize i as the rightmost index

    if (isLeaf) { // When it is a leaf node
      // Find the location where the new key should be inserted
      while (i >= 0 && keys.get(i).compareTo(value) == 1) {
        keys.set(i + 1, keys.get(i)); // keys backward shift
        i--;
      }
      keys.set(i + 1, value);
      num = num + 1;
    } else {
      // Find the child node location that should be inserted
      while (i >= 0 && keys.get(i).compareTo(value) == 1)
        i--;
      if (children.get(i + 1).num == 2 * MinDeg - 1) { // When the child node is full
        splitChild(i + 1, children.get(i + 1));
        // After splitting, the key in the middle of the child node moves up, and the
        // child node splits into two
        if (keys.get(i + 1).compareTo(value) == -1)
          i++;
      }
      children.get(i + 1).insertNotFull(value);
    }
  }

  // REMOVE MODE

  public int findKey(T value) {

    int idx = 0;
    // The conditions for exiting the loop are: 1.idx == num, i.e. scan all of them
    // once
    // 2. IDX < num, i.e. key found or greater than key
    while (idx < num && keys.get(idx).compareTo(value) == -1)
      ++idx;
    return idx;
  }

  public void remove(T value) {

    int idx = findKey(value);
    if (idx < num && keys.get(idx).compareTo(value) == 0) { // Find key
      if (isLeaf) // key in leaf node
        removeFromLeaf(idx);
      else // key is not in the leaf node
        removeFromNonLeaf(idx);
    } else {
      if (isLeaf) { // If the node is a leaf node, then the node is not in the B tree
        System.out.printf("The key %d is does not exist in the tree\n", value);
        return;
      }

      // Otherwise, the key to be deleted exists in the subtree with the node as the
      // root

      // This flag indicates whether the key exists in the subtree whose root is the
      // last child of the node
      // When idx is equal to num, the whole node is compared, and flag is true
      boolean flag = idx == num;

      if (children.get(idx).num < MinDeg) // When the child node of the node is not full, fill it first
        fill(idx);

      // If the last child node has been merged, it must have been merged with the
      // previous child node, so we recurse on the (idx-1) child node.
      // Otherwise, we recurse to the (idx) child node, which now has at least the
      // keys of the minimum degree
      if (flag && idx > num)
        children.get(idx - 1).remove(value);
      else
        children.get(idx).remove(value);
    }
  }

  public void removeFromLeaf(int idx) {

    // Shift from idx
    for (int i = idx + 1; i < num; ++i)
      keys.set(i - 1, keys.get(i));
    num--;
  }

  public void removeFromNonLeaf(int idx) {

    T value = keys.get(idx);

    // If the subtree before key (children[idx]) has at least t keys
    // Then find the precursor 'pred' of key in the subtree with children[idx] as
    // the root
    // Replace key with 'pred', recursively delete pred in children[idx]
    if (children.get(idx).num >= MinDeg) {
      T pred = getPred(idx);
      keys.set(idx, pred);
      children.get(idx).remove(pred);
    }
    // If children[idx] has fewer keys than MinDeg, check children[idx+1]
    // If children[idx+1] has at least MinDeg keys, in the subtree whose root is
    // children[idx+1]
    // Find the key's successor 'succ' and recursively delete succ in
    // children[idx+1]
    else if (children.get(idx + 1).num >= MinDeg) {
      T succ = getSucc(idx);
      keys.set(idx, succ);
      children.get(idx + 1).remove(succ);
    } else {
      // If the number of keys of children[idx] and children[idx+1] is less than
      // MinDeg
      // Then key and children[idx+1] are combined into children[idx]
      // Now children[idx] contains the 2t-1 key
      // Release children[idx+1], recursively delete the key in children[idx]
      merge(idx);
      children.get(idx).remove(value);
    }
  }

  public T getPred(int idx) { // The predecessor node is the node that always finds the rightmost node from
                              // the left subtree

    // Move to the rightmost node until you reach the leaf node
    BNodeGeneric<T> cur = children.get(idx);
    while (!cur.isLeaf)
      // REVISAR EN CASO DE ERROR
      cur = cur.children.get(cur.num);
    return cur.keys.get(cur.num - 1);
  }

  public T getSucc(int idx) { // Subsequent nodes are found from the right subtree all the way to the left

    // Continue to move the leftmost node from children[idx+1] until it reaches the
    // leaf node
    BNodeGeneric<T> cur = children.get(idx + 1);
    while (!cur.isLeaf)
      // REVISAR EN CASO DE ERROR
      cur = cur.children.get(0);
    return cur.keys.get(0);
  }

  // Merge childre[idx+1] into childre[idx]
  public void merge(int idx) {

    BNodeGeneric<T> child = children.get(idx);
    BNodeGeneric<T> sibling = children.get(idx + 1);

    // Insert the last key of the current node into the MinDeg-1 position of the
    // child node
    child.keys.set(MinDeg - 1, keys.get(idx));

    // keys: children[idx+1] copy to children[idx]
    for (int i = 0; i < sibling.num; ++i)
      child.keys.set(i + MinDeg, sibling.keys.get(i));

    // children: children[idx+1] copy to children[idx]
    if (!child.isLeaf) {
      for (int i = 0; i <= sibling.num; ++i)
        child.children.set(i + MinDeg, sibling.children.get(i));
    }

    // Move keys forward, not gap caused by moving keys[idx] to children[idx]
    for (int i = idx + 1; i < num; ++i)
      keys.set(i - 1, keys.get(i));
    // Move the corresponding child node forward
    for (int i = idx + 2; i <= num; ++i)
      children.set(i - 1, children.get(i));

    child.num += sibling.num + 1;
    num--;
  }

  // Fill children[idx] with less than MinDeg keys
  public void fill(int idx) {

    // If the previous child node has multiple MinDeg-1 keys, borrow from them
    if (idx != 0 && children.get(idx - 1).num >= MinDeg)
      borrowFromPrev(idx);
    // The latter sub node has multiple MinDeg-1 keys, from which to borrow
    else if (idx != num && children.get(idx + 1).num >= MinDeg)
      borrowFromNext(idx);
    else {
      // Merge children[idx] and its brothers
      // If children[idx] is the last child node
      // Then merge it with the previous child node or merge it with its next sibling
      if (idx != num)
        merge(idx);
      else
        merge(idx - 1);
    }
  }

  // Borrow a key from children[idx-1] and insert it into children[idx]
  public void borrowFromPrev(int idx) {

    BNodeGeneric<T> child = children.get(idx);
    BNodeGeneric<T> sibling = children.get(idx - 1);

    // The last key from children[idx-1] overflows to the parent node
    // The key[idx-1] underflow from the parent node is inserted as the first key in
    // children[idx]
    // Therefore, sibling decreases by one and children increases by one
    for (int i = child.num - 1; i >= 0; --i) // children[idx] move forward
      child.keys.set(i + 1, child.keys.get(i));

    if (!child.isLeaf) { // Move children[idx] forward when they are not leaf nodes
      for (int i = child.num; i >= 0; --i)
        child.children.set(i + 1, child.children.get(i));
    }

    // Set the first key of the child node to the keys of the current node [idx-1]
    child.keys.set(0, keys.get(idx - 1));
    if (!child.isLeaf) // Take the last child of sibling as the first child of children[idx]
      child.children.set(0, sibling.children.get(sibling.num));

    // Move the last key of sibling up to the last key of the current node
    keys.set(idx - 1, sibling.keys.get(sibling.num - 1));
    child.num += 1;
    sibling.num -= 1;
  }

  // Symmetric with borowfromprev
  public void borrowFromNext(int idx) {

    BNodeGeneric<T> child = children.get(idx);
    BNodeGeneric<T> sibling = children.get(idx + 1);

    child.keys.set(child.num, keys.get(idx));

    if (!child.isLeaf)
      child.children.set(child.num + 1, sibling.children.get(0));

    keys.set(idx, sibling.keys.get(0));

    for (int i = 1; i < sibling.num; ++i)
      sibling.keys.set(i - 1, sibling.keys.get(i));

    if (!sibling.isLeaf) {
      for (int i = 1; i <= sibling.num; ++i)
        sibling.children.set(i - 1, sibling.children.get(i));
    }
    child.num += 1;
    sibling.num -= 1;
  }

}
