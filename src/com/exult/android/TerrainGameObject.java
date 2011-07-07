package com.exult.android;

public class TerrainGameObject extends GameObject {
	public TerrainGameObject(int shapenum, int framenum, int tilex, 
			int tiley, int lft) {
		super(shapenum, framenum, tilex, tiley, lft);
	}
	public static class Animated extends TerrainGameObject {
		private Animator animator;
		public Animated(int shapenum, int framenum, int tilex, int tiley, int lft) {
			super(shapenum, framenum, tilex, tiley, lft);
			animator = Animator.create(this);
		}
		@Override
		public void removeThis() {
			super.removeThis();
			animator.delete();
		}
		@Override
		public void paint() {
			animator.wantAnimation();	// Be sure animation is on.
			super.paint();
		}
		// Get coord. where this was placed.
		@Override
		public void getOriginalTileCoord(Tile t) {
			getTile(t);
			t.tx -= animator.getDeltax();
			t.ty -= animator.getDeltay();
		}
	}
}
