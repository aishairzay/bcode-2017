package Fourth;

import battlecode.common.*;

public strictfp class Scout extends Bot {

	boolean[] visitedArchonIndexes;
	MapLocation enemyLocation;
	boolean collector;
	MapLocation justShot;

	MapLocation destination;
	boolean makeMove = false;
	MapLocation attackLoc;

	public Scout(RobotController rc) {
		super(rc);
		visitedArchonIndexes = new boolean[allyArchons.length];
		collector = false;
		destination = null;
		justShot = null;
	}

	private void setDestination(MapLocation dest, boolean makeMove) {
		this.destination = dest;
		this.makeMove = makeMove;
	}

	private void setAttackLoc(MapLocation loc) {
		this.attackLoc = loc;
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

		this.destination = null;
		this.makeMove = false;
		this.attackLoc = null;
		boolean shouldMove = true;
		boolean shouldAttack = true;
		// false if shouldn't move after this
		// true if should move after this

		if (shouldMove && !rc.hasMoved() && collector) {
			shouldMove = moveTowardsUnshookTrees(); // done
		}
		if (shouldMove && !rc.hasMoved() && collector) {
			shouldMove = moveInUnexploredDirection(0); // done
		}
		if (shouldMove && !rc.hasMoved()) {
			shouldMove = moveTowardsSensedEnemy(enemyRobots); // done
		}
		if (shouldMove && !rc.hasMoved()) {
			shouldMove = moveTowardsEnemy();
		}
		if (shouldMove && !rc.hasMoved()) {
			shouldMove = moveTowardsUnshookTrees(); // done
		}
		if (shouldMove) {
			shouldMove = moveInUnexploredDirection(0);
		}
		if (shouldAttack) {
			shouldAttack = attackEnemies(enemyRobots, RobotType.SCOUT);
		}
		if (shouldAttack) {
			shouldAttack = attackEnemies(enemyRobots, RobotType.GARDENER);
		}
		if (shouldAttack) {
			shouldAttack = attackEnemies(enemyRobots, null);
		}
		attackAndMove(enemyRobots);

		if (justShot != null) {
			justShot = null;
		}
	}

	private void attackAndMove(RobotInfo[] enemies) throws GameActionException {
		if (destination != null) {
			for (RobotInfo enemy : enemies) {
				if (enemy.type == RobotType.LUMBERJACK && destination.distanceTo(
						enemy.location) < (RobotType.LUMBERJACK.strideRadius + 1 + RobotType.LUMBERJACK.bodyRadius)) {
					this.setDestination(rc.getLocation().add(enemy.location.directionTo(rc.getLocation())), true);
					System.out.println("In lj range, running away");
					break;
				} else if (enemy.type == RobotType.LUMBERJACK
						&& destination.distanceTo(enemy.location) < (RobotType.SCOUT.strideRadius
								+ RobotType.LUMBERJACK.strideRadius + 1 + RobotType.LUMBERJACK.bodyRadius)) {
					System.out.println("In outer range, staying still");
					this.setDestination(null, true);
					break;
				} else {
					System.out.println("Not in lumberjack range");
				}
			}
		}
		if (attackLoc == null && destination == null) {
			return;
		}
		if (attackLoc != null && destination == null) {
			shoot();
			return;
		}
		if (attackLoc == null && destination != null) {
			move();
			return;
		}
		Direction towardsEnemy = rc.getLocation().directionTo(destination);
		float diff = towardsEnemy.degreesBetween(rc.getLocation().directionTo(attackLoc));
		if (Math.abs(diff) <= 90) {
			move();
			shoot();
		} else {
			shoot();
			move();
		}
	}

	private void move() throws GameActionException {
		if (destination == null) {
			return;
		}
		rc.setIndicatorLine(rc.getLocation(), destination, 0, 200, 0);
		if (makeMove) {
			this.makeMove(rc.getLocation().directionTo(destination));
		} else {
			if (rc.canMove(destination)) {
				rc.move(destination);
			}
		}
	}

	private void shoot() throws GameActionException {
		if (attackLoc == null || !rc.canFireSingleShot()) {
			return;
		}
		rc.fireSingleShot(rc.getLocation().directionTo(attackLoc));
		rc.setIndicatorLine(rc.getLocation(), attackLoc, 200, 0, 0);
	}

	private boolean moveTowardsSensedEnemy(RobotInfo[] enemies) throws GameActionException {
		RobotInfo closest = null;
		for (RobotInfo enemy : enemies) {
			if (enemy.type == RobotType.GARDENER) {
				if (closest == null) {
					closest = enemy;
				} else if (rc.getLocation().distanceSquaredTo(enemy.location) < rc.getLocation()
						.distanceSquaredTo(closest.location)) {
					closest = enemy;
				}
			}
		}
		if (closest != null) {
			RobotInfo closestHostile = getClosestHostileEnemy(enemies);
			if (rc.getHealth() <= 18) {
				if (this.bulletPathClear(rc.getLocation(), closestHostile)
						|| (closestHostile.type == RobotType.LUMBERJACK
								&& rc.getLocation().distanceTo(closestHostile.location) <= 3)) {
					Direction dir = closestHostile.location.directionTo(rc.getLocation());
					this.setDestination(rc.getLocation().add(dir, myType.strideRadius), true);
				}
			}
			if (rc.getHealth() <= 5 && rc.getLocation().distanceTo(closestHostile.location) < 6) {
				collector = true;
			}
			Direction dir = rc.getLocation().directionTo(closest.location);
			float dist = rc.getLocation().distanceTo(closest.location.add(dir.opposite(), closest.type.bodyRadius));
			if (dist < myType.strideRadius && rc.canMove(dir, dist / 2)) {
				this.setDestination(rc.getLocation().add(dir, dist / 2), false);
			} else if (rc.canMove(dir, myType.strideRadius)) {
				this.setDestination(rc.getLocation().add(dir, myType.strideRadius), false);
			} else {
				Direction towardsDir = rc.getLocation().directionTo(closest.location);
				this.setDestination(rc.getLocation().add(towardsDir, myType.strideRadius), true);
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
			closestEnemy = enemy;
			break;
		}
		return closestEnemy;
	}

	private boolean attackEnemies(RobotInfo[] enemies, RobotType type) throws GameActionException {
		RobotInfo closestEnemy = null;
		if (!rc.canFireSingleShot()) {
			return true;
		}
		for (RobotInfo enemy : enemies) {
			if (type == null || enemy.type == type) {
				float distance = rc.getLocation().distanceTo(enemy.location);
				if (this.bulletPathClear(rc.getLocation(), enemy)) {
					if (closestEnemy == null) {
						closestEnemy = enemy;
					} else if (distance <= rc.getLocation().distanceTo(closestEnemy.location)) {
						closestEnemy = enemy;
					}
				}
			}
		}
		if (closestEnemy != null) {
			this.setAttackLoc(closestEnemy.location);
			return false;
		}
		return true;
	}

	private boolean moveTowardsEnemy() throws GameActionException {
		if (enemyLocation != null) {
			Direction dir = rc.getLocation().directionTo(enemyLocation);
			this.setDestination(rc.getLocation().add(dir, myType.strideRadius), true);
			return false;
		}
		MapLocation closest = null;
		int closestIndex = 0;
		for (int i = 0; i < this.enemyArchons.length; i++) {
			MapLocation enemyArchon = enemyArchons[i];
			if (this.visitedArchonIndexes[i]) {
				return true;
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
			return true;
		}
		Direction dir = rc.getLocation().directionTo(closest);
		this.setDestination(rc.getLocation().add(dir, myType.strideRadius), true);
		return false;
	}

	protected void shake() throws GameActionException {
		super.shake();
	}

	protected boolean moveTowardsUnshookTrees() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees(myType.sensorRadius, Team.NEUTRAL);
		TreeInfo closest = null;
		for (TreeInfo tree : trees) {
			if (tree.team != Team.NEUTRAL) {
				continue;
			}
			if (myType != RobotType.SCOUT && myType.strideRadius < tree.radius) {
				continue;
			}
			boolean visited = rc.readBroadcast(this.getTreeChannel(tree.ID)) > 0;
			if (visited) {
				continue;
			}
			if (closest == null) {
				closest = tree;
			} else if (rc.getLocation().distanceSquaredTo(tree.location) < rc.getLocation()
					.distanceSquaredTo(closest.location)) {
				closest = tree;
			}
		}
		if (closest == null) {
			return true;
		}
		if (rc.canMove(closest.location)) {
			this.setDestination(closest.location, false);
			return false;
		} else {
			Direction dir = rc.getLocation().directionTo(closest.location);
			this.setDestination(rc.getLocation().add(dir, myType.strideRadius), true);
			return false;
		}
	}

	protected boolean moveInUnexploredDirection(int tries) throws GameActionException {
		if (tries == 8) {
			return true;
		}
		if (unexploredDir == null) {
			unexploredDir = getRandomDirection();
			if (unexploredDir == Direction.getEast() || unexploredDir == Direction.getNorth()
					|| unexploredDir == Direction.getEast() || unexploredDir == Direction.getSouth()) {
				unexploredDir = getRandomDirection();
			}
		}
		if (rc.canMove(unexploredDir)) {
			this.setDestination(rc.getLocation().add(unexploredDir, myType.strideRadius), true);
			return false;
		} else {
			unexploredDir = this.getRandomDirection();
			return moveInUnexploredDirection(tries + 1);
		}
	}

}
