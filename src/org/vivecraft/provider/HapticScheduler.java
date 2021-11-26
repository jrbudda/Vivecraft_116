package org.vivecraft.provider;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class HapticScheduler {
	protected ScheduledExecutorService executor;

	public HapticScheduler() {
		executor = Executors.newSingleThreadScheduledExecutor();
	}

	public abstract void queueHapticPulse(ControllerType controller, float durationSeconds, float frequency, float amplitude, float delaySeconds);
}
