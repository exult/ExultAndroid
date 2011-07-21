package com.exult.android;
import android.graphics.Point;

public final class DraggingInfo extends GameSingletons {
	public static GameObject lastDropped;	// For debugging.
	private GameObject obj;		// What's being dragged.
	private boolean is_new;			// Object was newly created.
	private Gump gump;
	private GumpWidget.Button button;
	private Tile old_pos;		// Original pos. of object if it wasn't
						//   in a container.
	private Rectangle old_foot;		// Original footprint.
	private int old_lift;			// Lift of obj OR its owner.
	private int quantity;			// Amount of object being moved.
	private int readied_index;		// If it was a 'readied' item.
					// Last mouse, paint positions:
	private int mousex, mousey;
	//UNUSED private int mouseShape;			// Save starting mouse shape.
	private Point paint;
	private Rectangle rect;			// Rectangle to repaint.
	private Rectangle paintRect;
	private boolean possible_theft;		// Moved enough to be 'theft'.
	
	public Gump getGump() {
		return gump;
	}
	private boolean start(int x, int y) { // First motion.
		int deltax = Math.abs(x - mousex),
		deltay = Math.abs(y - mousey);
		if (deltax <= 2 && deltay <= 2)
			return (false);		// Wait for greater motion.
		if (obj != null) {			// Don't want to move walls.
			if (!cheat.inHackMover() && !obj.isDragable() &&
					      obj.getOwner() == null) {
				mouse.flashShape(Mouse.tooheavy);
				obj = null;
				gump = null;
				return (false);
			}
			GameObject owner = obj.getOutermost();
			if (owner == obj) {
		    	if (!cheat.inHackMover() && 
				!PathFinder.FastClient.isGrabable(
				   gwin.getMainActor(), obj)) {
		    		mouse.flashShape(Mouse.blocked);
		    		obj = null;
		    		gump = null;
		    		return (false);
				}
			}
		}
					// Store original pos. on screen.
		if (gump != null) {
			if (obj != null) {
				gump.getShapeRect(rect, obj);
				ContainerGameObject owner = gump.getContOrActor(x,y);
				// Get the object
				GameObject owner_obj = 
					gump.getContainer().getOutermost(); 
				Actor main_actor = gwin.getMainActor();
				// Check the range
				if (!cheat.inHackMover() &&
						!PathFinder.FastClient.isGrabable(main_actor, owner_obj)) {
					obj = null;
					gump = null;
					mouse.flashShape(Mouse.outofrange);
					return false;
				}
				if (owner != null)
					readied_index = owner.findReadied(obj);
				gump.remove(obj);
			} else {
				gump.getDirty(rect);
				gumpman.removeGump(gump);
			}
		} else {
			gwin.getShapeRect(rect, obj);
			obj.removeThis();
		}
					// Make a little bigger.
		rect.enlarge(deltax > deltay ? deltax : deltay);
		
		gwin.clipToWin(paintRect);
		gwin.paint(paintRect);		// Paint over obj's. area.
		return true;
	}
	private void putBack() {	// Put back object.
		if (gump != null) {			// Put back remaining/orig. piece.
					// And don't check for volume!
			// Restore saved vals.
			obj.setShapePos(old_pos.tx, old_pos.ty);
			// 1st try with dont_check==false so usecode gets called.
			if (!gump.add(obj, -2, -2, -2, -2, false, false))
				gump.add(obj, -2, -2, -2, -2, true, false);
		} else if (is_new) {
			obj.setInvalid();	// It's not in the world.
			obj.removeThis();
		} else				// Normal object.  Put it back.
			obj.move(old_pos);
		obj = null;			// Just to be safe.
		is_new = false;
	}
	/*
	 *	Drop object on a gump.
	 *
	 *	Output:	False if not (all) of object was dropped.
	 */
	private boolean dropOnGump(int x, int y, GameObject to_drop, Gump on_gump) {
		GameObject owner_obj = on_gump.getContainer();
		if (owner_obj != null) 
			owner_obj = owner_obj.getOutermost();
		Actor main_actor = gwin.getMainActor(); 
		// Check the range
		if (owner_obj != null && !cheat.inHackMover() &&
			!PathFinder.FastClient.isGrabable(main_actor, owner_obj)) {
				  		// Object was not grabable
			mouse.flashShape(Mouse.outofrange);
			return false;
		}
		if (!checkWeight(to_drop, on_gump.getContOrActor(x,y)))
			return false;
		if (on_gump != gump)		// Not moving within same gump?
			possible_theft = true;
						// Add, and allow to combine.
		if (!on_gump.add(to_drop, x, y, paint.x, paint.y, false, true))
			{			// Failed.
			if (to_drop != obj)
				{		// Watch for partial drop.
				int nq = to_drop.getQuantity();
				if (nq < quantity)
					obj.modifyQuantity(quantity - nq);
				}
			mouse.flashShape(Mouse.wontfit);
			return false;
			}
		return true;
	}
	/*
	 *	See if there's something blocking an object at a given point.
	 */
	private static boolean isInaccessible
		(
		GameObject obj,
		int x, int y
		) {
		GameObject block = gwin.findObject(x, y);
		if (block != null && block != obj && !block.isDragable()) {
			return true;
		} else 
			return false;
	}
	/*
	 *	Check weight.
	 *
	 *	Output:	false if too heavy, with mouse flashed.
	 */
	private static boolean checkWeight
		(
		GameObject to_drop,
		GameObject owner		// Who the new owner will be.
		) {
		if (cheat.inHackMover())	// hack-mover  . no weight checking
			return true;
		if (owner == null)
			return true;
		owner = owner.getOutermost();
		if (!owner.getFlag(GameObject.in_party))
			return true;		// Not a party member, so okay.
		int wt = owner.getWeight() + to_drop.getWeight();
		if (wt/10 > owner.getMaxWeight()) {
			mouse.flashShape(Mouse.tooheavy);
			return false;
		}
		return true;
	}
	/*
	 *	Drop object onto the map.
	 *
	 *	Output:	False if not (all) of object was dropped.
	 */

