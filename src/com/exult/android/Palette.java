package com.exult.android;
import java.util.Arrays;
import android.graphics.Canvas;

public class Palette {
	/*
	 *	Palette #'s in 'palettes.flx':
	 */
	public static final int PALETTE_DAY = 0;
	public static final int PALETTE_DUSK = 1;
	public static final int PALETTE_DAWN = 1;		// Think this is it.
	public static final int PALETTE_NIGHT = 2;
	public static final int PALETTE_INVISIBLE = 3;	// When Avatar is invisible.
	public static final int PALETTE_OVERCAST = 4;		// When raining or overcast during daytime
	public static final int PALETTE_FOG = 5;
						// 6 looks a little brighter than #2.
						// 7 is somewhat warmer.  Torch?
	public static final int PALETTE_RED = 8;		// Used when hit in combat.
						// 9 has lots of black.
	public static final int PALETTE_LIGHTNING = 10;
	public static final int PALETTE_SINGLE_LIGHT = 11;
	public static final int PALETTE_MANY_LIGHTS = 12;
	
	private ImageBuf win;
	private byte pal1[];
	private byte pal2[];
	private int palette;		// Palette #.
	private int brightness;
	private int max_val;
	private boolean fadedOut;		// true if faded palette to black.
	private boolean fadesEnabled;	
	private boolean border255;
	private void take(Palette pal) {
		palette = pal.palette;
		brightness = pal.brightness;
		fadedOut = pal.fadedOut;
		fadesEnabled = pal.fadesEnabled;
		System.arraycopy(pal.pal1, 0, pal1, 0, 768);
		System.arraycopy(pal.pal2, 0, pal2, 0, 768);
	}
	public Palette(ImageBuf w) {
		win = w;
		palette = -1;
		brightness = 100;
		max_val = 63;
		fadedOut = false;
		fadesEnabled = false;
		pal1 = new byte[768];
		pal2 = new byte[768];
	}
	public Palette(Palette pal) {
		take(pal);
	}
	public void fade(int cycles, boolean inout, int pal_num) {
		/*++++++++++FINISH
		if (pal_num == -1) pal_num = palette;
		palette = pal_num;
		
		border255 = (palette >= 0 && palette <= 12) && palette != 9;

		load(EFile.PALETTES_FLX, EFile.PATCH_PALETTES, pal_num);
		if (inout)
			fadeIn(cycles);
		else
			fadeOut(cycles);
		*/
		fadedOut = !inout;		// Be sure to set flag.
	}
	public boolean isFadedOut() {
		return fadedOut;
	}
	public void setBrightness(int b) {
		brightness = b;
	}
	/*
	 * Read in a palette.
	 */
	public void set
		(
		int pal_num,			// 0-11, or -1 to leave unchanged.
		int new_brightness,		// New percentage, or -1.
		Canvas c				// Repaint if not null.
		) {
		if ((palette == pal_num || pal_num == -1) &&
				(brightness == new_brightness || new_brightness == -1))
					// Already set.
			return;
		if (pal_num != -1)
			palette = pal_num;	// Store #.
		if (new_brightness > 0)
			brightness = new_brightness;
		if (fadedOut)
			return;			// In the black.
			// could throw!
		load(EFile.PALETTES_FLX, EFile.PATCH_PALETTES, palette, null, -1);
		apply(c);
	}
	public void set(int pal_num) {
		set(pal_num, -1, null);
	}
	public void set(byte palnew[], int new_brightness, boolean repaint, 
								boolean border255) {
		this.border255 = border255;
		System.arraycopy(palnew, 0, pal1, 0, 768);
		Arrays.fill(pal2, (byte)0);
		palette = -1;
		if (new_brightness > 0)
			brightness = new_brightness;
		if (fadedOut)
			return;			// In the black.
		
		setBrightness(brightness);
		apply();
		if (repaint)
			GameWindow.instanceOf().setAllDirty();
	}
	public void apply(Canvas c) {
		win.setPalette(pal1, max_val, brightness);
		if (c != null)
			win.show(c);
	}
	public void apply() {
		apply(null);
	}
	public void load(String fname0, String fname1, int index, String xfname, int xindex) {
		byte buf[] = EFileManager.instanceOf().retrieve(fname0, fname1, index);
		setLoaded(buf, xfname, xindex);
	}
	public void load(String fname0, String fname1, int index) {
		load(fname0, fname1, index, null, -1);
	}
	/*
	 * This does the actual load.
	 */
	private void setLoaded(byte buf[], String xfname, int xindex) {
		int len = buf.length;
		if (len == 768) {	// Simple palette
			/*+++++++++FINISH
			if (xindex >= 0)
				// Get xform table.
				loadxform(buf, xfname, xindex);
			*/
			if (xindex < 0)		// Set the first palette
				System.arraycopy(buf, 0, pal1, 0, 768);
			// The second one is black.
			Arrays.fill(pal2, (byte)0);
		}
		else if (buf != null && len > 0) {			// Double palette
			for (int i=0; i<768; i++)
				{
				pal1[i]=buf[i*2];
				pal2[i]=buf[i*2+1];
				}
			}
		else {
			// Something went wrong during palette load. This probably
			// happens because a dev is being used, which means that
			// the palette won't be loaded.
			// For now, let's try to avoid overwriting any palette that
			// may be loaded and just cleanup.
			return;
		}
	}
	//	Find index (0-255) of closest color (r,g,b < 64).
	public int findColor(int r, int g, int b, int last) {
		int best_index = -1;
		long best_distance = 0xfffffff;
						// But don't search rotating colors.
		for (int i = 0; i < last; i++) {
						// Get deltas.
			long dr = r - pal1[3*i], dg = g - pal1[3*i + 1], 
								db = b - pal1[3*i + 2];
						// Figure distance-squared.
			long dist = dr*dr + dg*dg + db*db;
			if (dist < best_distance) {	// Better than prev?
				best_index = i;
				best_distance = dist;
			}
		}
		return best_index;
	}
	public int findColor(int r, int g, int b) {
		return findColor(r, g, b, 0xe0);
	}
	/*
	 *	Creates a palette in-between two palettes.
	 */
	public Palette createIntermediate(Palette to, int nsteps, int pos) {
		byte palnew[] = new byte[768];
		if (fadesEnabled) {
			for (int c=0; c < 768; c++)
				palnew[c] = (byte)(((to.pal1[c]-pal1[c])*pos)/nsteps+pal1[c]);
		} else {
			byte palold[];
			if (2*pos >= nsteps)
				palold = to.pal1;
			else
				palold = pal1;
			System.arraycopy(palold, 0, palnew, 0, 768);
		}
		Palette ret = new Palette(GameWindow.instanceOf().getWin());
		ret.set(palnew,-1,true,true);
		return ret;
	}
	/*
	 *	Smooth palette transition.
	 */
	public static class Transition extends GameSingletons {
		private Palette start, end, current;
		private int step, maxSteps;
		private int startHour, startMinute, rate;
		public Transition(int from, int to, int ch, int cm, int r,
					int nsteps, int sh, int smin) {
			startHour = sh; startMinute = smin; rate = r;
			maxSteps = nsteps;
			start = new Palette(gwin.getWin());
			start.load(EFile.PALETTES_FLX, EFile.PATCH_PALETTES, from);
			end = new Palette(gwin.getWin());
			end.load(EFile.PALETTES_FLX, EFile.PATCH_PALETTES, to);
			setStep(ch, cm);
		}
		
