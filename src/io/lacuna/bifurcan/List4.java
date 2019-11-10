package io.lacuna.bifurcan;

import io.lacuna.bifurcan.nodes.ListNodes4.Node;

import java.util.Iterator;

import static io.lacuna.bifurcan.utils.Bits.log2Ceil;
import static java.lang.Math.min;
import static java.lang.System.arraycopy;

/**
 * An implementation of an immutable list which allows for elements to be added and removed from both ends of the
 * collection, as well as random-access reads and writes.  Due to its
 * <a href=https://infoscience.epfl.ch/record/169879/files/RMTrees.pdf>relaxed radix structure</a>, {@code slice()},
 * {@code concat()}, and {@code split()} are near-constant time.
 *
 * @author ztellman
 */
public class List4<V> implements IList<V>, Cloneable {

  // These values must match the value of MAX_BRANCHES in class
  // ListNodes4
  private static final int SHIFT_INCREMENT = 2;
  private static final int MAX_BRANCHES = 1 << SHIFT_INCREMENT;

  private Node root;
  private byte prefixLen, suffixLen;
  public Object[] prefix, suffix;
  private final Object editor;

  public static <V> List4<V> of(V... elements) {
    List4<V> list = new List4<V>().linear();
    for (V e : elements) {
      list.addLast(e);
    }
    return list.forked();
  }

  public static <V> List4<V> from(IList<V> list) {
    if (list instanceof List4) {
      return ((List4<V>) list).forked();
    } else {
      return from(list.iterator());
    }
  }

  public static <V> List4<V> from(Iterable<V> iterable) {
    return from(iterable.iterator());
  }

  public static <V> List4<V> from(Iterator<V> iterator) {
    List4<V> list = new List4<V>().linear();
    iterator.forEachRemaining(list::addLast);
    return list.forked();
  }

  public List4() {
    this.editor = null;
    this.root = Node.EMPTY;
    this.prefixLen = 0;
    this.prefix = null;
    this.suffixLen = 0;
    this.suffix = null;
  }

  List4(boolean linear, Node root, int prefixLen, Object[] prefix, int suffixLen, Object[] suffix) {
    this.editor = linear ? new Object() : null;
    this.root = root;
    this.prefixLen = (byte) prefixLen;
    this.suffixLen = (byte) suffixLen;
    this.prefix = prefix;
    this.suffix = suffix;
  }

  ///

  @Override
  public V nth(long idx) {
    int rootSize = root.size();
    if (idx < 0 || idx >= (rootSize + prefixLen + suffixLen)) {
      throw new IndexOutOfBoundsException(idx + " must be within [0," + size() + ")");
    }

    int i = (int) idx;

    // look in the prefix
    if (i < prefixLen) {
      return (V) prefix[prefix.length + i - prefixLen];

      // look in the tree
    } else if (i - prefixLen < rootSize) {
      return (V) root.nth(i - prefixLen, false);

      // look in the suffix
    } else {
      return (V) suffix[i - (rootSize + prefixLen)];
    }
  }

  @Override
  public long size() {
    return root.size() + prefixLen + suffixLen;
  }

  @Override
  public boolean isLinear() {
    return editor != null;
  }

  @Override
  public List4<V> addLast(V value) {
    return (isLinear() ? this : clone()).pushLast(value);
  }

  @Override
  public List4<V> addFirst(V value) {
    return (isLinear() ? this : clone()).pushFirst(value);
  }

  @Override
  public List4<V> removeLast() {
    return (isLinear() ? this : clone()).popLast();
  }

  @Override
  public List4<V> removeFirst() {
    return (isLinear() ? this : clone()).popFirst();
  }

  @Override
  public List4<V> set(long idx, V value) {
    int size = (int) size();
    if (idx < 0 || idx > size) {
      throw new IndexOutOfBoundsException();
    }

    if (idx == size) {
      return addLast(value);
    } else {
      return (isLinear() ? this : clone()).overwrite((int) idx, value);
    }
  }

