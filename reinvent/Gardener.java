package reinvent;

import battlecode.common.*;

public strictfp class Gardener extends Bot {
	private boolean stationary;
	private int buildCount;

	public Gardener(RobotController rc) throws GameActionException {
		super(rc);
		stationary = false;
		buildCount = 0;
	}

	@Override
	public void run() throws GameActionException {
		ping();
		boolean shouldBuild = buildEarlyUnits();
		if (!rc.hasMoved()) {
			stayAlive();
		}
		if (shouldBuild) {
			build();
		}
		waterTrees();
		shake();
	}

	private Direction getDirAwayFromWall() throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		if (!rc.onTheMap(myLoc.add(Direction.EAST, 5))) {
			return Direction.WEST;
		}
		if (!rc.onTheMap(myLoc.add(Direction.WEST, 5))) {
			return Direction.EAST;
		}
		if (!rc.onTheMap(myLoc.add(Direction.NORTH, 5))) {
			return Direction.SOUTH;
		}
		if (!rc.onTheMap(myLoc.add(Direction.SOUTH, 5))) {
			return Direction.NORTH;
		}
		return this.getRandomDirection();
	}

	private boolean buildEarlyUnits() throws GameActionException {
		if (buildCount < 2 || !rc.onTheMap(rc.getLocation(), 4)) {
			if (this.countNearbyOpenSquares(true) <= 4) {
				Direction awayFromWall = getDirAwayFromWall();
				if (awayFromWall != null) {
					this.makeMove(awayFromWall);
					this.unexploredDir = this.getRandomDirection();
				}
			}
		}
		if (buildCount == 0) {
			this.buildUnit(RobotType.SCOUT);
			return false;
		} else if (buildCount == 1) {
			this.buildUnit(RobotType.LUMBERJACK);
			return false;
		} else {
			RobotInfo[] allies = rc.senseNearbyRobots(myType.sensorRadius);
			boolean hasLumberjack = false;
			for (RobotInfo ally : allies) {
				if (ally.type == RobotType.LUMBERJACK) {
					hasLumberjack = true;
					break;
				}
			}
			if (!hasLumberjack) {
				this.buildUnit(RobotType.LUMBERJACK);
				return false;
			}
		}
		return true;
	}

	// figure out how to fix this method. I think we need to water the tree
	// right on the edge of it?
	// It's radius is 1, so water the edge? maybe need to move, but the other
	// one wasn't doing that.
	private void waterTrees() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees(3, myTeam);
		System.out.println("Amount of trees: " + trees.length);
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

	private void build() throws GameActionException {
		RobotInfo[] teammates = rc.senseNearbyRobots(myType.sensorRadius, myTeam);
		RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);
		int allyCount = 0;
		int enemyCount = 0;
		for (RobotInfo t : teammates) {
			if (t.type == RobotType.LUMBERJACK) {
				allyCount++;
			}
		}
		for (RobotInfo e : enemies) {
			if (e.type != RobotType.ARCHON && e.type != RobotType.LUMBERJACK) {
				enemyCount++;
			}
		}
		if (enemyCount >= allyCount) {
			buildUnit(RobotType.LUMBERJACK);
			return;
		}
		int openSquares = countNearbyOpenSquares(true);
		System.out.println("Counted this many open squares: " + openSquares);
		if (openSquares > 1) {
			if (rand.nextBoolean()) {
				this.plantTree();
			}
		}
		if (rc.isBuildReady() && rc.hasRobotBuildRequirements(RobotType.LUMBERJACK)) {
			buildUnit(RobotType.LUMBERJACK);
		}
	}

	private void buildUnit(RobotType type) throws GameActionException {
		if (rc.getTeamBullets() < 100) {
			return;
		}
		if (rc.hasRobotBuildRequirements(type)) {
			Direction dir = rc.getLocation().directionTo(enemyLoc);
			for (int i = 0; i < 8; i++) {
				if (rc.canBuildRobot(type, dir)) {
					rc.buildRobot(type, dir);
					buildCount++;
					break;
				}
				dir = dir.rotateRightDegrees(45);
			}
		}
	}

	private void plantTree() throws GameActionException {
		Direction initial = Direction.getNorth();
		for (int i = 0; i < 6; i++) {
			int rotation = 360 / 6;
			Direction rotated = initial.rotateRightDegrees(rotation * i);
			if (rc.canPlantTree(rotated)) {
				rc.plantTree(rotated);
				break;
			}
		}
	}

	private int countNearbyOpenSquares(boolean stationary) throws GameActionException {
		int count = 0;
		Direction initial = Direction.getNorth();
		for (int i = 0; i < 6; i++) {
			int rotation = 360 / 6;
			Direction rotated = initial.rotateRightDegrees(rotation * i);
			MapLocation loc = rc.getLocation().add(rotated);
			if (stationary) {
				if (rc.canPlantTree(rotated)) {
					count++;
					continue;
				}
			} else {
				if (rc.canPlantTree(rotated) || rc.senseRobotAtLocation(loc) != null) {
					count++;
					continue;
				}
			}
		}
		return count;
	}

	// every 40 rounds send a ping, so that a
	// friendly archon knows how many gardeners are current alive.
	private void ping() throws GameActionException {
		if (rc.getRoundNum() % Constants.GARDNER_PING_RATE != 0) {
			return;
		}
		int currentPing = rc.readBroadcast(Channels.GARDENER_PING_CHANNEL);
		if (currentPing <= 0) {
			rc.broadcast(Channels.GARDENER_PING_CHANNEL, 1);
		} else {
			rc.broadcast(Channels.GARDENER_PING_CHANNEL, currentPing + 1);
		}
	}
}
