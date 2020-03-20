package org.vivecraft.settings.profile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Created by StellaArtois on 13/04/15
 *
 * This implementation is designed for ease of
 * integration into the existing Minecrift / Minecraft settings classes
 *
 * The JSON config should consist of roughly:
 *
 * selectedProfile: <profilename>
 * Profiles:
 *    Profile2:
 *       MC:
 *          setting:value
 *          setting:value
 *          setting:value
 *       OF:
 *          setting:value
 *          setting:value
 *          setting:value
 *       VR:
 *          setting:value
 *          setting:value
 *          setting:value
 *       Controller:
 *          setting:value
 *          setting:value
 *          setting:value
 *    Profile n...
 *
 * etc.. in no particular order.
 *
 */
public class ProfileManager
{
    public static final String DEFAULT_PROFILE = "Default";
    public static final String PROFILE_SET_OF = "Of";
    public static final String PROFILE_SET_MC = "Mc";
    public static final String PROFILE_SET_VR = "Vr";
    public static final String PROFILE_SET_CONTROLLER_BINDINGS = "Controller";

    static final String KEY_PROFILES = "Profiles";
    static final String KEY_SELECTED_PROFILE = "selectedProfile";
    static String currentProfileName = DEFAULT_PROFILE;

    static File vrProfileCfgFile = null;

    static JSONObject jsonConfigRoot = null;
    static JSONObject profiles = null;

    static boolean loaded = false;

    public static synchronized void init( File dataDir )
    {
        vrProfileCfgFile = new File(dataDir, "optionsviveprofiles.txt");        // VIVE - changed cfg file name to avoid conflicts
        load();
    }

    public static synchronized void load()
    {
        try {
            // Create vr profiles config file if it doesn't already exist
            if (vrProfileCfgFile.exists() == false) {
                vrProfileCfgFile.createNewFile();
            }

            // Read in json (may be empty)
            InputStreamReader isr = new InputStreamReader(new FileInputStream(vrProfileCfgFile), "UTF-8");
            try {
                JSONTokener jt = new JSONTokener(isr);
                jsonConfigRoot = new JSONObject(jt);
            }
            catch (Exception ex) {
                jsonConfigRoot = new JSONObject();
            }
            isr.close();

            // Read current profile (create if necessary)
            if (jsonConfigRoot.has(KEY_SELECTED_PROFILE))
                currentProfileName = jsonConfigRoot.getString(KEY_SELECTED_PROFILE);
            else {
                jsonConfigRoot.put(KEY_SELECTED_PROFILE, ProfileManager.DEFAULT_PROFILE);
            }

            // Read profiles section (create if necessary)
            if (jsonConfigRoot.has(KEY_PROFILES)) {
                profiles = jsonConfigRoot.getJSONObject(KEY_PROFILES);
            }
            else {
                profiles = new JSONObject();
                jsonConfigRoot.put(KEY_PROFILES, profiles);
            }

            // Add default profile if necessary
            if (profiles.has(ProfileManager.DEFAULT_PROFILE) == false) {
                JSONObject defaultProfile = new JSONObject();
                profiles.put(ProfileManager.DEFAULT_PROFILE, defaultProfile);
            }

            // Validate all profiles
            validateProfiles();

            loaded = true;

        } catch (Exception e) {
            System.out.println("FAILED to read VR profile settings!");
            e.printStackTrace();
            loaded = false;
        }
    }

