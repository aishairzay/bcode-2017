package yeezus;

import battlecode.common.*;

public abstract strictfp class RangedAttacker extends Bot {

	boolean defender;
	boolean reachedEnemyLoc;
	boolean[] archonLocReached;
	MapLocation lastAttackLoc;
	int lastAttackRound;
	MapLocation globalEnemyLoc;
	private int roundsAlive;

	boolean assignedToArchon;

	public RangedAttacker(RobotController rc) {
		super(rc);
		defender = false;
		reachedEnemyLoc = false;
		archonLocReached = new boolean[this.allyArchons.length];
		assignedToArchon = false;
		roundsAlive = 0;
	}

	private MapLocation getDestination() throws GameActionException {
		MapLocation dest = null;
		if (rc.getLocation().distanceTo(this.enemyLoc) <= myType.sensorRadius) {
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
			} else if (rc.getLocation().distanceTo(enemyArchons[i]) <= rc.getLocation().distanceTo(closestArchonLoc)) {
				closestArchonLoc = this.enemyArchons[i];
			}
		}
		if (globalEnemyLoc != null && rc.getLocation().distanceTo(globalEnemyLoc) <= myType.sensorRadius - 1) {
			globalEnemyLoc = null;
			rc.broadcast(Channels.GLOBAL_ENEMY_LOC, 0);
			rc.broadcast(Channels.GLOBAL_ENEMY_LOC + 1, 0);
		}
		if (closestArchonLoc != null) {
			dest = closestArchonLoc;
		} else if (this.globalEnemyLoc != null) {
			dest = globalEnemyLoc;
		} else if (!reachedEnemyLoc) {
			dest = enemyLoc;
			System.out.println("moving towards enemy loc");
		}
		return dest;
	}

	private void readGlobalEnemyLoc() throws GameActionException {
		if (roundsAlive % 10 != 2) {
			return;
		}
		this.globalEnemyLoc = Helper.getLocation(rc, Channels.GLOBAL_ENEMY_LOC);
	}

	@Override
	public void run() throws GameActionException {
		roundsAlive++;
		ping();
		readGlobalEnemyLoc();
		BulletInfo[] bullets = rc.senseNearbyBullets();
		RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);
		// RobotInfo[] allies = rc.senseNearbyRobots(myType.sensorRadius,
		// myTeam);
		MapLocation[] locs = this.getSafestLocs(bullets, enemies, 10000);

		MapLocation destination = getDestination();

		for (int i = 0; i < locs.length; i++) {
			System.out.println(locs[i]);
		}

		for (int i = 0; i < enemies.length; i++) {
			RobotInfo enemy = enemies[i];
			if (enemy.type == RobotType.ARCHON) {
				int id = enemy.ID;
				int lastRound = rc.readBroadcast(Channels.getIdChannel(id));
				if (rc.getRoundNum() - lastRound >= 10 || assignedToArchon) {
					assignedToArchon = true;
					rc.broadcast(Channels.getIdChannel(id), rc.getRoundNum());
					break;
				}
			}
		}

		MapLocation bestLoc = null;
		if (locs.length > 0) {
			bestLoc = locs[rand.nextInt(locs.length)];
		}
		RobotInfo closest = null;
		if (enemies.length > 0) {
			closest = enemies[0];
			int i = 0;
			while (closest.type == RobotType.ARCHON) {
				if (i >= enemies.length) {
					break;
				} else {
					closest = enemies[i];
					i++;
				}
			}
		}
		boolean runAway = false;
		boolean runSideways = false;
		int length = enemies.length;

		if (closest != null && globalEnemyLoc == null) {
			this.broadcastEnemy(enemies);
		}
		if (closest != null && closest.type == RobotType.LUMBERJACK
				&& rc.getLocation().distanceTo(closest.location) <= RobotType.LUMBERJACK.strideRadius
						+ RobotType.LUMBERJACK.bodyRadius + GameConstants.INTERACTION_DIST_FROM_EDGE
						+ rc.getType().strideRadius + 1) {
			runAway = true;
		} else if (closest != null && closest.type == RobotType.LUMBERJACK
				&& rc.getLocation().distanceTo(closest.location) <= RobotType.LUMBERJACK.strideRadius
						+ RobotType.LUMBERJACK.bodyRadius + GameConstants.INTERACTION_DIST_FROM_EDGE
						+ rc.getType().strideRadius + 2) {
			runSideways = true;
		}
		if (closest != null && (closest.type == RobotType.SOLDIER || closest.type == RobotType.TANK)) {
			runSideways = true;
		}
		boolean attack = false;
		if (closest != null && (closest.type == RobotType.GARDENER || closest.type == RobotType.SCOUT
				|| (closest.type == RobotType.ARCHON && this.assignedToArchon))) {
			attack = true;
		}
		if (closest != null && closest.type == RobotType.ARCHON && !assignedToArchon) {
			length--;
		}

		if (length > 0) {
			for (MapLocation next : locs) {
				if (runAway) {
					if (next.distanceTo(closest.location) > bestLoc.distanceTo(closest.location)) {
						bestLoc = next;
					}
				} else if (runSideways) {
					Direction away = next.directionTo(closest.location);
					Direction dir = rc.getLocation().directionTo(next);
					float diff = 180 - Math.abs(away.degreesBetween(dir));
					Direction bestAway = bestLoc.directionTo(closest.location);
					float bestDiff = 180 - Math.abs(bestAway.degreesBetween(dir));
					if (diff > bestDiff) {
						bestLoc = next;
					}
				} else if (attack) {
					if (next.distanceTo(closest.location) < bestLoc.distanceTo(closest.location)) {
						bestLoc = next;
					}
				} else {
					if (destination == null && rand.nextBoolean()) {
						bestLoc = next;
					} else if (next.distanceTo(destination) < bestLoc.distanceTo(destination)) {
						bestLoc = next;
					}
				}
			}
			System.out.println("Used micro: bestloc is: " + bestLoc);
		} else if (bullets.length > 0 && inDanger) {
			if (destination == null) {
				bestLoc = locs[rand.nextInt(locs.length)];
			} else {
				for (MapLocation next : locs) {
					if (next.distanceTo(destination) < bestLoc.distanceTo(destination)) {
						bestLoc = next;
					}
				}
			}
			System.out.println("Used dodge: bestloc is: " + bestLoc);

		} else {
			if (destination != null) {
				System.out.println("Moving towards destination now");
				this.moveTowards(destination);
			} else {
				this.moveInUnexploredDirection(0);
			}
		}
		if (destination != null) {
			rc.setIndicatorLine(rc.getLocation(), destination, 100, 100, 100);
		}

		attackingArchon = false;
		archonToAttack = null;
		MapLocation toAttack = this.getToAttack(enemies);

		if (rc.hasMoved() && toAttack != null) {
			this.attack(toAttack, attackingArchon, blankShot);
		}

		if (toAttack == null && bestLoc == null) {
			// do nothing
		}
		if (toAttack != null && bestLoc != null && !rc.hasMoved()) {
			if (bestLoc.distanceTo(toAttack) < rc.getLocation().distanceTo(toAttack)) {
				rc.move(bestLoc);
				attack(toAttack, attackingArchon, blankShot);
			} else {
				attack(toAttack, attackingArchon, blankShot);
				rc.move(bestLoc);
			}
		}
		if (toAttack == null && bestLoc != null && !rc.hasMoved()) {
			rc.move(bestLoc);
		}
		if (!rc.hasAttacked()) {
			MapLocation afterMoveAttack = this.getToAttack(rc.senseNearbyRobots(myType.sensorRadius, enemyTeam));
			this.attack(afterMoveAttack, attackingArchon, blankShot);
		}

		this.shake(rc.senseNearbyTrees(
				myType.bodyRadius + myType.strideRadius + GameConstants.INTERACTION_DIST_FROM_EDGE, Team.NEUTRAL));
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

	private boolean attackRotationDirectionIsRight = false;

	private void attack(MapLocation attackLoc, boolean attackingArchon, boolean blankShot) throws GameActionException {
		if (attackLoc == null) {
			return;
		}
		if (!blankShot) {
			this.lastAttackRound = rc.getRoundNum();
		}
		this.lastAttackLoc = attackLoc;
		boolean five = rc.getLocation().distanceTo(attackLoc) <= 5;
		boolean three = true;
		if (attackingArchon) {
			three = false;
			five = false;
		}
		Direction toEnemy = rc.getLocation().directionTo(attackLoc);
		float distance = rc.getLocation().distanceTo(attackLoc);

		float r = (float) (RobotType.SOLDIER.bodyRadius * 2);
		if (blankShot) {
			r = RobotType.SOLDIER.bodyRadius;
		}
		int rotation = rand.nextInt((int) ((distance) / (r)));
		if (attackRotationDirectionIsRight) {
			toEnemy = toEnemy.rotateLeftDegrees(rotation);
		} else {
			toEnemy = toEnemy.rotateRightDegrees(rotation);
		}
		attackRotationDirectionIsRight = !attackRotationDirectionIsRight;
		rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(toEnemy, distance), 250, 250, 250);
		rc.setIndicatorDot(attackLoc, 150, 150, 150);
		if (five && rc.canFirePentadShot()) {
			rc.firePentadShot(toEnemy);
		} else if (three && rc.canFireTriadShot()) {
			rc.fireTriadShot(toEnemy);
		} else if (rc.canFireSingleShot()) {
			rc.fireSingleShot(toEnemy);
		}
	}

	MapLocation archonToAttack = null;
	boolean attackingArchon = false;
	boolean blankShot = false;

	private MapLocation getToAttack(RobotInfo[] enemies) throws GameActionException {
		blankShot = false;
		MapLocation toAttack = null;
		for (RobotInfo enemy : enemies) {
			if (this.bulletPathClear(rc.getLocation(), enemy)
					|| rc.getLocation().distanceTo(enemy.location) + myType.bodyRadius <= myType.strideRadius) {
				if (enemy.type.equals(RobotType.ARCHON)) {
					archonToAttack = enemy.location;
				} else {
					toAttack = enemy.location;
					break;
				}
			}
		}

		if (toAttack == null && archonToAttack != null && this.assignedToArchon) {
			attackingArchon = true;
			toAttack = archonToAttack;
		}

		if (toAttack == null && rc.getRoundNum() - 1 == this.lastAttackRound) {
			toAttack = this.lastAttackLoc;
			blankShot = true;
		}

		return toAttack;

	}
}