  @Override
  public Iterator<V> iterator() {

    final Object[] initChunk;
    final int initOffset, initLimit;
    final int size = (int) size();
    final int rootSize = root.size();

    if (prefixLen > 0) {
      initChunk = prefix;
      initOffset = pIdx(0);
      initLimit = prefix.length;
    } else if (rootSize > 0) {
      initChunk = (Object[]) root.nth(0, true);
      initOffset = 0;
      initLimit = initChunk.length;
    } else {
      initChunk = suffix;
      initOffset = 0;
      initLimit = suffixLen;
    }

    return new Iterator<V>() {

      int idx = 0;

      Object[] chunk = initChunk;
      int offset = initOffset;
      int limit = initLimit;
      int chunkSize = limit - offset;

      @Override
      public boolean hasNext() {
        return idx < size;
      }

      @Override
      public V next() {
        V val = (V) chunk[offset++];

        if (offset == limit) {
          idx += chunkSize;
          if (idx < size) {
            if (idx == prefixLen + rootSize) {
              chunk = suffix;
              limit = suffixLen;
            } else {
              chunk = (Object[]) root.nth(idx - prefixLen, true);
              limit = chunk.length;
            }
            offset = 0;
            chunkSize = limit;
          }
        }

        return val;
      }
    };
  }

  @Override
  public List4<V> slice(long start, long end) {
    if (start < 0 || end > size()) {
      throw new IndexOutOfBoundsException();
    }

    int s = (int) start;
    int e = (int) end;

    // Consider adding a special case like this.  This seems perfectly
    // fine if the input List4 is forked, but if the input list is
    // linear, should the returned one always be the identical object
    // as the input parameter, and still linear?
    /*
    if (e - s <= (2 * MAX_BRANCHES)) {
      // The entire result fits in a single tree node plus suffix.
      // Simply create a new List4 with the specified elements.
      List4<V> list = new List4<V>().linear();
      for (int i = s; i < e; i++) {
        list.addLast(nth(i));
      }
      return list.forked();
    }
    */
    int pStart = min(prefixLen, s);
    int pEnd = min(prefixLen, e);
    int pLen = pEnd - pStart;
    Object[] pre = null;
    if (pLen > 0) {
      pre = new Object[1 << log2Ceil(pLen)];
      arraycopy(prefix, pIdx(pStart), pre, pre.length - pLen, pLen);
    }

    int sStart = Math.max(0, s - (prefixLen + root.size()));
    int sEnd = Math.max(0, e - (prefixLen + root.size()));
    int sLen = sEnd - sStart;
    Object[] suf = null;
    if (sLen > 0) {
      suf = new Object[1 << log2Ceil(sLen)];
      arraycopy(suffix, sStart, suf, 0, sLen);
    }

    return new List4<V>(
      isLinear(),
      root.slice(Math.max(0, min(root.size(), s - prefixLen)), Math.max(0, min(root.size(), e - prefixLen)), new Object()),
      pLen, pre, sLen, suf);
  }

  @Override
  public IList<V> concat(IList<V> l) {
    if (l instanceof List4) {
      List4<V> b = (List4<V>) l;
      Node r = root;
      Object editor = new Object();

      // append our own suffix
      if (suffixLen > 0) {
        r = r.pushLast(suffixArray(), editor);
      }

      // append their prefix
      if (b.prefixLen > 0) {
        r = r.pushLast(b.prefixArray(), editor);
      }

      if (b.root.size() > 0) {
        r = r.concat(b.root, editor);
      }

      return new List4<V>(
        isLinear(), r,
        prefixLen, prefixLen > 0 ? prefix.clone() : null,
        b.suffixLen, b.suffixLen > 0 ? b.suffix.clone() : null);

    } else {
      return Lists.concat(this, l);
    }
  }

  @Override
  public List4<V> forked() {
    return isLinear() ? new List4(false, root, prefixLen, prefix, suffixLen, suffix).clone() : this;
  }