    private static void validateProfiles() throws Exception {
        // Iterate each profile
            // check each profile set mc, of, vr, controller
                // if doesn't exist, set profile set defaults

        // For each profile
        for (Object profileKey : profiles.keySet()) {
            String profileName = (String) profileKey;
            Object profileObj = profiles.get(profileName);
            if (profileObj instanceof JSONObject) {
                JSONObject profile = (JSONObject)profileObj;

                JSONObject Mc = null;
                JSONObject Of = null;
                JSONObject Vr = null;
                JSONObject Controller = null;

                // For each profile set
                for (Object profileSetKey : profile.keySet()) {
                    String profileSetName = (String) profileSetKey;
                    Object profileSetObj = profile.get(profileSetName);
                    if (profileSetObj instanceof JSONObject) {
                        if (profileSetName.equals(ProfileManager.PROFILE_SET_MC)) {
                            Mc = (JSONObject)profileSetObj;
                        }
                        if (profileSetName.equals(ProfileManager.PROFILE_SET_OF)) {
                            Of = (JSONObject)profileSetObj;
                        }
                        if (profileSetName.equals(ProfileManager.PROFILE_SET_VR)) {
                            Vr = (JSONObject)profileSetObj;
                        }
                        if (profileSetName.equals(ProfileManager.PROFILE_SET_CONTROLLER_BINDINGS)) {
                            Controller = (JSONObject)profileSetObj;
                        }
                    }
                }

                // VIVE START - comment, don't load legacy config, will be wrong settings and plugins for room scale VR
                /*
                if (Mc == null) {
                    // Attempt legacy file read
                    if (!loadLegacySettings(legacyMcProfileCfgFile, profile, ProfileManager.PROFILE_SET_MC))
                    {
                        // Add empty profile set - defaults will be used automatically
                        Map<String, String> settings = new HashMap<String, String>();
                        setProfileSet(profile, ProfileManager.PROFILE_SET_MC, settings);
                    }
                }
                if (Of == null) {
                    // Attempt legacy file read
                    if (!loadLegacySettings(legacyOfProfileCfgFile, profile, ProfileManager.PROFILE_SET_OF))
                    {
                        // Add empty profile set - defaults will be used automatically
                        Map<String, String> settings = new HashMap<String, String>();
                        setProfileSet(profile, ProfileManager.PROFILE_SET_OF, settings);
                    }
                }
                if (Vr == null) {
                    // Attempt legacy file read
                    if (!loadLegacySettings(legacyVrProfileCfgFile, profile, ProfileManager.PROFILE_SET_VR))
                    {
                        // Add empty profile set - defaults will be used automatically
                        Map<String, String> settings = new HashMap<String, String>();
                        setProfileSet(profile, ProfileManager.PROFILE_SET_VR, settings);
                    }
                }
                if (Controller == null) {
                    // Attempt legacy file read
                    if (!loadLegacySettings(legacyControllerProfileCfgFile, profile, ProfileManager.PROFILE_SET_CONTROLLER_BINDINGS))
                    {
                        // Use defaults
                        loadLegacySettings(ProfileManager.DEFAULT_BINDINGS, profile, ProfileManager.PROFILE_SET_CONTROLLER_BINDINGS);
                    }
                }
                */
                // VIVE END - comment, don't load legacy config, will be wrong settings and plugins for room scale VR
            }
        }
    }

    private static synchronized boolean loadLegacySettings(File settingsFile, JSONObject theProfile, String set) throws Exception
    {
        if (settingsFile.exists() == false)
            return false;

        FileReader fr = new FileReader(settingsFile);
        BufferedReader br = new BufferedReader(fr);
        String s;
        Map<String, String> settings = new HashMap<String, String>();
        int count = 0;
        while ((s = br.readLine()) != null) {
            String[] array = splitKeyValue(s);
            String setting = array[0];
            String value = "";
            if (array.length > 1) {
                value = array[1];
            }
            settings.put(setting, value);
            count++;
        }
        setProfileSet(theProfile, set, settings);
        if (count == 0)
            return false;

        return true;
    }

    private static synchronized boolean loadLegacySettings(String settingStr, JSONObject theProfile, String set) throws Exception
    {
        StringReader stringReader = new StringReader(settingStr);
        BufferedReader br = new BufferedReader(stringReader);
        String s;
        Map<String, String> settings = new HashMap<String, String>();
        int count = 0;
        while ((s = br.readLine()) != null) {
            String[] array = splitKeyValue(s);
            String setting = array[0];
            String value = "";
            if (array.length > 1) {
                value = array[1];
            }
            settings.put(setting, value);
            count++;
        }
        setProfileSet(theProfile, set, settings);
        if (count == 0)
            return false;

        return true;
    }

