package infra;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TreeInfo;

public class EnemyScoreHelper {

	public static float getDangerScore(RobotController rc, MapLocation myLoc, RobotInfo enemy)
			throws GameActionException {
		RobotType type = enemy.type;
		if (type.equals(RobotType.LUMBERJACK)) {
			return getLumberjackScore(rc, myLoc, enemy);
		} else if (type.equals(RobotType.SOLDIER)) {
			return getSoldierScore(rc, myLoc, enemy);
		} else if (type.equals(RobotType.TANK)) {
			return getTankScore(rc, myLoc, enemy);
		} else if (type.equals(RobotType.SCOUT)) {
			return getScoutScore(rc, myLoc, enemy);
		}
		return 0;
	}

	private static float getScoutScore(RobotController rc, MapLocation myLoc, RobotInfo enemy)
			throws GameActionException {
		if (!rc.getType().equals(RobotType.SCOUT)) {
			return 0;
		}
		MapLocation enemyLoc = enemy.location;
		boolean hiding = hidingBehindSomething(rc, myLoc, enemy);
		float scoutDangerZone = RobotType.SCOUT.strideRadius + RobotType.SCOUT.bodyRadius + RobotType.SCOUT.bulletSpeed;
		if (!hiding && myLoc.distanceTo(enemyLoc) <= scoutDangerZone) {
			return RobotType.SCOUT.attackPower;
		}
		return 0;
	}

	private static float getTankScore(RobotController rc, MapLocation myLoc, RobotInfo enemy)
			throws GameActionException {
		float tankDangerZone = RobotType.TANK.strideRadius + RobotType.TANK.bodyRadius + RobotType.TANK.bulletSpeed;
		boolean hiding = hidingBehindSomething(rc, myLoc, enemy);
		float dist = myLoc.distanceTo(enemy.location);
		if (dist <= tankDangerZone) {
			return RobotType.TANK.attackPower * 5;
		} else if (!hiding && dist <= tankDangerZone + 2) {
			return RobotType.TANK.attackPower * 3;
		} else if (!hiding && dist <= tankDangerZone + 4) {
			return RobotType.TANK.attackPower;
		}
		return 0;
	}

	private static float getSoldierScore(RobotController rc, MapLocation myLoc, RobotInfo enemy)
			throws GameActionException {
		float soldierDangerZone = RobotType.SOLDIER.strideRadius + RobotType.SOLDIER.bodyRadius
				+ RobotType.SOLDIER.bulletSpeed;
		boolean hiding = hidingBehindSomething(rc, myLoc, enemy);
		float dist = myLoc.distanceTo(enemy.location);
		if (dist <= soldierDangerZone) {
			return RobotType.SOLDIER.attackPower * 5;
		} else if (!hiding && dist <= soldierDangerZone + 2) {
			return RobotType.SOLDIER.attackPower * 3;
		} else if (!hiding && dist <= soldierDangerZone + 4) {
			return RobotType.SOLDIER.attackPower;
		}
		return 0;
	}

	private static float getLumberjackScore(RobotController rc, MapLocation myLoc, RobotInfo enemy) {
		float lumberDangerZone = RobotType.LUMBERJACK.strideRadius + RobotType.LUMBERJACK.bodyRadius
				+ GameConstants.INTERACTION_DIST_FROM_EDGE;
		if (myLoc.distanceTo(enemy.location) <= lumberDangerZone) {
			return RobotType.LUMBERJACK.attackPower;
		}
		return 0;
	}

	private static boolean hidingBehindSomething(RobotController rc, MapLocation myLoc, RobotInfo enemy)
			throws GameActionException {
		int enemyId = enemy.ID;
		MapLocation enemyLoc = enemy.location;
		Direction towardsEnemy = myLoc.directionTo(enemyLoc);
		if (rc.isLocationOccupied(myLoc.add(towardsEnemy, (float) 2))) {
			RobotInfo r = rc.senseRobotAtLocation(myLoc);
			if (r != null) {
				if (r.team == rc.getTeam().opponent()) {
					return true;
				}
				if (r.ID == enemyId) {
					return false;
				}
				if (r.ID == rc.getID()) {
					myLoc = myLoc.add(towardsEnemy, 1);
				}
			}
			TreeInfo t = rc.senseTreeAtLocation(myLoc);
			if (t != null) {
				return true;
			}
		}
		return false;
		/*
		 * MapLocation enemyLoc = enemy.location; Direction towardsEnemy =
		 * myLoc.directionTo(enemyLoc); float dist =
		 * myLoc.distanceTo(enemy.location); int steps = 0; myLoc =
		 * myLoc.add(towardsEnemy, rc.getType().bodyRadius + (float) 0.1); while
		 * (myLoc.distanceTo(enemyLoc) >= enemy.type.bodyRadius) { if (steps >=
		 * dist) { break; } steps++; boolean occupied =
		 * rc.isLocationOccupied(myLoc); if (occupied) { RobotInfo r =
		 * rc.senseRobotAtLocation(myLoc); if (r != null) { if (r.team ==
		 * rc.getTeam().opponent()) { return true; } if (r.ID == enemyId) {
		 * return false; } if (r.ID == rc.getID()) { myLoc =
		 * myLoc.add(towardsEnemy, 1); continue; } continue; } TreeInfo t =
		 * rc.senseTreeAtLocation(myLoc); if (t != null) { return true; } }
		 * myLoc = myLoc.add(towardsEnemy, 1); steps++; }
		 */
	}

}
