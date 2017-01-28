package yeezus;

import battlecode.common.*;

public strictfp class Gardener extends Bot {

	private boolean needLumberjack;
	private boolean needSoldier;
	private int rangedCount;
	private boolean stationary;
	private boolean first;

	Direction[] plantingDirections;

	public Gardener(RobotController rc) throws GameActionException {
		super(rc);
		rangedCount = 1000;
		plantingDirections = new Direction[6];
		Direction initial = Direction.getNorth();
		for (int i = 0; i < 6; i++) {
			int rotation = 360 / 6;
			Direction rotated = initial.rotateRightDegrees(rotation * i);
			plantingDirections[i] = rotated;
		}
		needLumberjack = true;
		needSoldier = true;
		stationary = false;
		if (rc.getRoundNum() <= 10) {
			first = true;
			stationary = true;
		}
	}

	@Override
	public void run() throws GameActionException {
		ping();

		TreeInfo[] neutralTrees = rc.senseNearbyTrees(myType.sensorRadius, Team.NEUTRAL);
		RobotInfo[] allies = rc.senseNearbyRobots(myType.sensorRadius, myTeam);
		RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);

		build(neutralTrees, allies, enemies);

		waterTrees();
		shake(rc.senseNearbyTrees(3, myTeam));
	}

	private void setStationary(RobotInfo[] allies) throws GameActionException {
		for (RobotInfo ally : allies) {
			if ((ally.type == RobotType.GARDENER || ally.type == RobotType.ARCHON)
					&& rc.getLocation().distanceTo(ally.location) < 6) {
				return;
			}
		}
		int neutral = this.countBlockingNeutralTrees();
		int open = this.countNearbyOpenSquares();
		int totalSquares = neutral + open;
		if (totalSquares >= 5) {
			stationary = true;
		}
	}

	private void move() throws GameActionException {
		Direction toMove = getDirAwayFromWall();
		if (toMove != null) {
			MapLocation destination = this.getBugDest();
			if (rc.getLocation().distanceTo(destination) >= 80) {
				this.moveTowards(destination);
			} else {
				this.moveTowards(rc.getLocation().add(toMove, 100));
			}
		} else {
			this.moveInUnexploredDirection(0);
		}
	}

	// figure out how to fix this method. I think we need to water the tree
	// right on the edge of it?
	// It's radius is 1, so water the edge? maybe need to move, but the other
	// one wasn't doing that.
	private void waterTrees() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees(3, myTeam);
		TreeInfo lowestHealth = null;
		for (TreeInfo tree : trees) {
			if (rc.canWater(tree.ID)) {
				if (lowestHealth == null) {
					lowestHealth = tree;
				} else if (tree.health < lowestHealth.health) {
					lowestHealth = tree;
				}
			}
		}
		if (lowestHealth != null) {
			rc.water(lowestHealth.ID);
		}

	}

	private void build(TreeInfo[] neutralTrees, RobotInfo[] allies, RobotInfo[] enemies) throws GameActionException {
		int allyCount = 0, enemyCount = 0, rangedCount = 0;
		for (RobotInfo enemy : enemies) {
			if (Helper.isHostile(enemy.type)) {
				enemyCount++;
			}
		}
		for (RobotInfo ally : allies) {
			if (Helper.isHostile(ally.type) && ally.type != RobotType.SCOUT) {
				allyCount++;
				if (ally.type == RobotType.SOLDIER || ally.type == RobotType.TANK) {
					rangedCount++;
				}
			}
		}
		int countNeutralTrees = countBlockingNeutralTrees();
		if (countNeutralTrees >= 1 && !needLumberjack) {
			boolean foundLumberjack = false;
			for (RobotInfo ally : allies) {
				if (ally.type.equals(RobotType.LUMBERJACK)) {
					foundLumberjack = true;
					break;
				}
			}
			if (!foundLumberjack) {
				needLumberjack = true;
			}
		}
		if (neutralTrees.length >= 1 && needLumberjack) {
			if (this.buildUnit(RobotType.LUMBERJACK)) {
				needLumberjack = false;
			}
			return;
		} else if (neutralTrees.length == 0 && needLumberjack && first) {
			if (this.buildUnit(RobotType.SOLDIER)) {
				needLumberjack = false;
			}
		}
		if (needSoldier && first) {
			if (this.buildUnit(RobotType.SOLDIER)) {
				needSoldier = false;
			}
			return;
		}
		boolean shouldBuildUnit = enemyCount > 0 && enemyCount >= allyCount;
		if (shouldBuildUnit) {
			this.buildUnit(RobotType.SOLDIER);
		}
		int openSquares = countNearbyOpenSquares();
		if (this.rangedCount <= 1) {
			this.buildUnit(RobotType.SOLDIER);
		} else if (openSquares > 1) {
			this.plantTree();
		} else if (this.rangedCount >= Constants.MAX_RANGED_COUNT) {
			float extraBullets = (rc.getTeamBullets() - 100);
			if (extraBullets > 0) {
				float toDonate = ((int) (extraBullets / rc.getVictoryPointCost()) * rc.getVictoryPointCost());
				rc.donate(toDonate);
			}
		} else {
			if (rangedCount > 2) {
				return;
			}
			this.buildUnit(RobotType.TANK);
			this.buildUnit(RobotType.SOLDIER);
		}
	}

	private int countBlockingNeutralTrees() throws GameActionException {
		int count = 0;
		Direction initial = Direction.getNorth();
		for (int i = 0; i < 6; i++) {
			int rotation = 360 / 6;
			Direction rotated = initial.rotateRightDegrees(rotation * i);
			MapLocation loc = rc.getLocation().add(rotated);
			TreeInfo tree = rc.senseTreeAtLocation(loc);
			if (tree != null && tree.team.equals(Team.NEUTRAL)) {
				count++;
			}
		}
		return count;
	}

	private boolean buildUnit(RobotType type) throws GameActionException {
		if (rc.getTeamBullets() < 100 || !rc.isBuildReady()) {
			return false;
		}
		if (rc.hasRobotBuildRequirements(type)) {
			Direction dir = rc.getLocation().directionTo(enemyLoc);
			for (int i = 0; i < 8; i++) {
				if (rc.canBuildRobot(type, dir)) {
					rc.buildRobot(type, dir);
					return true;
				}
				dir = dir.rotateRightDegrees(45);
			}
			int initial = rand.nextInt(6);
			for (int i = 0; i < 6; i++) {
				Direction rotated = plantingDirections[(initial + i) % 6];
				if (rc.canBuildRobot(type, rotated)) {
					rc.buildRobot(type, rotated);
					return true;
				}
			}
		}
		return false;
	}

	private void plantTree() throws GameActionException {
		int initial = rand.nextInt(6);
		for (int i = 0; i < 6; i++) {
			Direction rotated = plantingDirections[(initial + i) % 6];
			if (rc.canPlantTree(rotated)) {
				rc.plantTree(rotated);
				break;
			}
		}
	}

	private int countNearbyOpenSquares() throws GameActionException {
		int count = 0;
		Direction initial = Direction.getNorth();
		for (int i = 0; i < 6; i++) {
			int rotation = 360 / 6;
			Direction rotated = initial.rotateRightDegrees(rotation * i);
			MapLocation loc = rc.getLocation().add(rotated, (float) (myType.bodyRadius + 0.1));
			if (rc.canPlantTree(rotated) || rc.senseRobotAtLocation(loc) != null) {
				count++;
			}
		}
		return count;
	}

	// every 40 rounds send a ping, so that a
	// friendly archon knows how many gardeners are current alive.
	private void ping() throws GameActionException {
		if (rc.getRoundNum() % Constants.GARDENER_PING_RATE != 0) {
			return;
		}
		int currentPing = rc.readBroadcast(Channels.GARDENER_PING_CHANNEL);
		if (currentPing <= 0) {
			rc.broadcast(Channels.GARDENER_PING_CHANNEL, 1);
		} else {
			rc.broadcast(Channels.GARDENER_PING_CHANNEL, currentPing + 1);
		}

		if ((rc.getRoundNum() % Constants.RANGED_PING_RATE) == 1) {
			rangedCount = rc.readBroadcast(Channels.RANGED_PING_CHANNEL);
		} else if (rc.getRoundNum() % Constants.RANGED_PING_RATE == 2) {
			rc.broadcast(Channels.RANGED_PING_CHANNEL, -1);
		}
	}
}
