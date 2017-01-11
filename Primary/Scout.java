package Primary;


import battlecode.common.BulletInfo;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public strictfp class Scout extends Bot{

	Direction dir;
	
	public Scout(RobotController rc) {
		super(rc);
		dir = this.getRandomDirection();
	}

	@Override
	public void run() throws GameActionException {
		micro();
		//shake();
	}


	private void micro() throws GameActionException {
		BulletInfo[] bullets = rc.senseNearbyBullets();
		
		getBulletDamagesByMoveLocs(bullets);
		
		if (!rc.hasMoved()) {
			moveTowardsUnshookTrees();
		}
		if (!rc.hasMoved()) {
			moveTowardsEnemy();
		}
		if (!rc.hasMoved()) {
			moveInUnexploredDirection(0);
		}
		
		/*if (!rc.hasAttacked()) {
			RobotInfo[] enemyRobots = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);
			attackEnemies(enemyRobots);
		}
		if (!rc.hasAttacked()) {
			TreeInfo[] trees = rc.senseNearbyTrees();
			attackPlants();
		}*/
	}
	
	private void moveTowardsEnemy() {
		
		
	}

	protected void shake() throws GameActionException {
		super.shake();
	}
	
	private void moveTowardsUnshookTrees() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees(myType.sensorRadius, Team.NEUTRAL);
		TreeInfo closest = null;
		for (TreeInfo tree : trees) {
			boolean visited = false;//rc.readBroadcast(this.getLocChannel(tree.location)) > 0;
			if (visited) {
				continue;
			}
			if (closest == null) {
				closest = tree;
			} else if (rc.getLocation().distanceSquaredTo(tree.location) < rc.getLocation().distanceSquaredTo(closest.location)) {
				closest = tree;
			}
		}
		if (closest == null) {
			return;
		}
		Direction dir = rc.getLocation().directionTo(closest.location);
		makeMove(dir);
		rc.setIndicatorLine(rc.getLocation(), closest.location, 200, 0, 0);
	}

	private void moveInUnexploredDirection(int tries) throws GameActionException {
		if (tries == 8) {
			return;
		}
		// Store a list of all directions we have explored
		// Mark direction as explored once we hit a wall.
		if (rc.onTheMap(rc.getLocation().add(dir), (float) 2.5)) {
			rc.setIndicatorLine(rc.getLocation(), allyArchons[0], 0, 200, 0);
			makeMove(dir);
		} else {
			rc.setIndicatorLine(rc.getLocation(), allyArchons[0], 0, 0, 200);
			dir = this.dir.rotateRightDegrees(45);
			moveInUnexploredDirection(tries+1);
		}
	}
	

	
	// Attacks the most optimal to attack nearby enemy.
	// Need to account for trees being in the way of a bullet being shot.
	// And not take that shot
	private void attackEnemies(RobotInfo[] enemyRobots) throws GameActionException {
		if (enemyRobots.length == 0) {
			return;
		}
		RobotInfo bestToAttack = enemyRobots[0];
		for (RobotInfo robot:enemyRobots) {
			float distance = rc.getLocation().distanceSquaredTo(robot.location);
			double enemyHealth = bestToAttack.health;
			if (distance < rc.getLocation().distanceSquaredTo(bestToAttack.location)) {
				bestToAttack = robot;
			}
		}
		if (rc.canFireSingleShot()) {
			rc.fireSingleShot(rc.getLocation().directionTo(bestToAttack.location));
		}
	}

	private void attackPlants() {

	}

}
