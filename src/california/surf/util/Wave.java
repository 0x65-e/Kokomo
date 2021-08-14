package california.surf.util;

import java.awt.geom.Point2D;

import robocode.util.Utils;

public class Wave {
	public Point2D.Double fireLocation;
	public long fireTime;
	public double startBearing, bulletVelocity, distanceTraveled;
	public int direction;
	double[] buffer;
	
	double MAX_ESCAPE_ANGLE;
	int BINS, MIDDLE_BIN;
	
	public Wave(Point2D.Double gunLocation, double bearing, double power, 
			int direction, long time, double[] segment) {
		fireLocation = gunLocation;
		startBearing = bearing;
		bulletVelocity = GameUtils.bulletVelocity(power);
		distanceTraveled = 0;
		this.direction = direction;
		this.fireTime = time;
		buffer = segment;
		
		MAX_ESCAPE_ANGLE = GameUtils.maxEscapeAngle(bulletVelocity);
		BINS = buffer.length;
		MIDDLE_BIN = (BINS - 1) / 2;
	}
	
	public int getCurrentBin(Point2D.Double targetLocation) {
		double bin = getGuessFactor(targetLocation) * MIDDLE_BIN + MIDDLE_BIN;
		// Normalize the bin to the buffer's bounds
		// Should we round instead of casting?
		return (int)GameUtils.limit(0, bin, BINS - 1);
	}
	
	public boolean checkHit(Point2D.Double targetLocation) {
		// Maybe the minus is a mistake, and it should be plus?
		return /*if*/ (fireLocation.distance(targetLocation) - 18 < distanceTraveled); /* {
			buffer[getCurrentBin(targetLocation)]++; // Maybe move this to the phaser level
			return true;
		}
		return false; */
	}
	
	public void incrementBins(Point2D.Double targetLocation) {
		int index = getCurrentBin(targetLocation);
		
		for (int x = 0; x < BINS; x++) {
            // for the spot bin that we were hit on, add 1;
            // for the bins next to it, add 1 / 2;
            // the next one, add 1 / 5; and so on...
            buffer[x] += 1.0 / (Math.pow(index - x, 2) + 1.0);
        }
	}
	
	public void incrementSingleBin(Point2D.Double targetLocation) {
		buffer[getCurrentBin(targetLocation)]++;
	}
	
	public double[] getBuffer() {
		return buffer;
	}
	
	public double getGuessFactor(Point2D.Double targetLocation) {
		double angleOffset = Utils.normalRelativeAngle((GameUtils.absoluteBearing(fireLocation, targetLocation) - startBearing));
		double guessfactor = (angleOffset /  MAX_ESCAPE_ANGLE) * direction;
		return guessfactor;
	}
}
