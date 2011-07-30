package com.exult.android;

public class GumpWidget extends GameSingletons {
	protected Gump parent;
	protected ShapeFrame shape;
	protected int shapeNum, frameNum;
	protected ShapeFiles shapeFile;
	protected int x, y;	// Coords relative to parent.
	
	protected void initShape(int shnum, ShapeFiles file) {
		shapeFile = file == null ? ShapeFiles.GUMPS_VGA : file;
		frameNum = 0;
		shapeNum = shnum;
		shape = shapeFile.getShape(shapeNum, frameNum);
	}
	public GumpWidget(Gump par, int shnum, int px, int py) {
		initShape(shnum, ShapeFiles.GUMPS_VGA);
		parent = par;
		x = px; y = py;
	}
	public GumpWidget(Gump par, int shnum, int px, int py, ShapeFiles file) {
		initShape(shnum, file);
		parent = par;
		x = px; y = py;
	}
	public GumpWidget(Gump par, ShapeFrame s, int px, int py) {
		parent = par;
		shape = s;
		x = px; y = py;
	}
	public ShapeFrame getShape() {
		return shape;
	}
	public void setFrame(int f) {
		frameNum = f;
		shape = shapeFile.getShape(shapeNum, frameNum);
	}
	public boolean onWidget(int mx, int my) {
		mx -= parent.getX() + x;	// Get point rel. to gump.
		my -= parent.getY() + y;
		return shape != null && shape.hasPoint(mx, my);
	}
	//	Get area covered by gump and its contents.
	public Rectangle getDirty(Rectangle rect) {
		if (shape == null) 
			rect.set(0,0,0,0);
		else {
			int px = 0, py = 0;
			if (parent != null) {
				px = parent.getX();
				py = parent.getY();
			}
			rect.set(x+px - shape.getXLeft(), y+py - shape.getYAbove(),
				shape.getWidth(), shape.getHeight());
		}
		return rect;
	}
	public void paint() {
		int px = 0, py = 0;
		if (parent != null) {
			px = parent.getX();
			py = parent.getY();
		}
		shape.paint(gwin.getWin(), x+px, y+py);
	}
	public Button onButton(int mx, int my) {
		return null;
	}
	public boolean isDraggable() {
		return true;
	}
	public static abstract class Button extends GumpWidget {
		private boolean pushed;		// In pushed state.
		protected ShapeFrame onShape;	// Show this when pushed (or mouse over in some cases).
		
