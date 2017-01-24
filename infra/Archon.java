package infra;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

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
		TreeInfo[] neutralTrees = rc.senseNearbyTrees(myType.sensorRadius, Team.NEUTRAL);
		build();
		micro();
		this.moveTowardsUnshookTrees();
		shake(neutralTrees);
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
		if (!isLeader && rc.getRoundNum() <= 40) {
			return false;
		}
		if (isLeader && rc.getRoundNum() <= 1) {
			return true;
		}
		if ((rc.getRoundNum() % Constants.GARDENER_PING_RATE) == 1) {
			gardnerCount = rc.readBroadcast(Channels.GARDENER_PING_CHANNEL);
		} else if (rc.getRoundNum() % Constants.GARDENER_PING_RATE == 2) {
			rc.broadcast(Channels.GARDENER_PING_CHANNEL, -1);
		}
		if (rc.getRoundNum() >= 21 && rc.getTeamBullets() >= 115 && gardnerCount <= Constants.MAX_GARDNER_COUNT) {
			return true;
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
			moveAwayFromLastSpawn();
		}

	}

	private void moveAwayFromLastSpawn() throws GameActionException {
		if (lastSpawn == null) {
			return;
		}
		if (rc.getLocation().distanceTo(lastSpawn) <= 6) {
			this.moveInUnexploredDirection(0);
		}
	}

}
