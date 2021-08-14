package california.surf.util;

import java.awt.geom.Point2D;
import java.util.Random;

public class GameUtils {

	public static double bulletVelocity(double power) {
		return 20D - (3D * power);
	}
	
	public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length) {
		return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
				sourceLocation.y + Math.cos(angle) * length);
	}
	
	public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
		return Math.atan2(target.x - source.x, target.y - source.y);
	}

	public static int sign(double v) {
		return v < 0 ? -1 : 1;
	}
	
	public static int minMax(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}

	public static double limit(double min, double value, double max) {
		return Math.max(min, Math.min(value, max));
	}

	public static double maxEscapeAngle(double velocity) {
		return Math.asin(8.0/velocity);
	}
	
	// This may not be perfect. I don't know if it should use distance as well?
	// Should be deltaBearing instead of lateralVelocity. Close enough?
	public static int lateralAccelerationSegment(double lateralVelocity, double oldLateralVelocity, double distance) {
		int delta = (int)Math.round(5 * distance * Math.abs((lateralVelocity) - Math.abs(oldLateralVelocity)));
		if (delta < 0) return 0;
		if (delta > 0) return 2;
		return 1;
	}
	
	public static int wallSegment(double width, double height, Point2D.Double targetLocation) {
		double WALL_STICK = 200; // GFTarget bot changes its behavior within 160 pixels of a wall
		int walls = 0;
		if (targetLocation.x < WALL_STICK || targetLocation.x > width - WALL_STICK) walls++;
		if (targetLocation.y < WALL_STICK || targetLocation.y > height - WALL_STICK) walls++;
		return walls;
	}
	
	public static String getSaltString(int len) {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < len) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }
}