	private boolean dropOnMap(int x, int y, GameObject to_drop) {
		// Attempting to drop off screen?
		if (x < 0 || y < 0 || x >= gwin.getWidth() || y >= gwin.getHeight()) {
			
			mouse.flashShape(Mouse.redx);
			audio.playSfx(Audio.gameSfx(76));
			return false;
		}
		int max_lift = cheat.inHackMover() ? 255 :
						gwin.getMainActor().getLift() + 5;
		int skip = gwin.getRenderSkipLift();
		if (max_lift >= skip)		// Don't drop where we cannot see.
			max_lift = skip - 1;
						// Drop where we last painted it.
		int posx = paint.x, posy = paint.y;
		if (posx == -1000)		// Unless we never painted.
			{ posx = x; posy = y; }
		int lift;
						// Was it dropped on something?
		GameObject found = gwin.findObject(x, y);
		int dropped = 0;		// 1 when dropped.
		if (found != null && found != obj) {
			if (!checkWeight(to_drop, found))
				return false;
			if (found.drop(to_drop)) {
				dropped = 1;
				possible_theft = true;
			} else if ((lift = found.getLift() +
				     found.getInfo().get3dHeight()) <= max_lift)
				// Try to place on 'found'.
				dropped = dropAtLift(to_drop,posx, posy, lift);
			else {		// Too high.
				
				mouse.flashShape(Mouse.redx);
				audio.playSfx(Audio.gameSfx(76));
				return false;
			}
		}
						// Find where to drop it, but stop if
						//   it will end up hidden (-1).
		for (lift = old_lift; dropped == 0 && lift <= max_lift; lift++)
			dropped = dropAtLift(to_drop, posx, posy, lift);
		System.out.println("Dropping " + to_drop.getShapeNum() +
				" with old_lift = " + old_lift);
		if (dropped <= 0) {
			
			mouse.flashShape(Mouse.blocked);
			audio.playSfx(Audio.gameSfx(76));
			System.out.println("Could not find spot to drop " + 
					to_drop.getShapeNum());
			return false;
		}
		System.out.println("New pos is " + to_drop.getTileX() +
				", " + to_drop.getTileY() + ", " + to_drop.getLift());
						// Moved more than 2 tiles.
		if (gump == null && !possible_theft) {
			Tile t = new Tile();
			to_drop.getTile(t);
			if (t.distance(old_pos) > 2)
				possible_theft = true;
		}
		return true;
	}	
	public static boolean startDragging(int x, int y) {
		if (drag == null)
			drag = new DraggingInfo();
		if (drag.init(x, y))
			return true;
		return false;
	}
	public DraggingInfo() {
		old_lift = -1;
		readied_index = -1;
		old_pos = new Tile();
		old_foot = new Rectangle();
		rect = new Rectangle();
		paintRect = new Rectangle(rect);
	}
	public static GameObject getObject() {
		return drag.obj;
	}
	public static void abort() {
		drag.clear();
	}
	public void clear() {
		obj = null;
		gump = null;
		button = null;
		rect.w = -1;
		GameWindow.onObj = null;
	}
	public boolean init(int x, int y) {		
		mousex = x; mousey = y;
		obj = null;
		button = null;
		//UNUSED mouseShape = mouse.getShape();
		mouse.setShape(Mouse.hand);
		rect.w = -1;
		// First see if it's a gump.
		gump = gumpman.findGump(x, y);
		if (gump != null) {
			obj = gump.findObject(x, y);
			if (obj != null) {		// Save location info.
				paint = new Point();
				gump.getShapeLocation(paint, obj);
				old_pos.set(obj.getTx(), obj.getTy(), 0);
			} else if ((button = gump.onButton(x, y)) != null) {
				gump = null;
				if (!button.isDraggable())
					return false;
				button.push(true);
						// Pushed button, so make noise.
				audio.playSfx(Audio.gameSfx(73));
				gwin.setPainted();
			} else if (gump.isDraggable()) {	// Dragging whole gump.
				paint = new Point(gump.getX(), gump.getY());
			} else 			// the gump isn't draggable
				return false;
		} else if (x >0 && y > 0 && 
				   x < gwin.getWidth() && y < gwin.getHeight()) {
			obj = gwin.findObject(x, y);
			if (obj == null)
				return false;
						// Get coord. where painted.
			gwin.getShapeLocation(paint = new Point(), obj);
			obj.getTile(old_pos);
			obj.getFootprint(old_foot);
		}
		if (obj != null) {
			quantity = obj.getQuantity();
						// Save original lift.
			old_lift = obj.getOutermost().getLift();
		}
		return true;
	}
	/*
	 *	Mouse was moved while dragging.
	 *
	 *	Output:	true iff movement started/continued.
	 */
	public boolean moved(int x, int y) {	// Mouse moved.
		if (obj == null && gump == null)
			return (false);
		if (rect.w == -1) {
			if (!start(x, y))
				return false;
		} else {
			gwin.clipToWin(rect);
			gwin.addDirty(rect);
		}
		gwin.setPainted();
		int deltax = x - mousex, deltay = y - mousey;
		mousex = x;
		mousey = y;
						// Shift to new position.
		rect.shift(deltax, deltay);
		paint.x += deltax;
		paint.y += deltay;
		if (gump != null && obj == null)			// Dragging a gump?
			gump.setPos(paint.x, paint.y);
		gwin.clipToWin(rect);
		gwin.addDirty(rect);
		// See if we're over something.
		Gump onGump = gumpman.findGump(x, y);
		GameObject onObj = null;
		if (onGump != null)
			onObj = onGump.findObject(x, y);
		else
			onObj = gwin.findObject(x, y);
		if (onObj != GameWindow.onObj) {
			if (GameWindow.onObj != null)
				gwin.addDirty(GameWindow.onObj);
			if (onObj != null)
				gwin.addDirty(onObj);
			GameWindow.onObj = onObj;
		}
		return true;
	}
	public void paint() {			// Paint object being dragged.
		if (rect.w <= 0)			// Not moved enough yet?
			return;
		if (obj != null) {
			if (obj.getFlag(GameObject.invisible))
				obj.paintInvisible(paint.x, paint.y);
			else {
				int x = paint.x, y = paint.y;
				obj.paintShape(x, y);
				obj.paintOutline(x, y, ShapeID.HIT_PIXEL);
			}
		} else if (gump != null) {
			gump.paint();
		}
	}
	/*
	 *	Mouse was released, so drop object. 
	 *      Return true iff the dropping mouseclick has been handled. 
	 *		(by buttonpress, drag)
	 */
	private int dropx, dropy;
	public boolean drop(int x, int y) {	// Drop obj. at given position.
						// First see if it's a gump.
		Gump on_gump = gumpman.findGump(x, y);
						// Don't prompt if within same gump.
		if (quantity > 1 && (on_gump == null || on_gump != gump)) {
			dropx = x; dropy = y;
			Thread t = new Thread() {
	    		public void run() {
	    			quantity = gumpman.promptForNumber(0, quantity, 1, quantity);
	    			dropQuantityAndClear(dropx, dropy, quantity);
	    		}
	    	};
	    	t.start();
			return true;
		}
		return dropQuantityAndClear(x, y, quantity);
	}
	private boolean dropQuantityAndClear(int x, int y, int quantity) {
		Boolean ret = dropQuantity(x, y, quantity);
		if (!ret)
			putBack();		// Not all dropped.
		if (obj != null)
			lastDropped = obj;
		obj = null;			// Clear so we don't paint them.
		gump = null;
		GameWindow.onObj = null;
		gwin.paint();
		return ret;
	}
	private boolean dropQuantity(int x, int y, int quantity) {
		// Get orig. loc. info.
		int oldcx = old_pos.tx/EConst.c_tiles_per_chunk, 
		    oldcy = old_pos.ty/EConst.c_tiles_per_chunk;
		GameObject to_drop = obj;	// If quantity, split it off.
		ShapeInfo info = obj.getInfo();
		// Being liberal about taking stuff:
		boolean okay_to_move = obj.getFlag(GameObject.okay_to_take);
		int old_top = old_pos.tz + info.get3dHeight();
		
		//System.out.println("dropQuantity: " + quantity + ", obj quant = " + obj.getQuantity());
		if (quantity <= 0)
			return false;
		if (quantity < obj.getQuantity()) {
						// Need to drop a copy.
			to_drop = IregGameObject.create(info,
					obj.getShapeNum(), obj.getFrameNum());
			to_drop.modifyQuantity(quantity - 1);
			if (okay_to_move)	// Make sure copy is okay to take.
				to_drop.setFlag(GameObject.okay_to_take);
		}
		Gump on_gump = gumpman.findGump(x, y);
						// Drop it.
		if (!(on_gump != null ? dropOnGump(x, y, to_drop, on_gump)
				  : dropOnMap(x, y, to_drop)))
			return false;
						// Make a 'dropped' sound.
		audio.playSfx(Audio.gameSfx(74));
		if (gump == null)			// Do eggs where it came from.
			gmap.getChunk(oldcx, oldcy).activateEggs(obj,
				    (int)old_pos.tx, (int)old_pos.ty, (int)old_pos.tz, 
						(int)old_pos.tx, (int)old_pos.ty, false);
						// Special:  BlackSword in SI.
		else if (readied_index >= 0 && obj.getShapeNum() == 806) {
						// Do 'unreadied' usecode.
			Actor act = gump.getContOrActor(x,y).asActor();
			if (act != null)
				act.callReadiedUsecode(
								readied_index, obj, UsecodeMachine.unreadied);
		}
						// On a barge?
		BargeObject barge = gwin.getMovingBarge();
		if (barge != null)
			barge.setToGather();	// Refigure what's on barge.
						// Check for theft.
		if (!okay_to_move && !cheat.inHackMover() && possible_theft &&
								gwin.isInDungeon() == 0)
			gwin.theft();			
		if (to_drop == obj) {		// Whole thing?
						// Watch for stuff on top of it.
			if (old_foot.w > 0)
				MapChunk.gravity(old_foot, old_top);
			System.out.println("Dropped whole object");
			return true;		// All done.
		}
						// Subtract quantity moved.
		obj.modifyQuantity(-quantity);	
		return false;	// Put back the rest.		
	}
	/*
	 *	Mouse was released, so drop object. 
	 *      Return true iff the dropping mouseclick has been handled. 
	 *		(by buttonpress, drag)
	 */
	public boolean drop(int x, int y, boolean moved) {
		boolean handled = moved;
		if (button != null) {
			button.unpush(true);
			if (button.onWidget(x, y))
						// Clicked on button.
				button.activate(true);
			handled = true;
		} else if (obj == null) {		// Only dragging a gump?
			if (gump == null)
				return handled;
			if (!moved)		// A click just raises it to the top.
				gumpman.removeGump(gump);
			gumpman.addGump(gump);
		}
		else if (!moved)		// For now, if not moved, leave it.
			return handled;
		else if (!drop(x, y)) {		// Drop it.
			System.out.println("Failed to drop.  Putting back.");
			obj = null;			// Clear so we don't paint them.
			gump = null;
		}
		gwin.paint();
		return handled;
	}
	/*
	 *	Try to drop at a given lift.  Note:  None of the drag state variables
	 *	may be used here, as it's also called from the outside.
	 *
	 *	Output:	1 if successful.
	 *		0 if blocked
	 *		-1 if it would end up hidden by a non-moveable object.
	 */

