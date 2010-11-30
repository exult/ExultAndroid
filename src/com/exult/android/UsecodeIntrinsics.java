package com.exult.android;

public class UsecodeIntrinsics extends GameSingletons {
	private static final GameObject getItem(UsecodeValue v) {
		return ucmachine.get_item(v);
	}
	
	/*
	 * The intrinsics:
	 */
	private UsecodeValue getRandom(UsecodeValue p0) {
		int range = p0.getIntValue();
		if (range == 0)
			return UsecodeValue.getZero();
		return new UsecodeValue.IntValue(1 + (EUtil.rand() % range));
	}
	private UsecodeValue getItemShape(UsecodeValue p0) {
		GameObject obj = getItem(p0);
		return obj == null ? UsecodeValue.getZero() :
			new UsecodeValue.IntValue(obj.getShapeReal());
	}
	
	//	For BlackGate.
	public UsecodeValue execute(int id, int event, int num_parms, UsecodeValue parms[]) {
		switch (id) {
		case 0x00:
			return getRandom(parms[0]);
		case 0x11:
			return getItemShape(parms[0]);
		default:
			System.out.println("UNHANDLED intrinsic # " + id);
			return UsecodeValue.getZero();
		}
	}
}
