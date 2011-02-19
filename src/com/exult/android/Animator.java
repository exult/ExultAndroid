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
		while (tqueue.remove(this))		// Remove from queue.
			;
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
				objsfx.update(play);
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
		private Rectangle rect = new Rectangle();
		public Sfx(GameObject o) {
			super(o);
		}
						// For Time_sensitive:
		public void handleEvent(int time, Object udata) {
			final int delay = 1;		// Guessing this will be enough.
			gwin.getShapeRect(rect, obj);
			gwin.clipToWin(rect);
			if (rect.w <= 0 || rect.h <= 0) {	// No longer on screen.
				animating = false;
				// Stop playing sound.
				if (objsfx != null)
					objsfx.stop();
				return;
			}
			if (objsfx != null)		// Sound effect?
				objsfx.update(true);
							// Add back to queue for next time.
			if (animating)
				tqueue.add(time + delay, this, udata);
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
	public static class ShapeSfx extends GameSingletons {
		public static boolean off = false;
		private static Rectangle rangeRect = new Rectangle();	// Temp.
		private static Tile pos = new Tile(), cent = new Tile();// Temps.
		private GameObject obj;		// Object that caused the sound.
		private SFXInfo sfxinf;
		private int channel[] = new int[2];			// ID of sound effect being played.
		private int distance;			// Distance in tiles from Avatar.
		//UNUSED private int dir;			// Direction (0-15) from Avatar.
		private int curSfx;		// For playing sequential sfx ranges.
		private boolean looping;		// If the SFX should loop until stopped.
		public static boolean getSfxOutOfRange(GameObject obj) {
			gwin.getWinTileRect(rangeRect);
			Rectangle size = rangeRect;
			pos.set(size.x+size.w/2,size.y+size.h/2,
					gwin.getCameraActor().getLift());
			obj.getCenterTile(cent);
			return pos.squareDistanceScreenSpace(cent) > 
			(Audio.MAX_SOUND_FALLOFF*Audio.MAX_SOUND_FALLOFF);
		}
										// Create & start playing sound.
		public ShapeSfx(GameObject o) {
			obj = o;
			curSfx = -1;
			channel[0] = channel[1] = -1;
			sfxinf = obj.getInfo().getSfxInfo();
			setLooping();	// To avoid including sfxinf.h.
		}
		int getDistance()
			{ return distance; }
		void update(boolean play) {	// Set to new object.
			if (obj.isPosInvalid()) { 			// Not on map.
				stop();
				return;
			}
			if (off || sfxinf == null)
				return;
			if (looping)
				play = true;
			boolean active[] = {false, false};
			for (int i = 0; i < channel.length; i++) {
				if (channel[i] != -1)
					active[i] = audio.isPlaying(channel[i]);
				if (!active[i] && channel[i] != -1) {
					audio.stopSfx(channel[i]);
					channel[i] = -1;
				}
			}
			//System.out.println("channel[0] = " + channel[0] + ", active: " + active[0]);
			// If neither channel is playing, and we are not going to
			// play anything now, we have nothing to do.
			if (!play && channel[0] == -1 && channel[1] == -1)
				return;
			int sfxnum[] = {-1, -1};
			if (play && channel[0] == -1) {
				if (!sfxinf.timeToPlay())
					return;
				sfxnum[0] = curSfx = sfxinf.getNextSfx(curSfx);
			}
			//System.out.println("ShapeSfx.update: play = " + sfxnum[0] +
			//		", " + sfxnum[1]);

			int rep[] = {looping ? -1 : 0, 0};
			if (play && channel[1] == -1 && sfxinf.playHourlyTicks()) {
				if (clock.getMinute() == 0) {
					// Play sfx.extra every hour for reps = hour
					int reps = clock.getHour()%12;
					rep[1] = (reps != 0 ? reps : 12) - 1;
					sfxnum[1] = sfxinf.getExtraSfx();
				}
			}
			//UNUSED dir = 0;
			int volume = Audio.MAX_VOLUME;	// Set volume based on distance.
			boolean halt = getSfxOutOfRange(obj);
			if (play && halt)
				play = false;

			for (int i = 0; i < channel.length; i++) {
				if (play && channel[i] == -1 && sfxnum[i] > -1) {	// First time?
							// Start playing.
					// System.out.println("channel " + i + ", sfx = " + sfxnum[i]);
					channel[i] = audio.playSfx(sfxnum[i], obj, volume, rep[i]);
				} else if (channel[i] != -1) {
					if(halt) {
						audio.stopSfx(channel[i]);
						channel[i] = -1;
					} else {
						// System.out.println("Update channel " + i);
						channel[i] = audio.updateSfx(channel[i], obj);
					}
				}
			}
		}
		void setLooping() {
			looping = sfxinf != null ? (sfxinf.getSfxRange() == 1
                    && sfxinf.getChance() == 100
                    && !sfxinf.playHourlyTicks())
                 : false;
		}
		void stop() {
			for (int i = 0; i < channel.length; i++) {
			if(channel[i] >= 0) {
				audio.stopSfx(channel[i]);
				channel[i] = -1;
				}
			}
		}
	}
	/*
	 *	A class for playing sound effects that get updated by position
	 *	and distance. Adds itself to time-queue, deletes itself when done.
	 */
	public static class ObjectSfx 
				extends GameSingletons implements TimeSensitive {
		private GameObject obj;	// Object that caused the sound.
		private int timeQueueCount;
		private int sfx;			// ID of sound effect being played.
		private int channel;		// Channel of sfx being played.
		public ObjectSfx(GameObject o, int sx, int delay) {
			obj = o; sfx = sx; channel = -1;
			tqueue.add(TimeQueue.ticks + delay, this, gwin);
		}
		public void stop() {
			while (tqueue.remove(this))
				;
			if (channel >= 0) {
				audio.stopSfx(channel);
				channel = -1;
			}
		}
		public int getSfxnum()
			{ return sfx; }
		public void handleEvent(int curtime, Object udata) {
			final int delay = 3;		// Guessing this will be enough.
			boolean active = channel != -1 ? audio.isPlaying(channel) : false;

			if (obj.isPosInvalid()) { // || (distance >= 0 && !active))
				stop();// Quitting time.
				return;
			}
			int volume = Audio.MAX_VOLUME;	// Set volume based on distance.
			boolean halt = ShapeSfx.getSfxOutOfRange(obj);

			if (!halt && channel == -1 && sfx > -1)		// First time?
							// Start playing.
				channel = audio.playSfx(sfx, obj, volume, 0);
			else if (channel != -1) {
				if (halt) {
					audio.stopSfx(channel);
					channel = -1;
				} else {
					channel = audio.updateSfx(channel,obj);
				}
			}
			if (channel != -1)
				tqueue.add(curtime + delay, this, udata);
			else
				stop();
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
	}
}
