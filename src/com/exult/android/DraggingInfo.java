package com.exult.android;

public final class DraggingInfo {
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
	private int mousex, mousey, paintx, painty;
	//+++++Mouse::Mouse_shapes mouse_shape;// Save starting mouse shape.
	private Rectangle rect;			// Rectangle to repaint.
	private ImageBuf save;		// Image below dragged object.
	private boolean okay;			// True if drag constructed okay.
	private boolean possible_theft;		// Moved enough to be 'theft'.
	
	private boolean start(int x, int y) { // First motion.
		return false;//+++++++FINISH
	}
	private void putBack() {	// Put back object.
		//++++++++++
	}
	private boolean dropOnGump(int x, int y, GameObject to_drop, Gump gump) {
		return false;//++++++++FINISH
	}
	private boolean dropOnMap(int x, int y, GameObject to_drop) {
		return false;//++++++++FINISH
	}
}
