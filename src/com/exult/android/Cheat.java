package com.exult.android;

import android.view.KeyEvent;

public final class Cheat extends GameSingletons {
	private boolean hackMover;
	private boolean godMode;
	private boolean wizardMode;
	private boolean infravision;
	private boolean pickpocket;
	
	public boolean inHackMover() {
		return hackMover;
	}
	public void toggleHackMover() {
		hackMover = !hackMover;
		ExultActivity.showToast(hackMover?"HackMover Mode":"Ending HackMover");
	}
	public boolean inGodMode() {
		return godMode;
	}
	public void toggleGodMode() {
		godMode = !godMode;
		ExultActivity.showToast(godMode?"GodMode Mode":"Ending GodMode");
	}
	public boolean inWizardMode() {
		return wizardMode;
	}
	public void toggleWizardMode() {
		wizardMode = !wizardMode;
		ExultActivity.showToast(wizardMode?"WizardMode Mode":"Ending WizardMode");
	}
	public boolean inPickpocket() {
		return pickpocket;
	}
	public void togglePickpocket() {
		pickpocket = !pickpocket;
		ExultActivity.showToast(pickpocket?"Pickpocket Mode":"Ending Pickpocket");
	}
	public boolean inInfravision() {
		return infravision;
	}
	public void toggleInfravision() {
		infravision = !infravision;
		ExultActivity.showToast(infravision?"Infravision Mode":"Ending Infravision");
		clock.setPalette();
	}
	public void mapTeleport() {
		ShapeFrame map;
		if (EUtil.U7exists(EFile.PATCH_MINIMAPS) != null) {
			VgaFile mini = new VgaFile(EFile.PATCH_MINIMAPS, null);
			if ((map = mini.getShape(0, gwin.getMap().getNum())) == null)
				map = mini.getShape(0, 0);
		} else {
			int shnum = game.getShape("sprites/cheatmap");
			map = ShapeFiles.GAME_FLX.getShape(shnum, 1);
		}
		CheatMapGump g = new CheatMapGump(map);
		g.track();
	}
	private static class CheatMapGump extends Gump.Modal {
		Tile t = new Tile();	// For Avatar's pos.
		CheatMapGump(ShapeFrame map) {
			super(map);	// Manage everything here.
		}
		// Handle events for ClickTracker
		@Override
		public void onUp(int mx, int my) {	
			mx -= x - shape.getXLeft() + border;
			my -= y - shape.getYAbove() + border;
			
			t.tx = (short)(((mx + 0.5)*worldsize) / (shape.getWidth() - 2*border));
			t.ty = (short)(((my + 0.5)*worldsize) / (shape.getHeight() - 2*border));
			System.out.printf("mouseUp at %1$d, %2$d, tx = %3$d, ty = %4$d\n", mx, my,
					t.tx, t.ty);
			// World-wrapping.
			t.tx = (short)((t.tx + EConst.c_num_tiles)%EConst.c_num_tiles);
			t.ty = (short)((t.ty + EConst.c_num_tiles)%EConst.c_num_tiles);
			System.out.println("Teleporting to " + t.tx + "," + t.ty + "!");
			t.tz = 0;			
			ExultActivity.showToast("Teleport!!!");
			gwin.teleportParty(t, false, -1);
			close();
		}
		@Override
		public void keyDown(int chr) { // Character typed.
			if (chr == KeyEvent.KEYCODE_BACK)
				close();
		}
		private static final int border=2;			// For showing map.
		private static final int worldsize = EConst.c_tiles_per_chunk * EConst.c_num_chunks;
		public void paint() {
			super.paint();
			// mark current location
			int xx, yy;
			gwin.getMainActor().getTile(t);
			xx = ((t.tx * (shape.getWidth() - border*2)) / worldsize);
			yy = ((t.ty * (shape.getHeight() - border*2)) / worldsize);

			xx += x - shape.getXLeft() + border;
			yy += y - shape.getYAbove() + border;
			gwin.getWin().fill8((byte)255, 1, 5, xx, yy - 2);
			gwin.getWin().fill8((byte)255, 5, 1, xx - 2, yy);
		}
	}
}
