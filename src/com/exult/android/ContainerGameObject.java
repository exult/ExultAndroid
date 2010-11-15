package com.exult.android;

public class ContainerGameObject extends IregGameObject {
	private int volumeUsed;		// Amount of volume occupied.
	private byte resistance;	// Resistance to attack.
	protected ObjectList objects;
	
	public ContainerGameObject(int shapenum, int framenum, int tilex, 
			int tiley, int lft,	int res) {
		super(shapenum, framenum, tilex, tiley, lft);
		resistance = (byte)res;
	}
}
