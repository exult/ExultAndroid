package com.exult.android;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

public class UsecodeScript extends GameSingletons implements TimeSensitive {
	public static boolean debug = false;
	private static int count;		// Total # of these around.
	private static LinkedList<UsecodeScript> scripts =
						new LinkedList<UsecodeScript>();
	private GameObject obj;		// From objval.
	private UsecodeValue code;		// Array of code to execute.
	private int cnt;			// Length of arrval.
	private int i;				// Current index.
	private int frame_index;		// For taking steps.
	private boolean no_halt;			// 1 to ignore halt().
	private boolean must_finish;		// 1 to finish before deleting.
	private boolean killed_barks;		// 1 to prevent barks from showing.
	private int delay;			// Used for restoring.
	protected int timeQueueCount;		// # times in timeQueue.
	// The number of times that the game clock ticks in one game minute.
	// ++++++FOR NOW.  Later, move to GameClock class.
	public static final int ticks_per_minute = 25;
	public static final int // enum Ucscript_ops
		cont = 		0x01,		// Continue without painting.
		reset =		0x0a,		// Resets script ip.
		repeat = 	0x0b,		// Loop(offset, cnt).
		repeat2 =	0x0c,		// Loop(offset, cnt1, cnt2).
		nop =		0x21,		// Not sure about this.
		dont_halt =	0x23,		// Not right?
		wait_while_near = 0x24,	// wait_while_near(dist). Halt on opcode for as long
								// as avatar is within dist tiles.
		delay_ticks =	0x27,		// Delay(ticks).
		delay_minutes =	0x28,		// Delay(minutes).
		delay_hours =	0x29,		// Delay nn game hours.
		wait_while_far = 0x2b,	// wait_while_far(dist). Halt on opcode for as long
								// as avatar is further than dist tiles.
		finish =	0x2c,		// Finish script if killed.
		remove =	0x2d,		// Remove item & halt.
		step_n = 	0x30,		// Step in given direction.
		step_ne = 	0x31,
		step_e =	0x32,
		step_se =	0x33,
		step_s =	0x34,
		step_sw =	0x35,
		step_w =	0x36,
		step_nw =	0x37,
		descend =	0x38,		// Decr. lift.
		rise =		0x39,		// Incr. lift.
		frame =		0x46,		// Set_frame(frnum).
		egg = 		0x48,		// Activate egg.
		set_egg =	0x49,		// Set egg's criteria, distance.
		next_frame_max =0x4d,		// Next frame, but stop at max.
		next_frame =	0x4e,		// Next frame, but wrap.
		prev_frame_min =0x4f,		// Prev frame, but stop at 0.
		prev_frame =	0x50,		// Prev. frame, but wrap.
		say =		0x52,		// Say(string).
		step =		0x53,		// Step(dir).
		music =		0x54,		// Play(track#).
		usecode =	0x55,		// Call usecode(fun).
		speech =	0x56,		// Speech(track#).
		sfx =		0x58,		// Sound_effect(#).
		face_dir =	0x59,		// Face_dir(dir), dir=0-7, 0=north.
		weather =	0x5A,		// Set weather(type).
		npc_frame =	0x61,		// 61-70:  Set frame, but w/ cur. dir.
		hit =		0x78,		// Hit(hps, type).  Item attacked.
		attack = 	0x7a,		// Attack using vals from
								//   set_to_attack intrinsic.
		/*
		 *	These are (I think) not in the original: 
		 */
		usecode2 =	0x80,		// Call usecode(fun, eventid).
		resurrect =	0x81;		// Parm. is body.

