package unstuckpls;

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
	protected boolean inSecondaryDanger;
	protected float furthestDist;

	public Bot(RobotController rc) {
		this.rc = rc;

		rand = new Random(rc.getID());
		rotationDir = rand.nextBoolean();
		if (rc.getRoundNum() % 2 == 0) {
			rotationDir = !rotationDir;
		}
		unexploredDir = this.getRandomDirection();
		myTeam = this.rc.getTeam();
		enemyTeam = myTeam.opponent();
		myType = rc.getType();
		Direction dir = Direction.getWest().rotateRightDegrees(rand.nextInt(4) * 45);
		int rotation = 45;

		allyArchons = rc.getInitialArchonLocations(myTeam);
		enemyArchons = rc.getInitialArchonLocations(enemyTeam);
		furthestDist = 0;
		for (MapLocation m : enemyArchons) {
			float dist = rc.getLocation().distanceTo(m);
			furthestDist = dist > furthestDist ? dist : furthestDist;
		}

		for (int i = 0; i < 8; i++, rotation += 45) {
			directions[i] = dir.rotateRightDegrees(rotation);
		}
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
				if (tree.containedBullets > 0 && rc.canShake(tree.ID)) {
					rc.shake(tree.ID);
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

		this.moveTowards(closest.location);

	}

	protected MapLocation randomDest;

	private int unexploredSteps;

	private void setRandomDest(boolean includeTrees) throws GameActionException {
		MapLocation dest = rc.getLocation().add(this.getRandomDirection(), 100);
		float dist = 7;
		float best = 300;
		for (Direction dir : directions) {// lowest score is best here
			float score = 10;
			MapLocation potential = rc.getLocation().add(dir, (float) ((dist / 2) - 0.1));
			if (!rc.onTheMap(potential)) {
				continue;
			}
			if (myType.equals(RobotType.GARDENER)) {
				RobotInfo[] enemies = rc.senseNearbyRobots(potential, dist / 2, enemyTeam);
				score += enemies.length * 5;
			}

			if ((myType.equals(RobotType.GARDENER) || myType.equals(RobotType.SOLDIER))
					&& !rc.onTheMap(potential, dist / 2)) {
				score += 10;
			} else if (myType.equals(RobotType.ARCHON) && !rc.onTheMap(potential, 2)) {
				score += 10;
			}
			score += rc.senseNearbyRobots(potential, dist / 2, null).length;
			if (myType.equals(RobotType.SOLDIER) || myType.equals(RobotType.TANK)) {
				int enemies = rc.senseNearbyTrees(potential, dist / 2, enemyTeam).length;
				score -= enemies;
			}
			if (includeTrees) {
				int n = rc.senseNearbyTrees(potential, dist / 2, Team.NEUTRAL).length;
				int m = rc.senseNearbyTrees(potential, dist / 2, myTeam).length;
				score += n + m > 0 ? n + m + 2 : 0;

			}
			if (score < best || (score == best && rand.nextBoolean())) {
				dest = potential;
				best = score;
				if (myType.equals(RobotType.SOLDIER)) {
					dest = rc.getLocation().add(dir, myType.sensorRadius);
				}
			}
		}
		unexploredSteps = 0;
		randomDest = dest;
	}

	protected void moveInUnexploredDirection(boolean includeTrees) throws GameActionException {
		if (randomDest == null) {
			setRandomDest(includeTrees);
		}
		int stepLimit = 6;
		if (Helper.isHostile(myType)) {
			stepLimit = 12;
		}
		if (unexploredSteps >= stepLimit) {
			setRandomDest(includeTrees);
			moveInUnexploredDirection(includeTrees);
		} else if (rc.getLocation().distanceTo(randomDest) > 2) {
			unexploredSteps++;
			this.moveTowards(randomDest);
		} else {
			setRandomDest(includeTrees);
			moveInUnexploredDirection(includeTrees);
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

	// Taken from lecture code. thank you dev gods
	static float bulletWillCollide(BulletInfo bullet, MapLocation loc, float bodyRadius) {
		Direction propagationDirection = bullet.dir;
		MapLocation bulletLocation = bullet.location;
		float distToRobot = bulletLocation.distanceTo(loc);
		float theta = propagationDirection.radiansBetween(bulletLocation.directionTo(loc));

		if (Math.abs(theta) > Math.PI / 2) {
			return 0;
		}
		float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta));
		return perpendicularDist > bodyRadius ? -1 : perpendicularDist;
	}

	private int primaryScore;
	private int secondaryScore;

	protected void getBulletDangerScore(MapLocation loc, BulletInfo[] bullets, int bytecodeLimit)
			throws GameActionException {
		int bytecodeStart = Clock.getBytecodeNum();
		primaryScore = 0;
		secondaryScore = 0;
		int bulletCount = 0;

		for (int i = 0; i < bullets.length; i++) {
			if (Clock.getBytecodeNum() - bytecodeStart >= bytecodeLimit) {
				break;
			}
			bulletCount++;
			BulletInfo bullet = bullets[i];

			float perpDistance = bulletWillCollide(bullet, loc, myType.bodyRadius);
			if (perpDistance >= 0) {
				float dist = loc.distanceTo(bullet.location);
				if ((dist <= bullet.speed + (2 * myType.bodyRadius))) {
					primaryScore += bullet.damage - perpDistance;
				} else if (dist <= (bullet.speed * 2) + (2 * myType.bodyRadius)) {
					secondaryScore += (bullet.damage - perpDistance) / 3;
				}
			}
		}
	}

	protected boolean shootSingles;

	protected boolean bulletPathClear(MapLocation source, RobotInfo toAttack) throws GameActionException {
		shootSingles = false;
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
			if (r != null && r.team == enemyTeam && r.type == RobotType.ARCHON) {
				shootSingles = true;
			}
			TreeInfo t = rc.senseTreeAtLocation(iter);
			if (r != null && r.equals(toAttack.location)) {
				break;
			}
			if (t != null && t.team == enemyTeam && myType == RobotType.SCOUT) {
				return false;
			}
			if (t != null && t.team != enemyTeam) {
				break;
			}
			iter = iter.add(towardsEnemy, (float) 1.0);
		}
		if (!rc.canSenseLocation(iter)) {
			Direction d = rc.getLocation().directionTo(iter);
			iter = rc.getLocation().add(d, (float) (myType.sensorRadius - 0.1));
		}
		TreeInfo t = rc.senseTreeAtLocation(iter);
		if (t != null) {
			boolean distanceToAttack = source.distanceTo(toAttack.location) <= 1;
			return distanceToAttack;
		}
		boolean bulletPathIsClear = dest.distanceTo(iter) <= toAttack.type.bodyRadius;

		return bulletPathIsClear;
	}

	boolean shouldStayStill = false;

	protected MapLocation[] getSafestLocs(BulletInfo[] bullets, RobotInfo[] enemies, int bytecodeLimit)
			throws GameActionException {
		inDanger = false;
		shouldStayStill = false;
		inSecondaryDanger = false;
		int originalBytecode = Clock.getBytecodeNum();
		List<MapLocation> locs = new ArrayList<MapLocation>(16);
		if (bullets.length == 0 && enemies.length == 0) {
			return new MapLocation[] {};
		}
		float stride = myType.strideRadius;
		float minScore = -1;

		for (int i = 0; i < 2; i++) {
			for (Direction dir : directions) {
				if (Clock.getBytecodeNum() - originalBytecode >= bytecodeLimit) {
					break;
				}
				MapLocation next = rc.getLocation().add(dir, stride);
				if (!rc.canMove(next)) {
					continue;
				}
				float score = 0;
				getBulletDangerScore(next, bullets, (bytecodeLimit / 16) - 15);
				float bulletScore = primaryScore + secondaryScore;
				score += (float) ((int) bulletScore);
				if (minScore == -1 || score == minScore) {
					minScore = score;
					MapLocation loc = rc.getLocation().add(dir, stride);
					locs.add(loc);
				} else if (score < minScore) {
					minScore = score;
					locs.clear();
					MapLocation loc = rc.getLocation().add(dir, stride);
					locs.add(loc);
				}
				if (primaryScore > 0) {
					inDanger = true;
				}
				if (secondaryScore > 0) {
					inSecondaryDanger = true;
				}

			}
			stride /= 2;
		}
		return locs.toArray(new MapLocation[] {});
	}

	// --------------------- Movement ------------------------

	protected void moveTowards(MapLocation dest) throws GameActionException {
		if (dest == null) {
			return;
		}
		setDestination(dest);
		bugWithoutReturn();
		if (!rc.hasMoved()) {
			this.makeMove(rc.getLocation().directionTo(dest));
		}
	}

	private void bugWithoutReturn() throws GameActionException {
		if (!rc.onTheMap(rc.getLocation(), myType.bodyRadius + myType.strideRadius)) {
			reset(this.dest);
			rotationDir = !rotationDir;
		}
		bugWithoutReturn(0);
	}

	private void bugWithoutReturn(int failures) throws GameActionException {
		if (failures >= 20) {
			return;
		}
		if (rc.hasMoved()) {
			return;
		}
		Direction towards = rc.getLocation().directionTo(dest);
		if (onWall) {
			if (rc.canMove(rotateRight(cur))) { // get off the wall
				cur = rotateRight(cur);
				int i = 0;
				while (rc.canMove(cur)) {
					if (i >= 20) {
						reset(dest);
						rotationDir = !rotationDir;
						break;
					}
					i++;
					Direction rotate = rotateRight(cur);
					if (cur.equals(towards, (float) 0.1964)) {
						reset(dest);
						bugWithoutReturn(failures + 1);
					} else if (rc.canMove(rotate)) {
						cur = rotate;
						continue;
					} else {
						bugWithoutReturn(failures + 1);
					}
				}
			} else if (rc.canMove(cur)) { // move forward
				rc.move(cur);
			} else {
				int i = 0;
				while (!rc.canMove(cur)) {
					if (i >= 20) {
						reset(dest);
						rotationDir = !rotationDir;
						break;
					}
					cur = rotateLeft(cur);
					i++;
				}
				if (rc.canMove(cur)) {
					rc.move(cur);
				}
			}
		} else {
			if (rc.canMove(towards)) {
				reset(dest);
				rc.move(towards);
			} else {
				onWall = true;
				bugWithoutReturn(failures + 1);
			}
		}
	}

	protected MapLocation getNextBugLoc(MapLocation dest) throws GameActionException {
		if (dest == null) {
			return null;
		}
		setDestination(dest);
		MapLocation n = bug();
		return n;
	}

	protected MapLocation getBugDest() {
		return this.dest;
	}

	private MapLocation dest;
	private boolean onWall;
	Direction cur;
	private boolean rotationDir;
	private int lastRound = 0;

	protected void hardResetBug() {
		onWall = false;
		rotationDir = !rotationDir;
		cur = rc.getLocation().directionTo(dest);
	}

	private void reset(MapLocation loc) {
		onWall = false;
		cur = rc.getLocation().directionTo(loc);
		Direction best = Direction.NORTH;
		for (Direction dir : directions) {
			float bestDiff = Math.abs(cur.degreesBetween(best));
			float diff = Math.abs(cur.degreesBetween(dir));
			if (diff < bestDiff) {
				best = dir;
			}
		}
		cur = best;
		dest = loc;
	}

	private void setDestination(MapLocation loc) {
		if (rc.getRoundNum() - lastRound >= 8) {
			reset(loc);
		}
		lastRound = rc.getRoundNum();
		if (dest == null || !dest.equals(loc)) {
			reset(loc);
			if (rand.nextBoolean()) {
				this.rotationDir = !rotationDir;
			}
		}
	}

	private MapLocation bug() throws GameActionException {
		if (!rc.onTheMap(rc.getLocation(), myType.bodyRadius + myType.strideRadius)) {
			reset(this.dest);
			rotationDir = !rotationDir;
		}
		return bug(0);
	}

	private MapLocation bug(int failures) throws GameActionException {
		if (failures >= 20) {
			return null;
		}
		float stride = myType.strideRadius;
		Direction towards = rc.getLocation().directionTo(dest);
		if (onWall) {
			if (rc.canMove(rotateRight(cur), stride)) { // get off the wall
				cur = rotateRight(cur);
				int i = 0;
				while (rc.canMove(cur, stride)) {
					if (i >= 20) {
						reset(dest);
						rotationDir = !rotationDir;
						break;
					}
					i++;
					Direction rotate = rotateRight(cur);
					if (cur.equals(towards, (float) 0.4)) {
						reset(dest);
						return bug(failures + 1);
					} else if (rc.canMove(rotate, stride)) {
						cur = rotate;
						continue;
					} else {
						return bug(failures + 1);
					}
				}
			} else if (rc.canMove(cur, stride)) { // move forward
				MapLocation next = rc.getLocation().add(cur, stride);
				return next;
			} else {
				int i = 0;
				while (!rc.canMove(cur, stride)) {
					if (i >= 20) {
						reset(dest);
						break;
					}
					cur = rotateLeft(cur);
					i++;
				}
				if (rc.canMove(cur, stride)) {
					MapLocation next = rc.getLocation().add(cur, stride);
					return next;
				}
			}
		} else {
			if (rc.canMove(towards, stride)) {
				reset(dest);
				MapLocation next = rc.getLocation().add(towards, stride);
				return next;
			} else {
				onWall = true;
				return bug(failures + 1);
			}
		}
		return null;
	}

	private Direction rotateRight(Direction dir) {
		if (rotationDir) {
			return dir.rotateRightDegrees((float) 22.5);
		} else {
			return dir.rotateLeftDegrees((float) 22.5);
		}
	}

	private Direction rotateLeft(Direction dir) {
		if (rotationDir) {
			return dir.rotateLeftDegrees((float) 22.5);
		} else {
			return dir.rotateRightDegrees((float) 22.5);
		}
	}

	private int steps = 0;

	protected MapLocation broadcastEnemy(RobotInfo[] enemies) throws GameActionException {
		if (enemies.length > 0) {
			RobotInfo closest = null;
			for (int i = 0; i < enemies.length; i++) {
				RobotInfo r = enemies[i];
				if (r.type == RobotType.GARDENER) {
					closest = r;
					break;
				}
			}
			if (closest != null) {
				Helper.broadcastLocation(Channels.GLOBAL_ENEMY_LOC, rc, closest.location);
				return closest.location;
			}
		}
		return null;
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
