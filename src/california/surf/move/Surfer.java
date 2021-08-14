package california.surf.move;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import california.surf.RobotBase;
import california.surf.util.GameUtils;
import california.surf.util.Wave;
import robocode.AdvancedRobot;
import robocode.BulletHitBulletEvent;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class Surfer {
	
	private RobotBase _robot;

	// Static surfing stats
	private static int BINS = 47; // Bins for wave surfing
    private static double _surfStats[] = new double[BINS];
    
    // Location tracking
    private Point2D.Double _myLocation;
    private Point2D.Double _enemyLocation;  // this should eventually be encapsulated in a separate class
 
    // Wave tracking
    private ArrayList<Wave> _enemyWaves; 
    private ArrayList<Integer> _surfDirections;
    private ArrayList<Double> _surfAbsBearings;
 
    private double _oppEnergy = 100.0; // should eventually be encapsulated into separate enemy class
 
    public static Rectangle2D.Double _fieldRect;
        
    public static double WALL_STICK = 160;
	
	public Surfer(RobotBase _robot) {
		this._robot = _robot;
		
		_enemyWaves = new ArrayList<>();
        _surfDirections = new ArrayList<>();
        _surfAbsBearings = new ArrayList<>();
        
        _fieldRect = new Rectangle2D.Double(18, 18, _robot.getBattleFieldWidth() - 36, _robot.getBattleFieldHeight() - 36);
        
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
        _myLocation = _robot.getLocation();
 
        double lateralVelocity = _robot.getVelocity() * Math.sin(e.getBearingRadians());
        double absBearing = e.getBearingRadians() + _robot.getHeadingRadians();
        
        // Track our own stats to accurately calculate guess factors
        _surfDirections.add(0, new Integer(GameUtils.sign(lateralVelocity)));
        _surfAbsBearings.add(0, new Double(absBearing + Math.PI));
 
        // Check if the enemy has fired
        double bulletPower = _oppEnergy - e.getEnergy();
        if (bulletPower < 3.01 && bulletPower > 0.09 && _surfDirections.size() > 2) {
            // Don't update the enemy position yet, because the wave needs the previous enemy position.
        	Wave wave = new Wave((Point2D.Double)_enemyLocation.clone(), (_surfAbsBearings.get(2)).doubleValue(),
        			bulletPower, (_surfDirections.get(2)).intValue(), _robot.getTime() - 1, _surfStats);

            _enemyWaves.add(wave);
        }
 
        // Is there a better way to update these?
        _oppEnergy = e.getEnergy(); // Separate class
        _enemyLocation = GameUtils.project(_myLocation, absBearing, e.getDistance()); // Encapsulate this in an enemy class at some point
    
        updateWaves();
        doSurfing();
	}
	
	public void updateWaves() {
        for (int x = 0; x < _enemyWaves.size(); x++) {
            Wave ew = (Wave)_enemyWaves.get(x);
 
            ew.distanceTraveled = (_robot.getTime() - ew.fireTime) * ew.bulletVelocity;
            if (ew.distanceTraveled > _myLocation.distance(ew.fireLocation) + 18) {
            	// New change: add danger at the place we are when a wave passes by, since an enemy likely does this too
            	// The following line doesn't do very much against Kirk, but it really drops score against BasicGFSurfer 1.02
            	// by a couple % to nearly 10%
            	//logHit(ew, _robot.getLocation());
            	// Remove the wave from the list
                _enemyWaves.remove(x);
                x--;
            }
        }
    }
    
	/**
	 * Get the closest wave to the robot in order to surf it.
	 * @return The wave with the closest absolute distance, or null if there are no waves
	 */
    public Wave getClosestSurfableWave() {
        double closestDistance = 50000;
        Wave surfWave = null;
 
        for (int x = 0; x < _enemyWaves.size(); x++) {
            Wave ew = _enemyWaves.get(x);
            double distance = _myLocation.distance(ew.fireLocation) - ew.distanceTraveled;
 
            if (distance > ew.bulletVelocity && distance < closestDistance) {
                surfWave = ew;
                closestDistance = distance;
            }
        }
 
        return surfWave;
    }
    
    // Update the surfing stats array on a hit
    public void logHit(Wave ew, Point2D.Double targetLocation) {
    	/*
        int index = ew.getCurrentBin(targetLocation);
 
        for (int x = 0; x < BINS; x++) {
            // for the spot bin that we were hit on, add 1;
            // for the bins next to it, add 1 / 2;
            // the next one, add 1 / 5; and so on...
            _surfStats[x] += 1.0 / (Math.pow(index - x, 2) + 1.0);
        }
        */
    	ew.incrementBins(targetLocation); // Why is this 1-2% worse than the above code?
    }
    
    public void onHitByBullet(HitByBulletEvent e) {
        // If the _enemyWaves collection is empty, we must have missed the
        // detection of this wave somehow.
        if (!_enemyWaves.isEmpty()) {
            Point2D.Double hitBulletLocation = new Point2D.Double(
                e.getBullet().getX(), e.getBullet().getY());
            Wave hitWave = null;
 
            // look through the tracked waves, and find one that could've hit us.
            for (int x = 0; x < _enemyWaves.size(); x++) {
                Wave ew = _enemyWaves.get(x);
 
                if (Math.abs(ew.distanceTraveled - _myLocation.distance(ew.fireLocation)) < 50
                    && Math.abs(GameUtils.bulletVelocity(e.getBullet().getPower()) - ew.bulletVelocity) < 0.001) {
                    hitWave = ew;
                    break;
                }
            }
 
            if (hitWave != null) {
                logHit(hitWave, hitBulletLocation);
 
                // We can remove this wave now, of course.
                _enemyWaves.remove(_enemyWaves.lastIndexOf(hitWave));
            }
        }
        
        // TODO: Update the enemy's energy to reflect what they just got back from hitting me
    }
    
    public void onBulletHitBulletEvent(BulletHitBulletEvent e) {
    	// If the _enemyWaves collection is empty, we must have missed the
        // detection of this wave somehow.
        if (!_enemyWaves.isEmpty()) {
            Point2D.Double hitBulletLocation = new Point2D.Double(e.getHitBullet().getX(), e.getHitBullet().getY());
            Wave hitWave = null;
 
            // look through the tracked waves, and find one that could've intersected
            for (int x = 0; x < _enemyWaves.size(); x++) {
                Wave ew = _enemyWaves.get(x);
 
                if (Math.abs(ew.distanceTraveled - hitBulletLocation.distance(ew.fireLocation)) < 50
                    && Math.abs(GameUtils.bulletVelocity(e.getBullet().getPower()) - ew.bulletVelocity) < 0.001) {
                    hitWave = ew;
                    break;
                }
            }
 
            if (hitWave != null) { 
            	// Stop surfing this wave
                _enemyWaves.remove(_enemyWaves.lastIndexOf(hitWave));
            }
        }
    }
    
    /**
     * Predict the robot's position
     * @param surfWave
     * @param direction
     * @return
     */
    public Point2D.Double predictPosition(Wave surfWave, int direction) {
        Point2D.Double predictedPosition = (Point2D.Double)_myLocation.clone();
        double predictedVelocity = _robot.getVelocity();
        double predictedHeading = _robot.getHeadingRadians();
        double maxTurning, moveAngle, moveDir;
 
        int counter = 0; // number of ticks in the future
        boolean intercepted = false;
 
        do {    // the rest of these code comments are rozu's
            moveAngle =
                wallSmoothing(predictedPosition, GameUtils.absoluteBearing(surfWave.fireLocation,
                predictedPosition) + (direction * (Math.PI/2)), direction)
                - predictedHeading;
            moveDir = 1;
 
            if(Math.cos(moveAngle) < 0) {
                moveAngle += Math.PI;
                moveDir = -1;
            }
 
            moveAngle = Utils.normalRelativeAngle(moveAngle);
 
            // maxTurning is built in like this, you can't turn more then this in one tick
            maxTurning = Math.PI/720d*(40d - 3d*Math.abs(predictedVelocity));
            predictedHeading = Utils.normalRelativeAngle(predictedHeading
                + GameUtils.limit(-maxTurning, moveAngle, maxTurning));
 
            // this one is nice ;). if predictedVelocity and moveDir have
            // different signs you want to brake down
            // otherwise you want to accelerate (look at the factor "2")
            predictedVelocity += (predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
            predictedVelocity = GameUtils.limit(-8, predictedVelocity, 8);
 
            // calculate the new predicted position
            predictedPosition = GameUtils.project(predictedPosition, predictedHeading,
                predictedVelocity);
 
            counter++;
 
            if (predictedPosition.distance(surfWave.fireLocation) <
                surfWave.distanceTraveled + (counter * surfWave.bulletVelocity)
                + surfWave.bulletVelocity) {
                intercepted = true;
            }
        } while(!intercepted && counter < 500);
 
        return predictedPosition;
    }
    
    public double checkDanger(Wave surfWave, int direction) {
        int index = surfWave.getCurrentBin(predictPosition(surfWave, direction));
 
        return _surfStats[index];
    }
 
    public void doSurfing() {
        Wave surfWave = getClosestSurfableWave();
 
        if (surfWave == null) { return; }
 
        double dangerLeft = checkDanger(surfWave, -1);
        double dangerRight = checkDanger(surfWave, 1);
 
        double goAngle = GameUtils.absoluteBearing(surfWave.fireLocation, _myLocation);
        if (dangerLeft < dangerRight) {
            goAngle = wallSmoothing(_myLocation, goAngle - (Math.PI/2), -1);
        } else {
            goAngle = wallSmoothing(_myLocation, goAngle + (Math.PI/2), 1);
        }
 
        setBackAsFront(_robot, goAngle);
    }
 
    public double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) {
        while (!_fieldRect.contains(GameUtils.project(botLocation, angle, WALL_STICK))) {
            angle += orientation*0.05;
        }
        return angle;
    }
    
	public static void setBackAsFront(AdvancedRobot robot, double goAngle) {
		double angle = Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
		if (Math.abs(angle) > (Math.PI/2)) {
			if (angle < 0) {
				robot.setTurnRightRadians(Math.PI + angle);
			} else {
				robot.setTurnLeftRadians(Math.PI - angle);
			}
			robot.setBack(100);
		} else {
			if (angle < 0) {
				robot.setTurnLeftRadians(-1*angle);
			} else {
				robot.setTurnRightRadians(angle);
			}
			robot.setAhead(100);
		}
	}
}


