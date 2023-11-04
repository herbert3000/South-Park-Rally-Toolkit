package entities.model;

import java.util.Locale;

public class Vertex {
	public float x;
	public float y;
	public float z;

	public Vertex(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void add(Vertex other) {
		this.x += other.x;
		this.y += other.y;
		this.z += other.z;
	}

	public String toString() {
		return String.format(Locale.US, "v %.6f %.6f %.6f", x, y, z);
	}

}
