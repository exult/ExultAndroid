package com.exult.android;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

public class UsecodeScript extends GameSingletons implements TimeSensitive {
	private static int count;		// Total # of these around.
	private static UsecodeScript first;// ->chain of all of them.
	private static LinkedList<UsecodeScript> scripts;
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
		Object udata			// ->usecode machine.
		) {
		/* +++++++++++
		Actor act = obj->as_actor();
		if (act && act->get_casting_mode() == Actor::init_casting)
			act->display_casting_frames();
		Usecode_internal *usecode = (Usecode_internal *) udata;
		int delay = exec(usecode, false);
		if (i < cnt)			// More to do?
			{
			usecode->gwin->get_tqueue()->add(curtime + delay, this, udata);
			return;
			}
		if (act && act->get_casting_mode() == Actor::show_casting_frames)
			act->end_casting_mode(delay);
		delete this;			// Hope this is safe.
		*/
		}

}
