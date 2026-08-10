package net.minecraft.railsys.validation;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.railsys.geometry.HorizontalBezierGeometry;
import net.minecraft.railsys.geometry.RailGeometry;
import net.minecraft.railsys.geometry.RailSample;
import net.minecraft.railsys.geometry.StraightGeometry;
import net.minecraft.railsys.path.PathSample;
import net.minecraft.railsys.path.RailPath;
import net.minecraft.railsys.path.RailPiece;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Phase 1.2 validation-only RailPath debug visualization.
 *
 * <p>Each fixture runs along +Z (north→south) at its own X column so the
 * capture cameras (positioned north, yaw 0) see the whole path recede in view
 * direction — the same framing that worked for Phase 1.1. Markers are built
 * from the production {@code net.minecraft.railsys.path} classes.
 *
 * <p>World-name gated (AutoValidate only); never used as production math.
 */
public final class RailsysPathDebugEvidence {

	public static final double ORIGIN_Y = 64.0D;
	public static final double Z0 = 400.0D;

	private RailsysPathDebugEvidence() {
	}

	public static void placeAll(World world) {
		System.out.println("[RAILSYSTEM] Phase1.2 PathDebugEvidence START");
		placeMultiStraight(world, 0.0D);
		placeStraightCurveStraight(world, 120.0D);
		placeSCurve(world, 360.0D);
		placeGradientChain(world, 460.0D);
		placeCurveGradientChain(world, 560.0D);
		placeReversePath(world, 700.0D);
		preloadCameraChunks(world);
		System.out.println("[RAILSYSTEM] Phase1.2 PathDebugEvidence DONE");
	}

	/** 80 + 65 + 100 = 245 m along +Z at column x, with boundary + piece markers. */
	private static void placeMultiStraight(World world, double x) {
		RailPath path = RailPath.of(
				new RailPiece(new StraightGeometry(x, ORIGIN_Y, Z0, x, ORIGIN_Y, Z0 + 80.0D, 201)),
				new RailPiece(new StraightGeometry(x, ORIGIN_Y, Z0 + 80.0D, x, ORIGIN_Y, Z0 + 145.0D, 202)),
				new RailPiece(new StraightGeometry(x, ORIGIN_Y, Z0 + 145.0D, x, ORIGIN_Y, Z0 + 245.0D, 203)));
		placeCenterline(world, path, new Block[] { Blocks.gold_block, Blocks.diamond_block, Blocks.emerald_block },
				2, true);
		markBoundary(world, x, Z0 + 80.0D);
		markBoundary(world, x, Z0 + 145.0D);
	}

	/** Straight 80 -> 90deg curve (~152m) -> straight 80 (RailV2Course style, +Z entry). */
	private static void placeStraightCurveStraight(World world, double x) {
		RailPath path = RailPath.of(
				new RailPiece(new StraightGeometry(x, ORIGIN_Y, Z0, x, ORIGIN_Y, Z0 + 80.0D, 204)),
				new RailPiece(new HorizontalBezierGeometry(x, ORIGIN_Y, Z0 + 80.0D,
						x, ORIGIN_Y, Z0 + 140.0D, x + 60.0D, ORIGIN_Y, Z0 + 200.0D,
						x + 80.0D, ORIGIN_Y, Z0 + 200.0D, 205)),
				new RailPiece(new StraightGeometry(x + 80.0D, ORIGIN_Y, Z0 + 200.0D,
						x + 160.0D, ORIGIN_Y, Z0 + 200.0D, 206)));
		placeCenterline(world, path, new Block[] { Blocks.gold_block, Blocks.redstone_block, Blocks.lapis_block },
				2, true);
		markBoundary(world, x, Z0 + 80.0D);
		markBoundary(world, x + 80.0D, Z0 + 200.0D);
	}

	/** Two tight curves forming an S. */
	private static void placeSCurve(World world, double x) {
		RailPath path = RailPath.of(
				new RailPiece(new HorizontalBezierGeometry(x, ORIGIN_Y, Z0,
						x + 5.0D, ORIGIN_Y, Z0, x + 10.0D, ORIGIN_Y, Z0 + 5.0D,
						x + 10.0D, ORIGIN_Y, Z0 + 10.0D, 207)),
				new RailPiece(new HorizontalBezierGeometry(x + 10.0D, ORIGIN_Y, Z0 + 10.0D,
						x + 10.0D, ORIGIN_Y, Z0 + 15.0D, x + 5.0D, ORIGIN_Y, Z0 + 20.0D,
						x, ORIGIN_Y, Z0 + 20.0D, 208)));
		placeCenterline(world, path, new Block[] { Blocks.redstone_block, Blocks.lapis_block }, 1, true);
		markBoundary(world, x + 10.0D, Z0 + 10.0D);
	}

	/** Two 8% grade straights in a chain (pitch continuity). */
	private static void placeGradientChain(World world, double x) {
		RailPath path = RailPath.of(
				new RailPiece(new StraightGeometry(x, ORIGIN_Y, Z0, x, ORIGIN_Y + 8.0D, Z0 + 80.0D, 209)),
				new RailPiece(new StraightGeometry(x, ORIGIN_Y + 8.0D, Z0 + 80.0D, x, ORIGIN_Y + 16.0D, Z0 + 160.0D, 210)));
		placeCenterline(world, path, new Block[] { Blocks.iron_block, Blocks.coal_block }, 2, true);
		markBoundary(world, x, Z0 + 80.0D);
	}

