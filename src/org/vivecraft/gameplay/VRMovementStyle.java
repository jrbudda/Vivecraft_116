package org.vivecraft.gameplay;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

// VIVE
public class VRMovementStyle
{
    public String name;
    public boolean cameraSlide;
    public boolean airSparkles;
    public boolean destinationSparkles;
    public boolean showBeam;
    public boolean beamWave;
    public boolean beamArc;
    public boolean beamSpiral;
    public boolean beamGrow;
    public boolean renderVerticalStrip;
    public float beamHalfWidth;
    public float beamSegmentLength;
    public float beamSpiralRadius;
    public int beamVStrips;
    public float textureScrollSpeed;
    public ResourceLocation texture;
    public String startTeleportingSound;
    public float startTeleportingSoundVolume;
    public String endTeleportingSound;
    public float endTeleportingSoundVolume;
    public boolean teleportOnRelease;
    public boolean arcAiming;

    private static final ResourceLocation beamPng = new ResourceLocation("textures/entity/endercrystal/endercrystal_beam.png");

    public VRMovementStyle()
    {
        setStyle( "Arc" );
    }

    public void setStyle( String requestedStyle ) {
        boolean changedStyle = true;
        if (requestedStyle == "Minimal" )
        {
            name = requestedStyle;
            cameraSlide = false;
            airSparkles = true;
            destinationSparkles = true;
            showBeam = false;
            startTeleportingSound = null;
            endTeleportingSoundVolume = 0.8f;
            endTeleportingSound = "mob.endermen.portal";
            teleportOnRelease = false;
            arcAiming = false;
        }
        else if (requestedStyle == "Beam" )
        {
            name = requestedStyle;
            cameraSlide = false;
            airSparkles = true;
            destinationSparkles = true;
            showBeam = true;
            beamWave = false;
            beamArc = false;
            beamSpiral = false;
            beamGrow = true;
            beamHalfWidth = 0.1f;
            beamSegmentLength = 0.1f;
            beamVStrips = 16;
            renderVerticalStrip = true;
            textureScrollSpeed = 3.0f;
            texture = beamPng;
            startTeleportingSound = null;
            endTeleportingSoundVolume = 0.8f;
            endTeleportingSound = "mob.endermen.portal";
            teleportOnRelease = false;
            arcAiming = false;
        }
        else if (requestedStyle == "Tunnel" )
        {
            name = requestedStyle;
            cameraSlide = false;
            airSparkles = true;
            destinationSparkles = true;
            showBeam = true;
            beamWave = false;
            beamArc = false;
            beamSpiral = true;
            beamGrow = true;
            beamHalfWidth = 0.1f;
            beamSpiralRadius = 1.6f;
            renderVerticalStrip = true;
            beamVStrips = 16;
            textureScrollSpeed = 3.0f;
            texture = beamPng;
            startTeleportingSound = null;
            endTeleportingSoundVolume = 0.8f;
            endTeleportingSound = "mob.endermen.portal";
            teleportOnRelease = false;
            arcAiming = false;
        }
        else if (requestedStyle == "Grapple" )
        {
            name = requestedStyle;
            cameraSlide = true;
            airSparkles = false;
            destinationSparkles = true;
            showBeam = true;
            beamWave = true;
            beamArc = false;
            beamSpiral = false;
            beamGrow = true;
            beamHalfWidth = 0.05f;
            beamSegmentLength = 0.05f;
            renderVerticalStrip = false;
            beamVStrips = 2;
            textureScrollSpeed = 7.0f;
            texture = beamPng;
            startTeleportingSoundVolume = 0.5f;
            endTeleportingSoundVolume = 0.5f;
            startTeleportingSound = null;
            endTeleportingSound = "mob.endermen.portal";
            teleportOnRelease = false;
            arcAiming = false;
        }
        else if (requestedStyle == "Arc")
        {
            name = requestedStyle;
            cameraSlide = false;
            airSparkles = false;
            destinationSparkles = false;
            showBeam = true;
            beamWave = false;
            beamArc = false;
            beamSpiral = false;
            beamGrow = false;
            beamHalfWidth = 0.1f;
            beamVStrips = 1;
            renderVerticalStrip = true;
            textureScrollSpeed = 3.0f;
            texture = beamPng;
            startTeleportingSound = null;
            endTeleportingSoundVolume = 0.7f;
            endTeleportingSound = null;
            teleportOnRelease = true;
            arcAiming = true;
        }
        else
        {
            changedStyle = false;
            Minecraft.getInstance().printChatMessage("Unknown teleport style requested: " + requestedStyle);
        }

        if (changedStyle && Minecraft.getInstance()!=null)
        {
				    Minecraft.getInstance().printChatMessage("Teleport style (RCTRL-M): " + name);
        }
    }
}
