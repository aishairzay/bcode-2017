package finale;

import battlecode.common.*;

public strictfp class Gardener extends Bot {

	private int soldiersNeeded;
	private int rangedCount;
	private boolean first;
	private int lumberjackCooldown = -50;
	private boolean hasBuiltTree;

	Direction[] plantingDirections;

	public Gardener(RobotController rc) throws GameActionException {
		super(rc);
		rangedCount = 0;
		plantingDirections = new Direction[6];
		Direction initial = Direction.getNorth();
		for (int i = 0; i < 6; i++) {
			int rotation = 360 / 6;
			Direction rotated = initial.rotateRightDegrees(rotation * i);
			plantingDirections[i] = rotated;
		}
		soldiersNeeded = 1;
		if (rc.getRoundNum() <= 10) {
			first = true;
		}
		if (first) {
			soldiersNeeded = 1;
		}
		hasBuiltTree = false;
	}

	@Override
	public void run() throws GameActionException {
		ping();

		TreeInfo[] neutralTrees = rc.senseNearbyTrees(myType.sensorRadius, Team.NEUTRAL);
		RobotInfo[] allies = rc.senseNearbyRobots(myType.sensorRadius, myTeam);
		RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadius, enemyTeam);
		BulletInfo[] bullets = rc.senseNearbyBullets(myType.bulletSightRadius);

		build(neutralTrees, allies, enemies, bullets);
		if (!hasBuiltTree && !rc.hasMoved()) {
			this.moveInUnexploredDirection(true);
		}

		waterTrees();
		shake(rc.senseNearbyTrees(3, Team.NEUTRAL));
	}

	private void waterTrees() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees(3, myTeam);
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

	private void build(TreeInfo[] neutralTrees, RobotInfo[] allies, RobotInfo[] enemies, BulletInfo[] bullets)
			throws GameActionException {
		int allyCount = 0, enemyCount = 0, localRangedCount = 0, enemyScoutCount = 0;
		float enemyDistForScoutSpawn = rc.readBroadcastFloat(Channels.SCOUT_NEEDED);
		System.out.println("Got this scout need score: " + enemyDistForScoutSpawn);
		for (RobotInfo enemy : enemies) {
			if (Helper.isHostile(enemy.type) && enemy.type != RobotType.GARDENER) {
				enemyCount++;
				if (enemy.type == RobotType.SCOUT) {
					enemyScoutCount++;
				}
			}
		}
		for (RobotInfo ally : allies) {
			if (Helper.isHostile(ally.type) && ally.type != RobotType.SCOUT) {
				allyCount++;
				if (ally.type == RobotType.SOLDIER || ally.type == RobotType.TANK) {
					localRangedCount++;
				}
			}
		}
		int blockingNeutralTrees = countBlockingNeutralTrees();
		int openSquares = countNearbyOpenSquares();
		int robotContainedTrees = 0;
		for (TreeInfo tree : neutralTrees) {
			RobotType r = tree.containedRobot;
			if (r != null && r != RobotType.SCOUT && r != RobotType.ARCHON) {
				robotContainedTrees++;
			}
		}

		System.out.println("Blocking neutral trees: " + blockingNeutralTrees);
		System.out.println("Open spots nearby: " + openSquares);

		if (enemyDistForScoutSpawn >= Constants.MIN_SCOUT_FIRST_DISTANCE) {
			System.out.println("Trying to build a scout first");
			if (this.buildUnit(RobotType.SCOUT)) {
				rc.broadcastFloat(Channels.SCOUT_NEEDED, 0);
			}
			return;
		}

		if (soldiersNeeded > 0 && first) {
			if (this.buildUnit(RobotType.SOLDIER)) {
				soldiersNeeded--;
			}
			System.out.println("Wanted to build soldiers");
			return;
		}
		boolean shouldBuildUnit = (enemyCount > 0 && enemyCount >= allyCount);
		if (shouldBuildUnit) {
			if (enemyScoutCount == enemyCount) {
				this.buildUnit(RobotType.LUMBERJACK);
			} else {
				this.buildUnit(RobotType.SOLDIER);
			}
			return;
		}
		boolean inPotentialDanger = bullets.length >= 3;
		if (inPotentialDanger) {
			this.buildUnit(RobotType.SOLDIER);
			return;
		}

		if (enemyDistForScoutSpawn >= Constants.MIN_SCOUT_DISTANCE
				&& enemyDistForScoutSpawn < Constants.MIN_SCOUT_FIRST_DISTANCE) {
			System.out.println("Trying to build a scout");
			if (this.buildUnit(RobotType.SCOUT)) {
				rc.broadcastFloat(Channels.SCOUT_NEEDED, 0);
			}
			return;
		}

		boolean needLumberjack = false;
		if ((blockingNeutralTrees >= 1 && openSquares <= 1) || robotContainedTrees > 1) {
			needLumberjack = true;
		}
		System.out.println("Need lumberjack: " + needLumberjack);
		if (needLumberjack && rc.getRoundNum() - this.lumberjackCooldown >= 70) {
			if (this.buildUnit(RobotType.LUMBERJACK)) {
				this.lumberjackCooldown = rc.getRoundNum();
			}
			return;
		}
		System.out.println("Ranged count is: " + this.rangedCount);

		if (this.rangedCount <= 1) {
			this.buildUnit(RobotType.SOLDIER);
		} else if (openSquares > 1) {
			this.plantTree();
		} else if (this.rangedCount >= Constants.MAX_RANGED_COUNT) {
			float extraBullets = (rc.getTeamBullets() - 200);
			if (extraBullets > 0) {
				float toDonate = ((int) (extraBullets / rc.getVictoryPointCost()) * rc.getVictoryPointCost());
				rc.donate(toDonate);
			}
		} else {
			if (localRangedCount > 0) {
				return;
			}
			this.buildUnit(RobotType.SOLDIER);
		}
	}

	private int countBlockingNeutralTrees() throws GameActionException {
		int count = 0;
		Direction initial = Direction.getNorth();
		for (int i = 0; i < 6; i++) {
			int rotation = 360 / 6;
			Direction rotated = initial.rotateRightDegrees(rotation * i);
			MapLocation loc = rc.getLocation().add(rotated,
					(float) (myType.bodyRadius + GameConstants.BULLET_TREE_RADIUS + 0.1));
			TreeInfo[] nextToMe = rc.senseNearbyTrees(loc, GameConstants.BULLET_TREE_RADIUS, Team.NEUTRAL);
			if (nextToMe.length > 0) {
				count++;
			}
		}
		return count;
	}

	private boolean buildUnit(RobotType type) throws GameActionException {
		if (rc.getTeamBullets() < 100 || !rc.isBuildReady()) {
			return false;
		}
		if (rc.hasRobotBuildRequirements(type)) {
			Direction dir = rc.getLocation().directionTo(enemyLoc);
			for (int i = 0; i < 8; i++) {
				if (rc.canBuildRobot(type, dir)) {
					rc.buildRobot(type, dir);
					return true;
				}
				dir = dir.rotateRightDegrees(45);
			}
			int initial = rand.nextInt(6);
			for (int i = 0; i < 6; i++) {
				Direction rotated = plantingDirections[(initial + i) % 6];
				if (rc.canBuildRobot(type, rotated)) {
					rc.buildRobot(type, rotated);
					return true;
				}
			}
		}
		return false;
	}

	private void plantTree() throws GameActionException {
		int initial = rand.nextInt(6);
		for (int i = 0; i < 6; i++) {
			Direction rotated = plantingDirections[(initial + i) % 6];
			if (rc.canPlantTree(rotated)) {
				rc.plantTree(rotated);
				this.hasBuiltTree = true;
				rc.broadcastBoolean(Channels.GARDENER_IS_SETUP, true);
				break;
			}
		}
	}

	private int countNearbyOpenSquares() throws GameActionException {
		int count = 0;
		Direction initial = Direction.getNorth();
		for (int i = 0; i < 6; i++) {
			int rotation = 360 / 6;
			Direction rotated = initial.rotateRightDegrees(rotation * i);
			if (rc.canPlantTree(rotated)) {
				count++;
			}
		}
		return count;
	}

	// every 40 rounds send a ping, so that a
	// friendly archon knows how many gardeners are current alive.
	private void ping() throws GameActionException {
		if (rc.getRoundNum() % Constants.GARDENER_PING_RATE == 0) {
			int currentPing = rc.readBroadcast(Channels.GARDENER_PING_CHANNEL);
			if (currentPing <= 0) {
				rc.broadcast(Channels.GARDENER_PING_CHANNEL, 1);
			} else {
				rc.broadcast(Channels.GARDENER_PING_CHANNEL, currentPing + 1);
			}
		}

		if ((rc.getRoundNum() % Constants.RANGED_PING_RATE) == 1) {
			System.out.println("Read ping");
			rangedCount = rc.readBroadcast(Channels.RANGED_PING_CHANNEL);
		} else if (rc.getRoundNum() % Constants.RANGED_PING_RATE == 2) {
			System.out.println("Clear ping");
			rc.broadcast(Channels.RANGED_PING_CHANNEL, -1);
		}
	}
}
