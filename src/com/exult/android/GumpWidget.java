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
	public boolean isDraggable() {
		return true;
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
			super(par, game.getShape("gumps/check"), px, py);
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
			/* +++++++FINISH
			else
				gumpman.addGump(parent.getContainer(), game.getShape(
						"gumps/statsdisplay"));
			*/
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
			gumpman->do_modal_gump(menu, Mouse::hand);
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
			//++++++FINISH gwin.toggleCombat();
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
			/* +++++++FINISH
			boolean prot = !actor.isCombatProtected();
			setPushed(prot);
			parent.paint();
			actor.setCombatProtected(prot);
			if (!prot)			// Toggled off?
				return true;
			// On?  Got to turn off others.
			Actor *party[9];		// Get entire party, including Avatar.
			int cnt = gwin->get_party(party, 1);
			for (int i = 0; i < cnt; i++) {
				if (party[i] != actor && party[i]->is_combat_protected())
					party[i]->set_combat_protected(false);
			// +++++Should also update gumps.
			}
			*/
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
			setFrame((getFrameNum() + 1)%nframes);
			// Flag that player set the mode.
			// +++++++FINISH actor.setAttackMmode(getFrameNum(), true);
			paint();
			gwin.setPainted();
			return true;
		}
	};

}
