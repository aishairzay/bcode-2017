package yeezus;

import java.util.ArrayList;
import java.util.List;
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
	protected float westBoundary;
	protected float northBoundary;
	protected float eastBoundary;
	protected float southBoundary;
	protected boolean inDanger;

	public Bot(RobotController rc) {
		this.rc = rc;
		rand = new Random(rc.getID() + (int) (Math.random() * 100));
		rotationDir = rand.nextBoolean();
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
		float bestScore = -10000000;
		for (MapLocation ally : allyArchons) {
			float score = 0;
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

	protected void shake(TreeInfo[] trees) throws GameActionException {
		if (rc.canShake()) {
			for (TreeInfo tree : trees) {
				if (rc.canShake(tree.ID)) {
					rc.shake(tree.ID);
					int channel = Channels.getIdChannel(tree.ID);
					rc.broadcast(channel, 1);
					break;
				}
			}
		}
	}

	protected int getLocChannel(MapLocation loc) {
		int x = (int) loc.x;
		int y = (int) loc.y;
		return 5 ^ (7 * (Math.abs(x + 1) * 10000)) ^ (13 * Math.abs(y + 1) * 1000) % 1000;
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
				break;
			}
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
			if (tree.team != Team.NEUTRAL || tree.containedBullets <= 1) {
				continue;
			}
			boolean visited = rc.readBroadcast(Channels.getIdChannel(tree.ID)) > 0;
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
		if (closest == null) {
			return;
		}
		if (rc.canMove(closest.location)) {
			rc.move(closest.location);
		} else {
			this.moveTowards(closest.location);
		}
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

	static boolean bulletWillCollide(BulletInfo bullet, MapLocation loc, float bodyRadius) {
		Direction propagationDirection = bullet.dir;
		MapLocation bulletLocation = bullet.location;
		Direction directionToRobot = bulletLocation.directionTo(loc);
		float distToRobot = bulletLocation.distanceTo(loc);
		float theta = propagationDirection.radiansBetween(directionToRobot);

		if (Math.abs(theta) > Math.PI / 2) {
			return false;
		}
		float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta));
		return (perpendicularDist <= bodyRadius);
	}

	protected int getBulletDangerScore(MapLocation loc, BulletInfo[] bullets, int bytecodeLimit)
			throws GameActionException {
		int bytecodeStart = Clock.getBytecodeNum();
		int score = 0;
		for (int i = 0; i < bullets.length; i++) {
			if (Clock.getBytecodeNum() - bytecodeStart >= bytecodeLimit) {
				break;
			}
			BulletInfo bullet = bullets[i];
			if (bulletWillCollide(bullet, loc, myType.bodyRadius)
					&& (loc.distanceTo(bullet.location) <= bullet.speed + (2 * myType.bodyRadius))) {
				score += bullet.damage;
			}
		}

		int red = 50 + (score * 20) >= 256 ? 255 : 50 + (score * 20);
		// rc.setIndicatorDot(loc, red, 0, 0);
		return score;
	}

	protected float getEnemyLocScore(MapLocation loc, RobotInfo[] enemies) throws GameActionException {
		if (rc.getType().equals(RobotType.LUMBERJACK)) {
			return 0;
		}
		float score = 0;
		int count = 0;
		for (RobotInfo enemy : enemies) {
			if (count >= 2) {
				break;
			}
			if (!Helper.isHostile(enemy.type)) {
				continue;
			}
			score += EnemyScoreHelper.getDangerScore(rc, loc, enemy);
			count++;
		}
		return score;
	}

	protected boolean bulletPathClear(MapLocation source, RobotInfo toAttack) throws GameActionException {
		MapLocation dest = toAttack.location;
		Direction towardsEnemy = source.directionTo(dest);
		int steps = 0;
		MapLocation iter = source.add(towardsEnemy, (float) (rc.getType().bodyRadius + 0.1));
		while (dest.distanceTo(iter) >= toAttack.type.bodyRadius) {
			if (steps >= 10) {
				break;
			}
			steps++;
			if (!rc.canSenseLocation(iter)) {
				break;
			}
			RobotInfo r = rc.senseRobotAtLocation(iter);
			if (r != null && r.team == myTeam) {
				return false;
			}
			TreeInfo t = rc.senseTreeAtLocation(iter);
			if (r != null && r.equals(toAttack.location)) {
				break;
			}
			if (t != null && t.team != enemyTeam) {
				break;
			}
			MapLocation next = iter.add(towardsEnemy, (float) 1.1);
			rc.setIndicatorLine(iter, next, 0, 200, 0);
			iter = iter.add(towardsEnemy, (float) 1.0);
		}
		if (!rc.canSenseLocation(iter)) {
			Direction d = rc.getLocation().directionTo(iter);
			iter = rc.getLocation().add(d, (float) (myType.sensorRadius - 0.1));
		}
		TreeInfo t = rc.senseTreeAtLocation(iter);
		if (t != null) {
			rc.setIndicatorDot(iter, 0, 0, 200);
			boolean distanceToAttack = source.distanceTo(toAttack.location) <= 1;
			// System.out.println("Distance to attack: " + distanceToAttack);
			return distanceToAttack;
		}
		boolean bulletPathIsClear = dest.distanceTo(iter) <= toAttack.type.bodyRadius;
		if (bulletPathIsClear) {
			rc.setIndicatorDot(iter, 0, 200, 0);
		} else {
			rc.setIndicatorDot(iter, 200, 0, 0);
		}
		return bulletPathIsClear;
	}

	protected Direction[] getSafestDirs(BulletInfo[] bullets, RobotInfo[] enemies, int bytecodeLimit)
			throws GameActionException {
		inDanger = false;
		int originalBytecode = Clock.getBytecodeNum();
		List<Direction> dirs = new ArrayList<Direction>();
		if (bullets.length == 0 && enemies.length == 0) {
			return new Direction[] {};
		}
		float stride = myType.strideRadius;
		float minScore = -1;

		for (Direction dir : directions) {
			if (Clock.getBytecodeNum() - originalBytecode >= bytecodeLimit) {
				break;
			}
			MapLocation next = rc.getLocation().add(dir, stride);
			if (rc.canMove(next)) {
				float score = 0;
				float bulletScore = this.getBulletDangerScore(next, bullets, 500);
				// float enemyLocScore = this.getEnemyLocScore(next, enemies);

				score += bulletScore;
				// score += enemyLocScore;

				if (minScore == -1 || score == minScore) {
					minScore = score;
					dirs.add(dir);
				} else if (score < minScore) {
					minScore = score;
					dirs = new ArrayList<Direction>();
					dirs.add(dir);
				}
				if (score > 0) {
					inDanger = true;
				}
			}
		}

		return dirs.toArray(new Direction[] {});
	}

	// --------------------- Movement ------------------------

	protected void moveTowards(MapLocation dest) throws GameActionException {
		if (dest == null) {
			return;
		}
		setDestination(dest);
		bug();
	}

	protected MapLocation getBugDest() {
		return this.dest;
	}

	private MapLocation dest;
	private boolean onWall;
	Direction cur;
	private boolean rotationDir;
	private int lastRound = 0;

	private void reset(MapLocation loc) {
		onWall = false;
		cur = rc.getLocation().directionTo(loc);
		dest = loc;
	}

	private void setDestination(MapLocation loc) {
		if (rc.getRoundNum() - lastRound >= 5) {
			reset(loc);
		}
		lastRound = rc.getRoundNum();
		if (dest != null) {
			// rc.setIndicatorLine(rc.getLocation(), dest, 0, 0, 140);
		}
		if (dest == null || !dest.equals(loc)) {
			reset(loc);
		}
	}

	private void bug() throws GameActionException {
		if (!rc.onTheMap(rc.getLocation(), myType.bodyRadius + myType.strideRadius)) {
			reset(this.dest);
			rotationDir = !rotationDir;
		}
		bug(0);
	}

	private void bug(int failures) throws GameActionException {
		if (failures >= 10) {
			return;
		}
		float stride = myType.strideRadius;
		Direction towards = rc.getLocation().directionTo(dest);
		if (onWall) {
			if (rc.canMove(rotateRight(cur), stride)) {
				cur = rotateRight(cur);
				int i = 0;
				while (rc.canMove(cur, stride)) {
					if (i >= 20) {
						reset(dest);
						break;
					}
					i++;
					Direction rotate = rotateRight(cur);
					if (cur.equals(towards, (float) 0.5)) {
						reset(dest);
						bug(failures + 1);
						return;
					} else if (rc.canMove(rotate, stride)) {
						cur = rotate;
						continue;
					} else {
						bug(failures + 1);
						return;
					}
				}
			} else if (rc.canMove(cur, stride)) { // move forward
				rc.move(cur, stride);
				// rc.setIndicatorDot(rc.getLocation(), 100, 100, 100);
			} else { // rotate left until i can move
				int i = 0;
				while (!rc.canMove(cur, stride)) {
					// rc.setIndicatorDot(rc.getLocation(), 150, 0, 0);
					if (i >= 20) {
						reset(dest);
						break;
					}
					cur = rotateLeft(cur);
					i++;
				}
				if (rc.canMove(cur, stride)) {
					rc.move(cur, stride);
				}
			}
		} else {
			if (rc.canMove(towards, stride)) {
				reset(dest);
				rc.move(towards);
			} else {
				onWall = true;
				MapLocation next = rc.getLocation().add(towards, stride);
				bug(failures + 1);
				return;
			}
		}
	}

	private Direction rotateRight(Direction dir) {
		if (rotationDir) {
			return dir.rotateRightDegrees(30);
		} else {
			return dir.rotateLeftDegrees(30);
		}
	}

	private Direction rotateLeft(Direction dir) {
		if (rotationDir) {
			return dir.rotateLeftDegrees(30);
		} else {
			return dir.rotateRightDegrees(30);
		}
	}

	private int steps = 0;

	protected void broadcastEnemy(RobotInfo[] enemies) throws GameActionException {
		if (enemies.length > 0) {
			RobotInfo closest = enemies[0];
			Helper.broadcastLocation(Channels.GLOBAL_ENEMY_LOC, rc, closest.location);
		}
	}

	protected void moveToEnemyLoc() throws GameActionException {
		if (steps > 100) {
			return;
		}
		this.moveTowards(this.enemyLoc);
		steps++;
	}

	protected Direction getDirAwayFromWall() throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		if (!rc.onTheMap(myLoc.add(Direction.EAST, 5))) {
			return Direction.WEST;
		}
		if (!rc.onTheMap(myLoc.add(Direction.WEST, 5))) {
			return Direction.EAST;
		}
		if (!rc.onTheMap(myLoc.add(Direction.NORTH, 5))) {
			return Direction.SOUTH;
		}
		if (!rc.onTheMap(myLoc.add(Direction.SOUTH, 5))) {
			return Direction.NORTH;
		}
		return null;
	}
}
