package drizzy;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
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
		// RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadius,
		// enemyTeam);
		// this.broadcastEnemy(enemies);
		build();
		micro();
		// this.moveTowardsUnshookTrees();
		shake(neutralTrees);
	}

	// ----------------------------------------------------------------------
	// BUILD
	private void build() throws GameActionException {
		boolean builtFirstGardener = rc.readBroadcast(Channels.BUILT_FIRST_GARDENER) != 0;
		boolean shouldBuildGardner = shouldBuildGardner(builtFirstGardener);
		if (shouldBuildGardner) {
			hireGardner(builtFirstGardener);
		}
	}

	private boolean shouldBuildGardner(boolean builtFirstGardener) throws GameActionException {
		if (!isLeader && rc.getRoundNum() == 2 && !builtFirstGardener) {
			return true;
		}
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
		if (rc.getRoundNum() >= 31 && rc.getTeamBullets() >= 105 && gardnerCount <= Constants.MAX_GARDNER_COUNT) {
			return true;
		}
		return false;
	}

	private void hireGardner(boolean firstBuiltGardener) throws GameActionException {
		if (gardnerCount > Constants.MAX_GARDNER_COUNT) {
			return;
		}
		for (Direction dir : directions) {
			if (rc.canHireGardener(dir)) {
				rc.hireGardener(dir);
				if (!firstBuiltGardener) {
					rc.broadcast(Channels.BUILT_FIRST_GARDENER, 1);
				}
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
			this.moveInUnexploredDirection(true);
		}
	}

}
