import xml.etree.ElementTree as ET
import xml.dom.minidom as MD
import sys, os
from sys import platform as _platform
import shutil

PROJECT_NAME = 'minecrift'

def createIdeaProject(project_root_dir, version, mcpDirName, are32bitNatives):

    # generate IML xml
    # VIVE - removed
    #os.path.join(project_root_dir, 'JOpenVR', 'JOpenVR', 'src'),
    srcPaths = [os.path.join(project_root_dir, 'JRift', 'JRift', 'src'), \
                os.path.join(project_root_dir, 'JMumbleLink', 'JMumble', 'src'), \
                os.path.join(project_root_dir, 'Sixense-Java', 'SixenseJava', 'src'), \
                os.path.join(project_root_dir, mcpDirName, 'src', 'minecraft')]
    libNames = [version]

    imlxml = IMLXML(srcDirPaths=srcPaths, rootDir=project_root_dir, libraryNames=libNames)
    #print "./%s.iml:\n\n%s\n" % (PROJECT_NAME, imlxml.xmlString)
    writeFile(os.path.join(project_root_dir, PROJECT_NAME +'.iml'), imlxml.xmlString)

    # generate library xml
    jarPaths = []
    jarDir = os.path.join(project_root_dir, 'lib', version)
    for root, dirs, files in os.walk(jarDir):
        for file in files:
            if file.endswith(".jar"):
                jarFile = os.path.join(root, file)
                jarPaths.append(jarFile)
    libName = version
    libXML = LibraryXML(libraryName=libName, rootDir=project_root_dir, libraryJarPathNames=jarPaths)
    #print "./.idea/libraries/%s.xml:\n\n%s\n" % (version, libXML.xmlString)
    writeFile(os.path.join(project_root_dir, '.idea', 'libraries', version + '.xml'), libXML.xmlString)

    if _platform == "linux" or _platform == "linux2":
        platformbits = 'linux/32' if are32bitNatives else 'linux/64'
        platform = 'linux'
    elif _platform == "darwin":
        platformbits = 'osx/32' if are32bitNatives else 'osx/64'
        platform = 'osx'
    elif _platform == "win32":
        platformbits = 'windows/32' if are32bitNatives else 'windows/64'
        platform = 'windows'

    mainClass = 'Start'
    # VIVE removed
    # os.path.join(project_root_dir, 'JOpenVR', 'JOpenVRLibrary', 'natives', platformbits),
    nativesPaths = [os.path.join(project_root_dir, 'JRift', 'JRiftLibrary', 'natives', platform), \
                    os.path.join(project_root_dir, 'JMumbleLink', 'JMumbleLibrary', 'natives', platform), \
                    os.path.join(project_root_dir, 'Sixense-Java', 'SixenseJavaLibrary', 'natives', platform), \
                    os.path.join(project_root_dir, 'lib', version, 'natives', platform)]
    prog_args = '--username mcuser --password pass'
    moduleName = PROJECT_NAME
    workspaceXML = WorkspaceXML(mainClassName=mainClass, mcpDir=mcpDirName, rootDir=project_root_dir, nativeDirs=nativesPaths, programParams=prog_args, moduleName=moduleName)
    #print "./.idea/workspace.xml:\n\n%s\n" % (workspaceXML.xmlString)
    writeFile(os.path.join(project_root_dir, '.idea', 'workspace.xml'), workspaceXML.xmlString)

    #print "./.idea/.name:\n\n%s\n" % (PROJECT_NAME)
    writeFile(os.path.join(project_root_dir, '.idea', '.name'), PROJECT_NAME)

    modulesXML = ModulesXML(projectName=PROJECT_NAME)
    #print "./.idea/modules.xml:\n\n%s\n" % (modulesXML.xmlString)
    writeFile(os.path.join(project_root_dir, '.idea', 'modules.xml'), modulesXML.xmlString)

class ModulesXML:

    Base_Modules = (
r"""<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="ProjectModuleManager">
    <modules>
    </modules>
  </component>
</project>
""")

    def __init__(self, projectName):

        # Read base xml
        xml = ET.ElementTree(ET.fromstring(self.Base_Modules))

        # Get ./project/component/modules element
        project = xml.getroot()
        component = project.find('component')
        modules = component.find('modules')

        # Add main class
        self._add_Module(projectName, modules)

        # get result
        self.xmlString = dumpXml(element=project)


    def _add_Module(self, name, modulesElement):

        module = ET.Element('module')
        module.attrib['fileurl'] = 'file://$PROJECT_DIR$/' + name + '.iml'
        module.attrib['filepath'] = '$PROJECT_DIR$/' + name + '.iml'
        modulesElement.append(module)


