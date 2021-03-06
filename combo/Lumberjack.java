package combo;

import battlecode.common.BulletInfo;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public strictfp class Lumberjack extends Bot {

	private boolean attacking;
	private int roundsAlive;

	public Lumberjack(RobotController rc) {
		super(rc);
		RobotInfo[] allies = rc.senseNearbyRobots(3, myTeam);
		for (RobotInfo ally : allies) {
			if (ally.type == RobotType.GARDENER) {
				home = ally.location;
				break;
			}
		}
		if (home == null) {
			home = rc.getLocation();
		}
		attacking = false;
		roundsAlive = 0;

	}

	@Override
	public void run() throws GameActionException {
		roundsAlive++;

		if (roundsAlive >= 300) {
			attacking = true;
		}

		// Have destination, need to decide if should attack before or after
		// moving.
		// Need to decide if should chop or attack
		// if home.equals destination, should chop trees if run into them.

		RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);
		TreeInfo[] neutralTrees = rc.senseNearbyTrees(myType.sensorRadius, Team.NEUTRAL);
		BulletInfo[] bullets = rc.senseNearbyBullets(myType.bulletSightRadius);

		finalChop(neutralTrees);
		MapLocation[] locs = this.getSafestLocs(bullets, enemies, 6000);
		if (!rc.hasMoved()) {
			micro(locs, enemies, bullets);
		}
		if (!rc.hasMoved()) {
			moveTowardsEnemy(locs, enemies, bullets);
		}
		if (!rc.hasMoved()) {
			moveTowardsNeutralTree(neutralTrees);
		}
		if (!attacking && !rc.hasMoved()) {
			TreeInfo[] nearbyNeutral = rc.senseNearbyTrees(
					(float) (myType.bodyRadius + GameConstants.INTERACTION_DIST_FROM_EDGE - 0.1), Team.NEUTRAL);
			moveTowardsHome(nearbyNeutral);
		}
		if (attacking && !rc.hasMoved()) {
			moveToEnemyLoc();
			if (!rc.hasMoved()) {
				this.moveInUnexploredDirection(false);
			}
		}
		shake(neutralTrees);
		if (!rc.hasAttacked()) {
			chop(neutralTrees);
		}
	}

	private void finalChop(TreeInfo[] trees) throws GameActionException {
		for (TreeInfo tree : trees) {
			if (!rc.canChop(tree.ID)) {
				return;
			}
			if (rc.canChop(tree.ID) && tree.health <= GameConstants.LUMBERJACK_CHOP_DAMAGE
					&& tree.containedRobot != null) {
				rc.chop(tree.ID);
			}
		}
	}

	private void moveTowardsEnemy(MapLocation[] locs, RobotInfo[] enemies, BulletInfo[] bullets)
			throws GameActionException {
		if (enemies.length == 0) {
			return;
		}
		MapLocation enemy = enemies[0].location;
		MapLocation bestLoc;
		if (locs.length != 0) {
			bestLoc = locs[Math.abs(rand.nextInt(locs.length))];
		} else {
			bestLoc = rc.getLocation().add(directions[rand.nextInt(directions.length)]);
		}
		for (MapLocation next : locs) {
			if (next.distanceTo(enemy) < bestLoc.distanceTo(enemy)) {
				bestLoc = next;
			}
		}
		if (bestLoc == null) {
			this.makeMove(rc.getLocation().directionTo(enemy));
		} else {
			if (rc.canMove(bestLoc)) {
				rc.move(bestLoc);
			}
		}
	}

	private void moveTowardsNeutralTree(TreeInfo[] trees) throws GameActionException {
		for (int i = 0; i < trees.length; i++) {
			TreeInfo tree = trees[i];
			if (tree.containedRobot != null && tree.containedRobot != RobotType.ARCHON) {
				this.makeMove(rc.getLocation().directionTo(tree.location));
				break;
			}
		}
	}

	private void moveTowardsHome(TreeInfo[] trees) throws GameActionException {
		if (trees.length == 0) {
			this.moveTowards(home);
		} else {
			roundsAlive = 1;
		}
	}

	private void chop(TreeInfo[] trees) throws GameActionException {
		TreeInfo toChop = null;
		for (TreeInfo tree : trees) {
			if (rc.canChop(tree.ID)) {
				if (toChop == null) {
					toChop = tree;
				} else if (tree.containedRobot != null) {
					toChop = tree;
					break;
				}
			}
		}
		if (toChop != null) {
			rc.chop(toChop.ID);
		}
	}

	private boolean micro(MapLocation[] locs, RobotInfo[] enemies, BulletInfo[] bullets) throws GameActionException {
		if (enemies.length == 0) {
			return true;
		}
		int highestScore = -1000;
		float bestDist = 100000;
		Direction next = null;
		for (MapLocation loc : locs) {
			float distToHome = loc.distanceTo(home);
			Direction dir = rc.getLocation().directionTo(loc);
			Integer score = getScore(dir);
			if (score == null || score <= 0) {
				continue;
			}
			if (score > highestScore) {
				highestScore = score;
				bestDist = distToHome;
				next = dir;
			} else if (score == highestScore && distToHome < bestDist) {
				bestDist = distToHome;
				next = dir;
			}
		}
		if (next == null) {
			return true;
		}
		int nextScore = highestScore;
		int curScore = getScore(null);
		float dist = rc.getLocation().distanceTo(home);

		// System.out.println("Moving this way: " + next);
		// goal here is to move to location with best score.
		// attack before moving if current location has better score.
		// move then attack if new location has best score.
		// if there is an equal score, then move only if the next location is
		// closer to home
		if (nextScore == curScore) {
			if (dist > bestDist) {
				rc.move(next);
			}
			if (rc.canStrike()) {
				strike(nextScore);
			}
			return false;
		} else if (nextScore > curScore) {
			rc.move(next);
			if (rc.canStrike()) {
				strike(nextScore);
			}
		} else {
			if (rc.canStrike()) {
				strike(curScore);
			}
			rc.move(next);
		}
		return true;
	}

	private void strike(Integer score) throws GameActionException {
		if (score < 0 && rc.getRoundLimit() - rc.getRoundNum() > 200) {
			return;
		}
		if (rc.canStrike()) {
			rc.strike();
		}
	}

	void safeMove(Direction nextDir) throws GameActionException {
		if (nextDir == null) {
			return;
		}
		rc.setIndicatorLine(rc.getLocation(), rc.getLocation().add(nextDir), 200, 0, 0);
		if (rc.canMove(nextDir)) {
			rc.move(nextDir);
		} else {
			this.makeMove(nextDir);
		}
	}

	private Integer getScore(Direction dir) {
		if (dir != null && !rc.canMove(dir, myType.strideRadius)) {
			return null;
		}
		MapLocation loc = rc.getLocation();
		if (dir != null) {
			loc = loc.add(dir, myType.strideRadius);
		}
		RobotInfo[] allies = rc.senseNearbyRobots(loc, myType.bodyRadius + 1, myTeam);
		RobotInfo[] enemies = rc.senseNearbyRobots(loc, myType.bodyRadius + 1, enemyTeam);
		int score = 0;
		if (enemies.length > 0) {
			score++;
		}
		score += enemies.length;
		score -= allies.length;
		return score;
	}

}
