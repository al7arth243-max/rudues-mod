package com.rudues.mod.ability;

/** Shared (client + server) numbers for the Hero of Hell form. Plain constants only. */
public class HeroConstants {
    public static final int GAUGE_MAX = 100;
    /** How long the demon form lasts: 60 seconds. */
    public static final int DEMON_TICKS = 1200;

    /** The demon is 7 blocks tall. */
    public static final float DEMON_HEIGHT = 7.0F;
    public static final float DEMON_WIDTH = 2.2F;
    public static final float DEMON_EYE = 6.0F;
    public static final float DEMON_SCALE = 1.65F;   // model is ~4.25 blocks at scale 1
    /** third-person camera is pulled this far back while a demon (on top of vanilla's 4 blocks) */
    public static final double CAMERA_EXTRA = 9.0;

    /** where the beam leaves the demon, relative to the eye */
    public static final double BEAM_FORWARD = 3.6;
    public static final double BEAM_DOWN = 1.7;
}