class WorkspaceXML:

    PROJECT_DIR = '$PROJECT_DIR$'

    BASE_Workspace = (
r"""<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="RunManager" selected="Application.Run Minecrift Client">
    <configuration default="false" name="Run Minecrift Client" type="Application" factoryName="Application" singleton="true">
      <extension name="coverage" enabled="false" merge="false" sample_coverage="true" runner="idea" />
      <option name="WORKING_DIRECTORY" value="file://$PROJECT_DIR$" />
      <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false" />
      <option name="ALTERNATIVE_JRE_PATH" value="" />
      <option name="ENABLE_SWING_INSPECTOR" value="false" />
      <option name="ENV_VARIABLES" />
      <option name="PASS_PARENT_ENVS" value="true" />
      <envs />
      <method />
    </configuration>
    <list size="1">
      <item index="0" class="java.lang.String" itemvalue="Application.Run Minecrift Client" />
    </list>
  </component>
  <component name="FileEditorManager">
    <leaf>
      <file leaf-file-name="Minecraft.java" pinned="false" current-in-tab="true">
        <entry file="file://$PROJECT_DIR$/$MCP_DIR$/src/minecraft/net/minecraft/client/Minecraft.java">
          <provider selected="true" editor-type-id="text-editor">
            <state vertical-scroll-proportion="0.01783591">
              <caret line="444" column="0" selection-start-line="444" selection-start-column="0" selection-end-line="444" selection-end-column="0" />
              <folding />
            </state>
          </provider>
        </entry>
      </file>
      <file leaf-file-name="EntityRenderer.java" pinned="false" current-in-tab="false">
        <entry file="file://$PROJECT_DIR$/$MCP_DIR$/src/minecraft/net/minecraft/client/renderer/EntityRenderer.java">
          <provider selected="true" editor-type-id="text-editor">
            <state vertical-scroll-proportion="0.0">
              <caret line="98" column="13" selection-start-line="98" selection-start-column="13" selection-end-line="98" selection-end-column="13" />
              <folding />
            </state>
          </provider>
        </entry>
      </file>
    </leaf>
  </component>
</project>
""")
    def __init__(self, mainClassName, rootDir, mcpDir, nativeDirs, programParams, moduleName):

        # Subst in MCP dir
        self.BASE_Workspace = self.BASE_Workspace.replace('$MCP_DIR$', mcpDir)

        # Read base xml
        xml = ET.ElementTree(ET.fromstring(self.BASE_Workspace))

        # Get ./project/component/configuration element
        project = xml.getroot()
        component = project.find('component')
        configuration = component.find('configuration')

        # Add main class
        self._add_Option('MAIN_CLASS_NAME', mainClassName, configuration)

        # Add vm args

        # Format natives path string
        pathSeparator = ':'
        if _platform == "win32":
            pathSeparator = ';'
        vm_args = '-Djava.library.path=\"'
        firstEntry = True

        # Add all native dirs, convert each to relative to project root
        for nativeDir in nativeDirs:
            nativeRelativeDir = os.path.relpath(nativeDir, rootDir)
            if firstEntry == True:
                firstEntry = False
            else:
                vm_args = vm_args + pathSeparator

            vm_args = vm_args + self.PROJECT_DIR + '/' + nativeRelativeDir

        vm_args = vm_args + '\"'

        self._add_Option('VM_PARAMETERS', vm_args, configuration)

        # Add program parameters
        self._add_Option('PROGRAM_PARAMETERS', programParams, configuration)

        # Add module name
        self._add_Module(moduleName, configuration)

        # get result
        self.xmlString = dumpXml(element=project)


    def _add_Option(self, name, value, configurationElement):

        option = ET.Element('option')
        option.attrib['name'] = name
        option.attrib['value'] = value
        configurationElement.append(option)


    def _add_Module(self, name, configurationElement):

        module = ET.Element('module')
        module.attrib['name'] = name
        configurationElement.append(module)



