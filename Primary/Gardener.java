package Primary;

import battlecode.common.*;

public strictfp class Gardener extends Bot {
	private static final int MIN_MOVES = 8;
	private boolean stationary;
	private int steps;

	public Gardener(RobotController rc) {
		super(rc);
		stationary = false;
		steps = 0;
	}

	@Override
	public void run() throws GameActionException {
		ping();
		move();
		build();
		waterTrees();
		shake();
	}

	// figure out how to fix this method. I think we need to water the tree right on the edge of it?
	// It's radius is 1, so water the edge? maybe need to move, but the other one wasn't doing that.
	private void waterTrees() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees(3, myTeam);
		System.out.println("Amount of trees: " + trees.length);
		for (TreeInfo tree : trees) {
			if (rc.canWater(tree.ID)) {
				System.out.println("Watered a plant");
				rc.water(tree.ID);
			}
		}
	}

	private void build() throws GameActionException {
		if (!stationary) {
			return;
		}
		int openSquares = countNearbyOpenSquares(true);
		System.out.println("Counted this many open squares: " + openSquares);
		if (openSquares == 1) {
			buildUnit(RobotType.SCOUT);
		} else if (rand.nextInt(4) > 0) {
			this.plantTree();
		} else if (rc.hasRobotBuildRequirements(RobotType.SCOUT)) {
			buildUnit(RobotType.SCOUT);
		}
	}

	private void buildUnit(RobotType type) throws GameActionException {
		if (rc.hasRobotBuildRequirements(RobotType.SCOUT)) {
			for (Direction dir : directions) {
				if (rc.canBuildRobot(RobotType.SCOUT, dir)) {
					rc.buildRobot(RobotType.SCOUT, dir);
					break;
				}
			}
		}
	}

	private void move() throws GameActionException {
		if (stationary) {
			return;
		}
		moveInUnexploredDirection(0);
		if (rc.hasMoved()) {
			steps++;
		}

		int nearbyOpenSquares = countNearbyOpenSquares(false);
		if (nearbyOpenSquares >= 4) {
			stationary = true;
		}
		if (steps >= MIN_MOVES && nearbyOpenSquares > 1) {
			stationary = true;
		}
		if (steps >= MIN_MOVES + 2) {
			plantTree();
		}
		if (stationary) {
			rc.broadcast(Channels.GARDENER_IS_SETUP, 1);
			System.out.println("Gardener is now stationary!");
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

	private int countNearbyOpenSquares(boolean shouldCountTrees) throws GameActionException {
		int count = 0;
		Direction initial = Direction.getNorth();
		for (int i = 0; i < 6; i++) {
			int rotation = 360 / 6;
			Direction rotated = initial.rotateRightDegrees(rotation * i);
			if (rc.canPlantTree(rotated)) {
				count++;
				continue;
			}
			/*
			 * MapLocation loc = rc.getLocation().add(rotated); TreeInfo t =
			 * rc.senseTreeAtLocation(loc); RobotInfo r =
			 * rc.senseRobotAtLocation(loc); if (t == null && r == null) {
			 * continue; }
			 */
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
