package org.vivecraft.api;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.vivecraft.render.PlayerModelController;

import net.minecraft.entity.player.PlayerEntity;
import net.optifine.Config;
import net.optifine.http.FileDownloadThread;

public class PatreonReceiver
{
	private static final Object lock = new Object();
	private static List<PlayerEntity> queuedPlayers = new LinkedList<>();
	private static Map<String, Integer> cache;
	private static boolean downloadStarted;
	private static boolean downloadFailed;

	private static void fileDownloadFinished(String url, byte[] data, Throwable exception) {
		synchronized (lock) {
			if (data != null) {
				try {
					HashMap<String, Integer> map = new HashMap<>();
					String s = new String(data, StandardCharsets.UTF_8);

					String[] lines = s.split("\\r?\\n");
					for (String string : lines) {
						try {
							String[] bits = string.split(":");
							int level = Integer.parseInt(bits[1]);
							map.put(bits[0], level);

							for (PlayerEntity player : queuedPlayers) {
								if (bits[0].equalsIgnoreCase(player.getGameProfile().getName())) {
									PlayerModelController.getInstance().setHMD(player.getUniqueID(), level);
								}
							}
						} catch (Exception e) {
							System.out.println("error with donors txt " + e.getMessage());
						}
					}

					cache = map;
				} catch (Exception e) {
					Config.dbg("Error parsing data: " + url + ", " + e.getClass().getName() + ": " + e.getMessage());
					downloadFailed = true;
				}
			} else {
				downloadFailed = true;
			}

			queuedPlayers.clear();
		}
	}

	public static void addPlayerInfo(PlayerEntity p) {
		if (downloadFailed)
			return;

		synchronized (lock) {
			if (cache == null) {
				queuedPlayers.add(p);
				PlayerModelController.getInstance().setHMD(p.getUniqueID(), 0);

				if (!downloadStarted) {
					downloadStarted = true;
					String s = "http://www.vivecraft.org/patreon/current.txt";
					FileDownloadThread filedownloadthread = new FileDownloadThread(s, PatreonReceiver::fileDownloadFinished);
					filedownloadthread.start();
				}
			} else {
				PlayerModelController.getInstance().setHMD(p.getUniqueID(), cache.getOrDefault(p.getGameProfile().getName(), 0));
			}
		}
	}
}
