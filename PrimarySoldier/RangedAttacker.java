package PrimarySoldier;

import battlecode.common.*;

public abstract strictfp class RangedAttacker extends Bot {

	boolean defender;
	int steps;
	Movement mover;
	boolean reached;

	public RangedAttacker(RobotController rc) {
		super(rc);
		defender = false;
		mover = new Movement(rc);
		steps = 0;
		reached = false;
	}

	@Override
	public void run() throws GameActionException {
		BulletInfo[] bullets = rc.senseNearbyBullets();
		RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);
		RobotInfo[] allies = rc.senseNearbyRobots(myType.sensorRadius, myTeam);

		if (enemies.length > 0) {
			attackAndMove(allies, enemies, bullets);
		} else {
			if (defender) {
				defend();
			} else {

				if (!reached) {
					if (rc.getLocation().distanceTo(enemyLoc) <= 2) {
						reached = true;
					}
					mover.setDestination(enemyLoc);
					mover.move();
					if (!rc.hasMoved()) {
						this.moveInUnexploredDirection(0);
					}
				} else {
					if (!rc.hasMoved()) {
						this.moveInUnexploredDirection(0);
					}
				}
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

	private void attackAndMove(RobotInfo[] allies, RobotInfo[] enemies, BulletInfo[] bullets)
			throws GameActionException {

		RobotInfo toAttack = null;
		// Loop through all threatening enemies, find closest that i can shoot.
		for (RobotInfo enemy : enemies) {
			if (isHostile(enemy.type) && this.bulletPathIsClear(rc.getLocation(), enemy.location, enemy.type)) {
				toAttack = enemy;
				break;
			}
		}
		if (toAttack == null) {
			for (RobotInfo enemy : enemies) {
				if (!isHostile(enemy.type) && this.bulletPathIsClear(rc.getLocation(), enemy.location, enemy.type)) {
					toAttack = enemy;
					break;
				}
			}
		}
		Direction towardsEnemy = rc.getLocation().directionTo(toAttack.location);

		Direction moveDir = findMoveDir(toAttack);

		if (moveDir == null) {
			attack(toAttack);
			return;
		}

		float diff = towardsEnemy.degreesBetween(moveDir);
		if (diff > -90 && diff < 90) {
			rc.move(moveDir, myType.strideRadius);
			attack(toAttack);
		} else {
			attack(toAttack);
			rc.move(moveDir, myType.strideRadius);
		}

		// if didn't find one, loop through all enemies and find closest I can
		// shoot.
	}

	private Direction findMoveDir(RobotInfo toAttack) throws GameActionException {
		Direction towards = rc.getLocation().directionTo(toAttack.location);
		if (rc.getLocation().distanceTo(toAttack.location) <= myType.bodyRadius + 2 + toAttack.type.bodyRadius) {
			shoot(5, towards);
		}

		int bestScore = 0;
		Direction best = null;
		for (int i = 0; i < 7; i++) {
			int score = 100;
			towards = towards.rotateRightDegrees(45);
			MapLocation next = rc.getLocation().add(towards);
			if (!rc.canMove(towards, myType.strideRadius)) {
				continue;
			}
			if (toAttack.health <= 4 && next.distanceTo(toAttack.location) < 3) {
				score += 100;
			}
			if (toAttack.type == RobotType.LUMBERJACK) {
				score += next.distanceTo(toAttack.location);
			}
			if (this.isHostile(toAttack.type)) {
				if (i == 2 || i == 6) {
					score += 10;
				}
				if (i == 7 || i == 1) {
					score += 5;
				}
				if (i == 3 || i == 5) {
					score += 7;
				}
				if (rc.getHealth() <= 15) {
					score += next.distanceTo(toAttack.location);
				}
			} else {
				score -= next.distanceTo(toAttack.location);
			}
			if (score > bestScore) {
				bestScore = score;
				best = towards;
			}
		}
		return best;
	}

	private void attack(RobotInfo toAttack) throws GameActionException {
		Direction towards = rc.getLocation().directionTo(toAttack.location);
		if (rc.getLocation().distanceTo(toAttack.location) <= myType.bodyRadius + 2 + toAttack.type.bodyRadius) {
			shoot(5, towards);
		}
		if (isHostile(toAttack.type)) {
			shoot(3, towards);
		} else {
			shoot(1, towards);
		}
	}

	private void shoot(int type, Direction dir) throws GameActionException {
		if (type >= 5) {
			if (rc.canFirePentadShot()) {
				rc.firePentadShot(dir);
			}
		}
		if (type >= 3) {
			if (rc.canFireTriadShot()) {
				rc.fireTriadShot(dir);
			}
		}
		if (rc.canFireSingleShot()) {
			rc.fireSingleShot(dir);
		}
	}

	private boolean bulletPathIsClear(MapLocation source, MapLocation dest, RobotType type) throws GameActionException {
		Direction towards = source.directionTo(dest);
		Direction opposite = towards.opposite();
		while (source.distanceTo(dest) > type.bodyRadius) {
			rc.setIndicatorLine(source, source.add(towards), 0, 0, 200);
			if (rc.senseRobotAtLocation(source) != null && rc.senseTreeAtLocation(source) != null) {
				return false;
			}
			source = source.add(towards, 1);
		}
		return true;
	}

	private void defend() {

	}

	private void moveToEnemyLoc() throws GameActionException {
		if (steps > 75) {
			return;
		}
		this.makeMove(rc.getLocation().directionTo(this.enemyLoc));
		steps++;
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
