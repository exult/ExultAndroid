package com.exult.android;

public class NpcActor extends Actor {
	public NpcActor(String nm, int shapenum) { //++++FINISH, num, uc
		super(nm, shapenum, -1, -1); 
	}
	/*
	 *	Handle a time event (for TimeSensitive).
	 */
	public void handleEvent(int ctime, Object udata) {
		/* +++++++++FINISH
		if ((cheat.in_map_editor() && party_id < 0) ||
				(get_flag(Obj_flags::paralyzed) || is_dead() ||
				get_property(static_cast<int>(health)) <= 0 ||
				(get_flag(Obj_flags::asleep) && schedule_type != Schedule::sleep)))
			{
			gwin->get_tqueue()->add(
					curtime + gwin->get_std_delay(), this, udata);
			return;
			}
		// Prevent actor from doing anything if not in the active map.
		// ... but not if the NPC is not on the map (breaks pathfinding
		// from offscreen if NPC not on map).
		if (get_map() && get_map() != gwin->get_map())
			{
			set_action(0);
			dormant = true;
			if (schedule)
				schedule->im_dormant();
			return;
			}
		if (schedule && party_id < 0 && can_act() && 
				(schedule_type != Schedule::combat ||	// Not if already in combat.
							// Patrol schedule already does this.
					schedule_type != Schedule::patrol ||
					schedule_type != Schedule::sleep ||
					schedule_type != Schedule::wait) &&
				!(rand()%3))	// Don't do it every time.
			schedule->seek_foes();

		if (!action)			// Not doing anything?
			{
			if (in_usecode_control() || !can_act())
					// Can't move on our own. Keep trying.
				gwin->get_tqueue()->add(
						curtime + gwin->get_std_delay(), this, udata);
			else if (schedule)
				{
					// Should try seeking foes?
				if (party_id < 0 && can_act() &&
							// Not if already in combat.
						(schedule_type != Schedule::combat ||
							// Patrol schedule already does this.
							schedule_type != Schedule::patrol ||
							schedule_type != Schedule::sleep ||
							schedule_type != Schedule::wait) &&
						!(rand()%4))	// Don't do it every time.
					{
					schedule->seek_foes();
						// Get back into queue.
					gwin->get_tqueue()->add(
							curtime + gwin->get_std_delay(), this, udata);
					}
				else
					schedule->now_what();
				}
			}
		else
			{			// Do what we should.
			int delay = party_id < 0 ? gwin->is_time_stopped() : 0;
			if (delay <= 0)		// Time not stopped?
				{
				int speed = action->get_speed();
				delay = action->handle_event(this);
				if (!delay)
					{	// Action finished. Add a slight delay.
					frame_time = speed;
					if (!frame_time)	// Not a path. Add a delay anyway.
						frame_time = gwin->get_std_delay();
					delay = frame_time;
					set_action(0);
					}
				}
			gwin->get_tqueue()->add(
					curtime + delay, this, udata);
			}
		*/
	}
}
