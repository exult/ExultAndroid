package com.exult.android;

public final class Cheat extends GameSingletons {
	private boolean hackMover;
	public boolean inHackMover() {
		return hackMover;
	}
	public void toggleHackMover() {
		hackMover = !hackMover;
		ExultActivity.showToast(hackMover?"HackMover Mode":"Ending HackMover");
	}
	public void mapTeleport() {
		new CheatMapGump(gmap.getNum());
	}
	private static class CheatMapGump extends Gump.Modal {
		ShapeFrame map;
		int w, h;
		Tile t = new Tile();	// For Avatar's pos.
		CheatMapGump(int mapnum) {
			super(0, 0, 0, null);	// Manage everything here.
			if (EUtil.U7exists(EFile.PATCH_MINIMAPS) != null) {
				VgaFile mini = new VgaFile(EFile.PATCH_MINIMAPS, null);
				if ((map = mini.getShape(0, mapnum)) == null)
					map = mini.getShape(0, 0);
			} else {
				int shnum = game.getShape("sprites/cheatmap");
				map = ShapeFiles.GAME_FLX.getShape(shnum, 1);
			}
			// Get coords. for centered view.
			w = map.getWidth();
			h = map.getHeight();
			x = (gwin.getWidth() - w)/2 + map.getXLeft();
			y = (gwin.getHeight() - h)/2 + map.getYAbove();
			System.out.printf("Show map at %1$d, %2$d, %3$d, %4$d\n", x, y, w, h);
		}
		// Handle events:
		public boolean mouseDown(int mx, int my, int button) {
			return true;
		}
		public boolean mouseUp(int mx, int my, int button) {	
			mx -= x - map.getXLeft() + border;
			my -= y - map.getYAbove() + border;
			
			t.tx = (short)(((mx + 0.5)*worldsize) / (w - 2*border));
			t.ty = (short)(((my + 0.5)*worldsize) / (h - 2*border));
			System.out.printf("mouseUp at %1$d, %2$d, tx = %3$d, ty = %4$d\n", mx, my,
					t.tx, t.ty);
			// World-wrapping.
			t.tx = (short)((t.tx + EConst.c_num_tiles)%EConst.c_num_tiles);
			t.ty = (short)((t.ty + EConst.c_num_tiles)%EConst.c_num_tiles);
			System.out.println("Teleporting to " + t.tx + "," + t.ty + "!");
			t.tz = 0;
			gwin.teleportParty(t, false, -1);
			ExultActivity.showToast("Teleport!!!");
			close();
			return true;
		}
		private static final int border=2;			// For showing map.
		private static final int worldsize = EConst.c_tiles_per_chunk * EConst.c_num_chunks;
		public void paint() {
			map.paint(gwin.getWin(), x, y);
			// mark current location
			int xx, yy;
			gwin.getMainActor().getTile(t);
			xx = ((t.tx * (w - border*2)) / worldsize);
			yy = ((t.ty * (h - border*2)) / worldsize);

			xx += x - map.getXLeft() + border;
			yy += y - map.getYAbove() + border;
			gwin.getWin().fill8((byte)255, 1, 5, xx, yy - 2);
			gwin.getWin().fill8((byte)255, 5, 1, xx - 2, yy);
		}
	}
}
