import org.json.*;
import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileFilter;

/**
 * Derived from https://github.com/MinecraftForge/Installer/
 * Copyright 2013 MinecraftForge developers, & Mark Browning, StellaArtois
 *
 * Licensed under GNU LGPL v2.1 or later.
 *
 * @author mabrowning
 *
 */
public class Installer extends JPanel  implements PropertyChangeListener
{
	private static final long serialVersionUID = -562178983462626162L;
	private String tempDir = System.getProperty("java.io.tmpdir");
	/* DO NOT RENAME THESE STRING CONSTS - THEY ARE USED IN (AND THE VALUES UPDATED BY) THE AUTOMATED BUILD SCRIPTS */
    private static final boolean ALLOW_FORGE_INSTALL  = true;
    private static final boolean DEFAULT_FORGE_INSTALL= false;
    private static final boolean ALLOW_KATVR_INSTALL  = true;
    private static final boolean ALLOW_KIOSK_INSTALL  = true;
    private static final boolean ALLOW_ZGC_INSTALL    = true;
    private static final boolean ALLOW_HRTF_INSTALL   = false;
    private static final boolean PROMPT_REMOVE_HRTF   = false;
    private static final String MINECRAFT_VERSION     = "1.16.5";
    private static final String MC_VERSION            = "1.16.5";
    private static final String MC_MD5                = "a57b9157ff3bb308208e79ef9e19187e";
    private static final String OF_FILE_NAME          = "1.16.5_HD_U_G6";
    private static final String OF_MD5                = "4a13bb8132744ab7beeff9f076d48125";
    private static final String OF_VERSION_EXT        = ".jar";
    private static String FORGE_VERSION               = "36.0.1";
    private static final String HOMEPAGE_LINK         = "http://www.vivecraft.org";
    private static final String DONATION_LINK         = "https://www.patreon.com/jrbudda";
    private static final String PROJECT_NAME          = "Vivecraft";
	/* END OF DO NOT RENAME */
	
	private static final String OF_LIB_PATH           = "libraries/optifine/OptiFine/";
	private static final String DEFAULT_PROFILE_NAME = PROJECT_NAME + " " + MINECRAFT_VERSION;
	private static final String DEFAULT_PROFILE_NAME_FORGE = PROJECT_NAME + "-Forge " + MINECRAFT_VERSION;
	private static final String ORIG_FORGE_VERSION = FORGE_VERSION;

	private InstallTask task;
	private static ProgressMonitor monitor;
	static private File targetDir;
	private String[] forgeVersions = null;
	private boolean forgeVersionInstalled = false;
	private static String FULL_FORGE_VERSION = MINECRAFT_VERSION + "-" + FORGE_VERSION;
	private String forge_url = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/" + FULL_FORGE_VERSION + "/forge-" + FULL_FORGE_VERSION + "-installer.jar";
	private File forgeInstaller;
	private JTextField selectedDirText;
	private JLabel infoLabel;
	private JDialog dialog;
	private JPanel fileEntryPanel;
	private Frame emptyFrame;
	private String jar_id;
	private String version;
	private String mod = "";
	private JCheckBox useForge;
	private JCheckBox useShadersMod;
	private ButtonGroup bg = new ButtonGroup();
	private JCheckBox createProfile;
	private JComboBox forgeVersion;
	private JCheckBox useHydra;
	private JCheckBox useHrtf;
	private JCheckBox katvr;
	private JCheckBox kiosk;
	private JCheckBox optCustomForgeVersion;
	private JCheckBox useZGC;
	private JTextField txtCustomForgeVersion;
	private JComboBox ramAllocation;
	private final boolean QUIET_DEV = false;
	private File releaseNotes = null;
	private static String releaseNotePathAddition = "";
	private static JLabel instructions;

	private JTextField txtCustomProfileName;
	private JTextField txtCustomGameDir;
	private JCheckBox chkCustomProfileName;
	private JCheckBox chkCustomGameDir;

	private String userHomeDir;
	private String osType;
	private boolean isWindows = false;
	private String appDataDir;
	boolean isMultiMC = false;
	File mmcinst = null;

