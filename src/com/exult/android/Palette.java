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
	private boolean faded_out;		// true if faded palette to black.
	private boolean fades_enabled;	

	public Palette(ImageBuf w) {
		win = w;
		palette = -1;
		brightness = 100;
		max_val = 63;
		faded_out = false;
		fades_enabled = false;
		pal1 = new byte[768];
		pal2 = new byte[768];
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
		if (faded_out)
			return;			// In the black.
			// could throw!
		//load(EFile.PALETTES_FLX, EFile.PATCH_PALETTES, palette, null, -1);
		load("/sdcard/Games/exult/blackgate/STATIC/PALETTES.FLX", null, palette, null, -1);
		apply(c);
	}
	public void apply(Canvas c) {
		win.setPalette(pal1, max_val, brightness);
		if (c != null)
			win.show(c);
	}
	private void load(String fname0, String fname1, int index, String xfname, int xindex) {
		byte buf[] = EFileManager.instanceOf().retrieve(fname0, fname1, index);
		setLoaded(buf, xfname, xindex);
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
}