	public UsecodeScript(GameObject o, UsecodeValue cd) {
		obj = o;
		code = cd;		// May be null for empty script.
		if (code == null)			// Empty?
			code = new UsecodeValue.ArrayValue(new Vector<UsecodeValue>());
		else {
			cnt = code.getArraySize();
			if (cnt == 0) {		// Not an array??  (This happens.)
								// Create with single element.
				code = UsecodeValue.ArrayValue.forceElem(null, 0, cd);
				cnt = 1;
			}
		}
	}
	public boolean isActivated() {
		return i > 0;
	}
	public boolean isNoHalt() {
		return no_halt;
	}
	/*
	 *	Enter into the time-queue and our own chain.  Terminate existing
	 *	scripts for this object unless 'dont_halt' is set.
	 */
	public void start
		(
		int d			// Start after this many clicks.
		) {
		int cnt = code.getArraySize();// Check initial elems.
		for (int i = 0; i < cnt; i++) {
			int opval0 = code.getElem(i).getIntValue();
			if (opval0 == dont_halt)
				no_halt = true;
			else if (opval0 == finish)
				must_finish = true;
			else
				break;
			}
		if (!no_halt)		// If flag not set,
						// Remove other entries that aren't
						//   'no_halt'.
			terminate(obj);
		count++;			// Keep track of total.
		scripts.addFirst(this);	// Put in chain.
		tqueue.add(d + TimeQueue.ticks, this, ucmachine);
	}
	/*
	 *	Terminate all scripts for a given object.
	 */
	public static void terminate(GameObject obj) {
		ListIterator<UsecodeScript> iter = scripts.listIterator();
		while (iter.hasNext()) {
			UsecodeScript each = iter.next();
			if (each.obj == obj) {
				if (debug)
					System.out.println("Halting script for obj " + obj.getName());
				each.halt();
				iter.remove();
			}
		}
	}
	public void halt() {
		if (!no_halt)
			i = cnt;
	}
	/*
	 *	Search list for one for a given item.
	 *
	 *	Output:	.Usecode_script if found, else 0.
	 */

	public static UsecodeScript find
		(
		GameObject srch,
		UsecodeScript last_found	// Find next after this.
		) {
		ListIterator<UsecodeScript> iter = scripts.listIterator();
		if (last_found != null) {
			while (iter.hasNext() && iter.next() != last_found)
				;
		}
		while (iter.hasNext()){
			UsecodeScript each = iter.next();
			if (each.obj == srch)
				return each;
		}
		return null;
	}
	/*
	 *	Search list for one for a given item.
	 *
	 *	Output:	.Usecode_script if found, else 0.
	 */

