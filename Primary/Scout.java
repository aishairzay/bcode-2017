package Primary;

import battlecode.common.BulletInfo;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public strictfp class Scout extends Bot {

	
	boolean[] visitedArchonIndexes;
	MapLocation enemyLocation;

	public Scout(RobotController rc) {
		super(rc);
		visitedArchonIndexes = new boolean[allyArchons.length];
	}

	@Override
	public void run() throws GameActionException {
		micro();
		shake();
	}

	// ----------------------------------------------------------------------
	// MICRO
	private void micro() throws GameActionException {
		BulletInfo[] bullets = rc.senseNearbyBullets();

		getBulletDamagesByMoveLocs(bullets);

		broadcastEnemies();

		if (!rc.hasMoved()) {
			moveTowardsUnshookTrees();
		}
		if (!rc.hasMoved()) {
			moveTowardsEnemy();
		}
		if (!rc.hasMoved()) {
			moveInUnexploredDirection(0);
		}

		RobotInfo[] enemyRobots = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);
		if (!rc.hasAttacked()) {
			attackEnemies(enemyRobots);
		}

		/*
		 * if (!rc.hasAttacked()) { RobotInfo[] enemyRobots =
		 * rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);
		 * attackEnemies(enemyRobots); } if (!rc.hasAttacked()) { TreeInfo[]
		 * trees = rc.senseNearbyTrees(); attackPlants(); }
		 */
	}

	private void broadcastEnemies() {

	}

	private void moveTowardsEnemy() throws GameActionException {
		if (enemyLocation != null) {
			makeMove(rc.getLocation().directionTo(enemyLocation));
			rc.setIndicatorLine(rc.getLocation(), enemyLocation, 0, 200, 200);
			return;
		}
		MapLocation closest = null;
		int closestIndex = 0;
		for (int i = 0; i < this.enemyArchons.length; i++) {
			MapLocation enemyArchon = enemyArchons[i];
			if (this.visitedArchonIndexes[i]) {
				return;
			}
			if (closest == null) {
				closest = enemyArchon;
				closestIndex = i;
			}
			if (rc.getLocation().distanceSquaredTo(enemyArchon) < rc.getLocation().distanceSquaredTo(closest)) {
				closest = enemyArchon;
				closestIndex = i;
			}
		}
		if (rc.getLocation().distanceSquaredTo(closest) < myType.sensorRadius) {
			visitedArchonIndexes[closestIndex] = true;
		}
		if (closest == null) {
			return;
		}
		makeMove(rc.getLocation().directionTo(closest));
		rc.setIndicatorLine(rc.getLocation(), closest, 200, 0, 200);
	}

	protected void shake() throws GameActionException {
		super.shake();
	}

	// Attacks the most optimal to attack nearby enemy.
	// Need to account for trees being in the way of a bullet being shot.
	// And not take that shot
	private void attackEnemies(RobotInfo[] enemyRobots) throws GameActionException {
		if (enemyRobots.length == 0) {
			return;
		}
		RobotInfo bestToAttack = enemyRobots[0];
		for (RobotInfo robot : enemyRobots) {
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
