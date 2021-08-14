package california.surf;

import java.awt.Color;
import java.awt.geom.Point2D;

import california.surf.gun.DSVCSGun;
import california.surf.gun.SSVCSGun;
import california.surf.move.Surfer;
import robocode.BulletHitBulletEvent;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class Kokomo extends RobotBase {
	
	private DSVCSGun _gun;
	private Surfer _surfer;
	
	
    @Override
    public void initComponents() {
    	System.out.println("Initializing components...");
    	_gun = new DSVCSGun(this);
    	_surfer = new Surfer(this);
    }
    
    @Override
    public void setColors() {
    	// Surf-themed/woodie colors
    	Color aqua = new Color(65, 101, 150);
    	Color teal = new Color(65, 150, 148);
    	Color chestnut = new Color(152, 91, 39);
    	Color sandy = new Color(210, 151, 73);
    	
    	setColors(aqua, chestnut, sandy, null, teal);
    }
    
    public void onScannedRobot(ScannedRobotEvent e) {	
    	// Fixed-width radar scan lock (wiki)
        double absBearing = e.getBearingRadians() + getHeadingRadians();
        setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians()) * 2);
        
    	_surfer.onScannedRobot(e);
        _gun.onScannedRobot(e);
    }
    
    /* Pass-through events to relevant component handlers */
    
    public void onHitByBullet(HitByBulletEvent e) {
    	_surfer.onHitByBullet(e);
    }
    
    public void onBulletHitBulletEvent(BulletHitBulletEvent e) {
    	_gun.onBulletHitBulletEvent(e);
    	_surfer.onBulletHitBulletEvent(e);
    }

}