		public Button(Gump par, int shnum, int px, int py) {
			super(par, shnum, px, py);
			onShape = shapeFile.getShape(shapeNum, frameNum + 1);
		}
		public Button(Gump par, int shnum, int px, int py, ShapeFiles file) {
			super(par, shnum, px, py, file);			
			onShape = shapeFile.getShape(shapeNum, frameNum + 1);
		}
		public Button(Gump par, ShapeFrame onShape, ShapeFrame offShape, int px, int py) {
			super(par, offShape, px, py);
			this.onShape = onShape;
		}
		public Button onButton(int mx, int my) {
			if (onWidget(mx, my))
				return this;
			else return null;
		}				// What to do when 'clicked':
		public abstract boolean activate(boolean button);
		// Or double-clicked.
		public void doubleClicked(int x, int y) {
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
		public void unpush(boolean button) {
			if (button) {
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
			if (pushed)
				onShape.paint(gwin.getWin(), x+px, y+py);
			else
				shape.paint(gwin.getWin(), x+px, y+py);
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
			super(par, game.getShape("gumps/check"), px, py);
		}
							// What to do when 'clicked':
		public boolean activate(boolean button) {
			if (!button) 
				return false;
			audio.playSfx(Audio.gameSfx(74));
			parent.close();
			return true;
		}
		@Override
		public boolean onWidget(int mx, int my) {
			mx -= parent.getX() + x;	// Get point rel. to gump.
			my -= parent.getY() + y;
			// Check for box, as we're on a touch-screen
			return shape != null && shape.boxHasPoint(mx, my);
		}
	}
	/*
	 *	A 'heart' button for bringing up stats.
	 */
	public static class HeartButton extends Button {
		public HeartButton(Gump par, int px, int py) {
			super(par, game.getShape("gumps/heart"), px, py);
		}
						// What to do when 'clicked':
		public boolean activate(boolean button) {
			if (!button) 
				return false;
			else
				gumpman.addGump(parent.getContainer(), game.getShape(
						"gumps/statsdisplay"), false);
			return true;
		}
	}
	/*
	 *	A diskette for bringing up the 'save' box.
	 */
	public static class DiskButton extends Button {
		public DiskButton(Gump par, int px, int py) {
			super(par, game.getShape("gumps/disk"), px, py);
		}
						// What to do when 'clicked':
		public boolean activate(boolean button) {
			if (!button) 
				return false;
			/* ++++++FINISH
			Gamemenu_gump *menu = new Gamemenu_gump();
			gumpman.do_modal_gump(menu, Mouse::hand);
			*/
			return true;
		}
	};
	/*
	 *	The combat toggle button.
	 */
	public static class CombatButton extends Button {
		public CombatButton(Gump par, int px, int py) {
			super(par, game.getShape("gumps/combat"), px, py);
		}
						// What to do when 'clicked':
		public boolean activate(boolean button) {
			if (!button) 
				return false;
			gwin.toggleCombat();
			setPushed(gwin.inCombat());
			parent.paint();
			return true;
		}
		public void paint() {
			setPushed(gwin.inCombat());
			super.paint();
		}
	};

	/*
	 *	The halo button.
	 */
	public static class HaloButton extends Button {
		Actor actor;			// Who this represents.
		public HaloButton(Gump par, int px, int py, Actor a) {
			super(par, game.getShape("gumps/halo"), px, py);
			actor = a;
		}
						// What to do when 'clicked':
		public boolean activate(boolean button) {
			if (!button) 
				return false;
			// Want to toggle it.
			
			boolean prot = !actor.isCombatProtected();
			setPushed(prot);
			parent.paint();
			actor.setCombatProtected(prot);
			if (!prot)			// Toggled off?
				return true;
			// On?  Got to turn off others.
			Actor party[] = new Actor[9];
					// Get entire party, including Avatar.
			for (Actor a : party) {
				if (a != actor && a.isCombatProtected())
					a.setCombatProtected(false);
			// +++++Should also update gumps.
			}
			return true;	
		}
	};

	/*
	 *	Combat mode.  Has 10 frames corresponding to Actor::Attack_mode.
	 */
	public static class CombatModeButton extends Button {
		Actor actor;			// Who this represents.
		public CombatModeButton(Gump par, int px, int py, Actor a) {
			super(par, game.getShape("gumps/combatmode"), px, py);
			actor = a;
		}
		public boolean activate(boolean button) {
			if (!button) 
				return false;
			// Only Avatar gets last frame (manual)
			int nframes = actor == gwin.getMainActor() ? 10 : 9;
			frameNum = (frameNum + 1)%nframes;
			shape = shapeFile.getShape(shapeNum, frameNum);
			// Flag that player set the mode.
			actor.setAttackMode(frameNum, true);
			paint();
			gwin.setPainted();
			return true;
		}
	};
	public static abstract class TextButton extends Button {
		// Palette Indices
		private static final byte 
			TB_OUTER_BORDER = (byte) 133,
			TB_OUTER_BORDER_CORNER = (byte)		142,
			TB_OUTER_BORDER_PUSHED_TOP = (byte)	144,
			//UNUSED TB_OUTER_BORDER_PUSHED_LEFT = (byte)	140,
			TB_INNER_BORDER_HIGHLIGHT = (byte)	138,
			TB_INNER_BORDER_LOWLIGHT = (byte)	142,
			TB_INNER_BORDER_CORNER = (byte)		141,
			TB_INNER_BORDER_TR_HIGH = (byte)		137,
			TB_INNER_BORDER_TR_CORNER = (byte)	138,
			TB_INNER_BORDER_BL_CORNER = (byte)	144,
			TB_BACKGROUND = (byte)			140,
			TB_RT_HIGHLIGHT = (byte)			139;
		protected String text;
		protected int		text_x;
		protected int		text_y;
		protected int		width;
		protected int		height;

		protected void init() {
			// Must be at least 11 units high
			if (height < 11) 
				height = 11;
			// Text y is based on gump height of 11
			text_y = 2 + (height - 11)/2;
			// We will get the text width
			int text_width = fonts.getTextWidth(2, text); // SB+++++ "SMALL_BLACK_FONT"
			if (width < text_width + 4) 
				width = text_width + 4;
			// We want to find the starting point for the text (horizontal)
			text_x = (width - text_width) >> 1;
		}
		public TextButton(Gump p, String str, int x, int y, int w, int h) {
			super(p, 0, x, y, null);
			text = str;
			init();
		}
		public void paint() {
			ImageBuf iwin = gwin.getWin();

			int offset = 0;
			int px = x;
			int py = y;

			if (parent != null) {
				px += parent.getX();
				py += parent.getY();
			}

			// The the push dependant edges
			if (isPushed()) {
				// Top left corner
				iwin.fill8(TB_OUTER_BORDER_CORNER, 1, 1, px, py);
				// Bottom left corner
				iwin.fill8(TB_OUTER_BORDER_CORNER, 1, 1, px, py+height-1);
				// Top right corner
				iwin.fill8(TB_OUTER_BORDER_CORNER, 1, 1, px+width-1, py);
				// Top edge
				iwin.fill8(TB_OUTER_BORDER_PUSHED_TOP, width-2, 1, px+1, py);
				// Left edge
				iwin.fill8(TB_OUTER_BORDER_PUSHED_TOP, 1, height-2, px, py+1);

				offset = 1;
			} else {
				// Bottom right corner
				iwin.fill8(TB_OUTER_BORDER_CORNER, 1, 1, px+width-1, py+height-1);
				// Bottom left corner
				iwin.fill8(TB_OUTER_BORDER_CORNER, 1, 1, px, py+height-1);
				// Top right corner
				iwin.fill8(TB_OUTER_BORDER_CORNER, 1, 1, px+width-1, py+height-1);
				// Bottom edge
				iwin.fill8(TB_OUTER_BORDER, width-2, 1, px+1, py+height-1);
				// Right edge
				iwin.fill8(TB_OUTER_BORDER, 1, height-2, px+width-1, py+1);
			}
			// 'Outer' Top and Left Edges
			// Top left corner
			iwin.fill8(TB_OUTER_BORDER_CORNER, 1, 1, px+offset, py+offset);
			// Top edge
			iwin.fill8(TB_OUTER_BORDER, width-2, 1, px+1+offset, py+offset);
			// Left edge
			iwin.fill8(TB_OUTER_BORDER, 1, height-2, px+offset, py+1+offset);
		
			// 'Inner' Edges
			// Top left corner
			iwin.fill8(TB_INNER_BORDER_CORNER, 1, 1, px+offset+1, py+offset+1);
			// Top Right corner
			iwin.fill8(TB_INNER_BORDER_TR_CORNER, 1, 1, px+width+offset-2, py+offset+1);
			// Top Right Highlight 1
			iwin.fill8(TB_INNER_BORDER_TR_HIGH, 1, 1, px+width+offset-3, py+offset+1);
			// Top Right Highlight 1
			iwin.fill8(TB_INNER_BORDER_TR_HIGH, 1, 1, px+width+offset-2, py+offset+2);
			// Bottom left corner
			iwin.fill8(TB_INNER_BORDER_BL_CORNER, 1, 1, px+offset+1, py+height+offset-2);

			// Top edge
			iwin.fill8(TB_INNER_BORDER_HIGHLIGHT, width-5, 1, px+2+offset, py+offset+1);
			// Left edge
			iwin.fill8(TB_INNER_BORDER_LOWLIGHT, 1, height-4, px+offset+1, py+2+offset);
			// Right edge
			iwin.fill8(TB_INNER_BORDER_HIGHLIGHT, 1, height-5, px+width+offset-2, py+3+offset);
			// Bottom edge
			iwin.fill8(TB_INNER_BORDER_LOWLIGHT, width-4, 1, px+2+offset, py+height+offset-2);

			// Background Fill 
			iwin.fill8(TB_BACKGROUND, width-4, height-4, px+2+offset, py+2+offset);
			// Top Right Highligh on Background 
			iwin.fill8(TB_RT_HIGHLIGHT, 1, 1, px+width+offset-3, py+offset+2);

			fonts.paintText(2, text, px+text_x+offset, py+text_y+offset);// SB+++++ "SMALL_BLACK_FONT"
		}

		public boolean onWidget(int mx, int my) {
			int px = x;
			int py = y;

			if (parent != null){
				px += parent.getX();
				py += parent.getY();
			}
			if (mx < px || mx >= px + width) return false;
			if (my < py || my >= py + height) return false;
			return true;
		}
	};
}
