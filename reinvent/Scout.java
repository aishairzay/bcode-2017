package reinvent;

import battlecode.common.*;

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
		RobotInfo[] enemyRobots = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);

		boolean shouldMove = true;

		if (shouldMove && !rc.hasMoved()) {
			moveTowardsUnshookTrees();
		}
		if (shouldMove && !rc.hasMoved()) {
			shouldMove = moveTowardsSensedEnemy(enemyRobots);
		}
		if (shouldMove && !rc.hasMoved()) {
			moveTowardsEnemy();
		}
		if (shouldMove && !rc.hasMoved()) {
			moveInUnexploredDirection(0);
		}
		if (!rc.hasAttacked()) {
			attackEnemies(enemyRobots, RobotType.GARDENER);
		}
		if (!rc.hasAttacked()) {
			attackEnemies(enemyRobots, null);
		}
	}

	private boolean moveTowardsSensedEnemy(RobotInfo[] enemies) throws GameActionException {
		RobotInfo closestGardener = null;
		for (RobotInfo enemy : enemies) {
			if (enemy.type == RobotType.GARDENER) {
				if (closestGardener == null) {
					closestGardener = enemy;
				} else if (rc.getLocation().distanceSquaredTo(enemy.location) < rc.getLocation()
						.distanceSquaredTo(closestGardener.location)) {
					closestGardener = enemy;
				}
			}
		}
		if (closestGardener != null) {
			RobotInfo closest = getClosestHostileEnemy(enemies);
			if (rc.getHealth() < 10 && rc.getLocation().distanceTo(closest.location) < 6) {
				return true;
			}
			if (rc.getLocation().distanceTo(closestGardener.location) >= 2.4) {
				Direction dir = rc.getLocation().directionTo(closestGardener.location);
				if (rc.canMove(dir)) {
					rc.move(dir);
				} else {
					this.makeMove(rc.getLocation().directionTo(closestGardener.location));
				}
			}
			return false;
		}
		return true;
	}

	private RobotInfo getClosestHostileEnemy(RobotInfo[] enemies) {
		RobotInfo closestEnemy = null;
		for (RobotInfo enemy : enemies) {
			if (enemy.type == RobotType.ARCHON || enemy.type == RobotType.GARDENER) {
				continue;
			}
			if (closestEnemy == null) {
				closestEnemy = enemy;
				break;
			}
		}
		return closestEnemy;
	}

	private void attackEnemies(RobotInfo[] enemies, RobotType type) throws GameActionException {
		RobotInfo closestEnemy = null;
		if (!rc.canFireSingleShot()) {
			return;
		}
		for (RobotInfo enemy : enemies) {
			if (type == null || enemy.type == type) {
				float distance = rc.getLocation().distanceTo(enemy.location);
				if (distance < myType.bodyRadius + enemy.type.bodyRadius + 2) {
					if (closestEnemy == null) {
						closestEnemy = enemy;
					} else if (distance <= rc.getLocation().distanceTo(closestEnemy.location)) {
						closestEnemy = enemy;
					}
				}
			}
		}
		if (closestEnemy != null) {
			rc.fireSingleShot(rc.getLocation().directionTo(closestEnemy.location));
		}
	}

	private void moveTowardsEnemy() throws GameActionException {
		if (enemyLocation != null) {
			makeMove(rc.getLocation().directionTo(enemyLocation));
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
	}

	protected void shake() throws GameActionException {
		super.shake();
	}

}
