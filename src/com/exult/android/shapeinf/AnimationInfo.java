package com.exult.android.shapeinf;
import java.io.InputStream;

import com.exult.android.ShapeInfo;
/*
 *	Information about shape animations.
 */
public class AnimationInfo extends BaseInfo {
	public static final int //	AniType				// Type of animation
			FA_TIMESYNCHED = 0,		// Frame based on current game ticks.
			FA_HOURLY = 1,			// Frame based on game hour.
			FA_NON_LOOPING = 2,		// Stop at last frame.
			FA_LOOPING = 3,	// Generic loop.
			FA_RANDOM_FRAMES = 4	// Frames completely random.
			;
	private int	type;
	private int		frameCount;	// Frame count of each animation cycle
	private int		frameDelay;	// Delay multiplier between frames.
	private int		sfxDelay;		// Extra sfx delay, in frames.
	private int		freezeFirst;	// % chance of advancing first frame of animation.
	private int		recycle;		// Repeat the last recycle frames when wrapping;
									// all frames if zero.
	public static AnimationInfo createFromTfa(int type, int nframes) {	
		AnimationInfo inf = new AnimationInfo();
		switch (type) {
		case 0:
		case 1:
			inf.set(FA_TIMESYNCHED, nframes, 0);
			break;
		case 5:
			inf.set(FA_LOOPING, nframes, 0, 20, 1, -1);
			break;
		case 6:
			inf.set(FA_RANDOM_FRAMES, nframes, 0);
			break;
		case 8:
			inf.set(FA_HOURLY, nframes, 0);
			break;
		case 9:
			inf.set(FA_LOOPING, nframes, 8);
			break;
		case 10:
			inf.set(FA_LOOPING, nframes, 6);
			break;
		case 11:
			inf.set(FA_LOOPING, nframes, 1, 0);
			break;
		case 12:	// Slow advance.
		case 14:	// Grandfather clock.
			inf.set(FA_TIMESYNCHED, nframes, 0, 100, 4, -1);
			break;
		case 13:
			inf.set(FA_NON_LOOPING, nframes, 0);
			break;
		case 15:
			inf.set(FA_TIMESYNCHED, 6, 0, 100, 4, 0);
			break;
		default:
			// Not handled yet. These would be cases 2, 3, 4, 7 and 12:
			// case 2 seems to be equal to case 0/1 except that
			//		frames advance randomly (maybe 1 in 4 chance)
			// case 3 is very strange. I have noted no pattern yet.
			// case 4 seems to be for case 3 what case 2 is for case 0/1.
			// case 7 toggles bit 0 of the frame (i.e., new_frame == frame ^ 1)
			// None of these are used for any animated shape, which is why
			// I haven't bothered implementing them.
			return null;
		}
		inf.infoFlags = 0;
		return inf;
	}
	private void set(int t, int count, int rec, int freeze, int delay, int sfxi) {
		type = t;
		frameCount = count;
		frameDelay = delay;
		sfxDelay = sfxi;
		freezeFirst = freeze;
		recycle = rec;
	}
	private void set(int t, int count, int rec, int freeze) {
		set(t, count, rec, freeze, 1, 0);
	}
	private void set(int t, int count, int rec) {
		set(t, count, rec, 100, 1, 0);
	}
	public int getType() 
		{ return type; }
	public int getFrameCount() 
		{ return frameCount; }
	public int getFrameDelay() 
		{ return frameDelay; }
	public int getSfxDelay() 
		{ return sfxDelay; }
	public int getFreezeFirstChance() 
		{ return freezeFirst; }
	public int get_recycle() 
		{ return recycle; }
	public static int getInfoFlag()
		{ return 0x40; }
	public static final int is_binary = 0, entry_size = 0;
	/*
	 *	Read in a animation-cycle-info entry from 'shape_inf.txt'.
	 */
	@Override
	public boolean read(InputStream in, int version, boolean patch, int game,
			ShapeInfo info) {
		// TODO Auto-generated method stub
		return false;
	}

}