	public static UsecodeScript findActive
		(
		GameObject srch,
		UsecodeScript last_found	// Find next after this.
		) {
		if (scripts == null)
			return null;
		ListIterator<UsecodeScript> iter = scripts.listIterator();
		if (last_found != null) {
			while (iter.hasNext() && iter.next() != last_found)
				;
		}
		while (iter.hasNext()){
			UsecodeScript each = iter.next();
			if (each.obj == srch && each.isActivated())
				return each;
		}
		return null;
	}
	/*
	 *	Remove all from global list (assuming they've already been cleared
	 *	from the time queue).
	 */
	public static void clear (){
		scripts.clear();
	}
	/*
	 *	Terminate all scripts for objects that are more than a given distance
	 *	in tiles from a particular spot.
	 */
	public static void purge(Tile spot, int dist) {
		ListIterator<UsecodeScript> iter = scripts.listIterator();
		while (iter.hasNext()) {
			UsecodeScript each = iter.next();
						// Only purge if not yet started.
			if (each.obj != null && each.i == 0 &&
			    each.obj.getOutermost().distance(spot) > dist) {
				// Force it to halt.
				each.no_halt = false;
				if (each.must_finish) {
					if (debug)
						System.out.println("MUST finish this script");
					each.exec(true);
				}
				each.halt();
			}
		}
	}			
	public void activate_egg(GameObject e) {
		if (e == null || !e.isEgg())
			return;
		/* +++++++++FINISH
		int type = ((Egg_object *) e).get_type();
						// Guess:  Only certain types:
		if (type == Egg_object::monster || type == Egg_object::button ||
		    type == Egg_object::missile)
			((Egg_object *) e).hatch(
				gwin.getMainActor(), true);
		*/
	}
	private static boolean IsActorNear(Actor avatar, GameObject obj, 
			int maxdist) {
		Tile t1 = new Tile(), t2 = new Tile();
		obj.getTile(t1);
		avatar.getTile(t2);
		if (t1.distance2d(t2) > maxdist ||
				(obj.getInfo().getShapeClass() == ShapeInfo.hatchable &&
						obj.getLift() != avatar.getLift()))
			return false;
		else
			return true;
	}
	/*
	 *	Execute an array of usecode, generally one instruction per tick.
	 *
	 *	Output:	Delay for next execution.
	 */
	public int exec(boolean finish) {	// If set, keep going to end.
		int delay = 1;	// Start with default delay.
		boolean do_another = true;			// Flag to keep going.
		int opcode;
		if (debug)
			System.out.println("UsecodeScript.exec: cnt = " + cnt);
		// If a 1 follows, keep going.
		for (; i < cnt && ((opcode = code.getElem(i).getIntValue()) 
							== 0x1 || do_another); i++)
			{
			if (debug)
				System.out.println("Opcode is " + opcode);
			do_another = finish;
			switch (opcode)
				{
			case cont:		// Means keep going without painting.
				do_another = true;
				gwin.setPainted();	// Want to paint when done.
				break;
			case reset:
				if (!finish)	// Appears to be right.
					i = -1;		// Matches originals.
				break;
			case repeat:		// ?? 2 parms, 1st one < 0.
				{		// Loop(offset, cnt).
				do_another = true;
				UsecodeValue cntval = code.getElem(i + 2);
				int cnt = cntval.getIntValue();
				if (cnt <= 0)
						// Done.
					i += 2;
				else
					{	// Decr. and loop.
					cntval = new UsecodeValue.IntValue(cnt - 1);
					code = code.putElem(i + 2, cntval);
					UsecodeValue offval = code.getElem(i + 1);
					i += offval.getIntValue() - 1;
					if (i < -1)	// Before start?
						i = -1;
					}
				break;
				}
			case repeat2:		// Loop with 3 parms.???
				{		// Loop(offset, cnt1, cnt2?).
					//Guessing: loop cnt2 each round. use cnt1 as loop var.
					//This is necessary for loop nesting.
					//(used in mining machine, orb of the moons)

					// Swapped cnt1 and cnt2 -- seems to better match
					// the originals.

				do_another = true;
				int cntInd = i + 2;
				UsecodeValue cntval = code.getElem(i + 2);
				UsecodeValue origval = code.getElem(i + 3);
				int cnt = cntval.getIntValue();
				if (cnt <= 0) {
						// Done.
					i += 3;
					code = code.putElem(cntInd, origval); // restore counter
				} else {	// Decr. and loop.
					code = code.putElem(cntInd, new UsecodeValue.IntValue(cnt - 1));
					UsecodeValue offval = code.getElem(i + 1);
					i += offval.getIntValue() - 1;
				}
				break;
				}
			case wait_while_near:
				{
				int dist = code.getElem(++i).getIntValue();
				if (!finish && IsActorNear(gwin.getMainActor(), obj, dist))
					i -= 2;		// Stay in this opcode.
				break;
				}
			case wait_while_far:
				{
				int dist = code.getElem(++i).getIntValue();
				if (!finish && !IsActorNear(gwin.getMainActor(), obj, dist))
					i -= 2;		// Stay in this opcode.
				break;
				}
			case nop:		// Just a nop.
				break;
			case UsecodeScript.finish:	// Flag to finish if deleted.
				must_finish = true;
				do_another = true;
				break;
			case dont_halt:
				// Seems to mean 'don't let intrinsic 5c halt it' as
				// well as 'allow actor to move during script'
				no_halt = true;
				do_another = true;
				break;
			case delay_ticks:	// 1 parm.
				{		//   delay before next instruction.
				UsecodeValue delayval = code.getElem(++i);
						// It's # of ticks.
				Actor act = obj != null ? obj.asActor() : null;
				if (act != null)
					act.clearRestTime();
				delay = delay*delayval.getIntValue();
				break;		
				}
			case delay_minutes:	// 1 parm., game minutes.
				{
				UsecodeValue delayval = code.getElem(++i);
						// Convert to ticks.
				delay = delay*ticks_per_minute*delayval.getIntValue();
				break;
				}
			case delay_hours:	// 1 parm., game hours.
				{
				UsecodeValue delayval = code.getElem(++i);
						// Convert to ticks.
				delay = delay*60*ticks_per_minute*delayval.getIntValue();
				break;
				}
			case UsecodeScript.remove:	// Remove obj.
				UsecodeIntrinsics.removeItem(obj);
				break;
			case rise:		// (For flying carpet.
				{
				int tx = obj.getTileX(), ty = obj.getTileY(), tz = obj.getLift();
				if (tz < 10)
					tz++;
				obj.move(tx, ty, tz, -1);
				break;
				}
			case descend:
				{
				int tx = obj.getTileX(), ty = obj.getTileY(), tz = obj.getLift();
				if (tz > 0)
					tz--;
				obj.move(tx, ty, tz, -1);
				break;
				}
			case frame:		// Set frame.
				UsecodeIntrinsics.setItemFrame(obj, 
						code.getElem(++i).getIntValue(), false, false);
				break;
			case egg:		// Guessing:  activate egg.
				/*+++++++++FINISH
				activateEgg(usecode, obj);
				*/
				break;
			case set_egg:		// Set_egg(criteria, dist).
				{
				int crit = code.getElem(++i).getIntValue();
				int dist = code.getElem(++i).getIntValue();
				/* ++++++++++++
				EggObject egg = obj.asEgg();
				if (egg != null)
					egg.set(crit, dist);
				*/
				break;
				}
			case next_frame_max:	// Stop at last frame.
				{
				int nframes = obj.getNumFrames();
				if (obj.getFrameNum()%32 < nframes - 1)
					UsecodeIntrinsics.setItemFrame(obj,
								1+obj.getFrameNum(), false, false);
				break;
				}
			case next_frame:
				{
				int nframes = obj.getNumFrames();
				UsecodeIntrinsics.setItemFrame(obj, 
						(1 + obj.getFrameNum())%nframes, false, false);
				break;
				}
			case prev_frame_min:
				if (obj.getFrameNum() > 0)
					UsecodeIntrinsics.setItemFrame(obj, 
							obj.getFrameNum() - 1, false, false);
				break;
			case prev_frame:
				{
				int nframes = obj.getNumFrames();
				int pframe = obj.getFrameNum() - 1;
				UsecodeIntrinsics.setItemFrame(obj, 
							(pframe + nframes)%nframes, false, false);
				break;
				}
			case say:		// Say string.
				{
				UsecodeValue strval = code.getElem(++i);
				if (!killed_barks) {
					eman.removeTextEffect(obj);
					eman.addText(strval.getStringValue(), obj);
				}
				break;
				}
			case step:	// Parm. is dir. (0-7).  0=north.
				{
					// Get dir.
				int val = code.getElem(++i).getIntValue();
					// Height change (verified).
				int dz = i < code.getArraySize() ?
						code.getElem(++i).getIntValue() : 0;
					// Watch for buggy SI usecode!
				int destz = obj.getLift() + dz;
				if (destz < 0 || dz > 15 || dz < -15)
					{	// Here, the originals would flash the step frame,
						// but not step or change height. Not worth emulating.
						// I am also allowing a high limit to height change.
					do_another = true;
					break;
					}
						// It may be 0x3x.
				step(val >= 0 ? val & 7 : -1, dz);
				break;
				}
			case music:		// Unknown.
				{
				UsecodeValue val = code.getElem(++i);
				int song = val.getIntValue();
				// Verified.
				audio.startMusic(song&0xff, (song >> 8) != 0);
				break;
				}
			case usecode:	// Call?
				{
				UsecodeValue val = code.getElem(++i);
				int fun = val.getIntValue();
						// Watch for eggs:
				int ev = UsecodeMachine.internal_exec;
				if (obj != null && obj.isEgg() 
					/* +++++++FINISH
					//Fixes the Blacksword's 'Fire' power in BG:
					&& ((Egg_object *)obj).get_type() < Egg_object::fire_field
					*/
					)
					ev = UsecodeMachine.egg_proximity;
						// And for telekenesis spell fun:
				/* +++++++FINISH
				else if (fun == ucmachine.telekenesis_fun)
					{
					ev = UsecodeMachine.double_click;
					ucmachine.telekenesis_fun = -1;
					}
				*/
				ucmachine.callUsecode(fun, obj, ev);
				break;
				}
			case usecode2:// Call(fun, eventid).
				{
				UsecodeValue val = code.getElem(++i);
				int evid = code.getElem(++i).getIntValue();
				ucmachine.callUsecode(val.getIntValue(), obj,  evid);
				break;
				}
			case speech:		// Play speech track.
				{
				UsecodeValue val = code.getElem(++i);
				int track = val.getIntValue();
				/*++++++++++++++
				if (track >= 0)
					Audio::get_ptr().start_speech(track);
				*/
				}
			case sfx:		// Play sound effect!
				{
				UsecodeValue val = code.getElem(++i);
				/*++++++++++++
				Audio::get_ptr().play_sound_effect(
								val.getIntValue(), obj);
				*/
				break;
				}
			case face_dir:		// Parm. is dir. (0-7).  0=north.
				{
						// Look in that dir.
				UsecodeValue val = code.getElem(++i);
						// It may be 0x3x.  Face dir?
				int dir = val.getIntValue()&7;
				Actor npc = obj.asActor();
				if (npc != null)
					npc.setUsecodeDir(dir);
				UsecodeIntrinsics.setItemFrame(obj, obj.getDirFramenum(
					dir, obj.getFrameNum()), true, true);
				frame_index = 0;// Reset walking frame index.
				break;
				}
			case weather:
				{
						// Set weather to that type.
				UsecodeValue val = code.getElem(++i);
				int type = val.getIntValue()&0xff;
					// Seems to match the originals:
				/*++++++++++
				if (type == 0xff || gwin.get_effects().get_weather() != 0)
					Egg_object::set_weather(type == 0xff ? 0 : type);
				*/
				break;
				}
			case hit:		// Hit(hps, type).
				{
				UsecodeValue hps = code.getElem(++i);
				UsecodeValue type = code.getElem(++i);
				obj.reduceHealth(hps.getIntValue(), type.getIntValue(), null);
				break;
				}
			case attack:		// Finish 'set_to_attack()'.
				{
				Actor act = obj.asActor();
				/*++++++++++++
				if (act != null)
					act.usecode_attack();
				*/
				break;
				}
			case resurrect:
				{
				/*+++++++++++
				Dead_body *body = (Dead_body *) obj;
				Actor act = gwin.getNpc(body.get_live_npc_num());
				if (act)
					act.resurrect(body);
				*/
				break;
				}
			default:
						// Frames with dir.  U7-verified!
				if (opcode >= 0x61 && opcode <= 0x70) {
					// But don't show empty frames.
					//Get the actor's actual facing:
					int v = (obj.getFrameNum()&48)|(opcode - 0x61);
					UsecodeIntrinsics.setItemFrame(obj, v, true, true);
				} else if (opcode >= 0x30 && opcode < 0x38) {	
					// Step in dir. opcode&7.
					step(opcode&7, 0);
					do_another = true;	// Guessing.
				} else {
					System.out.println("Und sched. opcode " + opcode);
				}
				break;
				}
			}
		return delay;
	}
	/*
	 *	Step in given direction.
	 */
	public void step
		(
		int dir,			// 0-7.
		int dz
		) {
		int frame = obj.getFrameNum();
		// ++++++FINISH Barge_object *barge;
		Actor act = obj.asActor();
		if (act != null) {
			Tile tile = new Tile();
			obj.getTile(tile);
			if (dir != -1)
				tile.getNeighbor(tile, dir);
			tile.tz += dz;
			act.clearRestTime();
			Actor.FramesSequence frames = act.getFrames(dir);
						// Get frame (updates frame_index).
			frame_index = frames.nextIndex(frame_index);
			frame = frames.get(frame_index);
			if (tile.tz < 0)
				tile.tz = 0;
			obj.step(tile, frame, true);
			}
		/* ++++++++++
		else if ((barge = obj->as_barge()) != 0)
			{
			for (int i = 0; i < 4; i++)
				{
				Tile_coord t = obj->get_tile();
				if (dir != -1)
					t = t.get_neighbor(dir);
				t.tz += dz/4 + (!i ? dz % 4 : 0);
				if (t.tz < 0)
					t.tz = 0;
				obj->step(t, 0, true);
				}
			}
		*/
	}

	/*
	/*
	 * For TimeSensitive
	 */
	public boolean alwaysHandle() {	
		return false;
	}
	public void addedToQueue() {
		++timeQueueCount;
	}
	public void removedFromQueue() {
		--timeQueueCount;
	}
	public final boolean inQueue() {
		return timeQueueCount > 0;
	}
	/*
	 *	Execute an array of usecode, generally one instruction per tick.
	 */
	public void handleEvent
		(
		int curtime,			// Current time of day.
		Object udata			// .usecode machine.
		) {
		Actor act = obj.asActor();
		/* +++++++FINISH
		if (act != null && act.get_casting_mode() == Actor::init_casting)
			act.display_casting_frames();
		*/
		int delay = exec(false);
		if (i < cnt) {			// More to do?
			tqueue.add(curtime + delay, this, udata);
			return;
		}
		/* ++++++++FINISH
		if (act && act.get_casting_mode() == Actor::show_casting_frames)
			act.end_casting_mode(delay);
		 */
		scripts.remove(this);	// All done.
	}
}
