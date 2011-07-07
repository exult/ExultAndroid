package com.exult.android;
import com.exult.android.shapeinf.*;

/*
 *	List of references to timers for an NPC.
 */
public class NpcTimers extends GameSingletons {
	private Actor npc;
	private Hunger hunger;
	private Poison poison;
	private Sleep sleep;
	private Invisibility invisibility;
	private Protection protection;
	// Indices into flags.
	protected static final int might = 0, curse = 1, charm = 2, paralyze = 3;
	private Flag flags[] = new Flag[4];
	
	protected static boolean wearingRing(Actor npc, int shnum, int frnum) {
		GameObject ring = npc.getReadied(Ready.lfinger);
		if (ring != null && ring.getShapeNum() == shnum &&
		    ring.getFrameNum() == frnum)
			return true;
		ring = npc.getReadied(Ready.rfinger);
		if (ring != null && ring.getShapeNum() == shnum &&
		    ring.getFrameNum() == frnum)
			return true;
		return false;
	}
	public NpcTimers(Actor n) {
		npc = n;
	}
	public void done() {
		hunger.done();
		poison.done();
		sleep.done();
		invisibility.done();
		protection.done();
		for (Flag f : flags)
			f.done();
	}
	public void startHunger() {
		if (hunger == null)
			hunger = new Hunger(this);
	}
	public void startPoison() {
		if (poison != null)
			poison.done();
		poison = new Poison(this);
	}
	public void startSleep() {
		if (sleep != null)
			sleep.done();
		sleep = new Sleep(this);
	}
	public void startInvisibility() {
		if (invisibility != null)
			invisibility.done();
		invisibility = new Invisibility(this);
	}
	public void startProtection() {
		if (protection != null)
			protection.done();
		protection = new Protection(this);
	}
	private void startFlag(int f, int i) {
		if (flags[i] != null)
			flags[i].done();
		flags[i] = new Flag(this, f, i);
	}
	public void startMight() {
		startFlag(GameObject.might, might);
	}
	public void startCurse() {
		startFlag(GameObject.cursed, curse);
	}
	public void startCharm() {
		startFlag(GameObject.charmed, charm);
	}
	public void startParalyze() {
		startFlag(GameObject.paralyzed, paralyze);
	}
	public static abstract class Timer extends TimeSensitive.Timer {
		protected NpcTimers list;
		protected int getMinute() {
			return 60*clock.getTotalHours() + clock.getMinute();
		}
		public Timer(NpcTimers l, int start_delay) {
			list = l;
			tqueue.add(TimeQueue.ticks + start_delay, this, null);
		}
		public void done() {
			if (inQueue())
				tqueue.remove(this);
		}
	}
	public static class Hunger extends Timer {
		private int lastTime; // Last game minute when penalized.
		public Hunger(NpcTimers l) {
			super(l, 5000/TimeQueue.tickMsecs);
			lastTime = getMinute();
		}
		@Override
		public void done() {
			list.hunger = null;
			super.done();
		}
		@Override
		public void handleEvent(int curtime, Object udata) {
			Actor npc = list.npc;
			// No longer a party member?
			if (!npc.getFlag(GameObject.in_party) ||
							//   or no longer hungry?
					npc.getProperty(Actor.food_level) > 0 ||
					npc.isDead()) {		// Obviously.
				done();
				return;
			}
			int minute = getMinute();
							// Once/hour.
			if (minute >= lastTime + 60) {
				int hp = EUtil.rand()%3;
				if (EUtil.rand()%4 != 0)
					npc.say(ItemNames.first_starving, ItemNames.first_starving + 2);
				npc.reduceHealth(hp, WeaponInfo.sonic_damage, null, null);
				lastTime = minute;
			}
			tqueue.add(curtime + 30000/TimeQueue.tickMsecs, this, null);
		}
	}
	/*
	 *	Handle poison.
	 */
	public static class Poison extends Timer {
		private int endTime;		// Time when it wears off.
		public Poison (NpcTimers l) {
			super(l, 5000/TimeQueue.tickMsecs);
			endTime = TimeQueue.ticks + 
						(60000 + EUtil.rand()%120000)/TimeQueue.tickMsecs;
		}
		@Override
		public void done() {
			list.poison = null;
			super.done();
		}
		@Override
		public void handleEvent(int curtime, Object udata) {
			Actor npc = list.npc;
			if (curtime >= endTime ||	// Long enough?  Or cured?
			    !npc.getFlag(GameObject.poisoned) ||
			    	npc.isDead()) {		// Obviously.
					npc.clearFlag(GameObject.poisoned);
				done();
				return;
			}
			int penalty = EUtil.rand()%3;
			npc.reduceHealth(penalty, WeaponInfo.sonic_damage, null, null);
			tqueue.add(curtime + 
					(10000 + EUtil.rand()%10000)/TimeQueue.tickMsecs, this, 0L);
		}
	}
	/*
	 *	Handle sleep.
	 */
	public static class Sleep extends Timer {
		private int endTime;		// Time when it wears off.
		public Sleep(NpcTimers l) {
			super(l, 0);
			// Lasts 5-10 secs.
			endTime = TimeQueue.ticks + 
						(5000 + EUtil.rand()%5000)/TimeQueue.tickMsecs;
		}
		@Override
		public void done() {
			list.sleep = null;
			super.done();
		}
		@Override
		public void handleEvent(int curtime, Object udata) {
			Actor npc = list.npc;
			if (npc.getProperty(Actor.health) >= 0
					&& (curtime >= endTime ||	// Long enough?  Or cured?
				    !npc.getFlag(GameObject.asleep))){
				// Avoid waking sleeping people.
				if (npc.getScheduleType() == Schedule.sleep)
					npc.clearSleep();
				else if (!npc.isDead()) {	// Don't wake the dead.
					npc.clearFlag(GameObject.asleep);
					int frnum = npc.getFrameNum();
					if ((frnum&0xf) == Actor.sleep_frame &&
								// Slimes don't change.
						    !npc.getInfo().hasStrangeMovement())
								// Stand up.
							npc.changeFrame(
								Actor.standing | (frnum&0x30));
				}
				done();
				return;
			}
			// Check again in 2 secs.
			tqueue.add(curtime + 2000/TimeQueue.tickMsecs, this, 0L);
		}
	}
	/*
	 *	Handle invisibility.
	 */
	public static class Invisibility extends Timer {
		private int endTime;		// Time when it wears off.
		public Invisibility(NpcTimers l) {
			super(l, 0);
			// Lasts 60-80 secs.
			endTime = TimeQueue.ticks + 
						(60000 + EUtil.rand()%20000)/TimeQueue.tickMsecs;
		}
		@Override
		public void done() {
			list.invisibility = null;
			super.done();
		}
		@Override
		public void handleEvent(int curtime, Object udata) {
			Actor npc = list.npc;
			if (wearingRing(npc, 296, 0)) {	// (Works for SI and BG.)
				// Wearing invisibility ring.
				done();		// Don't need timer.
				return;
			}
			if (curtime >= endTime ||	// Long enough?  Or cleared.
					!npc.getFlag(GameObject.invisible)) {
				npc.clearFlag(GameObject.invisible);
				if (!npc.isDead())
					gwin.addDirty(npc);
				done();
				return;
			}
			// Check again in 2 secs.
			tqueue.add(curtime + 2000/TimeQueue.tickMsecs, this, 0L);
		}
	}
	/*
	 *	Handle Protection.
	 */
	public static class Protection extends Timer {
		private int endTime;		// Time when it wears off.
		public Protection(NpcTimers l) {
			super(l, 0);
			// Lasts 60-80 secs.
			endTime = TimeQueue.ticks + 
						(60000 + EUtil.rand()%20000)/TimeQueue.tickMsecs;
		}
		@Override
		public void done() {
			list.protection = null;
			super.done();
		}
		@Override
		public void handleEvent(int curtime, Object udata) {
			Actor npc = list.npc;
			if (wearingRing(npc, 297, 0)) {	// ++++SI has an Amulet.
						// Wearing protection ring.
				done();		// Don't need timer.
				return;
			}
			if (curtime >= endTime ||	// Long enough?  Or cleared.
					!npc.getFlag(GameObject.protection)) {
				npc.clearFlag(GameObject.protection);
				if (!npc.isDead())
					gwin.addDirty(npc);
				done();
				return;
			}
			// Check again in 2 secs.
			tqueue.add(curtime + 2000/TimeQueue.tickMsecs, this, 0L);
		}
	}
	/*
	 *	Handle flags that don't need any other checks.
	 */
	public static class Flag extends Timer {
		private int flag;			// Flag # in GameObject flags.
		private int endTime;		// Time when it wears off.
		private int listInd;		// Index into list.flags.
		public Flag(NpcTimers l, int f, int ind) {
			super(l, 0);
			flag = f;
			listInd = ind;
			// Lasts 60-120 secs.
			endTime = TimeQueue.ticks + 
						(60000 + EUtil.rand()%60000)/TimeQueue.tickMsecs;
		}
		@Override
		public void done() {
			list.flags[listInd] = null;
			super.done();
		}
		@Override
		public void handleEvent(int curtime, Object udata) {
			Actor npc = list.npc;
			if (curtime >= endTime ||	// Long enough?  Or cleared?
						!npc.getFlag(flag)) {
				npc.clearFlag(flag);
				done();
				return;
			}
			// Again in 10 secs.
			tqueue.add(curtime + 
					10000/TimeQueue.tickMsecs, this, 0L);
		}
	}
}
