package unstuckpls;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public strictfp class Archon extends Bot {

	int gardenerCount;
	boolean isLeader;
	MapLocation lastSpawn;
	boolean needScout;

	public Archon(RobotController rc) throws GameActionException {
		super(rc);
		gardenerCount = 1;
		isLeader = home.equals(rc.getLocation());
		int myIndex = 0;
		for (int i = 0; i < allyArchons.length; i++) {
			MapLocation ally = allyArchons[i];
			if (rc.getLocation().equals(ally) || rc.getLocation().distanceTo(ally) < 2) {
				myIndex = i;
				break;
			}
		}
		boolean canMove = false;
		for (Direction dir : directions) {
			if (rc.canHireGardener(dir)) {
				canMove = true;
				break;
			}
		}
		if (!canMove) {
			rc.broadcast(Channels.ARCHON_IGNORE_LIST + myIndex, 1);
		}
	}

	public void run() throws GameActionException {
		TreeInfo[] neutralTrees = rc.senseNearbyTrees(myType.sensorRadius, Team.NEUTRAL);
		if (isLeader) {
			setNeedScout(neutralTrees);
		}
		this.moveTowardsUnshookTrees();
		if (!rc.hasMoved() && !isLeader && rc.getRoundNum() <= 80) {
			this.moveInUnexploredDirection(0);
		}
		build();
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
			if (t.containedBullets > 0) {
				count++;
			}
		}
		System.out.println("count is: " + count);
		if (count >= 6) {
			rc.broadcastFloat(Channels.SCOUT_NEEDED, Constants.MIN_SCOUT_FIRST_DISTANCE + 1);
			needScout = true;
		} else if (count >= 3) {
			rc.broadcastFloat(Channels.SCOUT_NEEDED, Constants.MIN_SCOUT_DISTANCE + 1);
			needScout = true;
		} else if (count >= 1 && closestDist >= 25) {
			rc.broadcastFloat(Channels.SCOUT_NEEDED, Constants.MIN_SCOUT_DISTANCE + 1);
			needScout = true;
		}
	}

	// ----------------------------------------------------------------------
	// BUILD
	private void build() throws GameActionException {
		boolean builtFirstGardener = rc.readBroadcast(Channels.BUILT_FIRST_GARDENER) != 0;
		boolean shouldBuildGardner = shouldBuildGardner(builtFirstGardener);
		if (shouldBuildGardner) {
			hireGardener(builtFirstGardener);
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
		if (rc.getRoundNum() == 2 && !builtFirstGardener) {
			return true;
		}
		if (!isLeader && rc.getRoundNum() <= 40) {
			return false;
		}
		if (isLeader && rc.getRoundNum() <= 1) {
			return true;
		}

		if ((rc.getRoundNum() % Constants.GARDENER_PING_RATE) == 1) {
			gardenerCount = rc.readBroadcast(Channels.GARDENER_PING_CHANNEL);
		} else if (rc.getRoundNum() % Constants.GARDENER_PING_RATE == 2) {
			rc.broadcast(Channels.GARDENER_PING_CHANNEL, -1);
		}
		if (rc.getRoundNum() >= 23 && rc.getTeamBullets() >= 110 && gardenerCount <= Constants.MAX_GARDNER_COUNT) {
			return true;
		}
		return false;
	}

	private void hireGardener(boolean firstBuiltGardener) throws GameActionException {
		if (gardenerCount > Constants.MAX_GARDNER_COUNT) {
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
			lastSpawn = rc.getLocation();
		}
		if (rc.getLocation().distanceTo(lastSpawn) <= 6 || !rc.onTheMap(rc.getLocation(), 5)) {
			this.moveInUnexploredDirection(true);
		}
	}

}
