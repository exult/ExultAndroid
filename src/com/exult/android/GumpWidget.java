package com.exult.android;

public class GumpWidget extends ShapeID {
	protected Gump parent;
	protected int x, y;	// Coords relative to parent.
	
	public GumpWidget(Gump par, int shnum, int px, int py) {
		super(shnum, 0, ShapeFiles.GUMPS_VGA);
		parent = par;
		x = px; y = py;
	}
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
	public static abstract class Button extends GumpWidget {
		private boolean pushed;		// In pushed state.

		public Button(Gump par, int shnum, int px, int py) {
			super(par, shnum, px, py);
		}
		public Button onButton(int mx, int my) {
			if (onWidget(mx, my))
				return this;
			else return null;
		}				// What to do when 'clicked':
		public abstract boolean activate(boolean button);
		// Or double-clicked.
		public void double_clicked(int x, int y) {
		}
		public boolean push(boolean button) {	// Redisplay as pushed.
			if (button) {
				setPushed(button);
				paint();
				gwin.setPainted();
				return true;
			}
			return false;
		}
		public void unpush(int button) {
			if (button == 1) {
				setPushed(false);
				paint();
				gwin.setPainted();
			}
		}
		public void paint() {
			int px = 0;
			int py = 0;
			if (parent != null){
				px = parent.getX();
				py = parent.getY();
			}
			int prev_frame = getFrameNum();
			setFrame(prev_frame + (isPushed()?1:0));
			paintShape(x+px, y+py);
			setFrame(prev_frame);
		}
		public final boolean isPushed() { 
			return pushed; 
		}
		public final void setPushed(boolean tf) {
			pushed = tf;
		}
	}
	public static class Checkmark extends Button {
		public Checkmark(Gump par, int px, int py) {
			super(par, 2, px, py);//+++FINISH: game->get_shape("gumps/check")
		}
							// What to do when 'clicked':
		public boolean activate(boolean button) {
			if (!button) 
				return false;
			/* ++++++FINISH
			Audio::get_ptr()->play_sound_effect(Audio::game_sfx(74));
			*/
			parent.close();
			return true;
		}
	}
}