	public Installer(File target)
	{
		targetDir = target;
		ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JPanel logoSplash = new JPanel();
		logoSplash.setLayout(new BoxLayout(logoSplash, BoxLayout.Y_AXIS));
		try {
			// Read png
			BufferedImage image;
			image = ImageIO.read(Installer.class.getResourceAsStream("logo.png"));
			ImageIcon icon = new ImageIcon(image.getScaledInstance(500, 200,  Image.SCALE_SMOOTH));
			JLabel logoLabel = new JLabel(icon);
			logoLabel.setAlignmentX(LEFT_ALIGNMENT);
			logoLabel.setAlignmentY(CENTER_ALIGNMENT);
			if (!QUIET_DEV)	// VIVE - hide oculus logo
				logoSplash.add(logoLabel);
		} catch (IOException e) {
		} catch( IllegalArgumentException e) {
		}

		userHomeDir = System.getProperty("user.home", ".");
		osType = System.getProperty("os.name").toLowerCase();
		if (osType.contains("win"))
		{
			isWindows = true;
			appDataDir = System.getenv("APPDATA");
		}

		version = "UNKNOWN";

		try {
			InputStream ver = Installer.class.getResourceAsStream("version");
			if( ver != null )
			{
				String[] tok = new BufferedReader(new InputStreamReader(ver)).readLine().split(":");
				if( tok.length > 0)
				{
					jar_id = tok[0];
					version = tok[1];
				} else {
					throw new Exception("token length is 0!");
				}
			} else {
				throw new Exception("version stream is null!");
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null,
					e.getMessage(),"",JOptionPane.WARNING_MESSAGE);
		}
		// Read release notes, save to file
		String tmpFileName = System.getProperty("java.io.tmpdir") + releaseNotePathAddition + "Vivecraft" + version.toLowerCase() + "_release_notes.txt";
		releaseNotes = new File(tmpFileName);
		InputStream is = Installer.class.getResourceAsStream("release_notes.txt");
		if (!copyInputStreamToFile(is, releaseNotes)) {
			releaseNotes = null;
		}

		JLabel tag = new JLabel("Welcome! This will install " + PROJECT_NAME + " " + version);
		tag.setAlignmentX(LEFT_ALIGNMENT);
		tag.setAlignmentY(CENTER_ALIGNMENT);
		logoSplash.add(tag);

		logoSplash.add(Box.createRigidArea(new Dimension(5,20)));
		tag = new JLabel("Select path to minecraft. (Only change this if using MultiMC.)");
		tag.setAlignmentX(LEFT_ALIGNMENT);
		tag.setAlignmentY(CENTER_ALIGNMENT);
		logoSplash.add(tag);

		logoSplash.setAlignmentX(LEFT_ALIGNMENT);
		logoSplash.setAlignmentY(TOP_ALIGNMENT);

		this.add(logoSplash);

		JPanel entryPanel = new JPanel();
		entryPanel.setLayout(new BoxLayout(entryPanel,BoxLayout.X_AXIS));
		entryPanel.setAlignmentX(LEFT_ALIGNMENT);
		entryPanel.setAlignmentY(TOP_ALIGNMENT);

		selectedDirText = new JTextField();
		selectedDirText.setEditable(false);
		selectedDirText.setToolTipText("Path to minecraft");
		selectedDirText.setAlignmentX(LEFT_ALIGNMENT);
		selectedDirText.setAlignmentY(TOP_ALIGNMENT);
		selectedDirText.setMaximumSize(new Dimension(400,20));

		JButton dirSelect = new JButton();
		dirSelect.setMaximumSize(new Dimension(20,20));
		dirSelect.setAction(new FileSelectAction());
		dirSelect.setText("...");
		dirSelect.setToolTipText("Select an alternative minecraft directory");
		dirSelect.setAlignmentX(LEFT_ALIGNMENT);
		dirSelect.setAlignmentY(TOP_ALIGNMENT);

		entryPanel.add(selectedDirText);
		entryPanel.add(dirSelect);

		infoLabel = new JLabel();
		infoLabel.setHorizontalTextPosition(JLabel.LEFT);
		infoLabel.setVerticalTextPosition(JLabel.TOP);
		infoLabel.setAlignmentX(LEFT_ALIGNMENT);
		infoLabel.setAlignmentY(TOP_ALIGNMENT);
		infoLabel.setVisible(false);

		fileEntryPanel = new JPanel();
		fileEntryPanel.setLayout(new BoxLayout(fileEntryPanel,BoxLayout.Y_AXIS));
		fileEntryPanel.setAlignmentX(LEFT_ALIGNMENT);
		fileEntryPanel.setAlignmentY(TOP_ALIGNMENT);

		fileEntryPanel.add(entryPanel);
		fileEntryPanel.add(infoLabel);

		this.add(fileEntryPanel);

		this.add(Box.createVerticalStrut(5));



		//Forge Options

		JPanel forgePanel = new JPanel();
		forgePanel.setLayout( new BoxLayout(forgePanel, BoxLayout.X_AXIS));
		//Create forge: no/yes buttons
		useForge = new JCheckBox();
		AbstractAction actf = new updateActionF();
		actf.putValue(AbstractAction.NAME, "Install Vivecraft with Forge");
		useForge.setAction(actf);
		useForge.setSelected(DEFAULT_FORGE_INSTALL);
		forgeVersion = new JComboBox();
		if (!ALLOW_FORGE_INSTALL)
			useForge.setEnabled(false);
		useForge.setToolTipText(
				"<html>" +
						"If checked, installs Vivecraft with Forge support.<br>" +
						"</html>");

		//Add "yes" and "which version" to the forgePanel
		useForge.setAlignmentX(LEFT_ALIGNMENT);
		forgeVersion.setAlignmentX(LEFT_ALIGNMENT);
		forgePanel.setAlignmentX(LEFT_ALIGNMENT);
		forgePanel.add(useForge);

		optCustomForgeVersion = new JCheckBox();
		

		AbstractAction actf2 = new updateActionF();
		actf2.putValue(AbstractAction.NAME, "Custom Version");
		optCustomForgeVersion.setAction(actf2);

		txtCustomForgeVersion = new JTextField(FORGE_VERSION);
		txtCustomForgeVersion.setMaximumSize(new Dimension(100,20));
		forgePanel.add(optCustomForgeVersion);
		forgePanel.add(txtCustomForgeVersion);
		//forgePanel.add(forgeVersion);

		//Create Profile
		createProfile = new JCheckBox("", true);
		AbstractAction actp = new updateActionP();
		actp.putValue(AbstractAction.NAME, "Create Vivecraft launcher profile");
		createProfile.setAction(actp);
		createProfile.setAlignmentX(LEFT_ALIGNMENT);
		createProfile.setSelected(true);
		createProfile.setToolTipText(
				"<html>" +
						"Creates or updates a Minecraft Launcher profile for Vivecraft with the selected settings.<br>" +
						"You should typically leave this checked." +
						"</html>");

		//Binaural Audio

		useHrtf = new JCheckBox("Enable binaural audio (Only needed once per PC)", false);
		useHrtf.setToolTipText(
				"<html>" +
						"If checked, the installer will create the configuration file needed for OpenAL HRTF<br>" +
						"ear-aware sound in Minecraft (and other games).<br>" +
						" If the file has previously been created, you do not need to check this again.<br>" +
						" NOTE: Your sound card's output MUST be set to 44.1Khz.<br>" +
						" WARNING, will overwrite " + (isWindows ? (appDataDir + "\\alsoft.ini") : (userHomeDir + "/.alsoftrc")) + "!<br>" +
						" Delete the " + (isWindows ? "alsoft.ini" : "alsoftrc") + " file to disable HRTF again." +
						"</html>");
		useHrtf.setAlignmentX(LEFT_ALIGNMENT);

		//ShadersMod

		useShadersMod = new JCheckBox();
		useShadersMod.setAlignmentX(LEFT_ALIGNMENT);
		AbstractAction acts = new updateActionSM();
		acts.putValue(AbstractAction.NAME, "Install Vivecraft with ShadersMod 2.3.29");
		useShadersMod.setAction(acts);
		useShadersMod.setToolTipText(
				"<html>" +
						"If checked, sets the vivecraft profile to use ShadersMod <br>" +
						"support." +
						"</html>");

		//RAM Allocation

		JPanel ramPanel = new JPanel();
		ramPanel.setLayout( new BoxLayout(ramPanel, BoxLayout.X_AXIS));
		ramPanel.setAlignmentX(LEFT_ALIGNMENT);
		ramPanel.setAlignmentY(TOP_ALIGNMENT);

		Integer[] rams = {1,2,4,6,8};

		ramAllocation = new JComboBox(rams);
		ramAllocation.setSelectedIndex(1);
		ramAllocation.setToolTipText(
				"<html>" +
						"Select the amount of Ram, in GB to allocate to the Vivecraft profile.<br>" +
						"2GB is recommended. More than 1GB of ram requires 64 bit PC and java." +
						"</html>");
		ramAllocation.setAlignmentX(LEFT_ALIGNMENT);
		ramAllocation.setMaximumSize( new Dimension((int)ramAllocation.getPreferredSize().getWidth(), 20));
		AbstractAction actram = new updateActionRam();
		actram.putValue(AbstractAction.NAME, "Profile Ram Allocation (GB)");
		ramAllocation.setAction(actram);

		JLabel ram = new JLabel("         Profile Ram Allocation (GB) ");
		ram.setAlignmentX(LEFT_ALIGNMENT);

		ramPanel.add(ram);
		ramPanel.add(ramAllocation);

		//Custom Profile

		JPanel namePanel = new JPanel();
		namePanel.setLayout( new BoxLayout(namePanel, BoxLayout.X_AXIS));
		namePanel.setAlignmentX(LEFT_ALIGNMENT);
		namePanel.setAlignmentY(TOP_ALIGNMENT);

		txtCustomProfileName = new JTextField();
		txtCustomProfileName.setAlignmentX(LEFT_ALIGNMENT);
		txtCustomProfileName.setMaximumSize(new Dimension(250,20));
		txtCustomProfileName.setEditable(false);

		chkCustomProfileName = new JCheckBox();
		chkCustomProfileName.setAlignmentX(LEFT_ALIGNMENT);
		AbstractAction u = new updateTxtEnabled();
		u.putValue(AbstractAction.NAME, "Custom Profile Name");
		chkCustomProfileName.setAction(u);
		chkCustomProfileName.setToolTipText(
				"<html>" +
						"Enter a custom name for this profile</html>");

		namePanel.add(Box.createRigidArea(new Dimension(36,20)));
		namePanel.add(chkCustomProfileName);
		namePanel.add(txtCustomProfileName);

		// Custom Game Dir

		JPanel gameDirPanel = new JPanel();
		gameDirPanel.setLayout( new BoxLayout(gameDirPanel, BoxLayout.X_AXIS));
		gameDirPanel.setAlignmentX(LEFT_ALIGNMENT);
		gameDirPanel.setAlignmentY(TOP_ALIGNMENT);

		txtCustomGameDir= new JTextField();
		txtCustomGameDir.setAlignmentX(LEFT_ALIGNMENT);
		txtCustomGameDir.setMaximumSize(new Dimension(400,20));
		txtCustomGameDir.setEditable(false);

		chkCustomGameDir = new JCheckBox("Modpack Directory");
		chkCustomGameDir.setAlignmentX(LEFT_ALIGNMENT);
		chkCustomGameDir.setToolTipText(
				"<html>" +
						"Points the profile at a different game directory.<br>" +
						"Select this to use Vivecraft with a modpack.<br>" +
						"The game directory should contain the 'mods' " +
						"directory of the desired pack." +
						"</html>");

		JButton gdirSelect = new JButton();
		gdirSelect.setAction(new GameDirSelectAction());
		gdirSelect.setText("...");
		gdirSelect.setMaximumSize(new Dimension(20,20));
		gdirSelect.setToolTipText("Select a modpack directory");
		entryPanel.add(gdirSelect);

		gameDirPanel.add(Box.createRigidArea(new Dimension(36,20)));
		gameDirPanel.add(chkCustomGameDir);
		gameDirPanel.add(txtCustomGameDir);
		gameDirPanel.add(gdirSelect);

		// KATVR

		katvr = new JCheckBox("KATVR Treadmill Driver", false);
		katvr.setToolTipText(
				"<html>" +
						"If checked, install the drivers needed for KATVR Treadmill<br>" +
						"DO NOT select this unless you have the KATVR runtime installed.</html>");
		katvr.setAlignmentX(LEFT_ALIGNMENT);
		katvr.setEnabled(isWindows);


		kiosk = new JCheckBox("Kiosk Mode", false);
		kiosk.setToolTipText(
				"<html>" +
						"If checked, disables use of in-game menu via controller" +
						"</html>");
		kiosk.setAlignmentX(LEFT_ALIGNMENT);

		useZGC = new JCheckBox();
		AbstractAction zgcAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (useZGC.isSelected()) {
					JPanel panel = new JPanel();
					panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
					panel.add(new JLabel(
							"<html>ZGC is an experimental garbage collector available in Java 14+.<br>" +
									"It can significantly reduce GC stutter, but may have stability issues as it is still in development.<br>" +
									"Your launcher profile must be configured to use Java 14 or the game will crash with this option enabled.<br>" +
									"The installer will prompt you to locate the Java 14 runtime and do this for you, however it must be installed before proceeding.<html>"
					));
					panel.add(linkify("You can download the latest release of Java 14 at AdoptOpenJDK.", "https://adoptopenjdk.net/archive.html?variant=openjdk14&jvmVariant=hotspot", "AdoptOpenJDK"));
					panel.add(new JLabel("<html><br>Do you wish to continue installation with this option enabled?</html>"));
					int res = JOptionPane.showOptionDialog(
							null, panel, "Warning!",
							JOptionPane.YES_NO_OPTION,
							JOptionPane.WARNING_MESSAGE, null, new String[]{"Yes", "No"}, "No"
					);
					if (res == JOptionPane.NO_OPTION)
						useZGC.setSelected(false);
				}
			}
		};
		zgcAction.putValue(AbstractAction.NAME, "Enable ZGC");
		useZGC.setAction(zgcAction);
		useZGC.setToolTipText("<html>Enables stutter-free Java 14+ garbage collector.</html>");
		useZGC.setAlignmentX(LEFT_ALIGNMENT);
		
		this.add(forgePanel);
		this.add(createProfile);
		this.add(ramPanel);
		this.add(namePanel);
		this.add(gameDirPanel);
		if(ALLOW_HRTF_INSTALL)this.add(useHrtf);
		this.add(new JLabel("         "));
		if(ALLOW_KATVR_INSTALL||ALLOW_KIOSK_INSTALL||ALLOW_ZGC_INSTALL) this.add(new JLabel("Advanced Options"));
		if(ALLOW_ZGC_INSTALL) this.add(useZGC);
		if(ALLOW_KIOSK_INSTALL) this.add(kiosk);
		if(ALLOW_KATVR_INSTALL) this.add(katvr);

		this.add(Box.createRigidArea(new Dimension(5,20)));

		instructions = new JLabel("",SwingConstants.CENTER);
		instructions.setAlignmentX(CENTER_ALIGNMENT);
		instructions.setAlignmentY(TOP_ALIGNMENT);
		instructions.setForeground(Color.RED);
		instructions.setPreferredSize(new Dimension(20, 40));
		this.add(instructions);


		this.add(Box.createVerticalGlue());
		JLabel wiki = linkify("Vivecraft home page",HOMEPAGE_LINK,"Vivecraft Home");
		JLabel donate = linkify("If you think Vivecraft is awesome, please consider supporting us on Patreon",DONATION_LINK,"jrbudda's Patreon");
		JLabel optifine = linkify("Vivecraft includes Optifine. Consider supporting it as well.","http://optifine.net/donate.php","http://optifine.net/donate.php");

		wiki.setAlignmentX(CENTER_ALIGNMENT);
		wiki.setHorizontalAlignment(SwingConstants.CENTER);
		donate.setAlignmentX(CENTER_ALIGNMENT);
		donate.setHorizontalAlignment(SwingConstants.CENTER);
		optifine.setAlignmentX(CENTER_ALIGNMENT);
		optifine.setHorizontalAlignment(SwingConstants.CENTER);

		this.add(Box.createRigidArea(new Dimension(5,20)));
		this.add( wiki );
		this.add( donate );
		this.add( optifine );
		updateFilePath();
		updateInstructions();
	}


	public void run()
	{
		JOptionPane optionPane = new JOptionPane(this, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, new String[]{"Install", "Cancel"});

		emptyFrame = new Frame(PROJECT_NAME + " Installer");
		emptyFrame.setUndecorated(true);
		emptyFrame.setVisible(true);
		emptyFrame.setLocationRelativeTo(null);
		dialog = optionPane.createDialog(emptyFrame, PROJECT_NAME + " Installer");
		dialog.setResizable(true);
		dialog.setSize(620,748);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setVisible(true);
		String str =  ((String)optionPane.getValue());
		if (str !=null && ((String)optionPane.getValue()).equalsIgnoreCase("Install"))
		{

			String check = System.getenv("_JAVA_OPTIONS");
			if (check != null && check.toLowerCase().contains("xmx")){
				JOptionPane.showOptionDialog(
						null,
						"The installer has detected a java override environment variable on your system\n"+
								"This will limit the maximum amount of memory available to java and may cause Minecraft to crash or run poorly.\n"+
								"You should remove this variable before launching the game.\n\n"+
								"Found _JAVA_OPTIONS " + check,
						"Warning!",
						JOptionPane.DEFAULT_OPTION,
						JOptionPane.ERROR_MESSAGE, null, null, null);
			}

			//check for multimc
			if (targetDir.exists())
			for(File f : targetDir.listFiles()){
				if(f.getName().equalsIgnoreCase("multimc.exe") || (f.getName().equalsIgnoreCase("multimc") && f.isFile()) || f.getName().equalsIgnoreCase("multimc.cfg")){
					ArrayList<File> ilist = new ArrayList<File>();
					File insts = new File(targetDir, "instances");
					if (!insts.exists()) {
						JOptionPane.showMessageDialog(null, "MultiMC files were detected in the install path, but the instances directory is missing, so we're going to assume it isn't MultiMC.\nIf it actually is MultiMC, set up an instance for Vivecraft first, then run this installer again.", "MultiMC Detection Failed", JOptionPane.WARNING_MESSAGE);
						break;
					}
					for(File inst : insts.listFiles()){
						if(inst.isDirectory() && !inst.getName().startsWith("_"))
							ilist.add(inst);
					}
					JComboBox icb = new JComboBox(ilist.toArray());
					File sel =(File) JOptionPane.showInputDialog(null,"Select MultiMC Instance.","MultiMC Detected", JOptionPane.PLAIN_MESSAGE, null, ilist.toArray(), null);
					if(sel != null){
						mmcinst = sel;
						isMultiMC = true;
					} else {
						dialog.dispose();
						emptyFrame.dispose();
					}
					break; // don't ask multiple times
				}
			}
			//

			int option = 0;
			String msg = "Please ensure you have closed the Minecraft Launcher before proceeding.";

			if(isMultiMC)
				msg = "Please ensure you have closed MultiMC before proceeding.";

			if(createProfile.isSelected() || isMultiMC)
				option = JOptionPane.showOptionDialog(
						null,
						msg,
						"Important!",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE, null, null, null);
			if (option == JOptionPane.OK_OPTION) {
				monitor = new ProgressMonitor(null, "Installing Vivecraft...", "", 0, 100);
				monitor.setMillisToDecideToPopup(0);
				monitor.setMillisToPopup(0);

				task = new InstallTask();
				task.addPropertyChangeListener(this);
				task.execute();
			}
			else{
				dialog.dispose();
				emptyFrame.dispose();
			}
		}
		else{
			dialog.dispose();
			emptyFrame.dispose();
		}
	}


	class InstallTask extends SwingWorker<Void, Void>{
		/*
		 * Main task. Executed in background thread.
		 */
		public String finalMessage;


		@Override
		public Void doInBackground()
		{
			StringBuilder sbErrors = new StringBuilder();
			String minecriftVersionName = "vivecraft-" + version + mod;
			boolean checkedRedists = false;
			boolean redistSuccess = true;
			boolean downloadedForge = false;
			boolean installedForge = false;

			if (useForge.isSelected())
				mod = "-forge";

			monitor.setProgress(0);

			try {
				// Set progress dialog size (using reflection - hacky)
				Field progressdialog = monitor.getClass().getDeclaredField("dialog");
				if (progressdialog != null) {
					progressdialog.setAccessible(true);
					Dialog dlg = (Dialog) progressdialog.get(monitor);
					if (dlg != null) {
						dlg.setSize(550, 200);
						dlg.setLocationRelativeTo(null);
					}
				}
			}
			catch (NoSuchFieldException e) {}
			catch (IllegalAccessException e) {}


			finalMessage = "Failed: Couldn't download C++ redistributables. ";
			monitor.setNote("Checking for required libraries...");
			monitor.setProgress(5);

			boolean downloadedOptifine = false;

			if(OF_FILE_NAME != ""){
				finalMessage = "Failed: Couldn't download Optifine. ";
				monitor.setNote("Checking Optifine... Please donate to them!");
				monitor.setProgress(42);
				// Attempt optifine download...
				monitor.setNote("Downloading Optifine... Please donate to them!");

				for (int i = 1; i <= 3; i++)
				{

					if (monitor.isCanceled()) return null;

					if (DownloadOptiFine())
					{
						// Got it!
						downloadedOptifine = true;
						break;
					}

					// Failed. Sleep a bit and retry...
					if (i < 3) {
						try {
							Thread.sleep(i * 1000);
						}
						catch (InterruptedException e) {
						}
						monitor.setNote("Downloading Optifine...retrying...");
					}
				}
			} else {
				downloadedOptifine = true;
			}

			monitor.setProgress(50);

			// VIVE START - install openVR
			monitor.setProgress(52);
			monitor.setNote("Installing OpenVR...");
			finalMessage = "Failed: Couldn't extract openvr_api.dll to .minecraft folder.";
			if(!InstallOpenVR())
			{
				monitor.close();
				return null;
			}
			// VIVE END - install openVR


			// Setup forge if necessary
			if(useForge.isSelected()){

				if(optCustomForgeVersion.isSelected())
					FORGE_VERSION = txtCustomForgeVersion.getText();

				FULL_FORGE_VERSION = MINECRAFT_VERSION + "-" + FORGE_VERSION;
				forgeInstaller = new File(tempDir + "/forge-" + FULL_FORGE_VERSION + "-installer.jar");
				forge_url = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/" + FULL_FORGE_VERSION + "/forge-" + FULL_FORGE_VERSION + "-installer.jar";

				if( targetDir.exists() ) {
					File ForgeDir = new File( targetDir, "libraries"+File.separator+"net"+File.separator+"minecraftforge"+File.separator+"forge");
					if( ForgeDir.isDirectory() ) {
						forgeVersions = ForgeDir.list();
						if (forgeVersions != null && forgeVersions.length > 0) {
							// Check for the currently required forge
							for (String forgeVersion : forgeVersions) {
								if (forgeVersion.contains(FORGE_VERSION)) {
									File forgeVersionDir = new File(ForgeDir, forgeVersion);
									if (forgeVersionDir.isDirectory()) {
										for (File forgeVersionFile : forgeVersionDir.listFiles()) {
											if (forgeVersionFile.length() > 512000) { // check for some realistically sized files because Mojang's launcher does stupid nonsense
												forgeVersionInstalled = true;
												break;
											}
										}
									}
									break;
								}
							}
						}
					}
				}

				if (useForge.isSelected() && !forgeVersionInstalled && !isMultiMC) {
					monitor.setProgress(55);
					monitor.setNote("Downloading Forge " + FULL_FORGE_VERSION + "...");
					downloadedForge = downloadFile(forge_url, forgeInstaller);
					if(!downloadedForge)
						JOptionPane.showMessageDialog(null, "Could not download Forge. Please exit this installer and download it manually", "Forge Installation", JOptionPane.WARNING_MESSAGE);
				}

				if (downloadedForge  && !forgeVersionInstalled) {
					monitor.setProgress(65);
					monitor.setNote("Installing Forge " + FULL_FORGE_VERSION + "...");
					installedForge = installForge(forgeInstaller);
				}
			}

			monitor.setProgress(75);
			monitor.setNote("Extracting correct Minecrift version...");
			finalMessage = "Failed: Couldn't extract Minecrift. Try redownloading this installer.";

			if(!ExtractVersion())
			{
				monitor.close();
				return null;
			}

			finalMessage = "Failed to setup HRTF.";

			if(useHrtf.isSelected())
			{
				monitor.setProgress(85);
				monitor.setNote("Configuring HRTF audio...");
				if(!EnableHRTF())
				{
					sbErrors.append("Failed to set up HRTF! Vivecraft will still work but audio won't be binaural.\n");
				}
			}

			if(PROMPT_REMOVE_HRTF)
				DeleteLegacyHRTF();

			boolean profileCreated = false;
			finalMessage = "Failed: Couldn't setup profile!";

			String profileName = getMinecraftProfileName(useForge.isSelected(), useShadersMod.isSelected());
			if(chkCustomProfileName.isSelected() && txtCustomProfileName.getText().trim() != ""){
				profileName = txtCustomProfileName.getText();
			}

			if(!isMultiMC){
				if (createProfile.isSelected())
				{
					monitor.setProgress(95);
					monitor.setNote("Creating Vivecraft profile...");

					if (!updateLauncherJson(targetDir, minecriftVersionName, profileName))
						sbErrors.append("Failed to set up 'Vivecraft' profile (you can still manually select Edit Profile->Use Version " + minecriftVersionName + " in the Minecraft launcher)\n");
					else
						profileCreated = true;
				}
			} else {
				if (!updateMMCInst(mmcinst, minecriftVersionName))
					sbErrors.append("Failed to set up 'Vivecraft' into instance.");
				else
					profileCreated = true;
			}

			if (!downloadedOptifine) {
				finalMessage = "Installed (but failed to download OptiFine). Restart Minecraft" +
						(profileCreated == false ? " and Edit Profile->Use Version " + minecriftVersionName : " and select the '" + getMinecraftProfileName(useForge.isSelected(), useShadersMod.isSelected()) + "' profile.") +
						"\nPlease download OptiFine " + OF_FILE_NAME + " from https://optifine.net/downloads before attempting to play." +
						"\nDo not run and install it, instead rename the file to OptiFine-" + OF_FILE_NAME + " (note the hyphen) and manually place it into the following directory:" +
						"\n" + (isMultiMC ? new File(mmcinst, "libraries").getAbsolutePath() : new File(targetDir, OF_LIB_PATH + OF_FILE_NAME).getAbsolutePath());
			}
			else {
				if(isMultiMC && mmcinst != null)
					if (profileCreated) finalMessage = "Installed successfully!. MultiMC Instance: " + mmcinst.toString();
					else finalMessage = "Installed but failed to update instance, launch may fail. See vivecraft.org for manual configuration.";
				else
					finalMessage = "Installed successfully! Restart Minecraft" +
							(profileCreated == false ? " and Edit Profile->Use Version " + minecriftVersionName : " and select the '" + profileName + "' profile.");
			}

			monitor.setProgress(100);
			monitor.close();
			return null;
		}

		/*
		 * Executed in event dispatching thread
		 */
		@Override
		public void done() {
			setCursor(null); // turn off the wait cursor
			JOptionPane.showMessageDialog(null, finalMessage, "Complete", JOptionPane.INFORMATION_MESSAGE);
			dialog.dispose();
			emptyFrame.dispose();
		}


		private boolean DownloadOptiFine()
		{
			boolean success = true;
			boolean deleted = false;

			try {
				File fod = new File(targetDir,OF_LIB_PATH+OF_FILE_NAME+"_LIB");
				if(isMultiMC)
					fod = new File(mmcinst,"libraries");
				fod.mkdirs();
				File fo = new File(fod,"OptiFine-"+OF_FILE_NAME+"_LIB.jar");

				// Attempt to get the Optifine MD5
				String optOnDiskMd5 = GetMd5(fo);
				System.out.println(optOnDiskMd5 == null ? fo.getCanonicalPath() : fo.getCanonicalPath() + " MD5: " + optOnDiskMd5);

				// Test MD5
				if (optOnDiskMd5 == null)
				{
					// Just continue...
					monitor.setNote("Optifine not found - downloading");
				}
				else if (!optOnDiskMd5.equalsIgnoreCase(OF_MD5)) {
					// Bad copy. Attempt delete just to make sure.
					monitor.setNote("Optifine MD5 bad - downloading");

					try {
						deleted = fo.delete();
					}
					catch (Exception ex1) {
						JOptionPane.showMessageDialog(null, "Could not delete existing Optifine jar " +ex1.getLocalizedMessage(), "Optifine Installation", JOptionPane.WARNING_MESSAGE);
						ex1.printStackTrace();
					}
				}
				else {
					// A good copy!
					monitor.setNote("Optifine MD5 good! " + OF_MD5);
					return true;
				}

				// Need to attempt download...
				success = downloadFile("http://vivecraft.org/jar/Optifine/OptiFine-" + OF_FILE_NAME + "_LIB" + OF_VERSION_EXT, fo);
				// Check (potentially) downloaded optifine md5
				optOnDiskMd5 = GetMd5(fo);
				if (success == false || optOnDiskMd5 == null || !optOnDiskMd5.equalsIgnoreCase(OF_MD5)) {
					// No good
					if (optOnDiskMd5 != null)
						monitor.setNote("Optifine - bad MD5. Got " + optOnDiskMd5 + ", expected " + OF_MD5);
					try {
						deleted = fo.delete();
					}
					catch (Exception ex1) {
						JOptionPane.showMessageDialog(null, "Could not delete existing Optifine jar " +ex1.getLocalizedMessage(), "Download File", JOptionPane.WARNING_MESSAGE);
						ex1.printStackTrace();
					}
					return false;
				}

				return true;
			} catch (Exception e) {
				finalMessage += " Error: "+e.getLocalizedMessage();
			}
			return false;
		}

		private boolean downloadFile(String surl, File fo)
		{
			return downloadFile(surl, fo, null);
		}

		private boolean downloadFile(String surl, File fo, String md5)
		{
			boolean success = true;

			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(fo);
				System.out.println(surl);
				URL url = new URL(surl);
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				conn.setConnectTimeout(15000);
				conn.setReadTimeout(60000);
				InputStream is = conn.getInputStream();

				byte[] bytes = new byte[16384];
				int count;
				while ((count = is.read(bytes, 0, bytes.length)) != -1) {
					fos.write(bytes, 0, count);
				}

				fos.flush();
			}
			catch(Exception ex) {
				JOptionPane.showMessageDialog(null, "Could not download from " + surl + " to " + fo.getName() + " \r\n " + ex.getLocalizedMessage(), "Error downloading", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
				success = false;
			}
			finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (Exception e) { }
				}
			}
			if (success) {
				if (!checkMD5(fo, md5)){
					JOptionPane.showMessageDialog(null, "Bad md5 for " + fo.getName() + "!" + " actual: " + GetMd5(fo).toLowerCase(),"Error downloading", JOptionPane.ERROR_MESSAGE);
					fo.delete();
					success = false;
				}
			} else {
				JOptionPane.showMessageDialog(null, "Could not install " + surl, "Download File", JOptionPane.INFORMATION_MESSAGE);
			}
			return success;
		}

		private boolean checkMD5(File a, String b){
			if (a.exists() == false) return false;
			if(b == null) return true;
			return GetMd5(a).equalsIgnoreCase(b);
		}

		private String GetMd5(File fo)
		{
			if (!fo.exists())
				return null;

			if (fo.length() < 1)
				return null;

			FileInputStream fis = null;
			try {
				MessageDigest md = MessageDigest.getInstance("MD5");
				fis = new FileInputStream(fo);

				byte[] buffer = new byte[(int)fo.length()];
				int numOfBytesRead = 0;
				while( (numOfBytesRead = fis.read(buffer)) > 0)
				{
					md.update(buffer, 0, numOfBytesRead);
				}
				byte[] hash = md.digest();
				StringBuilder sb = new StringBuilder();
				for (byte b : hash) {
					sb.append(String.format("%02X", b));
				}
				return sb.toString();
			}
			catch (Exception ex)
			{
				return null;
			}
			finally {
				if (fis != null)
				{
					try {
						fis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		// Shamelessly ripped from Forge ClientInstall
		private boolean installForge(File target)
		{
			try {
				JOptionPane.showMessageDialog(null, "The Forge installer will launch. In it, please ensure \"Install client\" is selected, and do not change the install directory.", "Forge Installation", JOptionPane.INFORMATION_MESSAGE);
				final Process proc = new ProcessBuilder(isWindows ? "javaw" : "java", "-jar", target.getAbsolutePath()).start();
				new Thread("Forge Installer Stdout") { // needed otherwise subprocess blocks
					@Override
					public void run() {
						try {
							BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
							String line;
							while ((line = br.readLine()) != null) {
								System.out.println(line);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}.start();
				new Thread("Forge Installer Stderr") { // same
					@Override
					public void run() {
						try {
							BufferedReader br = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
							String line;
							while ((line = br.readLine()) != null) {
								System.err.println(line);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}.start();
				proc.waitFor();
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(null, "Error occurred launching Forge installer: " + ex.getClass().getName() + ": " + ex.getMessage() + "\nYou will need to install Forge " + FULL_FORGE_VERSION + " manually.", "Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}

			return true;
		}

		private boolean ExtractVersion() {
			if( jar_id != null )
			{
				InputStream version_json;
				if(isMultiMC) {
					String filename = "version-multimc.json";
					if (useForge.isSelected())
						filename = "version-multimc-forge.json";
					version_json = Installer.class.getResourceAsStream(filename);
				}
				else if(useForge.isSelected())
				{
					String filename;

					filename = "version-forge.json";
					mod="-forge";

					version_json = new FilterInputStream( Installer.class.getResourceAsStream(filename) ) {
						public int read(byte[] buff) throws IOException {
							int ret = in.read(buff);
							if( ret > 0 ) {
								String s = new String( buff,0, ret, "UTF-8");
								if(optCustomForgeVersion.isSelected())
									s = s.replace(ORIG_FORGE_VERSION, FORGE_VERSION);
								ret = s.length();
								System.arraycopy(s.getBytes("UTF-8"), 0, buff, 0, ret);
							}
							return ret;
						}
					};
				} else {
					String filename;
					if( useShadersMod.isSelected() ) {
						filename = "version-shadersmod.json";
						mod="-shadersmod";
					} else {
						filename = "version.json";
					}
					version_json = Installer.class.getResourceAsStream(filename);
				}

				jar_id += mod;
				InputStream version_jar = Installer.class.getResourceAsStream("version.jar");
				if( version_jar != null && version_json != null )
					try {
						File ver_dir = null;
						if(isMultiMC){
							ver_dir = new File(mmcinst,"patches");
							jar_id = "vivecraft";
						}
						else
							ver_dir = new File(new File(targetDir,"versions"),jar_id);

						ver_dir.mkdirs();
						File ver_json_file = new File (ver_dir, jar_id+".json");
						FileOutputStream ver_json = new FileOutputStream(ver_json_file);
						int d;
						byte data[] = new byte[40960];

						// Extract json
						while ((d = version_json.read(data)) != -1) {
							ver_json.write(data,0,d);
						}
						ver_json.close();

						//modify json args if needed
						try {
							int jsonIndentSpaces = 2;
							File fileJson = ver_json_file;
							String json = readAsciiFile(fileJson);
							json = json.replace("$FILE",jar_id);
							JSONObject root = new JSONObject(json);

							String args = (String)root.opt("minecraftArguments");

							if(args!=null) {
								if(katvr.isSelected()) args += " --katvr";
								if(kiosk.isSelected()) args += " --kiosk";
								root.put("minecraftArguments", args);
							}

							if(isMultiMC)
								root.remove("id");
							
							/*if(isMultiMC && useForge.isSelected()) {
								JSONArray tw = (JSONArray) root.get("+tweakers");
								tw = new JSONArray();
								tw.put("org.vivecraft.tweaker.MinecriftForgeTweaker");
								tw.put("net.minecraftforge.fml.common.launcher.FMLTweaker");
								tw.put("optifine.OptiFineForgeTweaker");
								root.put("+tweakers", tw);
							}*/

							FileWriter fwJson = new FileWriter(fileJson);
							fwJson.write(root.toString(jsonIndentSpaces));
							fwJson.flush();
							fwJson.close();
						}
						catch (Exception e) {
							finalMessage += " Error: " + e.getMessage();
						}

						// Extract new lib
						File lib_dir = new File(targetDir,"libraries/com/mtbs3d/minecrift/"+version);
						if(isMultiMC)
							lib_dir = new File(mmcinst,"libraries");
						lib_dir.mkdirs();
						File ver_file = new File (lib_dir, "minecrift-"+version+".jar");
						FileOutputStream ver_jar = new FileOutputStream(ver_file);
						while ((d = version_jar.read(data)) != -1) {
							ver_jar.write(data,0,d);
						}
						ver_jar.close();

						return ver_json_file.exists() && ver_file.exists();
					} catch (Exception e) {
						finalMessage += " Error: " + e.getMessage();
					}

			}
			return false;
		}

		private boolean DeleteLegacyHRTF() {
			// Find the correct location 
			File alsoftrc;

			//I honestly have no clue where Mac stores this, so I'm assuming the same as Linux.
			if (isWindows && appDataDir != null)
			{
				alsoftrc = new File(appDataDir, "alsoft.ini");
			}
			else
			{
				alsoftrc = new File(userHomeDir, ".alsoftrc");
			}
			try
			{
				//check if exists and prompt
				if(alsoftrc.exists()) {
					int ret = JOptionPane.showConfirmDialog(null,
							"Binaural Audio .ini file found. Vivecraft now handles this setting in-game.\r\nWould you like to delete this file?\r\n\r\nChoose 'No' only if you play older versions of Vivecraft or have some other need for a system-wide alsoft.ini",
							"Remove legacy file",
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE);
					if(ret == JOptionPane.YES_OPTION) {
						alsoftrc.delete();
					}
				}
			}
			catch (Exception e)
			{
				finalMessage += " Error: "+e.getLocalizedMessage();
			}

			return false;
		}

		private boolean EnableHRTF()           // Implementation by Zach Jaggi
		{
			// Find the correct location to stick alsoftrc
			File alsoftrc;

			//I honestly have no clue where Mac stores this, so I'm assuming the same as Linux.
			if (isWindows && appDataDir != null)
			{
				alsoftrc = new File(appDataDir, "alsoft.ini");
			}
			else
			{
				alsoftrc = new File(userHomeDir, ".alsoftrc");
			}
			try
			{
				//Overwrite the current file.
				alsoftrc.createNewFile();
				PrintWriter writer = new PrintWriter(alsoftrc);
				writer.write("hrtf = true\n");
				writer.write("frequency = 44100\n");
				writer.close();
				return true;
			}
			catch (Exception e)
			{
				finalMessage += " Error: "+e.getLocalizedMessage();
			}

			return false;
		}

		// VIVE START - install openVR dlls
		private boolean InstallOpenVR() {
			//nope.
			return true;
		}

		private boolean installFile(String osFolder, String resource){
			File win32_dir = new File (targetDir, osFolder);
			win32_dir.mkdirs();
			InputStream openvrdll = Installer.class.getResourceAsStream(resource);
			File dll_out = new File (targetDir, resource);
			if (!copyInputStreamToFile(openvrdll, dll_out)){
				return false;
			}

			return true;
		}

		// VIVE END - install openVR dll

		private void sleep(int millis)
		{
			try {
				Thread.sleep(millis);
			} catch (InterruptedException e) {}
		}

		private String getGCOptions() {
			if (useZGC.isSelected()) {
				return "-XX:+UnlockExperimentalVMOptions -XX:+UseZGC";
			} else {
				return "-XX:+UseParallelGC -XX:ParallelGCThreads=3 -XX:MaxGCPauseMillis=3 -Xmn256M";
			}
		}

		private int[] getRamAlloc() {
			int maxAlloc = (int)ramAllocation.getSelectedItem();
			int minAlloc = /*useZGC.isSelected()*/ true ? maxAlloc : Math.min(maxAlloc, 2);
			return new int[]{minAlloc, maxAlloc};
		}

		private String getJavaVersionFromPath(String path) {
			String out = "";
			try {
				ProcessBuilder pb = new ProcessBuilder(path, "-version");
				Process p = pb.start();
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String line;
				while ((line = br.readLine()) != null) {
					if (line.toLowerCase().contains("version")) {
						out = line.substring(line.indexOf('"') + 1, line.lastIndexOf('"'));
						break;
					}
				}
				p.destroy();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return out;
		}

		private int parseJavaVersion(String version) {
			try {
				if (version.indexOf('.') != -1)
					return Integer.parseInt(version.substring(0, version.indexOf('.')));
				else
					return Integer.parseInt(version);
			} catch (Exception ex) {
				ex.printStackTrace();
				return 0;
			}
		}

		/*
		* If the user decides to not select the Java runtime at installation, this function will
		* return the same value that was passed to it. In this case, the profile should not be changed.
		 */
		private String checkForJava14(String path) {
			String newPath = path;
			boolean first = true;
			while (true) {
				String ver = !newPath.isEmpty() ? getJavaVersionFromPath(newPath) : "0.0.0";
				if (parseJavaVersion(ver) == 14)
					break;

				if (first) {
					String javaHome = System.getProperty("java.home") + (isWindows ? "\\bin\\javaw.exe" : "/bin/java");
					String homeVer = getJavaVersionFromPath(javaHome);
					if (parseJavaVersion(homeVer) == 14)
						return javaHome;
					first = false;
				}

				int res = JOptionPane.showConfirmDialog(null,
						"The currently selected Java executable is not at least Java 14.\n" +
						"Would you like to select the correct one now?",
						"Wrong Java Version",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.ERROR_MESSAGE
				);
				if (res != JOptionPane.YES_OPTION)
					return path;

				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fileChooser.setFileHidingEnabled(false);
				if (isWindows)
					fileChooser.setCurrentDirectory(new File(System.getenv("ProgramFiles")));
				fileChooser.setFileFilter(new FileFilter() {
					@Override
					public boolean accept(File f) {
						if (!f.isFile())
							return true;
						return isWindows ? f.getName().equals("javaw.exe") : f.getName().equals("java");
					}

					@Override
					public String getDescription() {
						return "Java Executable";
					}
				});
				int response = fileChooser.showOpenDialog(null);
				if (response == JFileChooser.APPROVE_OPTION)
					newPath = fileChooser.getSelectedFile().getAbsolutePath();
			}

			return newPath;
		}

		private boolean updateLauncherJson(File mcBaseDirFile, String minecriftVer, String profileName)
		{
			boolean result = false;

			try {
				int jsonIndentSpaces = 2;
				File fileJson = new File(mcBaseDirFile, "launcher_profiles.json");
				String json = readAsciiFile(fileJson);
				JSONObject root = new JSONObject(json);
				//System.out.println(root.toString(jsonIndentSpaces));

				JSONObject profiles = (JSONObject)root.get("profiles");
				JSONObject prof = null;
				try {
					prof = (JSONObject) profiles.get(profileName);
				}
				catch (Exception e) {
					//this is normal if doesnt exist.
				}
				java.text.DateFormat dateFormat=new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
				if (prof == null) {
					prof = new JSONObject();
					prof.put("created", dateFormat.format(new java.util.Date()));
					profiles.put(profileName, prof);
				}

				prof.put("lastVersionId", minecriftVer + mod);
				int[] ramAlloc = getRamAlloc();
				prof.put("javaArgs", "-Xmx" + ramAlloc[1] + "G -Xms" + ramAlloc[0] + "G " + getGCOptions());
				prof.put("name", profileName);
				prof.put("icon", ICON);
				prof.put("type", "custom");
				prof.put("lastUsed", dateFormat.format(new java.util.Date()));
				if(chkCustomGameDir.isSelected() && txtCustomGameDir.getText().trim() != ""){
					String dir = txtCustomGameDir.getText();
					if (dir.endsWith("\\mods")) dir = dir.substring(0, dir.length()-5);
					if (dir.endsWith("\\mods\\")) dir = dir.substring(0, dir.length()-6);
					prof.put("gameDir", dir);
				} else {
					prof.remove("gameDir");
				}

				if (useZGC.isSelected()) {
					String javaExe;
					if (prof.has("javaDir"))
						javaExe = prof.getString("javaDir");
					else {
						javaExe = "";
					}

					javaExe = checkForJava14(javaExe);
					if (!javaExe.isEmpty())
						prof.put("javaDir", javaExe);
				}

				FileWriter fwJson = new FileWriter(fileJson);
				fwJson.write(root.toString(jsonIndentSpaces));
				fwJson.flush();
				fwJson.close();

				result = true;
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			return result;
		}

		private boolean updateMMCInst(File mcBaseDirFile, String minecriftVer)
		{
			boolean result = false;

			try {
				File cfg = new File(mcBaseDirFile, "instance.cfg");
				if(!cfg.exists()) return result;

				boolean setupJavaPath = useZGC.isSelected();

				String javaPath = "javaw";
				if (setupJavaPath) {
					try (BufferedReader br = new BufferedReader(new FileReader(new File(mcBaseDirFile, "../../multimc.cfg")))) {
						String line;
						while ((line = br.readLine()) != null) {
							String[] split = line.split("=", 2);
							if (split[0].equals("JavaPath")) {
								javaPath = split[1];
								break;
							}
						}
					}
				}

				BufferedReader r = new BufferedReader(new FileReader(cfg));
				java.util.List<String> lines = new ArrayList<String>();
				String l;
				while((l = r.readLine()) != null){

					if(l.startsWith("JvmArgs"))
						continue;

					if(l.startsWith("MaxMemAlloc"))
						continue;

					if(l.startsWith("MinMemAlloc"))
						continue;

					if(l.startsWith("OverrideJavaArgs"))
						continue;

					if(l.startsWith("OverrideMemory"))
						continue;
					
					if(l.startsWith("OverrideJavaLocation") && setupJavaPath)
						continue;

					if (l.startsWith("JavaPath") && setupJavaPath) {
						javaPath = l.split("=", 2)[1];
						continue;
					}

					lines.add(l);
				}

				int[] ramAlloc = getRamAlloc();
				lines.add("MinMemAlloc=" + (ramAlloc[0] * 1024));
				lines.add("MaxMemAlloc=" + (ramAlloc[1] * 1024));
				lines.add("OverrideJavaArgs=true");
				lines.add("OverrideMemory=true");
				lines.add("JvmArgs=" + getGCOptions());

				if (setupJavaPath) {
					javaPath = javaPath.replace("\\\\", "\\");
					javaPath = checkForJava14(javaPath);
					javaPath = javaPath.replace("\\", "\\\\");
					lines.add("JavaPath=" + javaPath);
					lines.add("OverrideJavaLocation=true");
				}

				r.close();

				String[] arr = lines.toArray(new String[lines.size()]);
				Arrays.sort(arr);

				BufferedWriter w = new BufferedWriter(new FileWriter(cfg,false));

				for (String string : arr) {
					w.write(string);
					w.newLine();
				}

				w.close();

				File mmcpack = new File(mcBaseDirFile, "mmc-pack.json");
				if(!mmcpack.exists()) return result;
				String json = readAsciiFile(mmcpack);

				JSONObject root = new JSONObject(json);
				JSONArray components = (JSONArray)root.get("components");

				JSONObject v = new JSONObject();
				v.put("cachedName", "Vivecraft");
				v.put("uid", "vivecraft");

				components.put(v);

				FileWriter fwJson = new FileWriter(mmcpack);
				fwJson.write(root.toString(2));
				fwJson.flush();
				fwJson.close();

				result = true;
			}
			catch (Exception e) {
				JOptionPane.showMessageDialog(null,
						e.toString(),"",JOptionPane.WARNING_MESSAGE);
			}

			return result;
		}

	}// End InstallTask

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress" == evt.getPropertyName()) {
			int progress = (Integer) evt.getNewValue();
			System.out.println(progress);
		}
	}



	private static void createAndShowGUI() {
		String userHomeDir = System.getProperty("user.home", ".");
		String osType = System.getProperty("os.name").toLowerCase();
		String mcDir = ".minecraft";
		File minecraftDir;

		if (osType.contains("win") && System.getenv("APPDATA") != null)
		{
			minecraftDir = new File(System.getenv("APPDATA"), mcDir);
		}
		else if (osType.contains("mac"))
		{
			minecraftDir = new File(new File(new File(userHomeDir, "Library"),"Application Support"),"minecraft");
		}
		else
		{
			minecraftDir = new File(userHomeDir, mcDir);
			releaseNotePathAddition = "/";
		}

		Installer panel = new Installer(minecraftDir);
		panel.run();
	}


	private class FileSelectAction extends AbstractAction
	{
		private static final long serialVersionUID = 743815386102831493L;

		@Override
		public void actionPerformed(ActionEvent e)
		{
			JFileChooser dirChooser = new JFileChooser();
			dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			dirChooser.setFileHidingEnabled(false);
			dirChooser.ensureFileIsVisible(targetDir);
			dirChooser.setSelectedFile(targetDir);
			int response = dirChooser.showOpenDialog(Installer.this);
			switch (response)
			{
				case JFileChooser.APPROVE_OPTION:
					targetDir = dirChooser.getSelectedFile();
					updateFilePath();
					break;
				default:
					break;
			}
		}
	}

	private class GameDirSelectAction extends AbstractAction
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			JFileChooser dirChooser = new JFileChooser();
			dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			dirChooser.setFileHidingEnabled(false);
			dirChooser.ensureFileIsVisible(targetDir);
			dirChooser.setSelectedFile(targetDir);
			int response = dirChooser.showOpenDialog(Installer.this);
			switch (response)
			{
				case JFileChooser.APPROVE_OPTION:
					txtCustomGameDir.setText(dirChooser.getSelectedFile().toString());
					break;
				default:
					break;
			}
		}
	}

	private class updateTxtEnabled extends AbstractAction
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			txtCustomProfileName.setEditable(chkCustomProfileName.isSelected());
		}
	}


	private class updateActionF extends AbstractAction
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			updateInstructions();
			if (useForge.isSelected()) ramAllocation.setSelectedIndex(2);
			else ramAllocation.setSelectedIndex(1);
			updateInstructions();
		}
	}

	private class updateActionSM extends AbstractAction
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			updateInstructions();
		}
	}

	private class updateActionP extends AbstractAction
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			updateInstructions();
		}
	}

	private class updateActionRam extends AbstractAction
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			updateInstructions();
		}
	}


	private void updateInstructions(){
		String out = "<html>";
		if(createProfile.isSelected()){
			out += "Please make sure the Minecraft Launcher is not running.";
			if(chkCustomProfileName.isSelected() == false){
				txtCustomProfileName.setText(getMinecraftProfileName(useForge.isSelected(), useShadersMod.isSelected()));
			}
			if (ramAllocation.getSelectedIndex() == 0) {
				out += "<br>Vivecraft may not run well with only 1 GB of memory!";
			}
		}
		if (useForge.isSelected()){
			if(optCustomForgeVersion.isSelected())
				out += "<br>Custom Forge version NOT guaranteed to work!";
		}
		out+="</html>";
		instructions.setText(out);
		ramAllocation.setEnabled(createProfile.isSelected());
		txtCustomForgeVersion.setEnabled(optCustomForgeVersion.isSelected());
		txtCustomForgeVersion.setVisible(useForge.isSelected());
		optCustomForgeVersion.setVisible(useForge.isSelected());
		this.revalidate();
	}

	private void updateFilePath()
	{
		try
		{
			targetDir = targetDir.getCanonicalFile();
			selectedDirText.setText(targetDir.getPath());
			selectedDirText.setForeground(Color.BLACK);
			infoLabel.setVisible(false);
			fileEntryPanel.setBorder(null);
			if (dialog!=null)
			{
				dialog.invalidate();
				dialog.pack();
			}
		}
		catch (IOException e)
		{

			selectedDirText.setForeground(Color.RED);
			fileEntryPanel.setBorder(new LineBorder(Color.RED));
			infoLabel.setText("<html>"+"Error!"+"</html>");
			infoLabel.setVisible(true);
			if (dialog!=null)
			{
				dialog.invalidate();
				dialog.pack();
			}
		}
		if( forgeVersions == null || forgeVersions.length == 0 )
			forgeVersions =  new String[] { };
		forgeVersion.setModel( new DefaultComboBoxModel(forgeVersions));
	}


	public static void main(String[] args)
	{
		// I'm gonna shit a JVM
		System.setProperty("java.net.preferIPv4Stack" , "true");

		try {
			// Set System L&F
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) { }
		try {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					createAndShowGUI();
				}
			});
		} catch (Exception e) { e.printStackTrace(); }
	}

	public static JLabel linkify(final String text, String URL, String toolTip)
	{
		URI temp = null;
		try
		{
			temp = new URI(URL);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		final URI uri = temp;
		final JLabel link = new JLabel();
		link.setText("<HTML><FONT color=\"#000099\">"+text+"</FONT></HTML>");
		if(!toolTip.equals(""))
			link.setToolTipText(toolTip);
		link.setCursor(new Cursor(Cursor.HAND_CURSOR));
		link.addMouseListener(new MouseListener() {
			public void mouseExited(MouseEvent arg0) {
				link.setText("<HTML><FONT color=\"#000099\">"+text+"</FONT></HTML>");
			}

			public void mouseEntered(MouseEvent arg0) {
				link.setText("<HTML><FONT color=\"#000099\"><U>"+text+"</U></FONT></HTML>");
			}

			public void mouseClicked(MouseEvent arg0) {
				if (Desktop.isDesktopSupported()) {
					try {
						Desktop.getDesktop().browse(uri);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					JOptionPane pane = new JOptionPane("Could not open link.");
					JDialog dialog = pane.createDialog(new JFrame(), "");
					dialog.setVisible(true);
				}
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
			}
		});
		return link;
	}

	private String getMinecraftProfileName(boolean usingForge, boolean sm)
	{
		if(!usingForge)	return DEFAULT_PROFILE_NAME;
		else return DEFAULT_PROFILE_NAME_FORGE;
	}

	public static String readAsciiFile(File file)
			throws IOException
	{
		FileInputStream fin = new FileInputStream(file);
		InputStreamReader inr = new InputStreamReader(fin, "ASCII");
		BufferedReader br = new BufferedReader(inr);
		StringBuffer sb = new StringBuffer();
		for (;;) {
			String line = br.readLine();
			if (line == null)
				break;

			sb.append(line);
			sb.append("\n");
		}
		br.close();
		inr.close();
		fin.close();

		return sb.toString();
	}

	private boolean copyInputStreamToFile( InputStream in, File file )
	{
		if (in == null || file == null)
			return false;

		boolean success = true;

		try {
			OutputStream out = new FileOutputStream(file);
			byte[] buf = new byte[1024];
			int len;
			while((len=in.read(buf))>0){
				out.write(buf,0,len);
			}
			out.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			success = false;
		}

		return success;
	}

	private static final String ICON = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAA4RpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuNi1jMDY3IDc5LjE1Nzc0NywgMjAxNS8wMy8zMC0yMzo0MDo0MiAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iIHhtcE1NOk9yaWdpbmFsRG9jdW1lbnRJRD0ieG1wLmRpZDpiNWVjY2I2MC02NjE1LWQwNDQtYmE0Yi1iOWM4M2YxMTY3NzYiIHhtcE1NOkRvY3VtZW50SUQ9InhtcC5kaWQ6NEU3MUI3OTBDNTg5MTFFQTk5MzNCMEQ1REY1OTdGRjMiIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6NEU3MUI3OEZDNTg5MTFFQTk5MzNCMEQ1REY1OTdGRjMiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIENDIDIwMTUgKFdpbmRvd3MpIj4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6OWExNDM3OGQtNWU3ZS1jNjRhLTk0ZjYtYWM1NGE1NmRiN2I0IiBzdFJlZjpkb2N1bWVudElEPSJhZG9iZTpkb2NpZDpwaG90b3Nob3A6NzAxZjlmMDYtYzU4OC0xMWVhLWEyMzIt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";

}
