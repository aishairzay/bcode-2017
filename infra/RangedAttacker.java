package infra;

import battlecode.common.*;

public abstract strictfp class RangedAttacker extends Bot {

	boolean defender;

	public RangedAttacker(RobotController rc) {
		super(rc);
		defender = false;
	}

	@Override
	public void run() throws GameActionException {
		ping();
		BulletInfo[] bullets = rc.senseNearbyBullets();
		RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);
		RobotInfo[] allies = rc.senseNearbyRobots(myType.sensorRadius, myTeam);
		Direction[] dirs = this.getSafestDirs(bullets, enemies, 5000);
		if (enemies.length > 0) {
			AttackInfo info = getMoveDir(dirs, allies, enemies, bullets);
			boolean shouldAttack = info.score > 0 || info.score == -150;
			Direction towardsEnemy = rc.getLocation().directionTo(info.attackLoc);
			if (info.moveDir == null) {
				attack(info, shouldAttack);
			} else {
				float diff = towardsEnemy.degreesBetween(info.moveDir);
				if (diff > -130 && diff < 130) {
					rc.move(info.moveDir);
					attack(info, shouldAttack);
				} else {
					attack(info, shouldAttack);
					rc.move(info.moveDir);
				}
			}
		} else {
			moveToEnemyLoc();
			if (!rc.hasMoved()) {
				this.moveInUnexploredDirection(0);
			}
		}
	}

	private void ping() throws GameActionException {
		if (rc.getRoundNum() % Constants.RANGED_PING_RATE != 0) {
			return;
		}
		int currentPing = rc.readBroadcast(Channels.RANGED_PING_CHANNEL);
		if (currentPing <= 0) {
			rc.broadcast(Channels.RANGED_PING_CHANNEL, 1);
		} else {
			rc.broadcast(Channels.RANGED_PING_CHANNEL, currentPing + 1);
		}
	}

	private void attack(AttackInfo info, boolean shouldAttack) throws GameActionException {
		if (!shouldAttack) {
			return;
		}
		MapLocation next = rc.getLocation().add(info.moveDir);
		boolean five = next.distanceTo(info.attackLoc) <= 3 || info.type.equals(RobotType.TANK);
		boolean three = five || (next.distanceTo(info.attackLoc) <= 5);
		Direction toEnemy = rc.getLocation().directionTo(info.attackLoc);
		if (five && rc.canFirePentadShot()) {
			rc.firePentadShot(toEnemy);
		} else if (three && rc.canFireTriadShot()) {
			rc.fireTriadShot(toEnemy);
		} else if (rc.canFireSingleShot()) {
			rc.fireSingleShot(toEnemy);
		}
	}

	private AttackInfo getMoveDir(Direction[] dirs, RobotInfo[] allies, RobotInfo[] enemies, BulletInfo[] bullets)
			throws GameActionException {
		Direction toMove = null;
		RobotInfo closestThreat = findClosestThreat(enemies);
		if (closestThreat == null) {
			closestThreat = enemies[0];
		}
		float bestScore = -150;
		for (Direction dir : dirs) {
			if (!rc.canMove(dir, myType.strideRadius)) {
				continue;
			}
			float score = getScore(rc.getLocation().add(myType.strideRadius), allies, closestThreat);
			if (score > bestScore) {
				bestScore = score;
				toMove = dir;
			}
		}
		AttackInfo info = new AttackInfo(closestThreat.location, toMove, bestScore, closestThreat.type);
		System.out.println(info);
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

	private float getScore(MapLocation loc, RobotInfo[] allies, RobotInfo enemy) throws GameActionException {
		float score = 0;
		float dist = loc.distanceTo(enemy.location);

		if (this.bulletPathClear(loc, enemy)) {
			score += 30;
		} else {
			score -= 100;
		}

		if (enemy.type == RobotType.LUMBERJACK
				&& dist >= enemy.type.strideRadius + enemy.type.bodyRadius + GameConstants.INTERACTION_DIST_FROM_EDGE) {
			score -= 10;
		}
		if (enemy.type == RobotType.ARCHON || enemy.type == RobotType.GARDENER) {
			score += myType.sensorRadius - loc.distanceTo(enemy.location);
		}
		if (enemy.type == RobotType.SOLDIER || enemy.type == RobotType.TANK) {
			score += loc.distanceTo(enemy.location);
		}
		if (enemy.type == RobotType.SCOUT) {
			score += myType.sensorRadius - loc.distanceTo(enemy.location);
		}
		return score;
	}

	private class AttackInfo {
		MapLocation attackLoc;
		Direction moveDir;
		float score;
		RobotType type;

		public AttackInfo(MapLocation loc, Direction dir, float score, RobotType type) {
			this.attackLoc = loc;
			this.moveDir = dir;
			this.score = score;
			this.type = type;
		}

		public String toString() {
			return "AttackLoc: " + attackLoc + "\nmoveDir" + moveDir + "\nscore: " + score;
		}
	}
}
