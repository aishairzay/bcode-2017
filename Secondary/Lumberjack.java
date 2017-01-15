package Secondary;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public strictfp class Lumberjack extends Bot {

	public Lumberjack(RobotController rc) {
		super(rc);
		home = rc.getLocation();
	}

	@Override
	public void run() throws GameActionException {
		micro();
		chop();
		shake();
	}

	private void chop() {

	}

	private void micro() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);
		Direction toMove = null;

		if (toMove == null) {
			toMove = moveTowardsClosestEnemy(enemies);
		}
		Direction nextDir = getMoveDir(toMove);
		MapLocation nextLoc = rc.getLocation().add(nextDir);

		int curScore = getScore(rc.getLocation());
		int nextScore = this.getScore(nextLoc);

		if ((nextScore <= 0 && curScore <= 0) || (nextScore >= curScore)) {
			rc.move(nextDir);
			if (nextScore >= 0) {
				rc.strike();
			}
		} else {
			rc.strike();
			rc.move(nextDir);
		}

		if (!rc.hasMoved()) {
			this.moveInUnexploredDirection(0);
			if (this.getScore(rc.getLocation()) > 0) {
				rc.strike();
			}
		}
	}

	private int getScore(MapLocation loc) {
		RobotInfo[] allies = rc.senseNearbyRobots(loc, 1, myTeam);
		RobotInfo[] enemies = rc.senseNearbyRobots(loc, 1, enemyTeam);
		int score = 0;
		score += allies.length - enemies.length;
		return score;
	}

	private Direction getMoveDir(Direction toMove) {
		int rotation = 30;
		boolean toggle = rand.nextBoolean();
		Direction newDirection = toMove;
		Direction bestDirection = null;
		if (rc.canMove(toMove)) {
			bestDirection = toMove;
		}
		int bestDirectionScore = 0;
		for (int i = 0; i < 7; i++) {
			if (rc.canMove(toMove) && bestDirection == null) {
				bestDirection = toMove;
				bestDirectionScore = getScore(rc.getLocation().add(bestDirection));
			}
			int newScore = getScore(rc.getLocation().add(newDirection));
			if (newScore > bestDirectionScore) {
				bestDirectionScore = newScore;
				bestDirection = toMove;
			}
			if (toggle) {
				newDirection = toMove.rotateRightDegrees(rotation);
			} else {
				newDirection = toMove.rotateLeftDegrees(rotation);
			}
			if (i % 2 == 1) {
				rotation += 30;
			}
		}
		return bestDirection;
	}

	private RobotInfo findClosestEnemy(RobotInfo[] enemies) {
		RobotInfo closest = null;
		for (int i = 0; i < enemies.length; i++) {
			RobotInfo bot = enemies[i];
			if (closest == null) {
				closest = bot;
			}
			if (rc.getLocation().distanceTo(bot.location) < rc.getLocation().distanceTo(closest.location)) {
				closest = bot;
			}
		}
		return null;
	}

	private Direction moveTowardsClosestEnemy(RobotInfo[] enemies) {
		RobotInfo closest = findClosestEnemy(enemies);
		return rc.getLocation().directionTo(closest.location);
	}

}