	public static int dropAtLift
		(
		GameObject to_drop,
		int x, int y,			// Pixel coord. in window.
		int at_lift
		)
		{
		x += at_lift*4 - 1;		// Take lift into account, round.
		y += at_lift*4 - 1;
		int tx = (gwin.getScrolltx() + x/EConst.c_tilesize)%EConst.c_num_tiles;
		int ty = (gwin.getScrollty() + y/EConst.c_tilesize)%EConst.c_num_tiles;
		int cx = tx/EConst.c_tiles_per_chunk;
		int cy = ty/EConst.c_tiles_per_chunk;
		MapChunk chunk = gmap.getChunk(cx, cy);
		int lift = 0;			// Can we put it here?
		ShapeInfo info = to_drop.getInfo();
		int frame = to_drop.getFrameNum();
		int xtiles = info.get3dXtiles(frame), ytiles = info.get3dYtiles(frame);
		int max_drop, move_flags;
		if (cheat.inHackMover()) {
			max_drop = at_lift;
			move_flags = EConst.MOVE_WALK|EConst.MOVE_MAPEDIT;
		} else {			// Allow drop of 5;
			max_drop = 5;
			move_flags = EConst.MOVE_WALK;
		}
		Tile loc = new Tile(tx - xtiles + 1, ty - ytiles + 1, at_lift);
		if (!MapChunk.areaAvailable(xtiles, ytiles, info.get3dHeight(),
							loc, move_flags, max_drop, -1) ||
		      (!cheat.inHackMover() &&
						// Check for path to location.
		    !PathFinder.FastClient.isGrabable(gwin.getMainActor(), 
		    												tx, ty, lift)))
			return 0;
		lift = loc.tz;
		to_drop.setInvalid();
		to_drop.move(tx, ty, lift);
		Rectangle rect = new Rectangle();
		gwin.getShapeRect(rect, to_drop);
						// Avoid dropping behind walls.
		if (isInaccessible(to_drop, rect.x + 2, rect.y + 2) &&
		    isInaccessible(to_drop, 
					rect.x + rect.w - 3, rect.y + 2) &&
		    isInaccessible(to_drop, 
					rect.x + 2, rect.y + rect.h - 3) &&
		    isInaccessible(to_drop, 
					rect.x + rect.w - 3, rect.y + rect.h - 3) &&
			isInaccessible(to_drop, 
					rect.x + (rect.w >> 1), rect.y + (rect.h >> 1))) {
			to_drop.removeThis();
			return -1;
		}
						// On an egg?
		chunk.activateEggs(to_drop, tx, ty, lift, tx, ty, false);
		if (to_drop == gwin.getMainActor()) {
			gwin.centerView(to_drop.getTileX(), to_drop.getTileY(), to_drop.getLift());
			gwin.paint();
		}
		return (1);
	}

}
