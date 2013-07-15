/*
 * The MIT License
 *
 * Copyright (c) 2011, Milos Svasek, Kohsuke Kawaguchi, etc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.antexec;

import hudson.*;
import hudson.model.*;
import hudson.tasks.Ant;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks._ant.AntConsoleAnnotator;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.VariableResolver;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Invokes the Apache Ant script entered on the hudson build configuration.
 * <p/>
 *
 * @author Milos Svasek
 */
@SuppressWarnings("ALL")
public class AntExec extends Builder {
    private static final String myName = "antexec";
    protected static final String buildXml = myName + "_build.xml";
    private final String scriptSource;
    private final String extendedScriptSource;
    private final String scriptName;
    private final String properties;
    private final String antOpts;
    private final Boolean keepBuildfile;
    private final Boolean verbose;
    private final Boolean emacs;
    private final Boolean noAntcontrib;
    private final String antName;

    // Fields in config.groovy must match the parameter names in the "DataBoundConstructor"
    @SuppressWarnings("ALL")
    @DataBoundConstructor
    public AntExec(String scriptSource, String extendedScriptSource, String scriptName, String properties, String antName, String antOpts, Boolean keepBuildfile, Boolean verbose, Boolean emacs, Boolean noAntcontrib) {
        this.scriptSource = scriptSource;
        this.extendedScriptSource = extendedScriptSource;
        this.scriptName = scriptName;
        this.properties = properties;
        this.keepBuildfile = keepBuildfile;
        this.antName = antName;
        this.antOpts = antOpts;
        this.verbose = verbose;
        this.emacs = emacs;
        this.noAntcontrib = noAntcontrib;
    }

    /**
     * Returns content of text area with script source from job configuration screen
     *
     * @return String scriptSource
     */
    public String getScriptSource() {
        return scriptSource;
    }

    /**
     * Returns content of text area with script source from job configuration screen
     *
     * @return String extendedScriptSource
     */
    public String getExtendedScriptSource() {
        return extendedScriptSource;
    }


    /**
     * Returns content of text area with script name from job configuration screen
     *
     * @return String scriptName
     */
    public String getScriptName() {
        return scriptName;
    }

    /**
     * Returns content of text field with properties from job configuration screen
     *
     * @return String properties
     */
    public String getProperties() {
        return properties;
    }

    /**
     * Returns content of text field with java/ant options from job configuration screen.
     * It will be used for ANT_OPTS environment variable
     *
     * @return String antOpts
     */
    public String getAntOpts() {
        return antOpts;
    }

    /**
     * Returns checkbox boolean from job configuration screen
     *
     * @return Boolean keepBuildfile
     */
    public Boolean getKeepBuildfile() {
        return keepBuildfile;
    }

    /**
     * Returns checkbox boolean from job configuration screen
     *
     * @return Boolean verbose
     */
    public Boolean getVerbose() {
        return verbose;
    }

    /**
     * Returns checkbox boolean from job configuration screen
     *
     * @return Boolean emacs
     */
    public Boolean getEmacs() {
        return emacs;
    }

    /**
     * Returns checkbox boolean from job configuration screen
     *
     * @return Boolean noAntcontrib
     */
    public Boolean getNoAntcontrib() {
        return noAntcontrib;
    }

