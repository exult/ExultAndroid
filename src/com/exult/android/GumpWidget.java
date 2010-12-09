package com.exult.android;

public class GumpWidget extends ShapeID {
	protected Gump parent;
	protected short x, y;	// Coords relative to parent.
	
	public boolean onWidget(int mx, int my) {
		mx -= parent.getX() + x;	// Get point rel. to gump.
		my -= parent.getY() + y;
		ShapeFrame cshape = getShape();
		return cshape != null && cshape.hasPoint(mx, my);
	}
	public void paint() {
		int px = 0, py = 0;
		if (parent != null) {
			px = parent.getX();
			py = parent.getY();
		}
		paintShape(x+px, y+py);
	}
	public Button onButton(int mx, int my) {
		return null;
	}
	public static class Button extends GumpWidget {
		public Button onButton(int mx, int my) {
			if (onWidget(mx, my))
				return this;
			else return null;
		}
	}
}
