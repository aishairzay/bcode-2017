package Fourth;

import battlecode.common.*;

public abstract strictfp class RangedAttacker extends Bot {

	boolean defender;

	public RangedAttacker(RobotController rc) {
		super(rc);
		defender = false;
	}

	@Override
	public void run() throws GameActionException {
		BulletInfo[] bullets = rc.senseNearbyBullets();
		RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);
		RobotInfo[] allies = rc.senseNearbyRobots(myType.sensorRadius, myTeam);

		if (enemies.length > 0) {
			AttackInfo info = getMoveDir(allies, enemies, bullets);
			boolean shouldAttack = info.score > -10;
			Direction towardsEnemy = rc.getLocation().directionTo(info.attackLoc);
			if (info.moveDir == null) {
				attack(info.attackLoc, shouldAttack);
			} else {
				float diff = towardsEnemy.degreesBetween(info.moveDir);
				if (diff > -90 && diff < 90) {
					rc.move(info.moveDir);
					attack(info.attackLoc, shouldAttack);
				} else {
					attack(info.attackLoc, shouldAttack);
					rc.move(info.moveDir);
				}
			}
		} else {
			if (defender) {
				defend();
			} else {
				this.moveInUnexploredDirection(0);
			}
		}
	}

	private void attack(MapLocation loc, boolean shouldAttack) throws GameActionException {
		if (!shouldAttack) {
			return;
		}
		rc.setIndicatorLine(rc.getLocation(), loc, 0, 200, 0);
		Direction toEnemy = rc.getLocation().directionTo(loc);
		if (rc.canFireTriadShot()) {
			rc.fireTriadShot(toEnemy);
		} else if (rc.canFireSingleShot()) {
			rc.fireSingleShot(toEnemy);
		}
	}

	private AttackInfo getMoveDir(RobotInfo[] allies, RobotInfo[] enemies, BulletInfo[] bullets) {

		Direction toMove = null;
		RobotInfo closestThreat = findClosestThreat(enemies);
		if (closestThreat == null) {
			closestThreat = enemies[0];
		}
		float bestScore = getScore(rc.getLocation(), allies, closestThreat);
		for (Direction dir : this.directions) {
			if (!rc.canMove(dir, myType.strideRadius)) {
				continue;
			}
			float score = getScore(rc.getLocation().add(myType.strideRadius), allies, closestThreat);
			if (score > bestScore) {
				bestScore = score;
				toMove = dir;
			}
		}
		AttackInfo info = new AttackInfo(closestThreat.location, toMove, bestScore);
		return info;
	}

	private RobotInfo findClosestThreat(RobotInfo[] enemies) {
		RobotInfo closest = null;
		for (RobotInfo enemy : enemies) {
			if (enemy.type == RobotType.ARCHON || enemy.type == RobotType.GARDENER) {
				continue;
			}
			if (closest == null) {
				closest = enemy;
				break;
			}
		}
		return closest;
	}

	private float getScore(MapLocation loc, RobotInfo[] allies, RobotInfo enemy) {
		rc.setIndicatorDot(loc, 0, 0, 200);
		rc.setIndicatorDot(enemy.location, 200, 0, 0);
		float score = 0;
		float dist = loc.distanceTo(enemy.location);
		if (enemy.type == RobotType.LUMBERJACK && dist < enemy.type.strideRadius + 1) {
			score -= myType.sensorRadius;
		}
		if (enemy.type == RobotType.ARCHON || enemy.type == RobotType.GARDENER) {
			score -= loc.distanceTo(enemy.location);
		}
		if (enemy.type == RobotType.SOLDIER || enemy.type == RobotType.TANK) {
			score += loc.distanceTo(enemy.location);
		}
		if (enemy.type == RobotType.SCOUT) {
			score -= loc.distanceSquaredTo(enemy.location) / 2;
		}
		Direction dir = rc.getLocation().directionTo(enemy.location);
		for (RobotInfo ally : allies) {
			if (dir.degreesBetween(rc.getLocation().directionTo(ally.location)) < 30) {
				if (rc.getLocation().distanceTo(ally.location) < rc.getLocation().distanceTo(enemy.location)) {
					score -= 10;
				}
			}
		}
		Direction enemyDir = rc.getLocation().directionTo(enemy.location);
		Direction towardsLoc = rc.getLocation().directionTo(loc);
		if (towardsLoc == null) {
			return score;
		}
		if (enemyDir.degreesBetween(towardsLoc) > 70 && enemyDir.degreesBetween(towardsLoc) < 110) {
			if (enemy.type == RobotType.SOLDIER || enemy.type == RobotType.TANK) {
				score += 5;
			}
		}
		return score;
	}

	private void defend() {

	}

	private void moveTowardsEnemy() {

	}

	private void setDestination() {

	}

	private class AttackInfo {
		MapLocation attackLoc;
		Direction moveDir;
		float score;

		public AttackInfo(MapLocation loc, Direction dir, float score) {
			this.attackLoc = loc;
			this.moveDir = dir;
			this.score = score;
		}
	}
}
