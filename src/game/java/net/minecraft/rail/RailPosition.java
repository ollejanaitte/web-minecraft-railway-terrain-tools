package net.minecraft.rail;

public class RailPosition {
	private final int segmentId;
	private double progress;

	public RailPosition(int segmentId, double progress) {
		this.segmentId = segmentId;
		this.progress = progress;
		this.clampProgress();
	}

	public int getSegmentId() {
		return this.segmentId;
	}

	public double getProgress() {
		return this.progress;
	}

	public void setProgress(double progress) {
		this.progress = progress;
		this.clampProgress();
	}

	public void clampProgress() {
		if (this.progress < 0.0D) {
			this.progress = 0.0D;
		} else if (this.progress > 1.0D) {
			this.progress = 1.0D;
		}
	}
}