  @Override
  public List4<V> linear() {
    return isLinear() ? this : new List4(true, root, prefixLen, prefix, suffixLen, suffix).clone();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof IList) {
      return Lists.equals(this, (IList<V>) obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    Iterator it = iterator();
    int h = 1;
    while (it.hasNext()) {
      h = 31 * h + it.next().hashCode();
    }
    return h;
  }

  @Override
  public String toString() {
    return Lists.toString(this);
  }

  @Override
  public List4<V> clone() {
    return new List4<V>(
      isLinear(), root,
      prefixLen, prefix == null ? null : prefix.clone(),
      suffixLen, suffix == null ? null : suffix.clone());
  }

  ///

  private Object[] suffixArray() {
    Object[] suf = new Object[suffixLen];
    arraycopy(suffix, 0, suf, 0, suf.length);
    return suf;
  }

  private Object[] prefixArray() {
    Object[] pre = new Object[prefixLen];
    arraycopy(prefix, pIdx(0), pre, 0, pre.length);
    return pre;
  }

  private int pIdx(int idx) {
    return prefix.length - prefixLen + idx;
  }

  List4<V> overwrite(int idx, V value) {
    int rootSize = root.size();

    // overwrite prefix
    if (idx < prefixLen) {
      prefix[prefix.length - prefixLen + idx] = value;

      // overwrite tree
    } else if (idx < (prefixLen + rootSize)) {
      root = root.set(editor, idx - prefixLen, value);

      // overwrite suffix
    } else {
      suffix[idx - (prefixLen + rootSize)] = value;
    }

    return this;
  }

  List4<V> pushFirst(V value) {

    if (prefix == null) {
      prefix = new Object[2];
    } else if (prefixLen == prefix.length) {
      Object[] newPrefix = new Object[min(MAX_BRANCHES, prefix.length << 1)];
      arraycopy(prefix, 0, newPrefix, newPrefix.length - prefixLen, prefixLen);
      prefix = newPrefix;
    }

    prefix[pIdx(-1)] = value;
    prefixLen++;

    if (prefixLen == MAX_BRANCHES) {
      Object editor = isLinear() ? this.editor : new Object();
      root = root.pushFirst(prefix, editor);
      prefix = null;
      prefixLen = 0;
    }

    return this;
  }

  List4<V> pushLast(V value) {

    if (suffix == null) {
      suffix = new Object[2];
    } else if (suffixLen == suffix.length) {
      Object[] newSuffix = new Object[min(MAX_BRANCHES, suffix.length << 1)];
      arraycopy(suffix, 0, newSuffix, 0, suffix.length);
      suffix = newSuffix;
    }

    suffix[suffixLen++] = value;

    if (suffixLen == MAX_BRANCHES) {
      Object editor = isLinear() ? this.editor : new Object();
      root = root.pushLast(suffix, editor);
      suffix = null;
      suffixLen = 0;
    }

    return this;
  }

  List4<V> popFirst() {

    if (prefixLen == 0) {
      if (root.size() > 0) {
        Object[] chunk = root.first();
        if (chunk != null) {
          Object editor = isLinear() ? this.editor : new Object();
          prefix = chunk.clone();
          prefixLen = (byte) prefix.length;
          root = root.popFirst(editor);
        }
      } else if (suffixLen > 0) {
        arraycopy(suffix, 1, suffix, 0, --suffixLen);
        suffix[suffixLen] = null;
      }
    }

    if (prefixLen > 0) {
      prefixLen--;
      prefix[pIdx(-1)] = null;
    }

    return this;
  }

  List4<V> popLast() {

    if (suffixLen == 0) {
      if (root.size() > 0) {
        Object[] chunk = root.last();
        if (chunk != null) {
          Object editor = isLinear() ? this.editor : new Object();
          suffix = chunk.clone();
          suffixLen = (byte) suffix.length;
          root = root.popLast(editor);
        }
      } else if (prefixLen > 0) {
        // TBD: This definitely looks suspect in terms of safe
        // cross-thread visibility of changes, and mutating data out
        // from under a different thread that could simultaneously be
        // reading what it think is an immutable data structure.
        prefixLen--;
        arraycopy(prefix, pIdx(-1), prefix, pIdx(0), prefixLen);
        prefix[pIdx(-1)] = null;
      }
    }

    if (suffixLen > 0) {
      suffix[--suffixLen] = null;
    }

    return this;
  }
}

/* Local Variables:      */
/* mode: java            */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