		public Transition(Palette from, int to, int ch, int cm,
					int r, int nsteps, int sh, int smin) {
			startHour = sh; startMinute = smin; rate = r;
			maxSteps = nsteps;
			start = new Palette(from);
			end = new Palette(gwin.getWin());
			end.load(EFile.PALETTES_FLX, EFile.PATCH_PALETTES, to);
			setStep(ch, cm);
		}
		public Transition(Palette from, Palette to, int ch, int cm,
				int r, int nsteps, int sh, int smin) {
			startHour = sh; startMinute = smin; rate = r;
			maxSteps = nsteps;
			start = new Palette(from);
			end = new Palette(to);
			setStep(ch, cm);
		}
		public int getStep()
			{ return step; }
		boolean setStep(int hour, int min) {
			int new_step = 60 * (hour - startHour) + min - startMinute;
			while (new_step < 0)
				new_step += 60;
			new_step /= rate;
			if (gwin.getPal().isFadedOut())
				return false;

			if (current == null || new_step != step) {
				step = new_step;
				current = start.createIntermediate(end, maxSteps, step);
			}
			if (current != null) {
				current.apply();
				gwin.setAllDirty();
			}
			if (step >= maxSteps)
				return false;
			else
				return true;
		}
		Palette getCurrentPalette()
			{ return current; }
	}
}
