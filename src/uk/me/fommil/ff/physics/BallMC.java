/*
 * Copyright Samuel Halliday 2009
 * 
 * This file is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this file.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package uk.me.fommil.ff.physics;

import com.google.common.base.Preconditions;
import static java.lang.Math.*;
import java.util.Collection;
import java.util.logging.Logger;
import javax.media.j3d.BoundingSphere;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import uk.me.fommil.ff.Direction;
import uk.me.fommil.ff.Pitch;
import uk.me.fommil.ff.Tactics.BallZone;
import uk.me.fommil.ff.Utils;

/**
 * The model (M) and controller (C) for the ball during game play.
 * 
 * @author Samuel Halliday
 */
public class BallMC {

	private static final double MAX_SPEED = 125;

	/**
	 * The aftertouch that a ball may exhibit. Directional aftertouch is always relate to
	 * the direction of motion.
	 */
	public enum Aftertouch {

		UP, DOWN, LEFT, RIGHT

	}

	private static final Logger log = Logger.getLogger(BallMC.class.getName());

	private static final double GROUND_FRICTION = 200;

	private static final double AIR_FRICTION = 30;

	private static final double GRAVITY = 10;

	// position
	private final Point3d s = new Point3d(0, 0, 0);

	// velocity
	private final Vector3d v = new Vector3d();

	// aftertouch
	private final Vector3d after = new Vector3d();

	// no aftertouch after a bounce
	private volatile boolean bounced = false;

	/**
	 * @param pitch
	 * @return
	 */
	public BallZone getZone(Pitch pitch) {
		Preconditions.checkNotNull(pitch);
		return new BallZone(s, pitch);
	}

	/**
	 * Update the model.
	 *
	 * @param t with units of seconds
	 */
	public void tick(double t) {
		// TODO: aftertouch needs a lot more thought
		// aftertouch
		if (!bounced && s.z >= 0.5 && after.lengthSquared() > 0 && (v.z >= 0 || v.length() > 75)) {
			Vector3d a = (Vector3d) after.clone();
			if (s.z > 3 || v.z < 0) {
				a.z = 0;
			}
			a.scale(t);
			v.add(a);
		}

		// update position
		s.x += v.x * t;
		s.y += v.y * t;
		s.z += v.z * t;

		// ground bounce
		if (s.z < 0) {
			if (abs(v.z) < 1) {
				s.z = 0;
				v.z = 0;
			} else {
				bounced = true;
				s.z = abs(s.z) / 2;
				v.z = abs(v.z) / 2;
			}
		}
		// gravity
		if (s.z > 0)
			v.z -= t * GRAVITY;

		// friction
		v.x = signum(v.x) * max(0, abs(v.x) - friction(t, s.z));
		v.y = signum(v.y) * max(0, abs(v.y) - friction(t, s.z));

		// maximum
		double speed = v.length();
		if (speed > MAX_SPEED) {
//			log.info("SPEED " + speed);
			v.scale(MAX_SPEED / speed);
		}
	}

	private double friction(double t, double z) {
		if (z > 0.1)
			return AIR_FRICTION * t;
		return GROUND_FRICTION * t;
	}

	/**
	 * Controller.
	 *
	 * @param aftertouches
	 */
	public void setAftertouch(Collection<Aftertouch> aftertouches) {
		// TODO: consider the player who applies the aftertouch
		Vector3d aftertouch = new Vector3d();
		for (Aftertouch at : aftertouches) {
			switch (at) {
				case UP:
					aftertouch.y -= 1;
					break;
				case DOWN:
					aftertouch.y += 1;
					break;
				case LEFT:
					aftertouch.x -= 1;
					break;
				case RIGHT:
					aftertouch.x += 1;
					break;
			}
		}
//		log.info(aftertouches + " " + aftertouch);
		after.scale(0);
		Direction direction = Direction.valueOf(Utils.getBearing(v));
		if (v.lengthSquared() == 0 || direction == null) {
			return;
		}
		// TODO: clean up horrible code duplication
		double power = 200;
		double power_gravity = GRAVITY / 3;
		double power_bendy = 200;
		double lift = 20;
		double lift_gravity = 3 * GRAVITY;
		double lift_bendy = 50;

		// log.info("BALL FACING " + direction + " AFTERTOUCH " + aftertouch);
		switch (direction) {
			case UP_RIGHT:
			case UP_LEFT:
			case UP:
				if (aftertouch.y < 0) {
					// power shot
					after.x = power_bendy * aftertouch.x;
					after.y = -power;
					after.z = power_gravity;
				} else if (aftertouch.y > 0) {
					// lift
					after.x = lift_bendy * aftertouch.x;
					after.y = -lift;
					after.z = lift_gravity;
				}
				break;
			case DOWN_LEFT:
			case DOWN_RIGHT:
			case DOWN:
				if (aftertouch.y > 0) {
					after.x = power_bendy * aftertouch.x;
					after.y = power;
					after.z = power_gravity;
				} else if (aftertouch.y < 0) {
					after.x = lift_bendy * aftertouch.x;
					after.y = lift;
					after.z = lift_gravity;
				}
				break;
			case RIGHT:
				if (aftertouch.x > 0) {
					after.x = power;
					after.y = power_bendy * aftertouch.y;
					after.z = power_gravity;
				} else if (aftertouch.x < 0) {
					after.x = lift;
					after.y = lift_bendy * aftertouch.y;
					after.z = lift_gravity;
				}
				break;
			case LEFT:
				if (aftertouch.x < 0) {
					after.x = -power;
					after.y = power_bendy * aftertouch.y;
					after.z = power_gravity;
				} else if (aftertouch.x > 0) {
					after.x = -lift;
					after.y = lift_bendy * aftertouch.y;
					after.z = lift_gravity;
				}
				break;
		}
	}

	// <editor-fold defaultstate="collapsed" desc="BOILERPLATE GETTERS/SETTERS">
	public Point3d getPosition() {
		return (Point3d) s.clone();
	}

	public void setPosition(Point3d s) {
		Preconditions.checkNotNull(s);
		bounced = false;
		this.s.set(s);
	}

	public Vector3d getVelocity() {
		return (Vector3d) v.clone();
	}

	public void setVelocity(Vector3d v) {
		Preconditions.checkNotNull(v);
		bounced = false;
		this.v.set(v);
	}
	// </editor-fold>
}