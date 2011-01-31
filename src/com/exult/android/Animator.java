package com.exult.android;
import com.exult.android.shapeinf.*;

public class Animator extends GameSingletons implements TimeSensitive {
	private int timeQueueCount;
	protected GameObject obj;		// Object we're controlling.
	protected short deltax, deltay;	// If wiggling, deltas from
										//   original position.
	protected boolean animating;			// 1 if animation turned on.
	protected ShapeSfx objsfx;
	protected void startAnimation() {
		// Clean out old entry if there.
		tqueue.remove(this);
		tqueue.add(TimeQueue.ticks + 1, this, gwin);
		animating = true;
	}
	public Animator(GameObject o) {
		obj = o;
		objsfx = new ShapeSfx(obj);
	}
	public static Animator create(GameObject ob) {
		int frames = ob.getNumFrames();
		ShapeInfo info = ob.getInfo();
		if (!info.isAnimated())	// Assume it's just SFX.
			return new Sfx(ob);
		else if (frames > 1)
			return new Frame(ob);
		else
			return new Wiggle(ob);
	}
	public void delete() {
		if (objsfx != null)
			objsfx.stop();
	}
	public void wantAnimation() {		// Want animation on.
		if (!animating)
			startAnimation();
		}
	public void stopAnimation() {
		animating = false;
	}
	public int getDeltax()
		{ return deltax; }
	public int getDeltay()
		{ return deltay; }
	public int getFrameNum() {
		return obj.getFrameNum();
	}
	@Override
	public void addedToQueue() {
		++timeQueueCount;
	}
	@Override
	public boolean alwaysHandle() {
		return false;
	}
	@Override
	public void handleEvent(int ctime, Object udata) {
		// TODO Auto-generated method stub

	}
	@Override
	public void removedFromQueue() {
		--timeQueueCount;
	}
	/*
	 *	Animate by going through frames.
	 */
	public static class Frame extends Animator {
		AnimationInfo aniinf;
		short first_frame;	// Initial frame of animation cycle
		short currpos;			// Current position in the animation.
		short nframes;		// Number of frames in cycle.
		short frame_counter;		// When to increase frame.
		int created;		// Time created
		short last_shape;	// To check if we need to re init
		short last_frame;	// To check if we need to re init
		void initialize() {
			//++++++++++FINISH
		}
		public Frame(GameObject o) {
			super(o);
			initialize();
		}
		int getNextFrame() {
			return 0;//+++++++++FINISH
		}
						// For Time_sensitive:
		public void handleEvent(int time, Object udata) {
			
		}
		public int getFrameNum() {
			return obj.getFrameNum();
		}
	}
	/*
	 *	Just play SFX.
	 */
	public static class Sfx extends Animator {
		public Sfx(GameObject o) {
			super(o);
		}
						// For Time_sensitive:
		public void handleEvent(int time, Object udata) {
			//++++++FINISH
		}
	}
	/*
	 *	Animate by going through frames, but only do the lower frames once.
	 */
	public static class FieldFrame extends Frame {
		boolean activated;			// Time to check for damage.
		public FieldFrame(GameObject o) {
			super(o);
			activated = true;
		}
						// For Time_sensitive:
		public void handleEvent(int time, Object udata) {
			super.handleEvent(time, udata);
			if (activated && EUtil.rand()%10 == 0)// Check for damage?
				obj.activate(0);
		}
	}
	/*
	 *	Animate by wiggling.
	 */
	public static class Wiggle extends Animator {
		public Wiggle(GameObject o) {
			super(o);
		}
						// For Time_sensitive:
		public void handleEvent(int time, Object udata) {
			//++++++++++++FINISH
		}
	}

	/*
	 *	A class for playing sound effects when certain objects are nearby.
	 */
	public static class ShapeSfx extends GameSingletons
		{
		GameObject obj;		// Object that caused the sound.
		/* ++++++++FINISH
		SFX_info *sfxinf;
		int channel[2];			// ID of sound effect being played.
		int distance;			// Distance in tiles from Avatar.
		int dir;			// Direction (0-15) from Avatar.
		int last_sfx;		// For playing sequential sfx ranges.
		bool looping;		// If the SFX should loop until stopped.
		*/
						// Create & start playing sound.
		public ShapeSfx(GameObject o) {
			obj = o;
		}
			/* +++++++FINISH
			: obj(o), distance(0), last_sfx(-1)
			{
			channel[0] = channel[1] = -1;
			sfxinf = obj.get_info().get_sfx_info();
			if (sfxinf)
				last_sfx = 0;
			set_looping();	// To avoid including sfxinf.h.
			}
		int getSfxnum()
			{ return lastSfx; }
		int get_distance()
			{ return distance; }
		void update(bool play);	// Set to new object.
		void set_looping();
		*/
		void stop() {
			//+++++++FINISH
		}
	}

}
