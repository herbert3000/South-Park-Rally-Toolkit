package entities.rbh;

import java.util.Locale;

public class BodyNode {

	private BodyNode sibling;
	private BodyNode child;
	private float[] values;
	private int index3;
	private int[] indices2;

	public BodyNode(float[] values, int index3, int[] indices2) {
		this.values = values;
		this.index3 = index3;
		this.indices2 = indices2;
	}

	public BodyNode(BodyNode sibling, BodyNode child, float[] values) {
		this.sibling = sibling;
		this.child = child;
		this.values = values;
	}

	public BodyNode getSibling() {
		return sibling;
	}

	public BodyNode getChild() {
		return child;
	}

	public float[] getValues() {
		return values;
	}

	public int getIndex3() {
		return index3;
	}

	public int[] getIndices2() {
		return indices2;
	}

	public String toString() {
		String str = String.format(Locale.US, "{ %f, %f, %f, %f, ", values[0], values[1], values[2], values[3]);
		
		if (sibling != null) {
			return str += sibling.toString() + ", " + child.toString() + " } ";
		}
		
		str += index3 + ", [ ";
		for (int i = 0; i < indices2.length; i++) {
			str += indices2[i];
			if (i != indices2.length - 1) {
				str += ", ";
			}
		}
		return str += " ] } ";
	}
}
