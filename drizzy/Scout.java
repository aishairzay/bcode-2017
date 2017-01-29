package drizzy;

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
		TreeInfo[] neutralTrees = rc.senseNearbyTrees(myType.sensorRadius, Team.NEUTRAL);
		RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);
		BulletInfo[] bullets = rc.senseNearbyBullets(myType.bulletSightRadius);

		MapLocation destination = findClosest(enemies, RobotType.GARDENER);
		if (destination == null) {
			destination = findClosest(enemies, RobotType.ARCHON);
		}
		if (destination == null) {
			this.moveTowardsUnshookTrees();
		}
		if (!rc.hasMoved() && destination == null) {
			destination = getNextArchonLoc();
		}

		if (!rc.hasMoved()) {
			MapLocation[] moveLocs = this.getSafestLocs(bullets, enemies, 7000);
			if (moveLocs.length > 0) {
				MapLocation bestLoc = moveLocs[rand.nextInt(moveLocs.length)];
				if (destination != null) {
					for (MapLocation nextLoc : moveLocs) {
						if (nextLoc.distanceTo(destination) <= bestLoc.distanceTo(destination)) {
							bestLoc = nextLoc;
						}
					}
				}
				rc.move(bestLoc);
			} else {
				if (destination == null) {
					this.moveInUnexploredDirection(false);
				} else {
					moveTowards(destination);
					if (rc.canSenseLocation(destination)) {
						RobotInfo r = rc.senseRobotAtLocation(destination);
						if (r != null && r.type.equals(RobotType.GARDENER)
								&& rc.getLocation().distanceTo(destination) <= 3) {
							Direction dir = rc.getLocation().directionTo(r.location);
							MapLocation loc = rc.getLocation().add(dir, 1);
							if (rc.senseTreeAtLocation(loc) != null) {
								boolean moved = smallMoveTowardsGardener(dir, 1);
								if (!moved) {
									this.makeMove(dir);
								}
							}
						}
					}
				}
			}
		}

		attackEnemies(enemies, RobotType.GARDENER);
		attackEnemies(enemies, null);

		shake(neutralTrees);
	}

	private MapLocation findClosest(RobotInfo[] enemies, RobotType type) throws GameActionException {
		MapLocation dest = null;
		for (RobotInfo enemy : enemies) {
			if (enemy.type == type) {
				dest = enemy.location;
				break;
			}
		}
		return dest;
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
		}
	}

	private MapLocation getNextArchonLoc() throws GameActionException {
		MapLocation closest = null;
		int closestIndex = 0;
		for (int i = 0; i < this.enemyArchons.length; i++) {
			MapLocation enemyArchon = enemyArchons[i];
			if (this.visitedArchonIndexes[i]) {
				continue;
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
		if (closest != null && rc.getLocation().distanceTo(closest) < myType.sensorRadius) {
			visitedArchonIndexes[closestIndex] = true;
		}
		return closest;
	}

	void move(Direction dir) throws GameActionException {
		rc.move(dir);
	}

}
