package com.exult.android;
import com.exult.android.shapeinf.*;

public abstract class Animator extends GameSingletons implements TimeSensitive {
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
			last_shape = (short)obj.getShapeNum();
			// Catch rotated objects here.
			last_frame = (short)(obj.getFrameNum() & ~(1<<5));
			int rotflag = obj.getFrameNum() & (1<<5);
			int shpframes = ShapeFiles.SHAPES_VGA.getFile().getNumFrames(last_shape);
			aniinf = obj.getInfo().getAnimationInfoSafe(shpframes);
			int cnt = aniinf.getFrameCount();
			if (cnt >= 0)
				nframes = (short)shpframes;
			else
				nframes = (short)cnt;
			if (nframes == shpframes)
				first_frame = 0;
			else
				first_frame = (short)(last_frame - (last_frame%nframes));
			// Ensure proper bounds.
			if (first_frame + nframes >= shpframes)
				nframes = (short)(shpframes - first_frame);
			assert(nframes > 0);

			frame_counter = (short)aniinf.getFrameDelay();

			if (aniinf.getType() == AnimationInfo.FA_TIMESYNCHED)
				created = currpos = (short)(last_frame%nframes);
			else
				created = currpos = 0;
			// Add rotate flag back.
			first_frame |= rotflag;
			last_frame |= rotflag;
		}
		public Frame(GameObject o) {
			super(o);
			initialize();
		}
		int getNextFrame() {
			// Re-init if it's outside the range.
			// ++++++Should we do this for the other cases (jsf)?
			// ++++++Seeing if it breaks anything (marzo)
			int curframe = obj.getFrameNum();
			if (curframe < first_frame ||
				curframe >= first_frame + nframes)
				initialize();
			if (nframes == 1)	// No reason to do anything else.
				return first_frame;
			int framenum = first_frame;
			switch (aniinf.getType()) {
			case AnimationInfo.FA_HOURLY:
				framenum = clock.getHour() % nframes;  
				break;
			case AnimationInfo.FA_NON_LOOPING:
				currpos++;
				if (currpos >= nframes) 
					currpos = (short)(nframes - 1);
				framenum = first_frame + currpos;
				break;
			case AnimationInfo.FA_TIMESYNCHED:
				{
				int ticks = TimeQueue.ticks;
				final int delay = 1;
				currpos = (short)((ticks / (delay * aniinf.getFrameDelay())) + created);
				currpos %= nframes;
				framenum = first_frame + currpos;
				break;
				}
			case AnimationInfo.FA_LOOPING:
				{
				int chance = aniinf.getFreezeFirstChance();
				if (currpos != 0 || chance == 100
					|| (chance != 0 && EUtil.rand()%100 < chance)) {
					currpos++;
					currpos %= nframes;
					int rec = aniinf.get_recycle();
					if (currpos == 0 && nframes >= rec)
						currpos = (short)((nframes - rec) % nframes);
				}
				framenum = first_frame + currpos;
				break;
				}
			case AnimationInfo.FA_RANDOM_FRAMES:
				currpos = (short)(EUtil.rand() % nframes);
				framenum = first_frame + currpos;
				break;
			}
			return framenum;
		}
						// For Time_sensitive:
		public void handleEvent(int ctime, Object udata) {
			final int delay = 1;
			if (--frame_counter == 0) {
				frame_counter = (short)aniinf.getFrameDelay();
				boolean dirty_first = gwin.addDirty(obj);
				int framenum = getNextFrame();
				last_frame = (short)framenum;
				obj.setFrame(last_frame);
				if (!dirty_first && !gwin.addDirty(obj))
					{	// No longer on screen.
					animating = false;
					// Stop playing sound.
					if (objsfx != null)
						objsfx.stop();
					return;
				}
			}
			if (objsfx != null) {	// Sound effect?
				boolean play;
				if (frame_counter != aniinf.getFrameDelay())
					play = false;
				else if (aniinf.getSfxDelay() < 0)
					{	// Only in synch with animation.
					if (aniinf.getFreezeFirstChance() < 100)
						// Not in (frozen) first frame.
						play = (currpos == 1);
					else
						play = (currpos == 0);
					}
				else if (aniinf.getSfxDelay() > 1)
						// Skip (sfx_delay-1) frames.
					play = (currpos % aniinf.getSfxDelay()) == 0;
				else
					// Continuous.
					play = true;
				//++++++++FINISH objsfx.update(play);
			}
			// Add back to queue for next time.
			if (animating)
				// Ensure all animations are synched
				tqueue.add(ctime + delay, this, udata);
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
		public void handleEvent(int ctime, Object udata) {
			final int delay = 1;		// Delay between frames.
			if (!gwin.addDirty(obj)) {	// No longer on screen.
				animating = false;
				return;
			}
			int tx = obj.getTileX(), ty = obj.getTileY(), tz = obj.getLift();
			int newdx = EUtil.rand()%3;
			int newdy = EUtil.rand()%3;
			tx += -deltax + newdx;
			ty += -deltay + newdy;
			deltax = (short)newdx;
			deltay = (short)newdy;
			obj.move(tx, ty, tz);
							// Add back to queue for next time.
			if (animating)
				tqueue.add(ctime + delay, this, udata);
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
		void update(boolean play);	// Set to new object.
		void set_looping();
		*/
		void stop() {
			//+++++++FINISH
		}
	}

}
