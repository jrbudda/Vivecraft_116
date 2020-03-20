package org.vivecraft.control;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.vivecraft.provider.MCOpenVR;

public class HapticMusicPlayer {
	private static Map<String, Music> map = new HashMap<>();

	private HapticMusicPlayer() {
	}

	public static Music newMusic(String name) {
		Music music = new Music(name);
		map.put(name, music);
		return music;
	}

	public static boolean hasMusic(String name) {
		return map.containsKey(name);
	}

	public static Music getMusic(String name) {
		return map.get(name);
	}

	public static void removeMusic(String name) {
		map.remove(name);
	}

	public static class Music {
		final String name;

		private List<Object> data = new LinkedList<>();

		private Music(String name) {
			this.name = name;
		}

		public Music addNote(@Nullable ControllerType controller, float durationSeconds, float frequency, float amplitude) {
			data.add(new Note(controller, durationSeconds, frequency, amplitude));
			return this;
		}

		public Music addDelay(float durationSeconds) {
			data.add(new Delay(durationSeconds));
			return this;
		}

		public void clearData() {
			data.clear();
		}

		public void play() {
			float delayAccum = 0;
			for (Object obj : data) {
				if (obj instanceof Note) {
					Note note = (Note)obj;
					if (note.controller != null) {
						MCOpenVR.triggerHapticPulse(note.controller, note.durationSeconds, note.frequency, note.amplitude, delayAccum);
					} else {
						MCOpenVR.triggerHapticPulse(ControllerType.RIGHT, note.durationSeconds, note.frequency, note.amplitude, delayAccum);
						MCOpenVR.triggerHapticPulse(ControllerType.LEFT, note.durationSeconds, note.frequency, note.amplitude, delayAccum);
					}
				} else if (obj instanceof Delay) {
					Delay delay = (Delay)obj;
					delayAccum += delay.durationSeconds;
				}
			}
		}

		private class Note {
			final ControllerType controller;
			final float durationSeconds;
			final float frequency;
			final float amplitude;

			private Note(ControllerType controller, float durationSeconds, float frequency, float amplitude) {
				this.controller = controller;
				this.durationSeconds = durationSeconds;
				this.frequency = frequency;
				this.amplitude = amplitude;
			}
		}

		private class Delay {
			final float durationSeconds;

			private Delay(float durationSeconds) {
				this.durationSeconds = durationSeconds;
			}
		}
	}

	public class MusicBuilder {
		private Music music;
		private float tempo;
	}
}
