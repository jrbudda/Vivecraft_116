package org.vivecraft.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import net.minecraft.util.text.LanguageMap;
import org.apache.commons.io.IOUtils;

import net.minecraft.client.resources.I18n;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourceManagerReloadListener;
import net.optifine.Config;

public class LangHelper {
	public static void loadLocaleData(String code, Map<String, String> map) {
		String path = "lang/" + code + ".lang";
		InputStream is = Utils.getAssetAsStream(path, false);
		if (is == null)
			return;

		try {
			for (String s : IOUtils.readLines(is, StandardCharsets.UTF_8)) {
				if (!s.isEmpty() && s.charAt(0) != '#') {
					String[] split = s.split("=", 2);
					map.put(split[0], split[1]);
				}
			}

			is.close();
		} catch (IOException e) {
			System.out.println("Failed reading locale data");
			e.printStackTrace();
		}
	}
}
