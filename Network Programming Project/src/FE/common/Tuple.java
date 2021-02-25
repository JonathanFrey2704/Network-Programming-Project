package common;

import java.util.Date;

public class Tuple<L, M, R> {
	
	 private final L left;
	 private final M middle;
	 private final R right;
	  

	  public Tuple(L left, M middle, R right) {
	    assert left != null;
	    assert right != null;
	    assert middle != null;

	    this.left = left;
	    this.right = right;
	    this.middle = middle;
	  }

	  public L getLeft() { return left; }
	  public R getRight() { return right; }
	  public M getMiddle() {return middle; }

	  public int hashCode() { return left.hashCode() ^ right.hashCode() ^ middle.hashCode(); }

	  public boolean equals(Object o) {
	    if (!(o instanceof Tuple)) return false;
	    Tuple pair = (Tuple) o;
	    return this.left.equals(pair.getLeft()) &&
	           this.right.equals(pair.getRight()) &&
	           this.middle.equals(pair.getMiddle());
	  }
}
