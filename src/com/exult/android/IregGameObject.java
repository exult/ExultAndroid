package com.exult.android;

/*
 * These are moveable objects.
 */
public class IregGameObject extends GameObject {
	private ContainerGameObject owner;	// Container this is in, or 0.
	protected int flags;		// 32 flags used in 'usecode'.
	protected int flags2;		// Another 32 flags used in 'usecode'.
	
	public IregGameObject(int shapenum, int framenum, int tilex, int tiley, int lft) {
		super(shapenum, framenum, tilex, tiley, lft);
	}
	public final void setFlags(int f) {
		flags = f;
	}
	public static IregGameObject create
		(
		ShapeInfo info,		// Info. about shape.
		int shnum, int frnum,		// Shape, frame.
		int tilex, int tiley,		// Tile within chunk.
		int lift			// Desired lift.
		) {
			// (These are all animated.)
		/* +++++FINISH
		if (info.isField() && info.getFieldType() >= 0)
			return new Field_object(shnum, frnum, tilex, tiley,
					lift, Egg_object::fire_field + info.get_field_type());
		else if (info.is_animated() || info.has_sfx())
			return new Animated_ireg_object(
				   shnum, frnum, tilex, tiley, lift);
		else if (shnum == 607)		// Path.
			return new Egglike_game_object(
					shnum, frnum, tilex, tiley, lift);
		else if (info.is_mirror())	// Mirror
			return new Mirror_object(shnum, frnum, tilex, tiley, lift);
		else if (info.is_body_shape())
			return new Dead_body(shnum, frnum, tilex, tiley, lift, -1);
		else if (info.get_shape_class() == ShapeInfo.virtue_stone)
			return new Virtue_stone_object(
					shnum, frnum, tilex, tiley, lift);
		else if (info.get_shape_class() == ShapeInfo.spellbook) {
			static unsigned char circles[9] = {0};
			return new Spellbook_object(
				shnum, frnum, tilex, tiley, lift,
				&circles[0], 0);
		} else */ if (info.getShapeClass() == ShapeInfo.container) {
			/*
			if (info.is_jawbone())
				return new Jawbone_object(shnum, frnum, tilex, tiley,
									lift);
			else */
				return new ContainerGameObject(shnum, frnum, 
							tilex, tiley, lift, 0);
		} else
			return new IregGameObject(shnum, frnum, tilex, tiley, lift);
	}
}
