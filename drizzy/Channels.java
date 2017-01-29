package drizzy;

public strictfp class Channels {
	public static final Integer GARDENER_PING_CHANNEL = 1;
	public static final Integer GARDENER_IS_SETUP = 2;
	public static final Integer RANGED_PING_CHANNEL = 3;
	public static final Integer ENEMY_ATTACK_SCORE = 11;
	public static final Integer ENEMY_ATTACK_LOCATION = 12;

	public static final Integer WEST_BOUNDARY = 20;
	public static final Integer NORTH_BOUNDARY = 21;
	public static final Integer EAST_BOUNDARY = 22;
	public static final Integer SOUTH_BOUNDARY = 23;

	public static final Integer BUILT_FIRST_GARDENER = 10;
	public static final Integer GLOBAL_ENEMY_LOC = 15;// 16 taken too then

	/*
	 * Channels 1000 - 2000 are purely reserved for Tree location channels. Do
	 * not reuse these.
	 */

	protected static int getIdChannel(int id) {
		id = (id % 8000) + 1000;
		return id;
	}

}
