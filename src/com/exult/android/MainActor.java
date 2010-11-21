package com.exult.android;

public class MainActor extends Actor {
	public MainActor(String nm, int shapenum) {
		super(nm, shapenum);
		// ++++FINISHframes = &avatar_frames[0]; 
	}
	/*
	 *	Handle a time event (for TimeSensitive).
	 */
	public void handleEvent(int ctime, Object udata) {
		/* ++++++++FINISH
		if (action)			// Doing anything?
				{			// Do what we should.
				int speed = action->get_speed();
				int delay = action->handle_event(this);
				if (!delay)
					{	// Action finished.
						// This makes for a smoother scrolling and prevents the
						// avatar from skipping a step when walking.
					frame_time = speed;
					if (!frame_time)	// Not a path. Add a delay anyway.
						frame_time = gwin->get_std_delay();
					delay = frame_time;
					set_action(0);
					}

				gwin->get_tqueue()->add(
						curtime + delay, this, udata);
				}
			else if (in_usecode_control() || get_flag(Obj_flags::paralyzed))
				// Keep trying if we are in usecode control.
				gwin->get_tqueue()->add(
						curtime + gwin->get_std_delay(), this, udata);
			else if (schedule)
				schedule->now_what();
	*/
	}
}
