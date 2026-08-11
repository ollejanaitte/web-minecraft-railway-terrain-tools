package net.minecraft.railsys.validation;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.railsys.geometry.HorizontalBezierGeometry;
import net.minecraft.railsys.geometry.RailGeometry;
import net.minecraft.railsys.geometry.RailLocalFrame;
import net.minecraft.railsys.geometry.RailSample;
import net.minecraft.railsys.geometry.StraightGeometry;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Phase 1.1 validation-only geometry debug visualization.
 * Places centerline / tangent markers from production {@code railsys.geometry}
 * for Flat Validation World screenshots. Must NOT be used as production math.
 */
public final class RailsysGeomDebugEvidence {

	/** Offset so evidence does not collide with RailV2 spike course near origin. */
	public static final double ORIGIN_X = 0.0D;
	public static final double ORIGIN_Y = 64.0D;
	public static final double ORIGIN_Z = 200.0D;

	private RailsysGeomDebugEvidence() {
	}

	public static void placeAll(World world) {
		System.out.println("[RAILSYSTEM] Phase1.1 GeomDebugEvidence START");
		placeStraight100(world);
		placeGentle(world);
		placeTight(world);
		placeSCurve(world);
		placeGradient(world);
		placeCurveGradient(world);
		placeLocalFrameDemo(world);
		System.out.println("[RAILSYSTEM] Phase1.1 GeomDebugEvidence DONE");
	}

	private static void placeStraight100(World world) {
		StraightGeometry g = new StraightGeometry(ORIGIN_X, ORIGIN_Y, ORIGIN_Z,
				ORIGIN_X, ORIGIN_Y, ORIGIN_Z + 100.0D, 101);
		placeCenterline(world, g, Blocks.gold_block, 2);
	}

	private static void placeGentle(World world) {
		double ox = ORIGIN_X + 40.0D;
		HorizontalBezierGeometry g = new HorizontalBezierGeometry(
				ox, ORIGIN_Y, ORIGIN_Z,
				ox + 60, ORIGIN_Y, ORIGIN_Z,
				ox + 120, ORIGIN_Y, ORIGIN_Z + 60,
				ox + 120, ORIGIN_Y, ORIGIN_Z + 120, 102);
		placeCenterline(world, g, Blocks.diamond_block, 2);
	}

	private static void placeTight(World world) {
		double ox = ORIGIN_X + 180.0D;
		HorizontalBezierGeometry g = new HorizontalBezierGeometry(
				ox, ORIGIN_Y, ORIGIN_Z,
				ox + 5, ORIGIN_Y, ORIGIN_Z,
				ox + 10, ORIGIN_Y, ORIGIN_Z + 5,
				ox + 10, ORIGIN_Y, ORIGIN_Z + 10, 103);
		placeCenterline(world, g, Blocks.emerald_block, 1);
	}

	private static void placeSCurve(World world) {
		double ox = ORIGIN_X + 220.0D;
		HorizontalBezierGeometry a = new HorizontalBezierGeometry(
				ox, ORIGIN_Y, ORIGIN_Z,
				ox + 5, ORIGIN_Y, ORIGIN_Z,
				ox + 10, ORIGIN_Y, ORIGIN_Z + 5,
				ox + 10, ORIGIN_Y, ORIGIN_Z + 10, 104);
		HorizontalBezierGeometry b = new HorizontalBezierGeometry(
				ox + 10, ORIGIN_Y, ORIGIN_Z + 10,
				ox + 10, ORIGIN_Y, ORIGIN_Z + 15,
				ox + 5, ORIGIN_Y, ORIGIN_Z + 20,
				ox, ORIGIN_Y, ORIGIN_Z + 20, 105);
		placeCenterline(world, a, Blocks.redstone_block, 1);
		placeCenterline(world, b, Blocks.lapis_block, 1);
	}

	private static void placeGradient(World world) {
		double ox = ORIGIN_X + 280.0D;
		StraightGeometry g = new StraightGeometry(ox, ORIGIN_Y, ORIGIN_Z,
				ox, ORIGIN_Y + 8.0D, ORIGIN_Z + 100.0D, 106);
		placeCenterline(world, g, Blocks.iron_block, 2);
	}

	private static void placeCurveGradient(World world) {
		double ox = ORIGIN_X + 320.0D;
		HorizontalBezierGeometry g = new HorizontalBezierGeometry(
				ox, ORIGIN_Y, ORIGIN_Z,
				ox + 40, ORIGIN_Y, ORIGIN_Z,
				ox + 80, ORIGIN_Y, ORIGIN_Z + 40,
				ox + 80, ORIGIN_Y + 8.0D, ORIGIN_Z + 80, 107);
		placeCenterline(world, g, Blocks.coal_block, 2);
	}

	private static void placeLocalFrameDemo(World world) {
		double ox = ORIGIN_X + 420.0D;
		StraightGeometry g = new StraightGeometry(ox, ORIGIN_Y, ORIGIN_Z,
				ox, ORIGIN_Y, ORIGIN_Z + 40.0D, 108);
		placeCenterline(world, g, Blocks.gold_block, 1);
		for (int i = 0; i <= 8; i++) {
			double d = g.lengthM() * i / 8.0D;
			RailLocalFrame f = g.frameAt(d);
			safeSet(world, MathHelper.floor_double(f.x + f.rx * 2.0D),
					MathHelper.floor_double(f.y),
					MathHelper.floor_double(f.z + f.rz * 2.0D), Blocks.wool);
			safeSet(world, MathHelper.floor_double(f.x + f.fx * 2.0D),
					MathHelper.floor_double(f.y + 1),
					MathHelper.floor_double(f.z + f.fz * 2.0D), Blocks.glowstone);
		}
	}

	private static void placeCenterline(World world, RailGeometry g, Block marker, int stepM) {
		int n = (int) Math.ceil(g.lengthM());
		for (int i = 0; i <= n; i++) {
			RailSample s = g.sampleByDistance(i);
			int x = MathHelper.floor_double(s.x);
			int y = MathHelper.floor_double(s.y);
			int z = MathHelper.floor_double(s.z);
			safeSet(world, x, y - 1, z, Blocks.stone);
			// NOTE: Blocks.rail is NOT used here. Vanilla rail's onBlockAdded calls
			// isBlockPowered -> adjacent chunk reads, which races Alfheim chunk
			// relight on the freshly-generated Flat World and can crash the
			// integrated server. Use a non-redstone-reactive block instead.
			safeSet(world, x, y, z, Blocks.iron_block);
			if (i % stepM == 0) {
				safeSet(world, x, y + 1, z, marker);
			}
			// short tangent tick
			if (i % (stepM * 5) == 0) {
				int tx = MathHelper.floor_double(s.x + s.tx * 2.0D);
				int tz = MathHelper.floor_double(s.z + s.tz * 2.0D);
				safeSet(world, tx, y + 1, tz, Blocks.torch);
			}
		}
	}

	private static void safeSet(World world, int x, int y, int z, Block block) {
		try {
			int cx = x >> 4;
			int cz = z >> 4;
			if (world instanceof WorldServer) {
				((WorldServer) world).theChunkProviderServer.loadChunk(cx, cz);
			} else {
				world.getChunkFromChunkCoords(cx, cz);
			}
			world.setBlockState(new BlockPos(x, y, z), block.getDefaultState(), 2);
		} catch (RuntimeException ex) {
			// Validation-only marker placement can race chunk light generation on the
			// Flat Validation World (alfheim boundary relight). Never crash the game;
			// skip the marker and continue. Production rendering is unaffected.
		}
	}
}
