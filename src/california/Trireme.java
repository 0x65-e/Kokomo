package california;

import robocode.*;
import robocode.util.Utils;

public class Trireme extends AdvancedRobot {

	public void run() {
		
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true); //?

		// basic mini-radar code
		turnRadarRightRadians(Double.POSITIVE_INFINITY);
		//setTurnRadarRightRadians(Double.POSITIVE_INFINITY); // Do I need to fix if I drop a radar lock?
		// need to fix radar lock; it doesn't work
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		int integer = 30;
		double absBearing = e.getBearingRadians() + getHeadingRadians();
		int matchPos;

		// fixed-width radar lock
		//setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians()) * 2);
		setTurnRadarLeftRadians(getRadarTurnRemaining());
		
		// string based pattern matching based on lateral velocity
		enemyHistory = String.valueOf((char) (e.getVelocity() * (Math.sin(e.getHeadingRadians() - absBearing)))).concat(enemyHistory);
		
		// matching algorithm
		while((matchPos = enemyHistory.indexOf(enemyHistory.substring(0, integer--), 64)) < 0);
		
		setFire(2.3 + (127/(integer = (int)e.getDistance()))); // The closer the enemy is, the better the damage is
		
		// Play it forward
		do { 
	         absBearing += ((short) enemyHistory.charAt(--matchPos)) /  e.getDistance();
	    } while ((integer -= 13.1) > 0);
		
		// Turn gun to predicted position
		setTurnGunRightRadians(Utils.normalRelativeAngle(absBearing - getGunHeadingRadians()));
		
		// Turn towards and ram enemy
		setTurnRight(e.getBearing());

		// Ramming speed!
		setAhead(Double.POSITIVE_INFINITY);
	}

	
	public void onHitRobot(HitRobotEvent e) {
		//System.out.println("R A M D E T E C T E D");
		setTurnRight(e.getBearing());
		// Need to turn gun to target, because it might be off due to pattern matching
		setTurnGunRightRadians(Utils.normalRelativeAngle(e.getBearingRadians() + getHeadingRadians() - getGunHeadingRadians()));
		setFire(3);
		setAhead(Double.POSITIVE_INFINITY);
		//ahead(Double.POSITIVE_INFINITY);
	}
	
	// preloaded pattern matching table
	static String enemyHistory = ""
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 + (char) 0 
			   + (char)-1 + (char)-2 + (char)-3 + (char)-4 + (char)-5 + (char)-6 
			   + (char)-7 + (char)-8 + (char) 8 + (char) 7 + (char) 6 + (char) 5 
			   + (char) 4 + (char) 3 + (char) 2 + (char) 1 + (char) 0 + (char) 0
			   ;
}
