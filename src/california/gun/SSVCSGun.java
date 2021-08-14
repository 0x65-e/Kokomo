package california.gun;

import java.awt.geom.*;
import java.util.Vector;

import california.RobotBase;
import california.util.GameUtils;
import california.util.Wave;
import robocode.*;
import robocode.util.Utils;

public class SSVCSGun {
	
	Vector<Wave> waves = new Vector<>();
	private RobotBase _robot;
	private static final double BULLET_POWER = 1.9;
	
	// May want to segment on lateral acceleration as well as distance from center
	private final static int BINS = 31;
	private final static int MIDDLE_BIN = (BINS - 1) / 2;
	private final static int DISTANCE_SEGMENTS = 5;
	private final static int WALL_SEGMENTS = 3;
	private final static int VELOCITY_SEGMENTS = 5;
	private final static int ACCELERATION_SEGMENTS = 3;
	
	// Segmented on distance, velocity, last velocity, lateral velocity, and lateral acceleration
	//static double[][][][][][] stats = new double[DISTANCE_SEGMENTS][VELOCITY_SEGMENTS][VELOCITY_SEGMENTS][VELOCITY_SEGMENTS][ACCELERATION_SEGMENTS][BINS];
	//static double[][][][][][][] stats = new double[DISTANCE_SEGMENTS][DISTANCE_DISCRIMINATOR][VELOCITY_SEGMENTS][VELOCITY_SEGMENTS][VELOCITY_SEGMENTS][ACCELERATION_SEGMENTS][BINS];
	
	// Segmented on distance from robot, number of nearby walls, lateral velocity, advancing velocity, and acceleration
	static double[][][][][][] stats = new double[DISTANCE_SEGMENTS][WALL_SEGMENTS][VELOCITY_SEGMENTS][VELOCITY_SEGMENTS][ACCELERATION_SEGMENTS][BINS];
	
	// Useful for dynamic distance segments based on the size of the field
	private static double MAX_DISTANCE;
	private static double DISTANCE_DISCRIMINATOR;
	private static double BATTLEFIELD_WIDTH, BATTLEFIELD_HEIGHT;
	
	private static int lateralDirection;
	private static double lastEnemyVelocity, lastEnemyLateralVelocity;
	
	public SSVCSGun(RobotBase _robot) {
		this._robot = _robot;
		lateralDirection = 1;
		lastEnemyVelocity = lastEnemyLateralVelocity = 0;
		
		// Battlefield parameters
		BATTLEFIELD_WIDTH = _robot.getBattleFieldWidth();
		BATTLEFIELD_HEIGHT = _robot.getBattleFieldHeight();
		
		// Distance segment length
		MAX_DISTANCE = Math.max(1300, Math.max(BATTLEFIELD_HEIGHT, BATTLEFIELD_WIDTH));
		DISTANCE_DISCRIMINATOR = MAX_DISTANCE / DISTANCE_SEGMENTS;
		
		//midpoint = new Point2D.Double(_robot.getBattleFieldWidth()/2, _robot.getBattleFieldHeight()/2);
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		double absBearing = _robot.getHeadingRadians() + e.getBearingRadians();
		double enemyVelocity = e.getVelocity();
		double lateralVelocity = enemyVelocity * Math.sin(e.getHeadingRadians() - absBearing);
		double advancingVelocity = enemyVelocity * -1 * Math.cos(e.getHeadingRadians() - absBearing);

		Point2D.Double enemy = GameUtils.project(_robot.getLocation(), absBearing, e.getDistance());
 
		updateWaves(enemy);
 
		//double power = Math.min(3, Math.max(.1, /* some function */));
		double power = BULLET_POWER; // Static bullet power for now
		
		if (enemyVelocity != 0) {
			lateralDirection = GameUtils.sign(lateralVelocity);
		}
		
		// Segmentation calculations
		int distanceIndex = (int)Math.round(e.getDistance() / DISTANCE_DISCRIMINATOR);
		//int velocityIndex = (int)Math.abs(enemyVelocity / 2);
		//int lastVelocityIndex = (int)Math.abs(lastEnemyVelocity / 2);
		int lateralVelocityIndex = (int)Math.round(Math.abs(lateralVelocity / 2));
		int advancingVelocityIndex = (int)Math.round(Math.abs(advancingVelocity / 2));
		int lateralAccelerationIndex = GameUtils.lateralAccelerationSegment(lateralVelocity, lastEnemyLateralVelocity, e.getDistance());
		//int distCenterIndex = (int)Math.min(5, midpoint.distance(enemy)/100);
		int wallIndex = GameUtils.wallSegment(BATTLEFIELD_WIDTH, BATTLEFIELD_HEIGHT, enemy);
		
		// Get the buffer for the current stats
		// double[] buffer = stats[distanceIndex][velocityIndex][lastVelocityIndex][lateralVelocityIndex][lateralAccelerationIndex];
		double[] buffer = stats[distanceIndex][wallIndex][lateralVelocityIndex][advancingVelocityIndex][lateralAccelerationIndex];
				
		Wave wave = new Wave(_robot.getLocation(), absBearing, power,
                        lateralDirection, _robot.getTime(), buffer);
		lastEnemyVelocity = enemyVelocity;
		lastEnemyLateralVelocity = lateralVelocity;
 
		// Calculate the most likely guess factor for the current conditions
		double guessfactor = (double)((mostVisitedBin(buffer) - MIDDLE_BIN) / (double)MIDDLE_BIN); // This is the math in BasicGFSurfer
		// If the enemy is disabled, shoot head-on and FAST
		if (e.getEnergy() == 0.0) {
			guessfactor = 0.0;
			power = 0.1;
		}
		double angleOffset = lateralDirection * guessfactor * GameUtils.maxEscapeAngle(wave.bulletVelocity);
        double gunAdjust = Utils.normalRelativeAngle(absBearing - _robot.getGunHeadingRadians() + angleOffset);
        _robot.setTurnGunRightRadians(gunAdjust); // All the above math should be identical to that in BasicGFSurfer
        
        
        // Only tracking firing waves, because the other robot probably only responds to those
        if (_robot.getGunHeat() == 0 && gunAdjust < Math.atan2(9, e.getDistance()) && _robot.getEnergy() > power && _robot.setFireBullet(power) != null) {
        	waves.add(wave); // This means I'm only tracking firing waves I guess
        }
        
        //_robot.setFire(power);
        //waves.add(wave);
	}
	
	public void updateWaves(Point2D.Double targetLocation) {
		for (int i = 0; i < waves.size(); i++) {
			Wave currentWave = (Wave)waves.get(i);
			currentWave.distanceTraveled = (_robot.getTime() - currentWave.fireTime) * currentWave.bulletVelocity;
			if (currentWave.checkHit(targetLocation)) {
				// Added from BasicGFSurfer
				currentWave.incrementBins(targetLocation); // See the checkhit method for the old way of incrementing bins individually
				//currentWave.incrementSingleBin(targetLocation);
				waves.remove(currentWave);
				i--;
			}
		}
	}
	
	public int mostVisitedBin(double[] buffer) {
		int bestindex = MIDDLE_BIN;
		for (int i = 0; i < BINS; i++) {
			if (buffer[bestindex] < buffer[i]) {
				bestindex = i;
			}
		}
		return bestindex;
	}
}

