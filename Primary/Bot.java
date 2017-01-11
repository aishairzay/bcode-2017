package Primary;

import java.util.Random;

import battlecode.common.BulletInfo;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public strictfp abstract class Bot {
	protected RobotController rc;
	protected Random rand;
	protected Team myTeam;
	protected Team enemyTeam;
	protected RobotType myType;
	protected Direction[] directions = new Direction[8];
	protected MapLocation[] allyArchons;
	protected MapLocation[] enemyArchons;
	
	public Bot(RobotController rc) {
		this.rc = rc;
		rand = new Random(rc.getID() + (int)(Math.random() * 100));
		myTeam = this.rc.getTeam();
		enemyTeam = myTeam.opponent();
		myType = rc.getType();
		Direction dir = Direction.getWest();
		int rotation = 45;
		for (int i = 0; i < 8; i++,rotation+=45) {
			directions[i] = dir.rotateRightDegrees(rotation);
		}

		allyArchons = rc.getInitialArchonLocations(myTeam);
		enemyArchons = rc.getInitialArchonLocations(enemyTeam);
	}

	public abstract void run() throws GameActionException;
	
	
	public Direction getRandomDirection() {
		return directions[rand.nextInt(directions.length)];
	}
	
	
	protected MapLocation dest = null;
	protected void setDestination(MapLocation loc) {
		this.dest = loc;
	}
	protected void bug() {
		
	}
	protected void moveTowards() {
		
	}
	
	protected void shake() throws GameActionException {
		if (rc.canShake()) {
			TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().strideRadius);
		
			for (TreeInfo tree : trees) {
				if (rc.canShake(tree.ID)) {
					rc.shake(tree.ID);
					rc.broadcast(this.getLocChannel(tree.location), 1);
					break;
				}
			}
		}
	}
	
	// This method attempts to make a move
	// To the best square that will take the least amount of damage from bullets
	// returns true if a move was made to minimize bullet damage
	// returns false if a move was not made
	protected int[] getBulletDamagesByMoveLocs(BulletInfo[] bullets) {
		/*int[] directionScores = new int[9];
		int[] allDirections = new int[8];
		float myHealth = rc.getHealth();
		for (int i = 0; i < directionScores.length; i++) {
			Direction dir = directionFromIndex(i);
			if (!rc.canMove(dir)) {
				continue;
			}
			for (int j = 0; j < bullets.length; j++) {

			}
		}
		return directionScores;*/
		return null;
	}
	
	protected int indexFromDirection(Direction direction) {
		Direction dir = Direction.getWest();
		for (int i = 0; i < 8; i++) {
			if (dir == direction) {
				return i;
			}
			dir = dir.rotateRightDegrees(45);
		}
		return 8;
		
	}
	protected Direction directionFromIndex(int index) {
		if (index >= 0 && index < 8) {
			
		}
		return null;
	}

    protected int getLocChannel(MapLocation loc)
    {
        return (3 * (Math.abs((int)loc.x + 1) * 1000))
            ^ (7 * Math.abs((int)loc.y + 1) * 10);
    }
    
    void makeMove(Direction dir) throws GameActionException {
    	if (rc.hasMoved()) {
    		return;
    	}
    	Direction rotation = dir;
    	int i = 0;
    	int leftAngle = 45;
    	int rightAngle = 45;
    	boolean left = rand.nextBoolean();
    	while(true) {
    		if (rc.canMove(rotation)) {
    			break;
    		}
    		if (i >= 8) {
    			break;
    		}
    		i++;
    		if (left) {
        		rotation = dir.rotateLeftDegrees(leftAngle);
        		leftAngle += 45;
    		} else {
    			rotation = dir.rotateRightDegrees(rightAngle);
    			rightAngle += 45;
    		}
    		left = !left;
    	}
    	if (rc.canMove(rotation)) {
    		rc.move(rotation);
    	}
    }
	
}
