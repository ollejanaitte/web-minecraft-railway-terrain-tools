package net.minecraft.railv2;

import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Phase 0.1 validation spike: deterministic course
 *   straight(80m) -> big 90deg curve -> straight(80m)
 * plus world block placement so rails are VISIBLE in-game.
 *
 * Course direction: piece1 along +X, piece2 turns to +Z, piece3 along +Z.
 */
public final class RailV2Course {
	public static final double STRAIGHT_LEN = 80.0D;
	public static final double COURSE_Y = 64.0D;

	/** Shared instance (deterministic; usable on client and server). */
	public static final RailV2Course INSTANCE = new RailV2Course();

	private final RailV2Geometry[] pieces;
	private final double[] pieceStartGlobal;

	public RailV2Course() {
		this.pieces = new RailV2Geometry[3];
		this.pieces[0] = new RailV2Straight(0.0D, COURSE_Y, 0.0D, STRAIGHT_LEN, COURSE_Y, 0.0D, 1);
		this.pieces[1] = new RailV2Bezier(
				STRAIGHT_LEN, COURSE_Y, 0.0D,
				STRAIGHT_LEN + 60.0D, COURSE_Y, 0.0D,
				STRAIGHT_LEN + 120.0D, COURSE_Y, 60.0D,
				STRAIGHT_LEN + 120.0D, COURSE_Y, STRAIGHT_LEN,
				2);
		this.pieces[2] = new RailV2Straight(STRAIGHT_LEN + 120.0D, COURSE_Y, STRAIGHT_LEN,
				STRAIGHT_LEN + 120.0D + STRAIGHT_LEN, COURSE_Y, STRAIGHT_LEN, 3);
		this.pieceStartGlobal = new double[3];
		this.pieceStartGlobal[0] = 0.0D;
		this.pieceStartGlobal[1] = this.pieces[0].lengthM();
		this.pieceStartGlobal[2] = this.pieceStartGlobal[1] + this.pieces[1].lengthM();
	}

	public int pieceCount() {
		return this.pieces.length;
	}

	public RailV2Geometry piece(int i) {
		return this.pieces[i];
	}

	public double totalLength() {
		return this.pieceStartGlobal[2] + this.pieces[2].lengthM();
	}

	public double pieceStartGlobal(int i) {
		return this.pieceStartGlobal[i];
	}

	/** Resolve a global course distance to a local sample on the owning piece. */
	public RailV2Sample resolve(double globalM) {
		double g = globalM < 0.0D ? 0.0D : (globalM > totalLength() ? totalLength() : globalM);
		for (int i = 0; i < pieces.length; i++) {
			if (g <= pieceStartGlobal[i] + pieces[i].lengthM() || i == pieces.length - 1) {
				return pieces[i].sampleByDistance(g - pieceStartGlobal[i]);
			}
		}
		return pieces[0].sampleByDistance(0.0D);
	}

	/**
	 * Place visible rails + a stone platform in the world.
	 * Two parallel vanilla-rail lines (gauge visual) over a stone slab bed.
	 */
	public void placeRails(World world) {
		int n = (int) Math.ceil(totalLength()) + 1;
		for (int i = 0; i <= n; i++) {
			RailV2Sample s = resolve(i);
			int x = MathHelper.floor_double(s.x);
			int z = MathHelper.floor_double(s.z);
			int y = MathHelper.floor_double(s.y);
			// flatten + platform (3 wide bed)
			for (int dy = 0; dy < 3; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					setBlock(world, x, y - 1 - dy, z + dz, Blocks.stone);
				}
			}
			// clear above
			for (int dy = 0; dy < 6; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					setBlock(world, x, y + dy, z + dz, Blocks.air);
				}
			}
		}
		// rails: two lines at z-1 and z+1 (gauge), plus center, every 1m
		for (int i = 0; i <= n; i += 2) {
			RailV2Sample s = resolve(i);
			int x = MathHelper.floor_double(s.x);
			int z = MathHelper.floor_double(s.z);
			int y = MathHelper.floor_double(s.y);
			setBlock(world, x, y, z - 1, Blocks.rail);
			setBlock(world, x, y, z + 1, Blocks.rail);
		}
	}

	private static void setBlock(World world, int x, int y, int z, net.minecraft.block.Block block) {
		BlockPos pos = new BlockPos(x, y, z);
		if (world.isBlockLoaded(pos)) {
			world.setBlockState(pos, block.getDefaultState(), 2);
		}
	}
}
