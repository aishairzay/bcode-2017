package PrimarySoldier;

import java.util.Random;

import battlecode.common.*;

public strictfp abstract class Bot {
	protected RobotController rc;
	protected Random rand;
	protected Team myTeam;
	protected Team enemyTeam;
	protected RobotType myType;
	protected Direction[] directions = new Direction[8];
	protected MapLocation[] allyArchons;
	protected MapLocation[] enemyArchons;
	protected Direction unexploredDir;
	protected MapLocation home;
	protected MapLocation enemyLoc;

	public Bot(RobotController rc) {
		this.rc = rc;
		rand = new Random(rc.getID() + (int) (Math.random() * 100));
		unexploredDir = this.getRandomDirection();
		myTeam = this.rc.getTeam();
		enemyTeam = myTeam.opponent();
		myType = rc.getType();
		Direction dir = Direction.getWest();
		int rotation = 45;
		for (int i = 0; i < 8; i++, rotation += 45) {
			directions[i] = dir.rotateRightDegrees(rotation);
		}
		allyArchons = rc.getInitialArchonLocations(myTeam);
		enemyArchons = rc.getInitialArchonLocations(enemyTeam);
		float x = 0;
		float y = 0;
		for (MapLocation m : enemyArchons) {
			x += m.x;
			y += m.y;
		}
		x /= enemyArchons.length;
		y /= enemyArchons.length;
		enemyLoc = new MapLocation(x, y);
		findHome();
	}

	private void findHome() {
		home = allyArchons[0];
		int bestScore = -10000000;
		for (MapLocation ally : allyArchons) {
			int score = 0;
			for (MapLocation enemy : enemyArchons) {
				score -= rc.getLocation().distanceTo(enemy);
			}
			if (score > bestScore) {
				bestScore = score;
				home = ally;
			}
		}
	}

	public abstract void run() throws GameActionException;

	public Direction getRandomDirection() {
		return directions[rand.nextInt(directions.length)];
	}

	protected MapLocation dest = null;

	protected void setDestination(MapLocation loc) {
		this.dest = loc;
	}

	protected void shake() throws GameActionException {
		if (rc.canShake()) {
			TreeInfo[] trees = rc.senseNearbyTrees(rc.getType().strideRadius, Team.NEUTRAL);
			for (TreeInfo tree : trees) {
				if (rc.canShake(tree.ID)) {
					rc.shake(tree.ID);
					int channel = this.getTreeChannel(tree.ID);
					System.out.println("Got this channel: " + channel);
					rc.broadcast(channel, 1);
					break;
				}
			}
		}
	}

	// This method attempts to make a move
	// To the best square that will take the least amount of damage from bullets
	// returns true if a move was made to minimize bullet damage
	// returns false if a move was not made
	protected int[] getBulletDamagesByMoveLocs(BulletInfo[] bullets) {
		/*
		 * int[] directionScores = new int[9]; int[] allDirections = new int[8];
		 * float myHealth = rc.getHealth(); for (int i = 0; i <
		 * directionScores.length; i++) { Direction dir = directionFromIndex(i);
		 * if (!rc.canMove(dir)) { continue; } for (int j = 0; j <
		 * bullets.length; j++) {
		 * 
		 * } } return directionScores;
		 */
		return null;
	}

	protected int indexFromDirection(Direction direction) {
		Direction dir = Direction.getWest();
		for (int i = 0; i < 8; i++) {
			if (dir == direction) {
				return i;
			}
			dir = dir.rotateRightDegrees(45);
		}
		return 8;

	}

	protected void stayAlive() {

	}

	protected Direction directionFromIndex(int index) {
		if (index >= 0 && index < 8) {

		}
		return null;
	}

	protected int getTreeChannel(int treeId) {
		treeId = (treeId % 400) + 599;
		return treeId;
	}

	protected int getLocChannel(MapLocation loc) {
		return (3 * (Math.abs((int) loc.x + 1) * 1000)) ^ (7 * Math.abs((int) loc.y + 1) * 10);
	}

	void makeMove(Direction dir) throws GameActionException {
		if (rc.hasMoved()) {
			return;
		}
		Direction rotation = dir;
		int i = 0;
		int leftAngle = 45;
		int rightAngle = 45;
		boolean left = rand.nextBoolean();
		while (true) {
			if (rc.canMove(rotation)) {
				// System.out.println("Able to move this try");
				break;
			}
			// System.out.println("Rotating fam");
			if (i >= 8) {
				break;
			}
			i++;
			if (left) {
				rotation = dir.rotateLeftDegrees(leftAngle);
				leftAngle += 45;
			} else {
				rotation = dir.rotateRightDegrees(rightAngle);
				rightAngle += 45;
			}
			left = !left;
		}
		if (rc.canMove(rotation)) {
			rc.move(rotation);
		}
	}

	protected void moveTowardsUnshookTrees() throws GameActionException {
		TreeInfo[] trees = rc.senseNearbyTrees(myType.sensorRadius, Team.NEUTRAL);
		TreeInfo closest = null;
		for (TreeInfo tree : trees) {
			if (tree.team != Team.NEUTRAL) {
				continue;
			}
			if (myType != RobotType.SCOUT && myType.strideRadius < tree.radius) {
				continue;
			}
			boolean visited = rc.readBroadcast(this.getTreeChannel(tree.ID)) > 0;
			if (visited) {
				continue;
			}
			if (closest == null) {
				closest = tree;
			} else if (rc.getLocation().distanceSquaredTo(tree.location) < rc.getLocation()
					.distanceSquaredTo(closest.location)) {
				closest = tree;
			}
		}
		System.out.println("Got closest tree: " + null);
		if (closest == null) {
			return;
		}
		if (rc.canMove(closest.location)) {
			rc.move(closest.location);
		} else {
			Direction dir = rc.getLocation().directionTo(closest.location);
			makeMove(dir);
		}
		rc.setIndicatorLine(rc.getLocation(), closest.location, 200, 0, 0);
	}

	protected void moveInUnexploredDirection(int tries) throws GameActionException {
		if (tries == 8) {
			return;
		}
		if (unexploredDir == null) {
			unexploredDir = getRandomDirection();
			if (unexploredDir == Direction.getEast() || unexploredDir == Direction.getNorth()
					|| unexploredDir == Direction.getEast() || unexploredDir == Direction.getSouth()) {
				unexploredDir = getRandomDirection();
			}
		}
		if (rc.canMove(unexploredDir)) {
			makeMove(unexploredDir);
		} else {
			unexploredDir = this.getRandomDirection();
			moveInUnexploredDirection(tries + 1);
		}
	}
}
