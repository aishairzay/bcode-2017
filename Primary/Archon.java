package Primary;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;

public strictfp class Archon extends Bot {

	int gardnerCount;

	public Archon(RobotController rc) throws GameActionException {
		super(rc);
		rc.donate(10);
		gardnerCount = 1;
	}

	public void run() throws GameActionException {
		// System.out.println("Running archon now");
		build();
		micro();
		shake();
	}

	// ----------------------------------------------------------------------
	// BUILD
	private void build() throws GameActionException {
		boolean shouldBuildGardner = shouldBuildGardner();
		if (shouldBuildGardner) {
			// System.out.println("Trying to hire gardner now");
			hireGardner();
		}
	}

	private boolean shouldBuildGardner() throws GameActionException {
		if (rc.getRoundNum() <= 9 && allyArchons[0].equals(rc.getLocation())) {
			return true;
		}
		boolean needNewGardener = rc.readBroadcast(Channels.GARDENER_IS_SETUP) > 0;
		if (needNewGardener) {
			rc.broadcast(Channels.GARDENER_IS_SETUP, 0);
			return true;
		}
		if ((rc.getRoundNum() % Constants.GARDNER_PING_RATE) == 2) {
			gardnerCount = rc.readBroadcast(Channels.GARDENER_PING_CHANNEL);
			rc.broadcast(Channels.GARDENER_PING_CHANNEL, -1);
			if (rc.getTeamBullets() >= 150 || gardnerCount <= Constants.MAX_GARDNER_COUNT) {
				hireGardner();
			}
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
			moveTowardsUnshookTrees();
		}
		if (!rc.hasMoved()) {
			moveInUnexploredDirection(0);
		}
	}

	private void stayAlive() {

	}

}
