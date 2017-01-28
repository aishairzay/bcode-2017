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

	private void readGlobalEnemyLoc() throws GameActionException {
		if (globalEnemyLoc != null || roundsAlive % 10 != 2) {
			return;
		}
		this.globalEnemyLoc = Helper.getLocation(rc, Channels.GLOBAL_ENEMY_LOC);
		System.out.println("Got new global enemy loc: " + globalEnemyLoc);
	}

	@Override
	public void run() throws GameActionException {
		roundsAlive++;
		ping();
		readGlobalEnemyLoc();
		BulletInfo[] bullets = rc.senseNearbyBullets();
		RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);
		RobotInfo[] allies = rc.senseNearbyRobots(myType.sensorRadius, myTeam);
		Direction[] dirs = this.getSafestDirs(bullets, enemies, 5000);

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

		int bestScore = 0;
		Direction bestDir = null;
		if (dirs.length > 0) {
			bestDir = dirs[rand.nextInt(dirs.length)];
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
				|| (closest.type == RobotType.ARCHON && this.assignedToArchon))) {
			attack = true;
		}
		if (closest != null && closest.type == RobotType.ARCHON && !assignedToArchon) {
			length--;
		}

		// System.out.println("In danger is : " + inDanger);

		if (length > 0) {
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
		} else if (bullets.length > 0 && inDanger) {
			MapLocation dest = this.enemyLoc;
			for (Direction dir : dirs) {
				MapLocation best = rc.getLocation().add(bestDir, myType.strideRadius);
				MapLocation next = rc.getLocation().add(dir, myType.strideRadius);
				if (next.distanceTo(dest) < best.distanceTo(dest)) {
					bestDir = dir;
				}
			}
		} else {
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
				} else if (rc.getLocation().distanceTo(enemyArchons[i]) <= rc.getLocation()
						.distanceTo(closestArchonLoc)) {
					closestArchonLoc = this.enemyArchons[i];
				}
			}
			if (globalEnemyLoc != null && rc.getLocation().distanceTo(globalEnemyLoc) <= myType.sensorRadius - 1) {
				globalEnemyLoc = null;
				rc.broadcast(Channels.GLOBAL_ENEMY_LOC, 0);
				rc.broadcast(Channels.GLOBAL_ENEMY_LOC + 1, 0);
			}
			if (closestArchonLoc != null) {
				this.moveTowards(closestArchonLoc);
				System.out.println("Moving towards archon.");
			} else if (this.globalEnemyLoc != null) {
				this.moveTowards(globalEnemyLoc);
				System.out.println("Moving towards global enemy loc: " + globalEnemyLoc);
			} else if (!reachedEnemyLoc) {
				this.moveTowards(enemyLoc);
				System.out.println("moving towards enemy loc");
			} else {
				this.moveInUnexploredDirection(0);
				System.out.println("Moving unexplored");
			}

		}

		boolean attackingArchon = false;
		MapLocation archonToAttack = null;
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
			System.out.println("SETTING ATTACKING ARCHON TO TRUE!");
			attackingArchon = true;
			toAttack = archonToAttack;
		}
		boolean blankShot = false;
		if (toAttack == null && rc.getRoundNum() - 1 == this.lastAttackRound) {
			toAttack = this.lastAttackLoc;
			blankShot = true;
		}

		if (rc.hasMoved() && toAttack != null) {
			this.attack(toAttack, attackingArchon, blankShot);
			if (!blankShot) {
				this.lastAttackRound = rc.getRoundNum();
			}
		}

		if (toAttack == null && bestDir == null) {
			// do nothing
		}
		if (toAttack != null && bestDir != null && !rc.hasMoved()) {
			MapLocation next = rc.getLocation().add(bestDir, myType.strideRadius);
			if (next.distanceTo(toAttack) < rc.getLocation().distanceTo(toAttack)) {
				rc.move(bestDir);
				attack(toAttack, attackingArchon, blankShot);
			} else {
				attack(toAttack, attackingArchon, blankShot);
				rc.move(bestDir);
			}
			if (!blankShot) {
				this.lastAttackRound = rc.getRoundNum();
			}
		}
		if (toAttack == null && bestDir != null && !rc.hasMoved()) {
			rc.move(bestDir);
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
		System.out.println("Attacking archons is: " + attackingArchon);
		this.lastAttackLoc = attackLoc;
		boolean five = rc.getLocation().distanceTo(attackLoc) <= 4;
		boolean three = true;
		if (attackingArchon || blankShot) {
			three = false;
			five = false;
		}
		Direction toEnemy = rc.getLocation().directionTo(attackLoc);
		float distance = rc.getLocation().distanceTo(attackLoc);

		float r = (float) (RobotType.SOLDIER.bodyRadius * 2);
		// float rotation = r / distance;
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

}
