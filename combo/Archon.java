package combo;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public strictfp class Archon extends Bot {

	int gardnerCount;
	boolean isLeader;
	MapLocation lastSpawn;
	boolean needScout;

	public Archon(RobotController rc) throws GameActionException {
		super(rc);
		gardnerCount = 1;
		isLeader = home.equals(rc.getLocation());
	}

	public void run() throws GameActionException {
		TreeInfo[] neutralTrees = rc.senseNearbyTrees(myType.sensorRadius, Team.NEUTRAL);
		if (isLeader) {
			setNeedScout(neutralTrees);
		}
		build();
		this.moveTowardsUnshookTrees();
		micro();
		shake(neutralTrees);
	}

	private void setNeedScout(TreeInfo[] trees) throws GameActionException {
		if (needScout) {
			return;
		}
		float closestDist = rc.getLocation().distanceTo(enemyArchons[0]);
		for (MapLocation enemy : enemyArchons) {
			float dist = rc.getLocation().distanceTo(enemy);
			if (dist < closestDist) {
				closestDist = dist;
			}
		}
		if (rc.getRoundNum() <= 1) {
			if (closestDist >= Constants.MIN_SCOUT_DISTANCE) {
				rc.broadcastFloat(Channels.SCOUT_NEEDED, closestDist);
				needScout = true;
				return;
			}
		}
		int count = 0;
		for (TreeInfo t : trees) {
			if (t.containedBullets >= 2) {
				count++;
			}
		}
		if (count >= 2 && closestDist >= 25) {
			rc.broadcast(Channels.SCOUT_NEEDED, Constants.MIN_SCOUT_DISTANCE + 1);
			needScout = true;
		} else if (count >= 20) {
			rc.broadcast(Channels.SCOUT_NEEDED, Constants.MIN_SCOUT_DISTANCE + 1);
			needScout = true;
		}
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
		RobotInfo[] closeAllies = rc.senseNearbyRobots(myType.sensorRadius / 2, myTeam);
		int closeGardeners = 0;
		for (RobotInfo ally : closeAllies) {
			if (ally.type == RobotType.GARDENER) {
				closeGardeners++;
			}
		}
		if (closeGardeners >= 2) {
			return false;
		}
		if (!isLeader && rc.getRoundNum() == 2 && !builtFirstGardener) {
			return true;
		}
		if (!isLeader && rc.getRoundNum() <= 40) {
			return false;
		}
		if (isLeader && rc.getRoundNum() <= 1) {
			return true;
		}
		boolean gardenerSetup = rc.readBroadcastBoolean(Channels.GARDENER_IS_SETUP);
		if (!gardenerSetup) {
			return false;
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
		if (rc.getLocation().distanceTo(lastSpawn) <= 6 || !rc.onTheMap(rc.getLocation(), 5)) {
			this.moveInUnexploredDirection(true);
		}
	}

}