    private static synchronized boolean loadLegacySettings(String[] settingStr, JSONObject theProfile, String set) throws Exception
    {
        Map<String, String> settings = new HashMap<String, String>();
        int count = 0;
        for (String s : settingStr) {
            if (s != null) {
                String[] array = splitKeyValue(s);
                String setting = array[0];
                String value = "";
                if (array.length > 1) {
                    value = array[1];
                }
                settings.put(setting, value);
                count++;
            }
        }
        setProfileSet(theProfile, set, settings);
        if (count == 0)
            return false;

        return true;
    }

    public static synchronized Map<String, String> getProfileSet(String profile, String set)
    {
        Map<String, String> settings = new HashMap<String, String>();
        if (profiles.has(profile)) {
            JSONObject theProfile = profiles.getJSONObject(profile);
            if (theProfile.has(set)) {
                JSONObject theSet = theProfile.getJSONObject(set);
                Set<String> keys = theSet.keySet();
                for (String key : keys) {
                    String value = theSet.getString(key);
                    settings.put(key, value);
                }
            }
        }
        return settings;
    }

    public static synchronized Map<String, String> getProfileSet(JSONObject theProfile, String set)
    {
        Map<String, String> settings = new HashMap<String, String>();
        if (theProfile.has(set)) {
            JSONObject theSet = theProfile.getJSONObject(set);
            Set<String> keys = theSet.keySet();
            for (String key : keys) {
                String value = theSet.getString(key);
                settings.put(key, value);
            }
        }
        return settings;
    }

    public static synchronized void setProfileSet(String profile, String set, Map<String, String> settings)
    {
        JSONObject theProfile = null;
        JSONObject theSet = new JSONObject();

        if (profiles.has(profile)) {
            theProfile = profiles.getJSONObject(profile);
        }
        else {
            theProfile = new JSONObject();
            profiles.put(profile, theProfile);
        }

        // Add the settings
        for (String key : settings.keySet()) {
            String value = settings.get(key);
            theSet.put(key, value);
        }

        // Replace any current set in the profile
        theProfile.remove(set);
        theProfile.put(set, theSet);
    }

    public static synchronized void setProfileSet(JSONObject theProfile, String set, Map<String, String> settings)
    {
        JSONObject theSet = new JSONObject();

        // Add the settings
        for (String key : settings.keySet()) {
            String value = settings.get(key);
            theSet.put(key, value);
        }

        // Replace any current set in the profile
        theProfile.remove(set);
        theProfile.put(set, theSet);
    }

