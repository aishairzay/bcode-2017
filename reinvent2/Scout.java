package reinvent2;

import battlecode.common.*;

public strictfp class Scout extends Bot {

	boolean[] visitedArchonIndexes;
	MapLocation enemyLocation;
	Direction lastShot;
	int lastShotRound;

	public Scout(RobotController rc) {
		super(rc);
		visitedArchonIndexes = new boolean[allyArchons.length];
		lastShot = null;
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
			shouldMove = moveTowardsSensedEnemy(enemyRobots);
		}
		if (shouldMove && !rc.hasMoved()) {
			moveTowardsUnshookTrees();
		}
		if (shouldMove && !rc.hasMoved()) {
			moveTowardsEnemy();
		}
		if (shouldMove && !rc.hasMoved()) {
			while (this.isSimilarDirection(this.unexploredDir, this.lastShot)
					|| rc.getRoundNum() - 3 <= this.lastShotRound) {
				this.unexploredDir = this.getRandomDirection();
			}
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
		RobotInfo closestLumberjack = null;
		for (RobotInfo enemy : enemies) {
			if (enemy.type == RobotType.LUMBERJACK) {
				closestLumberjack = enemy;
				break;
			}
		}

		RobotInfo closestGardener = null;
		for (RobotInfo enemy : enemies) {
			if (enemy.type == RobotType.GARDENER) {
				closestGardener = enemy;
				break;
			}
		}
		if (closestGardener != null || closestLumberjack != null) {
			RobotInfo closest = getClosestHostileEnemy(enemies);
			if (closestLumberjack != null && rc.getHealth() <= 16
					&& rc.getLocation().distanceTo(closestLumberjack.location) <= 7) {
				this.makeMove(rc.getLocation().directionTo(closestLumberjack.location).opposite());
				return false;
			} else if (closest != null && rc.getHealth() <= 8 && rc.getLocation().distanceTo(closest.location) <= 6) {
				if (rand.nextBoolean()) {
					this.makeMove(rc.getLocation().directionTo(closest.location).opposite()
							.rotateLeftDegrees(rand.nextInt(10) + 15));
				} else {
					this.makeMove(rc.getLocation().directionTo(closest.location).opposite()
							.rotateRightDegrees(rand.nextInt(10) + 15));
				}
				return false;
			} else if (closestGardener != null && rc.getLocation().distanceTo(closestGardener.location) >= 3) {
				Direction dir = rc.getLocation().directionTo(closestGardener.location);
				if (rc.canMove(dir)) {
					move(dir);
				} else {
					this.makeMove(rc.getLocation().directionTo(closestGardener.location));
				}
				return false;
			} else if (closestGardener != null && rc.getLocation().distanceTo(closestGardener.location) <= 3) {
				Direction dir = rc.getLocation().directionTo(closestGardener.location);
				MapLocation loc = rc.getLocation().add(dir, 1);
				if (rc.senseTreeAtLocation(loc) != null) {
					boolean moved = smallMoveTowardsGardener(dir, 1);
					if (!moved) {
						this.makeMove(dir);
					}
				}
				return false;
			}
		}
		return true;
	}

	private boolean smallMoveTowardsGardener(Direction dir, float dist) throws GameActionException {
		if (dist == 0) {
			return false;
		}
		MapLocation m = rc.getLocation().add(dir, dist);
		if (rc.canMove(m)) {
			rc.move(m);
			return true;
		}
		return smallMoveTowardsGardener(dir, (float) (dist - 0.1));
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
			if ((type == null || enemy.type == type) && this.bulletPathClear(rc.getLocation(), enemy)) {
				float distance = rc.getLocation().distanceTo(enemy.location);
				if (distance < myType.bodyRadius + enemy.type.bodyRadius + 2 || enemy.type == RobotType.LUMBERJACK
						|| enemy.type == RobotType.GARDENER) {
					if (closestEnemy == null) {
						closestEnemy = enemy;
					}
					if (enemy.type == RobotType.GARDENER) {
						closestEnemy = enemy;
						break;
					}
				}
			}
		}
		if (closestEnemy != null) {
			Direction dir = rc.getLocation().directionTo(closestEnemy.location);
			rc.fireSingleShot(dir);
			this.lastShot = dir;
			this.lastShotRound = rc.getRoundNum();
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

	void move(Direction dir) throws GameActionException {
		if (!willHitOwnBullet(dir)) {
			rc.move(dir);
		}
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
		while (true) {
			if (rc.canMove(rotation) && !willHitOwnBullet(rotation)) {
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

	private boolean willHitOwnBullet(Direction dir) {
		if (lastShot == null || rc.getRoundNum() - 1 != this.lastShotRound) {
			return false;
		}
		return isSimilarDirection(dir, lastShot);
	}

	private boolean isSimilarDirection(Direction one, Direction two) {
		if (one == null || two == null) {
			return false;
		}
		float difference = one.degreesBetween(two);
		return (Math.abs(difference) <= 45);
	}

}
