package Secondary;

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
			System.out.println("Going towards closest gardener");

			if (rc.getLocation().distanceTo(closestGardener.location) >= 2.5) {
				Direction dir = rc.getLocation().directionTo(closestGardener.location);
				if (rc.canMove(dir)) {
					System.out.println("Making straight movement");
					rc.move(dir);
				} else {
					this.makeMove(rc.getLocation().directionTo(closestGardener.location));
				}
			}
			return false;
		}
		return true;
	}

	private void attackEnemies(RobotInfo[] enemies, RobotType type) throws GameActionException {
		RobotInfo closestEnemy = null;
		if (!rc.canFireSingleShot()) {
			return;
		}
		for (RobotInfo enemy : enemies) {
			if (type == null || enemy.type == type) {
				float distance = rc.getLocation().distanceTo(enemy.location);
				System.out.println("Distance is: " + distance);
				System.out.println("Combined body radius is: " + myType.bodyRadius + enemy.type.bodyRadius);
				if (distance < myType.bodyRadius + enemy.type.bodyRadius + 1) {
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

}
