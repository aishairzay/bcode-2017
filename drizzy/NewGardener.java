package drizzy;

import battlecode.common.*;

public strictfp class NewGardener extends Bot {

	public NewGardener(RobotController rc) {
		super(rc);
		// TODO Auto-generated constructor stub
	}
	/*
	 * private int buildCount;
	 * 
	 * public NewGardener(RobotController rc) throws GameActionException {
	 * super(rc); if (rc.getRoundNum() <= 0) {
	 * 
	 * } }
	 * 
	 * @Override public void run() throws GameActionException {
	 * BoundaryHelper.getAndBroadcastBoundaries(rc);
	 * 
	 * BulletInfo[] bullets = rc.senseNearbyBullets(myType.bulletSightRadius);
	 * RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadius,
	 * enemyTeam); TreeInfo[] myTrees = rc.senseNearbyTrees(myType.sensorRadius,
	 * myTeam); TreeInfo[] neutralTrees =
	 * rc.senseNearbyTrees(myType.sensorRadius, Team.NEUTRAL);
	 * 
	 * ping(); Direction toMove; if (!rc.hasMoved()) { toMove =
	 * simplyDodge(bullets, enemies, 8000); rc.move(toMove); } if
	 * (!rc.hasMoved()) { //move(); } buildUnit(); plantTree(); waterTrees();
	 * shake(); }
	 * 
	 * private void move(TreeInfo[] myTrees) {
	 * 
	 * }
	 * 
	 * private void buildUnit() {
	 * 
	 * }
	 * 
	 * private void plantTree() {
	 * 
	 * }
	 * 
	 * private Direction getDirAwayFromWall() throws GameActionException {
	 * MapLocation myLoc = rc.getLocation(); if
	 * (!rc.onTheMap(myLoc.add(Direction.EAST, 5))) { return Direction.WEST; }
	 * if (!rc.onTheMap(myLoc.add(Direction.WEST, 5))) { return Direction.EAST;
	 * } if (!rc.onTheMap(myLoc.add(Direction.NORTH, 5))) { return
	 * Direction.SOUTH; } if (!rc.onTheMap(myLoc.add(Direction.SOUTH, 5))) {
	 * return Direction.NORTH; } return this.getRandomDirection(); }
	 * 
	 * private void waterTrees() throws GameActionException { TreeInfo[] trees =
	 * rc.senseNearbyTrees(3, myTeam); TreeInfo lowestHealth = null; for
	 * (TreeInfo tree : trees) { if (rc.canWater(tree.ID)) { if (lowestHealth ==
	 * null) { lowestHealth = tree; } else if (tree.health <
	 * lowestHealth.health) { lowestHealth = tree; } } } if (lowestHealth !=
	 * null) { rc.water(lowestHealth.ID); }
	 * 
	 * }
	 * 
	 * // every 40 rounds send a ping, so that a // friendly archon knows how
	 * many gardeners are current alive. private void ping() throws
	 * GameActionException { if (rc.getRoundNum() % Constants.GARDNER_PING_RATE
	 * != 0) { return; } int currentPing =
	 * rc.readBroadcast(Channels.GARDENER_PING_CHANNEL); if (currentPing <= 0) {
	 * rc.broadcast(Channels.GARDENER_PING_CHANNEL, 1); } else {
	 * rc.broadcast(Channels.GARDENER_PING_CHANNEL, currentPing + 1); } }
	 */

	@Override
	public void run() throws GameActionException {
		// TODO Auto-generated method stub

	}
}