class LibraryXML:

    PROJECT_DIR = '$PROJECT_DIR$'

    BASE_Library = (
r"""<component name="libraryTable">
  <library>
    <CLASSES>
    </CLASSES>
    <SOURCES />
  </library>
</component>
""")

    def __init__(self, libraryName, rootDir, libraryJarPathNames):

        # Read base xml
        xml = ET.ElementTree(ET.fromstring(self.BASE_Library))

        # Get ./component/library element
        component = xml.getroot()
        library = component.find('library')

        # Add library name
        library.attrib['name'] = libraryName

        # Get classes element
        CLASSES = library.find('CLASSES')

        # Add libraries in order
        for libraryJarPathName in libraryJarPathNames:
            # convert to relative path
            relativeLibraryJarPathName = os.path.relpath(libraryJarPathName, rootDir)
            self._add_LibraryJar(self.PROJECT_DIR+'/' + relativeLibraryJarPathName, CLASSES)

        # get result
        self.xmlString = dumpXml(element=component)


    def _add_LibraryJar(self, libraryJarPathName, CLASSESElement):

        rootElement = ET.Element('root')
        rootElement.attrib['url'] = 'jar://' + libraryJarPathName + '!/'
        CLASSESElement.append(rootElement)


class IMLXML:

    PROJECT_DIR = '$MODULE_DIR$'

    BASE_IML = (
r"""<?xml version="1.0" encoding="UTF-8"?>
<module type="JAVA_MODULE" version="4">
  <component name="NewModuleRootManager" inherit-compiler-output="false">
    <output url="file://$MODULE_DIR$/bin/intelij_prod/out" />
    <output-test url="file://$MODULE_DIR$/bin/intelij_test/out" />
    <exclude-output />
    <orderEntry type="inheritedJdk" />
    <orderEntry type="sourceFolder" forTests="false" />
  </component>
</module>
""")

    def __init__(self, rootDir, srcDirPaths, libraryNames):

        # Read base xml
        xml = ET.ElementTree(ET.fromstring(self.BASE_IML))

        # Get ./module/component element
        module = xml.getroot()
        component = module.find('component')

        # Add libraries in dependency order, first has highest priority
        for libraryName in libraryNames:
            self._addIML_LibraryName(libraryName, component)

        # Add src paths
        for srcDirPath in srcDirPaths:
            # convert to relative path
            relativeSrcDirPath = os.path.relpath(srcDirPath, rootDir)
            self._addIML_SourceContent('file://' + self.PROJECT_DIR+'/' + relativeSrcDirPath, component)

        # get result
        self.xmlString = dumpXml(element=module)


    def _addIML_SourceContent(self, srcDirPath, componentElement):

        content = ET.Element('content')
        content.attrib['url'] = srcDirPath
        sourceFolder = ET.Element('sourceFolder')
        sourceFolder.attrib['isTestSource'] = 'false'
        sourceFolder.attrib['url'] = srcDirPath
        content.append(sourceFolder)
        componentElement.append(content)

    def _addIML_LibraryName(self, libraryName, componentElement):

        orderEntry = ET.Element('orderEntry')
        orderEntry.attrib['type'] = 'library'
        orderEntry.attrib['level'] = 'project'
        orderEntry.attrib['name'] = libraryName
        componentElement.append(orderEntry)


def removeIdeaProject(project_root_dir):
     # clean up
    shutil.rmtree(path=os.path.join(project_root_dir, '.idea'), ignore_errors=True)
    filename=os.path.join(project_root_dir, PROJECT_NAME + '.iml')
    if os.path.exists(filename):
        os.remove(filename)

def dumpXml(element):
    return '\n'.join([line for line in MD.parseString(ET.tostring(element)).toprettyxml(indent=' '*2).split('\n') if line.strip()])


def writeFile(filename, contents):
    dir = os.path.dirname(filename)
    try:
        os.makedirs(name=dir)
    except:
        pass

    f = open(filename, 'w+')
    f.write(contents)
    f.close()

if __name__ == "__main__":

    baseDir = '~/minecrift-public-1710'
    mcpDirName = 'mcp908'
    version = '1.7.10'
    is32 = False
    createIdeaProject(project_root_dir=baseDir, mcpDirName=mcpDirName, version=version, are32bitNatives=is32)