	/** Curve with gradient + matching continuation straight (pitch transition). */
	private static void placeCurveGradientChain(World world, double x) {
		RailPath path = RailPath.of(
				new RailPiece(new HorizontalBezierGeometry(x, ORIGIN_Y, Z0,
						x + 40.0D, ORIGIN_Y, Z0, x + 80.0D, ORIGIN_Y, Z0 + 40.0D,
						x + 80.0D, ORIGIN_Y + 8.0D, Z0 + 80.0D, 211)),
				new RailPiece(new StraightGeometry(x + 80.0D, ORIGIN_Y + 8.0D, Z0 + 80.0D,
						x + 80.0D, ORIGIN_Y + 16.0D, Z0 + 200.0D, 212)));
		placeCenterline(world, path, new Block[] { Blocks.diamond_block, Blocks.iron_block }, 2, true);
		markBoundary(world, x + 80.0D, Z0 + 80.0D);
	}

	/** Reverse traversal of the 80/65/100 path at column x, travel direction -Z. */
	private static void placeReversePath(World world, double x) {
		RailPath fwd = RailPath.of(
				new RailPiece(new StraightGeometry(x, ORIGIN_Y, Z0, x, ORIGIN_Y, Z0 + 80.0D, 213)),
				new RailPiece(new StraightGeometry(x, ORIGIN_Y, Z0 + 80.0D, x, ORIGIN_Y, Z0 + 145.0D, 214)),
				new RailPiece(new StraightGeometry(x, ORIGIN_Y, Z0 + 145.0D, x, ORIGIN_Y, Z0 + 245.0D, 215)));
		RailPath rev = fwd.reverse();
		for (int i = 0; i <= (int) Math.ceil(rev.totalLength()); i++) {
			PathSample s = rev.resolve(i);
			int xi = MathHelper.floor_double(s.sample.x);
			int y = MathHelper.floor_double(s.sample.y);
			int zi = MathHelper.floor_double(s.sample.z);
			safeSet(world, xi, y - 1, zi, Blocks.stone);
			safeSet(world, xi, y, zi, Blocks.rail);
			if (i % 2 == 0) {
				safeSet(world, xi, y + 1, zi, Blocks.lapis_block);
			}
			if (i % 10 == 0) {
				int tz = MathHelper.floor_double(s.sample.z + s.travelTz * 2.0D);
				safeSet(world, xi, y + 1, tz, Blocks.torch);
			}
		}
		markBoundary(world, x, Z0 + 80.0D);
		markBoundary(world, x, Z0 + 145.0D);
	}

	/** Centerline + per-piece marker + direction ticks from production PathSample. */
	private static void placeCenterline(World world, RailPath path, Block[] pieceMarkers, int stepM, boolean ticks) {
		int n = (int) Math.ceil(path.totalLength());
		for (int i = 0; i <= n; i++) {
			PathSample s = path.resolve(i);
			int x = MathHelper.floor_double(s.sample.x);
			int y = MathHelper.floor_double(s.sample.y);
			int z = MathHelper.floor_double(s.sample.z);
			safeSet(world, x, y - 1, z, Blocks.stone);
			safeSet(world, x, y, z, Blocks.rail);
			if (i % stepM == 0) {
				int idx = MathHelper.clamp_int(s.entryIndex, 0, pieceMarkers.length - 1);
				safeSet(world, x, y + 1, z, pieceMarkers[idx]);
			}
			if (ticks && i % (stepM * 5) == 0) {
				int tx = MathHelper.floor_double(s.sample.x + s.travelTx * 2.0D);
				int tz = MathHelper.floor_double(s.sample.z + s.travelTz * 2.0D);
				safeSet(world, tx, y + 1, tz, Blocks.torch);
			}
		}
	}

	/** Prominent white-wool boundary marker at a piece join. */
	private static void markBoundary(World world, double x, double z) {
		safeSet(world, MathHelper.floor_double(x), (int) ORIGIN_Y + 2, MathHelper.floor_double(z), Blocks.wool);
	}

	/**
	 * Camera positions used by the Phase 1.2 tour must be in LOADED chunks or
	 * the integrated single-player server can drop the local player channel
	 * when the camera teleports there. Preload a margin around every camera.
	 */
	private static void preloadCameraChunks(World world) {
		double[][] cams = new double[][] {
				{ 0.0D, 385.0D }, { 120.0D, 385.0D }, { 365.0D, 385.0D }, { 0.0D, 475.0D },
				{ 460.0D, 385.0D }, { 600.0D, 385.0D }, { 700.0D, 385.0D }, { 350.0D, 350.0D },
		};
		for (int i = 0; i < cams.length; i++) {
			int cx = (int) Math.floor(cams[i][0]) >> 4;
			int cz = (int) Math.floor(cams[i][1]) >> 4;
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					if (world instanceof WorldServer) {
						((WorldServer) world).theChunkProviderServer.loadChunk(cx + dx, cz + dz);
					} else {
						world.getChunkFromChunkCoords(cx + dx, cz + dz);
					}
				}
			}
		}
	}

	private static void safeSet(World world, int x, int y, int z, Block block) {
		int cx = x >> 4;
		int cz = z >> 4;
		if (world instanceof WorldServer) {
			((WorldServer) world).theChunkProviderServer.loadChunk(cx, cz);
		} else {
			world.getChunkFromChunkCoords(cx, cz);
		}
		world.setBlockState(new BlockPos(x, y, z), block.getDefaultState(), 2);
	}
}
