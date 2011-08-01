package com.exult.android;

public class PlasmaThread extends Thread {
	static final int BG_PLASMA_START_COLOR = 128;
	static final int BG_PLASMA_CYCLE_RANGE = 80;
	static final int SI_PLASMA_START_COLOR = 16;
	static final int SI_PLASMA_CYCLE_RANGE = 96;
	private int startColor, cycleRange;
	private Palette pal;
	private GameWindow gwin;
	public boolean finish = false;
	private void plasma(int w, int h, int x, int y, int startc, int endc) {
		ImageBuf win = GameSingletons.win;
		win.fill8((byte)startc, w, h, x, y);
		for (int i=0; i < w*h; i += 16) {	// Too many loops makes this too slow.
			int pc = startc + EUtil.rand()%(endc-startc+1);
			int px = x + EUtil.rand()%w;
			int py = y + EUtil.rand()%h;
			for (int j=0; j < 6; j++) {
				int px2 = px + EUtil.rand()%17 - 8;
				int py2 = py + EUtil.rand()%17 - 8;
				win.fill8((byte)pc, 3, 1, px2 - 1, py2);
				win.fill8((byte)pc, 1, 3, px2, py2 - 1);
			}
		}
		gwin.setPainted();
	}
	public PlasmaThread(Palette p) {
		pal = p;
		gwin = GameSingletons.gwin;
		if (GameSingletons.game.isBG()) {
			startColor = BG_PLASMA_START_COLOR;
			cycleRange = BG_PLASMA_CYCLE_RANGE;
		} else {
			startColor = SI_PLASMA_START_COLOR;
			cycleRange = SI_PLASMA_CYCLE_RANGE;
		}
		synchronized(gwin.getWin()) {
			// Load the palette
			if (GameSingletons.game.isBG())
				pal.load(EFile.INTROPAL_DAT, EFile.PATCH_INTROPAL, 2);
			else
				pal.load(EFile.MAINSHP_FLX, EFile.PATCH_MAINSHP, 1);
			pal.apply();
			plasma(gwin.getWidth(), gwin.getHeight(), 0, 0, startColor, startColor + cycleRange - 1);
		}
		
	}
	@Override
	public void run() {
		System.out.println("PlasmaThread: run: started: " + GameSingletons.tqueue.ticks);
		while (!finish) {
			for(int i = 0; i < 4; ++i)
				gwin.getWin().rotateColors(startColor, cycleRange);
			gwin.setPainted();
			try {
				sleep(100);
			} catch (InterruptedException e) {
				finish = true;
			}
		}
	}
}
