package Third2;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public strictfp class Archon extends Bot {

	int gardnerCount;
	boolean isLeader;
	MapLocation lastSpawn;

	public Archon(RobotController rc) throws GameActionException {
		super(rc);
		gardnerCount = 1;
		isLeader = home.equals(rc.getLocation());
	}

	public void run() throws GameActionException {
		build();
		micro();
		shake();
	}

	// ----------------------------------------------------------------------
	// BUILD
	private void build() throws GameActionException {
		boolean shouldBuildGardner = shouldBuildGardner();
		if (shouldBuildGardner) {
			hireGardner();
		}
	}

	private boolean shouldBuildGardner() throws GameActionException {
		if (!isLeader && rc.getRoundNum() <= 50) {
			return false;
		}
		if (isLeader) {
			if (rc.getRoundNum() <= 1) {
				return true;
			}
		}
		boolean needNewGardener = rc.readBroadcast(Channels.GARDENER_IS_SETUP) > 0;
		if (needNewGardener) {
			rc.broadcast(Channels.GARDENER_IS_SETUP, 0);
			return true;
		}
		if ((rc.getRoundNum() % Constants.GARDNER_PING_RATE) == 1) {
			gardnerCount = rc.readBroadcast(Channels.GARDENER_PING_CHANNEL);
			if (gardnerCount <= Constants.MAX_GARDNER_COUNT) {
				hireGardner();
			}
		}
		if (rc.getRoundNum() % Constants.GARDNER_PING_RATE == 2) {
			rc.broadcast(Channels.GARDENER_PING_CHANNEL, -1);
		}
		if (rc.getRoundNum() > 100 && rc.getTeamBullets() >= 125 && gardnerCount <= Constants.MAX_GARDNER_COUNT) {
			hireGardner();
		}
		return false;
	}

	private void hireGardner() throws GameActionException {
		if (gardnerCount > Constants.MAX_GARDNER_COUNT) {
			return;
		}
		for (Direction dir : directions) {
			if (rc.canHireGardener(dir)) {
				rc.hireGardener(dir);
				lastSpawn = rc.getLocation().add(dir, 1);
			}
		}
	}

	// ----------------------------------------------------------------------
	// MICRO
	private void micro() throws GameActionException {
		if (!rc.hasMoved()) {
			stayAlive();
		}
		if (!rc.hasMoved()) {
			moveAwayFromLastSpawn();
		}
		if (!rc.hasMoved()) {
			moveAwayFromWall();
		}
	}

	private void moveAwayFromLastSpawn() throws GameActionException {
		if (rc.getLocation().distanceTo(lastSpawn) <= 6) {
			this.moveInUnexploredDirection(0);
		}
	}

	private Direction getDirAwayFromWall() throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		if (!rc.onTheMap(myLoc.add(Direction.EAST, 4))) {
			return Direction.WEST;
		}
		if (!rc.onTheMap(myLoc.add(Direction.WEST, 4))) {
			return Direction.EAST;
		}
		if (!rc.onTheMap(myLoc.add(Direction.NORTH, 4))) {
			return Direction.SOUTH;
		}
		if (!rc.onTheMap(myLoc.add(Direction.SOUTH, 4))) {
			return Direction.NORTH;
		}
		return this.getRandomDirection();
	}

	private void moveAwayFromWall() throws GameActionException {
		Direction dir = getDirAwayFromWall();
		if (dir == null) {
			return;
		}
		this.makeMove(dir);
	}

}
