package org.vivecraft.settings.profile;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

/**
 * Created by StellaArtois on 13/04/15.
 */
public class ProfileWriter
{
    private String activeProfileName;
    private String set;
    private Map<String, String> data = new HashMap<String, String>();
    private JSONObject theProfile = null;

    public ProfileWriter(String set)
    {
        this.activeProfileName = ProfileManager.currentProfileName;
        this.set = set;

        // Add a new empty activeProfileName set
        data = new HashMap<String, String>();
    }

    public ProfileWriter(String set, JSONObject theProfile)
    {
        this.activeProfileName = ProfileManager.currentProfileName;
        this.set = set;
        this.theProfile = theProfile;

        // Add a new empty activeProfileName set
        data = new HashMap<String, String>();
    }

    public void println(String s)
    {
        String[] array = ProfileManager.splitKeyValue(s);
        String setting = array[0];
        String value = "";
        if (array.length > 1) {
            value = array[1];
        }
        data.put(setting, value);
    }

    public void close()
    {
        if (this.theProfile == null) {
            ProfileManager.setProfileSet(this.activeProfileName, this.set, this.data);
            ProfileManager.save();
        }
        else {
            ProfileManager.setProfileSet(this.theProfile, this.set, this.data);
        }
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