    /**
     * @return Ant to invoke, or null to invoke the default one.
     */
    Ant.AntInstallation getAnt() {
        for (Ant.AntInstallation i : getDescriptor().getInstallations()) {
            if (antName != null && antName.equals(i.getName()))
                return i;
        }
        return null;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        ArgumentListBuilder args = new ArgumentListBuilder();
        String scriptSourceResolved = scriptSource;
        String extendedScriptSourceResolved = extendedScriptSource;
        try {
            //Resolve all the envirionment variables and properties before creating the build.xml
            scriptSourceResolved = TokenMacro.expandAll(build, listener, scriptSource);
            extendedScriptSourceResolved = TokenMacro.expandAll(build, listener, extendedScriptSource);
        } catch (MacroEvaluationException ex) {
            Logger.getLogger(AntExec.class.getName()).log(Level.SEVERE, null, ex);
        }
        EnvVars env = build.getEnvironment(listener);
        env.overrideAll(build.getBuildVariables());

        Ant.AntInstallation ai = getAnt();
        if (ai == null) {
            args.add(launcher.isUnix() ? "ant" : "ant.bat");
        } else {
            ai = ai.forNode(Computer.currentComputer().getNode(), listener);
            ai = ai.forEnvironment(env);
            String exe = ai.getExecutable(launcher);
            if (exe == null) {
                return false;
            }
            args.add(exe);
        }


        //Create Ant build.xml file
        FilePath buildFile = makeBuildFile(scriptName, scriptSourceResolved, extendedScriptSourceResolved, build);

        //Make archive copy of build file to job directory
        //buildFile.copyTo(new FilePath(new File(build.getRootDir(), buildXml)));

        //Added build file to the command line
        args.add("-file", buildFile.getName());
        @SuppressWarnings("unchecked") VariableResolver<String> vr = build.getBuildVariableResolver();
        @SuppressWarnings("unchecked") Set<String> sensitiveVars = build.getSensitiveBuildVariables();
        //noinspection unchecked

        //Resolve the properties passed
        args.addKeyValuePairsFromPropertyString("-D", properties, vr, sensitiveVars);

        if (ai != null)
            env.put("ANT_HOME", ai.getHome());
        if (antOpts != null && antOpts.length() > 0 && !antOpts.equals("")) {
            env.put("ANT_OPTS", env.expand(antOpts));
        }

        //Get and prepare ant-contrib.jar
        if (noAntcontrib == null || !noAntcontrib) {
            //TODO: Replace this with better methot
            if (verbose != null && verbose) listener.getLogger().println(Messages.AntExec_UseAntContribTasks());
            FilePath antContribJarOnMaster = new FilePath(Hudson.getInstance().getRootPath(), "plugins/antexec/META-INF/lib/ant-contrib.jar");
            FilePath antLibDir = new FilePath(build.getWorkspace(), "antlib");
            FilePath antContribJar = new FilePath(antLibDir, "ant-contrib.jar");
            antContribJar.copyFrom(antContribJarOnMaster.toURI().toURL());
            args.add("-lib", antLibDir.getName());
        } else {
            if (verbose != null && verbose) listener.getLogger().println(Messages.AntExec_UseAntCoreTasksOnly());
        }

        //Add Ant option: -verbose
        if (verbose != null && verbose) args.add("-verbose");

        //Add Ant option: -emacs
        if (emacs != null && emacs) args.add("-emacs");

        //Fixing command line for windows
        if (!launcher.isUnix()) {
            args = args.toWindowsCommand();
            // For some reason, ant on windows rejects empty parameters but unix does not.
            // Add quotes for any empty parameter values:
            List<String> newArgs = new ArrayList<String>(args.toList());
            newArgs.set(newArgs.size() - 1, newArgs.get(newArgs.size() - 1).replaceAll("(?<= )(-D[^\" ]+)= ", "$1=\"\" "));
            args = new ArgumentListBuilder(newArgs.toArray(new String[newArgs.size()]));
        }

        //Content of scriptSourceResolved and properties (only if verbose is true
        if (verbose != null && verbose) {
            listener.getLogger().println();
            listener.getLogger().println(Messages.AntExec_DebugScriptSourceFieldBegin());
            listener.getLogger().println(scriptSourceResolved);
            listener.getLogger().println(Messages.AntExec_DebugScriptSourceFieldEnd());
            listener.getLogger().println();
            listener.getLogger().println(Messages.AntExec_DebugPropertiesFieldBegin());
            listener.getLogger().println(properties);
            listener.getLogger().println(Messages.AntExec_DebugPropertiesFieldEnd());
            listener.getLogger().println();
        }

        long startTime = System.currentTimeMillis();
        try {
            AntConsoleAnnotator aca = new AntConsoleAnnotator(listener.getLogger(), build.getCharset());
            int r;
            try {
                r = launcher.launch().cmds(args).envs(env).stdout(aca).pwd(buildFile.getParent()).join();
            } finally {
                aca.forceEol();
                //After the ant script has been executed, we delete the build.xml.
                //The plugin is a way to run an Ant Script from a small source code, we shoudn't keep the antexec_build.xml
                if (keepBuildfile == null || !keepBuildfile) {
                    boolean deleteResponse = buildFile.delete();
                    if (!deleteResponse) listener.getLogger().println("The temporary Ant Build Script coudn't be deleted");
                }
            }
            return r == 0;
        } catch (IOException e) {
            Util.displayIOException(e, listener);

            String errorMessage = hudson.tasks.Messages.Ant_ExecFailed();
            if (ai == null && (System.currentTimeMillis() - startTime) < 1000) {
                if (getDescriptor().getInstallations() == null)
                    // looks like the user didn't configure any Ant installation
                    errorMessage += hudson.tasks.Messages.Ant_GlobalConfigNeeded();
                else
                    // There are Ant installations configured but the project didn't pick it
                    errorMessage += hudson.tasks.Messages.Ant_ProjectConfigNeeded();
            }
            e.printStackTrace(listener.fatalError(errorMessage));
            return false;
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @SuppressWarnings("UnusedDeclaration")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @CopyOnWrite
        private volatile Ant.AntInstallation[] installations = new Ant.AntInstallation[0];

        @SuppressWarnings("UnusedDeclaration")
        public DescriptorImpl() {
            super(AntExec.class);
            load();
        }

        // for compatibility reasons, the persistence is done by Ant.DescriptorImpl
        public Ant.AntInstallation[] getInstallations() {
            return Hudson.getInstance().getDescriptorByType(Ant.DescriptorImpl.class).getInstallations();
        }

        //Check if entered script source is wellformed xml document
        public FormValidation doCheckScriptSource(@QueryParameter String value) throws IOException {
            String xmlContent = makeBuildFileXml("", value, "test_script");
            try {
                XMLReader reader = XMLReaderFactory.createXMLReader();
                reader.parse(new InputSource(new ByteArrayInputStream(xmlContent.getBytes())));
                return FormValidation.ok();
            } catch (SAXException sax) {
                return FormValidation.error("ERROR: " + sax.getLocalizedMessage());
            }
        }

        //Check if entered extended script source is wellformed xml document
        private FormValidation doCheckExtendedScriptSource(@QueryParameter String value) throws IOException {
            String xmlContent = makeBuildFileXml(value, "", "test_script");
            try {
                XMLReader reader = XMLReaderFactory.createXMLReader();
                reader.parse(new InputSource(new ByteArrayInputStream(xmlContent.getBytes())));
                return FormValidation.ok();
            } catch (SAXException sax) {
                return FormValidation.error("ERROR: " + sax.getLocalizedMessage());
            }
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // indicates that this builder can be used with all kinds of project types
            return true;
        }

        public String getDisplayName() {
            return Messages.AntExec_DisplayName();
        }
    }

    static String makeBuildFileXml(String scriptSource, String extendedScriptSource, String scriptName) {
        StringBuilder sb = new StringBuilder();
        String myScripName = buildXml;
        if (scriptName != null && scriptName.length() > 0 && !scriptName.equals("")) {
            myScripName = scriptName;
        }
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<project default=\"" + myScripName + "\" xmlns:antcontrib=\"antlib:net.sf.antcontrib\" basedir=\".\">\n\n");
        sb.append("<target name=\"" + myScripName + "\">\n");
        sb.append("<!-- Default target entered in the first textarea - begin -->\n");
        sb.append(scriptSource);
        sb.append("\n<!-- Default target entered in the first textarea -  end  -->\n");
        sb.append("</target>\n");
        if (extendedScriptSource != null && extendedScriptSource.length() > 0 && !extendedScriptSource.equals("")) {
            sb.append("<!-- Extended script source entered in the second textarea - begin -->\n");
            sb.append(extendedScriptSource);
            sb.append("\n<!-- Extended script source entered in the second textarea -  end  -->\n");
        }
        sb.append("</project>\n");
        return sb.toString();
    }

    static FilePath makeBuildFile(String scriptName, String targetSource, String extendedScriptSource, AbstractBuild build) throws IOException, InterruptedException {
        String myScripName = buildXml;
        if (scriptName != null && scriptName.length() > 0 && !scriptName.equals("")) {
            myScripName = scriptName;
        }
        FilePath buildFile = new FilePath(build.getWorkspace(), myScripName);
        buildFile.write(makeBuildFileXml(targetSource, extendedScriptSource, scriptName), null);
        return buildFile;
    }

}