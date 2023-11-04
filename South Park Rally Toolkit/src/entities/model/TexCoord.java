package entities.model;

import java.util.Locale;

public class TexCoord {
	private float u;
	private float v;

	public TexCoord(float u, float v) {
		this.u = u;
		this.v = v;
	}

	public String toString() {
		return String.format(Locale.US, "vt %.6f %.6f", u, v);
	}
}
