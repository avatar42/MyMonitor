/**
 * 
 */
package dea.monitor.gui;

import java.awt.Color;

import sun.swing.PrintColorUIResource;

/**
 * @author deabigt
 * 
 */
public class Colors extends PrintColorUIResource {

	public Colors(int rgb, Color printColor) {
		super(rgb, printColor);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// /**
	// * @param rgb
	// */
	// public Colors(int rgb) {
	// super(rgb);
	// // TODO Auto-generated constructor stub
	// }
	//
	// /**
	// * @param rgba
	// * @param hasalpha
	// */
	// public Colors(int rgba, boolean hasalpha) {
	// super(rgba, hasalpha);
	// // TODO Auto-generated constructor stub
	// }
	//
	/**
	 * @param r
	 * @param g
	 * @param b
	 */
	public Colors(int r, int g, int b) {
		this(r, g, b, 255);
	}

	/**
	 * @param r
	 * @param g
	 * @param b
	 */
	public Colors(float r, float g, float b) {
		this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5),
				(int) (b * 255 + 0.5));
	}

	// /**
	// * @param cspace
	// * @param components
	// * @param alpha
	// */
	// public Colors(ColorSpace cspace, float[] components, float alpha) {
	// super(cspace, components, alpha);
	// // TODO Auto-generated constructor stub
	// }
	//
	/**
	 * @param r
	 * @param g
	 * @param b
	 * @param a
	 */
	public Colors(int r, int g, int b, int a) {
		this(((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8)
				| ((b & 0xFF) << 0), Color.black);
	}

	// /**
	// * Return a Color made form the rgb values passed in hex.
	// *
	// * @param r
	// * @param g
	// * @param b
	// */
	// public Colors(String r, String g, String b) {
	// super(Integer.parseInt(r, 16), Integer.parseInt(g, 16), Integer
	// .parseInt(b, 16));
	// }
	//
	// /**
	// * Return a Color made form the rgb values passed in hex.
	// *
	// * @param rgb
	// * String format of 6 chars (RRGGBB)
	// */
	// public Colors(String rgb) {
	// super(Integer.parseInt(rgb.substring(0, 2), 16), Integer.parseInt(rgb
	// .substring(2, 4), 16), Integer
	// .parseInt(rgb.substring(4, 6), 16));
	// }
	//
	// /**
	// * Return a Color made form the rgb values passed in hex.
	// *
	// * @param rgb
	// * String format of 3 chars (RGB)
	// */
	// public Colors(String rgb, boolean ignored) {
	// super(Integer.parseInt(rgb.substring(0, 1), 16) * 16, Integer.parseInt(
	// rgb.substring(1, 2), 16) * 16, Integer.parseInt(rgb.substring(
	// 2, 3), 16) * 16);
	// }

	/**
	 * @param r
	 * @param g
	 * @param b
	 * @param a
	 */
	public Colors(float r, float g, float b, float a) {
		this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5),
				(int) (b * 255 + 0.5), (int) (a * 255 + 0.5));
	}

	/**
	 * Returns a string representation of this <code>Color</code>. This method
	 * is intended to be used only for debugging purposes. The content and
	 * format of the returned string might vary between implementations. The
	 * returned string might be empty but cannot be <code>null</code>.
	 * 
	 * @return a string representation of this <code>Color</code>.
	 */
	public String toString() {
		return getClass().getName() + "[r=" + getRed() + ",g=" + getGreen()
				+ ",b=" + getBlue() + "]";
	}

	/**
	 * return the inverse color to this one by subtracting 255 from each RGB
	 * value
	 * 
	 * @return
	 */
	public static Colors getInverse(Color c) {
		int r = c.getRed();
		int b = c.getBlue();
		int g = c.getGreen();

		return new Colors(Math.abs(r - 255), Math.abs(g - 255),
				Math.abs(b - 255));
	}

	/**
	 * return the gradient darker color to this one by subtracting 25 from each
	 * RGB value
	 * 
	 * @return
	 */
	public Colors getGradeDarker() {
		int r = getRed();
		if (r < 25)
			r = 25;
		int b = getBlue();
		if (b < 25)
			b = 25;
		int g = getGreen();
		if (g < 25)
			g = 25;

		return new Colors(Math.abs(r - 25), Math.abs(g - 25), Math.abs(b - 25));
	}

	/**
	 * return the gradient darker color to this one by adding 25 from each RGB
	 * value
	 * 
	 * @return
	 */
	public Colors getGradeLighter() {
		int r = getRed();
		if (r > 230)
			r = 230;
		int b = getBlue();
		if (b > 230)
			b = 230;
		int g = getGreen();
		if (g > 230)
			g = 230;

		return new Colors(Math.abs(r + 25), Math.abs(g + 25), Math.abs(b + 25));
	}

	public String toHexString() {
		String r = Integer.toHexString(getRed());
		if (r.length() < 2) {
			r = "0" + r;
		}
		String g = Integer.toHexString(getGreen());
		if (g.length() < 2) {
			g = "0" + r;
		}
		String b = Integer.toHexString(getBlue());
		if (b.length() < 2) {
			b = "0" + r;
		}

		return r + g + b;
	}
}
