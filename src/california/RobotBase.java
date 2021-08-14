package california;

import java.awt.geom.Point2D;

import robocode.AdvancedRobot;
import robocode.SkippedTurnEvent;

public abstract class RobotBase extends AdvancedRobot {
	
	/**
	 * Sets the colors of the base, gun, radar, scan, and bullets.
	 * Called once at the beginning of run()
	 */
	public abstract void setColors();
	
	/**
	 * Initializes the components (gun, movement, etc.)
	 * Called once at the beginning of run()
	 */
	public abstract void initComponents();
	
	public void run() {
    	setColors();
    	initComponents();
 
    	// Move all components separately
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
 
        do {
            turnRadarRightRadians(Double.POSITIVE_INFINITY);
        } while (true);
    }
	
	/**
	 * Get this robot's current location
	 * @return Point2D.Double current location
	 */
	public Point2D.Double getLocation() {
    	return new Point2D.Double(getX(), getY());
    }
	
	@Override
	public void onSkippedTurn(SkippedTurnEvent e) {
		System.out.println("Skipped turn " + e.getSkippedTurn());
	}

}
