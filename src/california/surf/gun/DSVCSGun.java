package california.surf.gun;

import java.awt.geom.Point2D;
import java.util.Vector;
import java.util.function.Supplier;

import california.surf.RobotBase;
import california.surf.util.GameUtils;
import california.surf.util.RegressionTree;
import california.surf.util.Wave;
import robocode.BulletHitBulletEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class DSVCSGun {
	
	Vector<Wave> waves = new Vector<>();
	private RobotBase _robot;
	private static final double BULLET_POWER = 1.9;
	
	private final static int BINS = 31;
	private final static int MIDDLE_BIN = (BINS - 1) / 2;
	private final static int DISTANCE_SEGMENTS = 5;
	private final static int WALL_SEGMENTS = 3;
	private final static int VELOCITY_SEGMENTS = 5;
	private final static int ACCELERATION_SEGMENTS = 3;
	
	// Segmented on distance from robot, number of nearby walls, lateral velocity, advancing velocity, and acceleration
	//static double[][][][][][] stats = new double[DISTANCE_SEGMENTS][WALL_SEGMENTS][VELOCITY_SEGMENTS][VELOCITY_SEGMENTS][ACCELERATION_SEGMENTS][BINS];
	double[] stats;
	double[] factors;
	private final static RegressionTree<GuessFactor> tree = new RegressionTree<>(5);
	
	// Useful for dynamic distance segments based on the size of the field
	private static double MAX_DISTANCE;
	private static double DISTANCE_DISCRIMINATOR;
	private static double BATTLEFIELD_WIDTH, BATTLEFIELD_HEIGHT;
	
	private static int lateralDirection;
	private static double lastEnemyVelocity, lastEnemyLateralVelocity;
	
	private static class GuessFactor implements Supplier<Double> {
		private double gf;
		
		public GuessFactor(double gf) {
			this.gf = gf;
		}
		
		public Double get() {
			return gf;
		}
	}
	
	public DSVCSGun(RobotBase _robot) {
		this._robot = _robot;
		lateralDirection = 1;
		lastEnemyVelocity = lastEnemyLateralVelocity = 0;
		
		// Battlefield parameters
		BATTLEFIELD_WIDTH = _robot.getBattleFieldWidth();
		BATTLEFIELD_HEIGHT = _robot.getBattleFieldHeight();
		
		// Distance segment length
		MAX_DISTANCE = Math.max(1300, Math.max(BATTLEFIELD_HEIGHT, BATTLEFIELD_WIDTH));
		DISTANCE_DISCRIMINATOR = MAX_DISTANCE / DISTANCE_SEGMENTS;
		
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		double absBearing = _robot.getHeadingRadians() + e.getBearingRadians();
		double enemyVelocity = e.getVelocity();
		double lateralVelocity = enemyVelocity * Math.sin(e.getHeadingRadians() - absBearing);
		double advancingVelocity = enemyVelocity * -1 * Math.cos(e.getHeadingRadians() - absBearing);

		Point2D.Double enemy = GameUtils.project(_robot.getLocation(), absBearing, e.getDistance());
 
		updateWaves(enemy);
 
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
		//double[] buffer = stats[distanceIndex][wallIndex][lateralVelocityIndex][advancingVelocityIndex][lateralAccelerationIndex];
				
		
		lastEnemyVelocity = enemyVelocity;
		lastEnemyLateralVelocity = lateralVelocity;
		
		factors = new double[] {e.getDistance(), wallIndex, lateralVelocity, advancingVelocity, lateralVelocity - lastEnemyLateralVelocity};
 
		// Calculate the most likely guess factor for the current conditions
		double[] gfArray = tree.getClassification(factors);
		//System.out.println("Array size: " + gfArray.length);
		double guessfactor = (double)((mostVisitedBin(gfArray) - MIDDLE_BIN) / (double)MIDDLE_BIN);
		
		// If the enemy is disabled, shoot head-on and FAST
		if (e.getEnergy() == 0.0) {
			guessfactor = 0.0;
			power = 0.1;
		}
		//System.out.println("Guess Factor: " + guessfactor);
		//System.out.println("Lat dir: " + lateralDirection);
		//System.out.println("Escape Angle: " + GameUtils.maxEscapeAngle(GameUtils.bulletVelocity(power)));
		double angleOffset = lateralDirection * guessfactor * GameUtils.maxEscapeAngle(GameUtils.bulletVelocity(power));
		//System.out.println("Angle offset: " + angleOffset);
        double gunAdjust = Utils.normalRelativeAngle(absBearing - _robot.getGunHeadingRadians() + angleOffset);
        _robot.setTurnGunRightRadians(gunAdjust); // All the above math should be identical to that in BasicGFSurfer
        
        
        // Only tracking firing waves, because the other robot probably only responds to those
        if (_robot.getGunHeat() == 0 && gunAdjust < Math.atan2(9, e.getDistance()) && _robot.getEnergy() > power && _robot.setFireBullet(power) != null) {
        	Wave wave = new Wave(_robot.getLocation(), absBearing, power, lateralDirection, _robot.getTime(), factors);
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
				// Add to the tree
				System.out.println("Wave landed. About to add to tree...");
				tree.add(currentWave.getBuffer(), new GuessFactor(currentWave.getGuessFactor(targetLocation)));
				// Remove from tracked waves
				waves.remove(currentWave);
				i--;
			}
		}
	}
	
	public int mostVisitedBin(double[] buffer) {
		stats = new double[BINS];
		for (int i = 0; i < buffer.length; i++) {
			double bin = buffer[i] * MIDDLE_BIN + MIDDLE_BIN;
			stats[(int)GameUtils.limit(0, bin, BINS - 1)]++;
		}
		int bestindex = MIDDLE_BIN;
		for (int i = 0; i < BINS; i++) {
			if (stats[bestindex] < stats[i]) {
				bestindex = i;
			}
		}
		return bestindex;
	}
	
	public void onBulletHitBulletEvent(BulletHitBulletEvent e) {
        if (!waves.isEmpty()) {
            Point2D.Double hitBulletLocation = new Point2D.Double(e.getHitBullet().getX(), e.getHitBullet().getY());
            Wave hitWave = null;
 
            // look through the tracked waves, and find one that could've intersected
            for (int x = 0; x < waves.size(); x++) {
                Wave ew = waves.get(x);
 
                if (Math.abs(ew.distanceTraveled - hitBulletLocation.distance(ew.fireLocation)) < 50
                    && Math.abs(GameUtils.bulletVelocity(e.getBullet().getPower()) - ew.bulletVelocity) < 0.001) {
                    hitWave = ew;
                    break;
                }
            }
 
            if (hitWave != null) {
            	// Don't track stats for this wave, to stop bullet shielders from screwing up our stats
                waves.remove(waves.lastIndexOf(hitWave));
            }
        }
    }

}
