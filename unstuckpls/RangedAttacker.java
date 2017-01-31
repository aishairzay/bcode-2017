package unstuckpls;

import battlecode.common.*;
import unstuckpls.Channels;
import unstuckpls.Constants;

public abstract strictfp class RangedAttacker extends Bot {

	boolean defender;
	boolean reachedEnemyLoc;
	boolean[] archonLocReached;
	MapLocation lastAttackLoc;
	int lastAttackRound;
	MapLocation globalEnemyLoc;
	private int roundsAlive;
	private MapLocation localEnemyLoc;
	private int archonAssignedLimit;
	boolean assignedToArchon;
	private int gardenerCount;

	public RangedAttacker(RobotController rc) {
		super(rc);
		defender = false;
		reachedEnemyLoc = false;
		archonLocReached = new boolean[this.allyArchons.length];
		assignedToArchon = false;
		roundsAlive = 0;
		archonAssignedLimit = 75;
	}

	public void getGardenerCount() throws GameActionException {
		if ((rc.getRoundNum() % Constants.GARDENER_PING_RATE) == 1) {
			gardenerCount = rc.readBroadcast(Channels.GARDENER_PING_CHANNEL);
		}
	}

	private MapLocation getDestination() throws GameActionException {
		MapLocation dest = null;
		if (enemyLoc != null && rc.getLocation().distanceTo(this.enemyLoc) <= myType.sensorRadius) {
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
		if (localEnemyLoc != null && rc.getLocation().distanceTo(localEnemyLoc) <= 3) {
			localEnemyLoc = null;
		}
		if (globalEnemyLoc != null && rc.getLocation().distanceTo(globalEnemyLoc) <= 5) {
			globalEnemyLoc = null;
			rc.broadcast(Channels.GLOBAL_ENEMY_LOC, 0);
			rc.broadcast(Channels.GLOBAL_ENEMY_LOC + 1, 0);
		}
		if (closestArchonLoc != null && globalEnemyLoc != null
				&& rc.getLocation().distanceTo(globalEnemyLoc) <= rc.getLocation().distanceTo(closestArchonLoc)) {
			closestArchonLoc = globalEnemyLoc;
		}
		if (closestArchonLoc != null) {
			dest = closestArchonLoc;
		} else if (this.globalEnemyLoc != null) {
			dest = globalEnemyLoc;
		} else if (localEnemyLoc != null) {
			dest = localEnemyLoc;
		} else if (!reachedEnemyLoc) {
			dest = enemyLoc;
		}
		return dest;
	}

	private void readGlobalEnemyLoc() throws GameActionException {
		if (roundsAlive % 10 != 2) {
			return;
		}
		this.globalEnemyLoc = Helper.getLocation(rc, Channels.GLOBAL_ENEMY_LOC);
		this.gardenerCount = 0;
	}

	@Override
	public void run() throws GameActionException {
		roundsAlive++;
		ping();
		readGlobalEnemyLoc();
		this.getGardenerCount();
		BulletInfo[] bullets = rc.senseNearbyBullets();
		RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);
		RobotInfo[] allies = rc.senseNearbyRobots(myType.sensorRadius, myTeam);
		MapLocation[] locs = this.getSafestLocs(bullets, enemies, 10000);

		MapLocation destination = getDestination();

		if (rc.getRoundNum() >= 400) {
			for (int i = 0; i < enemies.length; i++) {
				RobotInfo enemy = enemies[i];
				if (enemy.type == RobotType.ARCHON) {
					int id = enemy.ID;
					int lastRound = rc.readBroadcast(Channels.getIdChannel(id));
					if ((rc.getRoundNum() - lastRound >= 10 || assignedToArchon)
							&& rc.getRoundNum() >= archonAssignedLimit) {
						assignedToArchon = true;
						rc.broadcast(Channels.getIdChannel(id), rc.getRoundNum());
						break;
					}
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
			if (closest.type != RobotType.ARCHON) {
				localEnemyLoc = closest.location;
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
		if (bullets.length >= 0 && (inDanger || inSecondaryDanger)) {
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
		int hostileEnemyCount = 0;
		for (RobotInfo enemy : enemies) {
			if (Helper.isHostile(enemy.type) || enemy.type.equals(RobotType.SCOUT)) {
				hostileEnemyCount++;
			}
		}

		RobotInfo lastSeenClosest = closest;

		boolean dontMove = false;
		if (attack && hostileEnemyCount == 0 && closest != null) {
			MapLocation before = rc.getLocation();
			float touchDistance = closest.type.bodyRadius + myType.bodyRadius;
			if (before.distanceTo(closest.location) > 2) {
				this.moveTowards(closest.location);
				MapLocation after = rc.getLocation();
				if (after.distanceTo(closest.location) >= touchDistance + 2
						&& before.distanceTo(closest.location) <= touchDistance + 2) {
					System.out.println("Hard reset my bugging");
					this.hardResetBug();
				}
			} else {
				dontMove = true;
			}
		} else if (closest != null && closest.type == RobotType.LUMBERJACK
				&& !this.bulletPathClear(rc.getLocation(), closest)) {
			if (destination != null) {
				this.moveTowards(destination);
			} else {
				this.moveInUnexploredDirection(0);
			}
		} else if (length > 0) {
			System.out.println("Microing.");
			if (!this.shouldStayStill) {
				boolean defendingGardener = false;
				for (RobotInfo ally : allies) {
					if (ally.type == RobotType.GARDENER) {
						defendingGardener = true;
					}
				}
				if (defendingGardener) {
					MapLocation bugLoc = this.getNextBugLoc(closest.location);
					MapLocation best = null;
					if (bugLoc == null) {
						best = locs[rand.nextInt(locs.length)];
					} else {
						best = locs[0];
						for (MapLocation next : locs) {
							if (next.distanceTo(bugLoc) <= best.distanceTo(bugLoc)) {
								best = next;
							}
						}
					}
					rc.move(best);
				} else {
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
				}
			}
		} else if (bullets.length > 0 && inDanger) {
			if (!this.shouldStayStill) {
				if (destination == null) {
					bestLoc = locs[rand.nextInt(locs.length)];
				} else {
					for (MapLocation next : locs) {
						if (lastSeenClosest != null && runSideways) {
							Direction away = next.directionTo(lastSeenClosest.location);
							Direction dir = rc.getLocation().directionTo(next);
							float diff = 180 - Math.abs(away.degreesBetween(dir));
							Direction bestAway = bestLoc.directionTo(lastSeenClosest.location);
							float bestDiff = 180 - Math.abs(bestAway.degreesBetween(dir));
							if (diff > bestDiff) {
								bestLoc = next;
							}
						} else if (next.distanceTo(destination) < bestLoc.distanceTo(destination)) {
							bestLoc = next;
						}
					}
				}
			}
			System.out.println("Used dodge: bestloc is: " + bestLoc);

		} else {
			if (destination != null) {
				System.out.println("Moving towards destination now");
				this.moveTowards(destination);
			} else {
				this.moveInUnexploredDirection(false);
			}
		}
		if (destination != null) {
			rc.setIndicatorLine(rc.getLocation(), destination, 100, 100, 100);
		}

		attackingArchon = false;
		archonToAttack = null;
		MapLocation toAttack = this.getToAttack(enemies);
		float enemyHealth = 0;
		if (closest != null && (closest.type == RobotType.SOLDIER || closest.type == RobotType.TANK)) {
			enemyHealth = closest.health;
		}

		if (rc.hasMoved() && toAttack != null || (toAttack != null && dontMove)) {
			this.attack(toAttack, attackingArchon, blankShot, enemies, enemyHealth);
		}

		if (toAttack == null && bestLoc == null) {
			// do nothing
		}
		if (!this.shouldStayStill && !dontMove && toAttack != null && bestLoc != null && !rc.hasMoved()) {
			Direction attackDir = rc.getLocation().directionTo(toAttack);
			Direction moveDir = rc.getLocation().directionTo(bestLoc);
			float diff = Math.abs(attackDir.degreesBetween(moveDir));
			if (diff <= 90) {
				rc.move(bestLoc);
				attack(toAttack, attackingArchon, blankShot, enemies, enemyHealth);
			} else {
				attack(toAttack, attackingArchon, blankShot, enemies, enemyHealth);
				rc.move(bestLoc);
			}
		}
		if (!this.shouldStayStill && !dontMove && toAttack == null && bestLoc != null && !rc.hasMoved()) {
			rc.move(bestLoc);
		}
		if (!rc.hasAttacked()) {
			MapLocation afterMoveAttack = this.getToAttack(rc.senseNearbyRobots(myType.sensorRadius, enemyTeam));
			this.attack(afterMoveAttack, attackingArchon, blankShot, enemies, enemyHealth);
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

	private void attack(MapLocation attackLoc, boolean attackingArchon, boolean blankShot, RobotInfo[] enemies,
			float enemyHealth) throws GameActionException {
		if (attackLoc == null) {
			return;
		}
		if (!blankShot) {
			this.lastAttackRound = rc.getRoundNum();
		}
		int hostileEnemyCount = 0;
		for (RobotInfo enemy : enemies) {
			if (Helper.isHostile(enemy.type)) {
				hostileEnemyCount++;
			}
		}
		this.lastAttackLoc = attackLoc;
		boolean five = rc.getLocation().distanceTo(attackLoc) <= 5.0;
		boolean three = rc.getLocation().distanceTo(attackLoc) <= 6;
		if (attackingArchon && rc.getRoundNum() <= 300) {
			return;
		}
		if (attackingArchon || shootSingles || blankShot) {
			three = false;
			five = false;
		}
		if (this.gardenerCount == 0) {
			five = true;
			three = true;
		}
		Direction toEnemy = rc.getLocation().directionTo(attackLoc);
		float distance = rc.getLocation().distanceTo(attackLoc);

		float r = (float) (RobotType.SOLDIER.bodyRadius * 1.5);
		if (blankShot && hostileEnemyCount == 0) {
			r = RobotType.SOLDIER.bodyRadius;
		}
		if (rand.nextBoolean()) {
			float rotation = distance / r;
			if (attackRotationDirectionIsRight) {
				toEnemy = toEnemy.rotateLeftDegrees(rotation);
			} else {
				toEnemy = toEnemy.rotateRightDegrees(rotation);
			}
			attackRotationDirectionIsRight = !attackRotationDirectionIsRight;
		}
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