    public static synchronized void save()
    {
        try {
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(vrProfileCfgFile), "UTF-8");
            String s = jsonConfigRoot.toString(3);
			osw.write(s);
			osw.flush();
			osw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized boolean profileExists(String profileName)
    {
        return profiles.has(profileName);
    }

    public static synchronized SortedSet<String> getProfileList()
    {
        Set<String> theProfiles = profiles.keySet();
        return new TreeSet<String>(theProfiles);
    }

    private static JSONObject getCurrentProfile() {
        if (!profiles.has(currentProfileName))
            return null;

        Object profileObj = profiles.get(currentProfileName);
        if (profileObj == null || !(profileObj instanceof JSONObject)) {
            return null;
        }

        return (JSONObject)profileObj;
    }

    public static synchronized String getCurrentProfileName()
    {
        return currentProfileName;
    }

    public static synchronized boolean setCurrentProfile(String profileName, StringBuilder error)
    {
        if (!profiles.has(profileName)) {
            error.append("Profile '" + profileName + "' not found.");
            return false;
        }
        currentProfileName = profileName;
        jsonConfigRoot.put(KEY_SELECTED_PROFILE, currentProfileName);
        return true;
    }

    public static synchronized boolean createProfile(String profileName, StringBuilder error)
    {
        if (profiles.has(profileName)) {
            error.append("Profile '" + profileName + "' already exists.");
            return false;
        }
        JSONObject theProfile = new JSONObject();
        profiles.put(profileName, theProfile);
        return true;
    }

    public static synchronized boolean renameProfile(String existingProfileName, String newProfileName, StringBuilder error)
    {
        if (existingProfileName.equals(ProfileManager.DEFAULT_PROFILE)) {
            error.append("Cannot rename " + ProfileManager.DEFAULT_PROFILE + " profile.");
            return false;
        }
        if (!profiles.has(existingProfileName)) {
            error.append("Profile '" + existingProfileName + "' not found.");
            return false;
        }
        if (profiles.has(newProfileName)) {
            error.append("Profile '" + newProfileName + "' already exists.");
            return false;
        }

        JSONObject theProfile = new JSONObject(profiles.getJSONObject(existingProfileName));
        profiles.remove(existingProfileName);
        profiles.put(newProfileName, theProfile);
        if (existingProfileName.equals(currentProfileName)) {
            setCurrentProfile(newProfileName, error);
        }
        return true;
    }

    public static synchronized boolean duplicateProfile(String profileName, String duplicateProfileName, StringBuilder error)
    {
        if (!profiles.has(profileName)) {
            error.append("Profile '" + profileName + "' not found.");
            return false;
        }
        if (profiles.has(duplicateProfileName)) {
            error.append("Profile '" + duplicateProfileName + "' already exists.");
            return false;
        }

        JSONObject theProfile = new JSONObject(profiles.getJSONObject(profileName));
        profiles.put(duplicateProfileName, theProfile);
        return true;
    }

    public static synchronized boolean deleteProfile(String profileName, StringBuilder error)
    {
        if (profileName.equals(ProfileManager.DEFAULT_PROFILE)) {
            error.append("Cannot delete " + ProfileManager.DEFAULT_PROFILE + " profile.");
            return false;
        }
        if (!profiles.has(profileName)) {
            error.append("Profile '" + profileName + "' not found.");
            return false;
        }

        profiles.remove(profileName);
        if (profileName.equals(currentProfileName)) {
            setCurrentProfile(ProfileManager.DEFAULT_PROFILE, error);
        }

        return true;
    }

    public static void loadControllerDefaults()
    {
        if (loaded) {
            JSONObject currentProfile = getCurrentProfile();
            if (currentProfile != null) {
                try {
                    loadLegacySettings(ProfileManager.DEFAULT_BINDINGS, currentProfile, ProfileManager.PROFILE_SET_CONTROLLER_BINDINGS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String[] splitKeyValue(String s) {
        String[] array = s.split(":", 2);
        return array;
    }

    // Minecraft XBox controller defaults - TODO: Find a better place for this
    public static final String[] DEFAULT_BINDINGS = {
            "key.playerlist:b:6:Button 6",
            "axis.updown:a:2:-:Y Rotation",
            "walk.forward:a:0:-:Y ",
            "gui.axis.leftright:a:3:-:X Rotation",
            "gui.axis.updown:a:2:-:Y Rotation",
            "key.sneak:b:9:Button 9",
            "gui.Left:px:-",
            "key.itemright:b:5:Button 5",
            "gui.Right:px:+",
            "key.left:a:1:-:X ",
            "gui.Select:b:0:Button 0",
            "key.aimcenter:b:8:Button 8",
            "key.pickItem:b:2:Button 2",
            "key.menu:b:7:Button 7",
            "key.attack:a:4:-:Z ",
            "gui.Up:py:-",
            "key.use:a:4:+:Z ",
            "axis.leftright:a:3:-:X Rotation",
            "gui.Down:py:+",
            "key.right:a:1:+:X ",
            "key.back:a:0:+:Y ",
            "key.inventory:b:3:Button 3",
            "key.jump:b:0:Button 0",
            "key.drop:b:1:Button 1",
            "gui.Back:b:1:Button 1",
            "key.itemleft:b:4:Button 4"
    };


}
