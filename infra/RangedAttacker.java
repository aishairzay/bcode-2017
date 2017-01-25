package infra;

import battlecode.common.*;

public abstract strictfp class RangedAttacker extends Bot {

	boolean defender;
	boolean reachedEnemyLoc;
	boolean[] archonLocReached;
	MapLocation lastAttackLoc;
	int lastAttackRound;

	public RangedAttacker(RobotController rc) {
		super(rc);
		defender = false;
		reachedEnemyLoc = false;
		archonLocReached = new boolean[this.allyArchons.length];
	}

	@Override
	public void run() throws GameActionException {
		ping();
		BulletInfo[] bullets = rc.senseNearbyBullets();
		RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);
		RobotInfo[] allies = rc.senseNearbyRobots(myType.sensorRadius, myTeam);
		Direction[] dirs = this.getSafestDirs(bullets, enemies, 5000);

		int bestScore = 0;
		Direction bestDir = null;
		if (dirs.length > 0) {
			bestDir = dirs[rand.nextInt(dirs.length)];
		}
		RobotInfo closest = null;
		if (enemies.length > 0) {
			closest = enemies[0];
		}
		boolean runAway = false;
		boolean runSideways = false;
		if (closest != null && closest.type == RobotType.LUMBERJACK
				&& rc.getLocation().distanceTo(closest.location) <= RobotType.LUMBERJACK.strideRadius
						+ RobotType.LUMBERJACK.bodyRadius + GameConstants.INTERACTION_DIST_FROM_EDGE
						+ rc.getType().strideRadius) {
			runAway = true;
		} else if (closest != null && closest.type == RobotType.LUMBERJACK
				&& rc.getLocation().distanceTo(closest.location) <= RobotType.LUMBERJACK.strideRadius
						+ RobotType.LUMBERJACK.bodyRadius + GameConstants.INTERACTION_DIST_FROM_EDGE
						+ rc.getType().strideRadius + 1) {
			runSideways = true;
		}
		if (closest != null && (closest.type == RobotType.SOLDIER || closest.type == RobotType.TANK)) {
			runSideways = true;
		}
		boolean attack = false;
		if (closest != null && (closest.type == RobotType.GARDENER || closest.type == RobotType.SCOUT
				|| closest.type == RobotType.ARCHON)) {
			attack = true;
		}
		if (enemies.length > 0) {
			for (Direction dir : dirs) {
				MapLocation best = rc.getLocation().add(bestDir, myType.strideRadius);
				MapLocation next = rc.getLocation().add(dir, myType.strideRadius);
				if (runAway) {
					if (next.distanceTo(closest.location) > best.distanceTo(closest.location)) {
						bestDir = dir;
					}
				} else if (runSideways) {
					Direction away = next.directionTo(closest.location);
					float diff = 180 - Math.abs(away.degreesBetween(dir));
					Direction bestAway = best.directionTo(closest.location);
					float bestDiff = 180 - Math.abs(bestAway.degreesBetween(dir));
					if (diff > bestDiff) {
						bestDir = dir;
					}
				} else if (attack) {
					if (next.distanceTo(closest.location) < best.distanceTo(closest.location)) {
						bestDir = dir;
					}
				}
			}
		} else if (bullets.length > 0) {
			MapLocation dest = this.enemyLoc;
			for (Direction dir : dirs) {
				MapLocation best = rc.getLocation().add(bestDir, myType.strideRadius);
				MapLocation next = rc.getLocation().add(dir, myType.strideRadius);
				if (next.distanceTo(dest) < best.distanceTo(dest)) {
					bestDir = dir;
				}
			}
		} else {
			if (rc.getLocation().distanceTo(this.enemyLoc) < myType.sensorRadius) {
				this.reachedEnemyLoc = true;
			}
			MapLocation closestArchonLoc = null;
			for (int i = 0; i < this.archonLocReached.length; i++) {
				if (this.archonLocReached[i]) {
					continue;
				}
				if (rc.getLocation().distanceTo(this.enemyArchons[i]) <= 5) {
					this.archonLocReached[i] = true;
				} else if (closestArchonLoc == null) {
					closestArchonLoc = this.enemyArchons[i];
				} else if (rc.getLocation().distanceTo(enemyArchons[i]) <= rc.getLocation()
						.distanceTo(closestArchonLoc)) {
					closestArchonLoc = this.enemyArchons[i];
				}
			}
			if (closestArchonLoc != null) {
				this.moveTowards(closestArchonLoc);
			} else if (!reachedEnemyLoc) {
				this.moveTowards(enemyLoc);
			} else {
				this.moveInUnexploredDirection(0);
			}

		}

		boolean attackingArchon = false;
		MapLocation archonToAttack = null;
		MapLocation toAttack = null;
		for (RobotInfo enemy : enemies) {
			if (this.bulletPathClear(rc.getLocation(), enemy)
					|| rc.getLocation().distanceTo(enemy.location) + myType.bodyRadius <= myType.strideRadius) {
				toAttack = enemy.location;
				if (enemy.type == RobotType.ARCHON) {
					archonToAttack = enemy.location;
				} else {
					break;
				}
			}
		}
		if (toAttack == null) {
			attackingArchon = true;
			toAttack = archonToAttack;
		}
		boolean blankShot = false;
		if (toAttack == null && rc.getRoundNum() - 1 == this.lastAttackRound) {
			toAttack = this.lastAttackLoc;
			blankShot = true;
		}

		if (rc.hasMoved() && toAttack != null) {
			this.attack(toAttack, attackingArchon);
			if (!blankShot) {
				this.lastAttackRound = rc.getRoundNum();
			}
		}

		if (toAttack == null && bestDir == null) {
			// do nothing
		}
		if (toAttack != null && bestDir != null) {// move and attack
			MapLocation next = rc.getLocation().add(bestDir, myType.strideRadius);
			if (next.distanceTo(toAttack) < rc.getLocation().distanceTo(toAttack)) {
				rc.move(bestDir);
				attack(toAttack, attackingArchon);
			} else {
				attack(toAttack, attackingArchon);
				rc.move(bestDir);
			}
			if (!blankShot) {
				this.lastAttackRound = rc.getRoundNum();
			}
		}
		if (toAttack == null && bestDir != null) {
			rc.move(bestDir);
		}
		this.shake(rc.senseNearbyTrees(
				myType.bodyRadius + myType.strideRadius + GameConstants.INTERACTION_DIST_FROM_EDGE, Team.NEUTRAL));
	}

	private MapLocation getAttackLoc() {
		MapLocation loc = null;
		if (!reachedEnemyLoc && rc.getLocation().distanceTo(enemyLoc) <= myType.sensorRadius - 1) {
			this.reachedEnemyLoc = true;
		} else {
			return enemyLoc;
		}
		return loc;
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

	private void attack(MapLocation attackLoc, boolean attackingArchon) throws GameActionException {
		this.lastAttackLoc = attackLoc;
		boolean five = rc.getLocation().distanceTo(attackLoc) <= 4;
		boolean three = five || (rc.getLocation().distanceTo(attackLoc) <= myType.sensorRadius + myType.strideRadius);
		if (attackingArchon) {
			three = false;
			five = false;
		}
		Direction toEnemy = rc.getLocation().directionTo(attackLoc);
		if (five && rc.canFirePentadShot()) {
			rc.firePentadShot(toEnemy);
		} else if (three && rc.canFireTriadShot()) {
			rc.fireTriadShot(toEnemy);
		} else if (rc.canFireSingleShot()) {
			rc.fireSingleShot(toEnemy);
		}
	}

}
