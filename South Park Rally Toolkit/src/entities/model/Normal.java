package entities.model;

import java.util.Locale;

public class Normal extends Vertex {

	public Normal(float x, float y, float z) {
		super(x, y, z);
	}

	public String toString() {
		return String.format(Locale.US, "vn %.6f %.6f %.6f", x, y, z);
	}
}
