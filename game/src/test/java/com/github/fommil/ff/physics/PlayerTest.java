/*
 * Copyright Samuel Halliday 2010
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
package com.github.fommil.ff.physics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.github.fommil.ff.Pitch;
import com.github.fommil.ff.PlayerStats;

/**
 * @author Samuel Halliday
 */
public class PlayerTest {

	private final Pitch pitch = new Pitch();

	private static final long DT = 100L;

	interface Tester {
		void test(Position s, Velocity v);
	}

	void testHelper(Position position, Velocity velocity, Tester stepTest) {
		DummyPhysics physics = new DummyPhysics();
		Ball ball = physics.createBall();
		ball.setPosition(position);
		ball.setVelocity(velocity);
		for (int i = 0; i < 1000; i++) {
			physics.doTick(DT);
			stepTest.test(ball.getPosition(), ball.getVelocity());
		}
		physics.clean();
	}

	@Test
	public void testKick() throws Exception {
		Player p = new DummyPhysics().createPlayer(5, new PlayerStats());
		Ball ball = new DummyPhysics().createBall();
		ball.setVelocity(new Velocity(0, 0, 0));
		p.setBallOwner(true);
		p.kick(ball);
		assertEquals(ball.getVelocity(), new Velocity(0.0, 15, 4.0));
	}

	@Test
	public void playerState_OUT_OF_CONTROL() {
		Player p = new DummyPhysics().createPlayer(5, new PlayerStats());

		p.body.setAngularVel(5, 5, 5);
		p.body.setPosition(0.5, 0.5, 0.5);
		p.body.setLinearVel(5, 5, 5);

		assertEquals(p.getState(), Player.PlayerState.OUT_OF_CONTROL);
	}

	@Test
	public void playerState_HEAD_START() {
		Player p = new DummyPhysics().createPlayer(5, new PlayerStats());

		p.body.setAngularVel(0, 0, 0);
		p.body.setPosition(0.5, 0.5, 0.5);
		p.body.setLinearVel(5, 5, 5);

		assertEquals(p.getState(), Player.PlayerState.HEAD_START);
	}

	@Test
	public void playerState_HEAD_MID() {
		Player p = new DummyPhysics().createPlayer(5, new PlayerStats());

		p.body.setAngularVel(0, 0, 0);
		p.body.setPosition(1.7, 1.7, 1.2);
		p.body.setLinearVel(5, 5, 5);

		assertEquals(p.getState(), Player.PlayerState.HEAD_MID);
	}

	@Test
	public void playerState_HEAD_END() {
		Player p = new DummyPhysics().createPlayer(5, new PlayerStats());

		p.body.setAngularVel(0, 0, 0);
		p.body.setPosition(2, 2, 2);
		p.body.setLinearVel(5, 5, -1);

		assertEquals(p.getState(), Player.PlayerState.HEAD_END);

		p.body.setPosition(2, 2, 2);
		p.body.setLinearVel(5, 5, 5);

		assertEquals(p.getState(), Player.PlayerState.HEAD_END);
	}

	@Test
	public void playerState_RUN() {
		Player p = new DummyPhysics().createPlayer(5, new PlayerStats());

		p.body.setAngularVel(0, 0, 0);
		p.body.setPosition(2, 2, 0.5);
		p.body.setLinearVel(5, 5, -1);

		assertEquals(p.getState(), Player.PlayerState.RUN);
	}

	@Test
	public void createPlayer() {
		Player p = new DummyPhysics().createPlayer(5, new PlayerStats());
		assertEquals(p.getShirt(), 5);
		assertNull(p.getTeam());
	}
}
