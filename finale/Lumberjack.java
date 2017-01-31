package finale;

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
	private int steps;

	public Lumberjack(RobotController rc) {
		super(rc);
		home = rc.getLocation();
		attacking = false;
		roundsAlive = 0;
		steps = 0;
	}

	@Override
	public void run() throws GameActionException {
		roundsAlive++;
		if (!attacking && roundsAlive > 175 && rc.getRoundNum() % 100 == 0) {
			attacking = true;
		}
		TreeInfo[] trees = rc.senseNearbyTrees(myType.sensorRadius, Team.NEUTRAL);
		RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);
		boolean shouldMove = false;
		finalChop(trees);
		if (!rc.hasMoved()) {
			shouldMove = micro(enemies);
		}
		if (shouldMove && !rc.hasMoved()) {
			moveTowardsEnemy(enemies);
		}
		if (shouldMove && !rc.hasMoved()) {
			moveTowardsNeutralTree(trees);
		}
		if (!attacking && shouldMove && !rc.hasMoved()) {
			moveTowardsHome();
		}
		if (attacking && shouldMove && !rc.hasMoved()) {
			moveToEnemyLoc();
			if (!rc.hasMoved()) {
				this.moveInUnexploredDirection(0);
			}
		}
		if (!rc.hasAttacked()) {
			chop(trees);
		}
		shake(rc.senseNearbyTrees(3, Team.NEUTRAL));
	}

	private void finalChop(TreeInfo[] trees) throws GameActionException {
		for (TreeInfo tree : trees) {
			if (rc.canChop(tree.ID) && tree.health <= GameConstants.LUMBERJACK_CHOP_DAMAGE
					&& tree.containedRobot != null) {
				rc.chop(tree.ID);
			}
		}
	}

	protected void moveToEnemyLoc() throws GameActionException {
		if (steps > 75) {
			return;
		}
		this.makeMove(rc.getLocation().directionTo(this.enemyLoc));
		steps++;
	}

	private void moveTowardsEnemy(RobotInfo[] enemies) throws GameActionException {
		if (enemies.length == 0) {
			return;
		}
		RobotInfo enemy = enemies[0];
		this.makeMove(rc.getLocation().directionTo(enemy.location));
	}

	private void moveTowardsNeutralTree(TreeInfo[] trees) throws GameActionException {
		if (trees.length == 0) {
			return;
		}
		TreeInfo first = trees[0];
		if (home.distanceTo(first.location) > RobotType.LUMBERJACK.sensorRadius) {
			for (TreeInfo tree : trees) {
				if (tree.containedRobot != null) {
					this.makeMove(rc.getLocation().directionTo(first.location));
				}
			}
			return;
		}
		this.makeMove(rc.getLocation().directionTo(first.location));
	}

	private void moveTowardsHome() throws GameActionException {
		float dist = rc.getLocation().distanceTo(home);
		RobotInfo[] allies = rc.senseNearbyRobots(myType.sensorRadius, myTeam);
		MapLocation closest = null;
		for (RobotInfo ally : allies) {
			if (ally.type != RobotType.GARDENER) {
				continue;
			}
			if (closest == null) {
				closest = ally.location;
				break;
			}
		}
		if (closest != null && rc.getLocation().distanceTo(closest) < 5) {
			moveInUnexploredDirection(0);
		}
		if (dist <= 5) {
			moveInUnexploredDirection(0);
		}
		if (!rc.canSenseLocation(home) || dist > 5) {
			this.makeMove(rc.getLocation().directionTo(home));
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

	private boolean micro(RobotInfo[] enemies) throws GameActionException {
		if (enemies.length == 0) {
			return true;
		}
		Direction north = Direction.getNorth();
		int turns = 16;
		int highestScore = -1000;
		float bestDist = 100000;
		Direction next = null;
		for (int i = 0; i < turns; i++) {
			float rotation = i * (360 / turns);
			Direction dir = north.rotateRightDegrees(rotation);
			MapLocation loc = rc.getLocation().add(dir);
			float distToHome = loc.distanceTo(home);
			Integer score = getScore(dir);
			// System.out.println("Got score : " + i + ", " + score);
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
		// System.out.println("ally length: " + allies.length);
		// System.out.println("enemy length: " + enemies.length);
		score += enemies.length;
		score -= allies.length;
		// System.out.println("Got score: " + score);
		return score;
	}

}
