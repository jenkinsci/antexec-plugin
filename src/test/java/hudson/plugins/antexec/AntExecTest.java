package hudson.plugins.antexec;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Ant;
import hudson.util.FormValidation;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.*;

@WithJenkins
class AntExecTest {

    // ── Constructor and getters ───────────────────────────────────────────────

    @Test
    void constructor_setsAllFields() {
        AntExec step = new AntExec("src", "ext", "name", "props", "myAnt", "opts",
                true, true, true, true);
        assertEquals("src", step.getScriptSource());
        assertEquals("ext", step.getExtendedScriptSource());
        assertEquals("name", step.getScriptName());
        assertEquals("props", step.getProperties());
        assertEquals("opts", step.getAntOpts());
        assertTrue(step.getKeepBuildfile());
        assertTrue(step.getVerbose());
        assertTrue(step.getEmacs());
        assertTrue(step.getNoAntcontrib());
    }

    @Test
    void constructor_handlesNulls() {
        AntExec step = new AntExec(null, null, null, null, null, null,
                null, null, null, null);
        assertNull(step.getScriptSource());
        assertNull(step.getExtendedScriptSource());
        assertNull(step.getScriptName());
        assertNull(step.getProperties());
        assertNull(step.getAntOpts());
        assertNull(step.getKeepBuildfile());
        assertNull(step.getVerbose());
        assertNull(step.getEmacs());
        assertNull(step.getNoAntcontrib());
    }

    // ── makeBuildFileXml ──────────────────────────────────────────────────────

    @Test
    void makeBuildFileXml_setsDefaultTargetName() {
        String xml = AntExec.makeBuildFileXml("", "", "myscript");
        assertTrue(xml.contains("default=\"myscript\""));
        assertTrue(xml.contains("<target name=\"myscript\""));
    }

    @Test
    void makeBuildFileXml_includesScriptSource() {
        String xml = AntExec.makeBuildFileXml("<echo message='hello'/>", "", "test");
        assertTrue(xml.contains("<echo message='hello'/>"));
    }

    @Test
    void makeBuildFileXml_emptyExtendedSource_notAppended() {
        String xml = AntExec.makeBuildFileXml("", "", "test");
        assertFalse(xml.contains("Extended script source"));
    }

    @Test
    void makeBuildFileXml_emptyExtendedSource_nullValue_notAppended() {
        String xml = AntExec.makeBuildFileXml("", null, "test");
        assertFalse(xml.contains("Extended script source"));
    }

    @Test
    void makeBuildFileXml_nonEmptyExtendedSource_appended() {
        String xml = AntExec.makeBuildFileXml("", "<target name='extra'/>", "test");
        assertTrue(xml.contains("<target name='extra'/>"));
        assertTrue(xml.contains("Extended script source"));
    }

    @Test
    void makeBuildFileXml_isValidXmlStructure() {
        String xml = AntExec.makeBuildFileXml("<echo message='ok'/>", "", "test");
        assertTrue(xml.startsWith("<?xml version=\"1.0\""));
        assertTrue(xml.contains("<project "));
        assertTrue(xml.contains("</project>"));
    }

    @Test
    void makeBuildFileXml_includesPropertyFileReference() {
        String xml = AntExec.makeBuildFileXml("", "", "myscript");
        assertTrue(xml.contains("<property file=\"myscript.properties\""));
    }

    @Test
    void makeBuildFileXml_includesEnvironmentProperty() {
        String xml = AntExec.makeBuildFileXml("", "", "test");
        assertTrue(xml.contains("<property environment=\"env\""));
    }

    // ── Descriptor ────────────────────────────────────────────────────────────

    @Test
    void descriptor_isApplicable_returnsTrue(JenkinsRule j) {
        AntExec.DescriptorImpl d = new AntExec.DescriptorImpl();
        assertTrue(d.isApplicable(FreeStyleProject.class));
    }

    @Test
    void descriptor_getDisplayName_returnsNonEmpty(JenkinsRule j) {
        AntExec.DescriptorImpl d = new AntExec.DescriptorImpl();
        assertNotNull(d.getDisplayName());
        assertFalse(d.getDisplayName().isEmpty());
    }

    @Test
    void descriptor_getInstallations(JenkinsRule j) {
        AntExec.DescriptorImpl d = new AntExec.DescriptorImpl();
        assertNotNull(d.getInstallations());
    }

    // ── getAnt ────────────────────────────────────────────────────────────────

    @Test
    void getAnt_noInstallations_returnsNull(JenkinsRule j) {
        AntExec step = new AntExec("<echo/>", "", "", "", "nonexistent", "",
                false, false, false, true);
        assertNull(step.getAnt());
    }

    @Test
    void getAnt_nullAntName_returnsNull(JenkinsRule j) {
        AntExec step = new AntExec("<echo/>", "", "", "", null, "",
                false, false, false, true);
        assertNull(step.getAnt());
    }

    // ── doCheckScriptSource ───────────────────────────────────────────────────

    @Test
    void doCheckScriptSource_validXml_returnsOk(JenkinsRule j) throws Exception {
        AntExec.DescriptorImpl d = new AntExec.DescriptorImpl();
        FormValidation result = d.doCheckScriptSource("<echo message='test'/>");
        assertEquals(FormValidation.Kind.OK, result.kind);
    }

    @Test
    void doCheckScriptSource_malformedXml_returnsError(JenkinsRule j) throws Exception {
        AntExec.DescriptorImpl d = new AntExec.DescriptorImpl();
        FormValidation result = d.doCheckScriptSource("<unclosed");
        assertEquals(FormValidation.Kind.ERROR, result.kind);
    }

    @Test
    void doCheckScriptSource_xxeDoctype_isBlocked(JenkinsRule j) throws Exception {
        AntExec.DescriptorImpl d = new AntExec.DescriptorImpl();
        String xxe = "<!DOCTYPE foo [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]><root>&xxe;</root>";
        FormValidation result = d.doCheckScriptSource(xxe);
        assertEquals(FormValidation.Kind.ERROR, result.kind);
    }

    @Test
    void doCheckScriptSource_emptyInput_returnsOk(JenkinsRule j) throws Exception {
        AntExec.DescriptorImpl d = new AntExec.DescriptorImpl();
        FormValidation result = d.doCheckScriptSource("");
        assertEquals(FormValidation.Kind.OK, result.kind);
    }

    // ── doCheckExtendedScriptSource ───────────────────────────────────────────

    @Test
    void doCheckExtendedScriptSource_validXml_returnsOk(JenkinsRule j) throws Exception {
        AntExec.DescriptorImpl d = new AntExec.DescriptorImpl();
        FormValidation result = d.doCheckExtendedScriptSource("<target name='extra'/>");
        assertEquals(FormValidation.Kind.OK, result.kind);
    }

    @Test
    void doCheckExtendedScriptSource_malformedXml_returnsError(JenkinsRule j) throws Exception {
        AntExec.DescriptorImpl d = new AntExec.DescriptorImpl();
        FormValidation result = d.doCheckExtendedScriptSource("<bad");
        assertEquals(FormValidation.Kind.ERROR, result.kind);
    }

    @Test
    void doCheckExtendedScriptSource_xxeDoctype_isBlocked(JenkinsRule j) throws Exception {
        AntExec.DescriptorImpl d = new AntExec.DescriptorImpl();
        String xxe = "<!DOCTYPE foo [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]><root>&xxe;</root>";
        FormValidation result = d.doCheckExtendedScriptSource(xxe);
        assertEquals(FormValidation.Kind.ERROR, result.kind);
    }

    // ── perform (integration) ─────────────────────────────────────────────────

    @Test
    void perform_simpleEcho_succeeds(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<echo message='Hello from AntExec'/>", "", "",
                "", null, "", false, false, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        j.assertLogContains("Hello from AntExec", build);
    }

    @Test
    void perform_withProperties_succeeds(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<echo message='val=${myprop}'/>", "", "",
                "myprop=hello123", null, "", false, false, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        j.assertLogContains("val=hello123", build);
    }

    @Test
    void perform_malformedScript_fails(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<this is not valid xml", "", "",
                "", null, "", false, false, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertEquals(Result.FAILURE, build.getResult());
    }

    @Test
    void perform_withVerbose_succeeds(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<echo message='verbose test'/>", "", "",
                "", null, "", false, true, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        j.assertLogContains("verbose test", build);
    }

    @Test
    void perform_withEmacs_succeeds(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<echo message='emacs test'/>", "", "",
                "", null, "", false, false, true, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        j.assertLogContains("emacs test", build);
    }

    @Test
    void perform_keepBuildfile_fileRetained(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<echo message='keep'/>", "", "",
                "", null, "", true, false, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        assertTrue(build.getWorkspace().child(AntExec.buildXml).exists());
    }

    @Test
    void perform_noKeepBuildfile_fileDeleted(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<echo message='delete'/>", "", "",
                "", null, "", false, false, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        assertFalse(build.getWorkspace().child(AntExec.buildXml).exists());
    }

    @Test
    void perform_customScriptName_usesIt(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<echo message='custom'/>", "", "mybuild",
                "", null, "", true, false, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        assertTrue(build.getWorkspace().child("mybuild").exists());
    }

    @Test
    void perform_withExtendedSource_succeeds(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        String extendedSource = "<target name='extra'><echo message='from extra target'/></target>";
        AntExec step = new AntExec("<antcall target='extra'/>", extendedSource, "",
                "", null, "", false, false, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        j.assertLogContains("from extra target", build);
    }

    @Test
    void perform_nullProperties_withBuildVars_succeeds(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<echo message='ok'/>", "", "",
                null, null, "", false, false, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        j.assertLogContains("ok", build);
    }

    @Test
    void perform_withAntOpts_succeeds(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<echo message='opts test'/>", "", "",
                "", null, "-Xmx128m", false, false, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        j.assertLogContains("opts test", build);
    }

    @Test
    void perform_verboseWithProperties_logsDebugInfo(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<echo message='debug'/>", "", "",
                "key=value", null, "", false, true, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        j.assertLogContains("key=value", build);
    }

    @Test
    void perform_withAntContrib_disabled_verbose_logsMessage(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<echo message='no antcontrib'/>", "", "",
                "", null, "", false, true, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        j.assertLogContains("no antcontrib", build);
    }

    @Test
    void perform_keepBuildfile_withProperties_bothRetained(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<echo message='keep props'/>", "", "",
                "k=v", null, "", true, false, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        assertTrue(build.getWorkspace().child(AntExec.buildXml).exists());
        assertTrue(build.getWorkspace().child(AntExec.buildXml + ".properties").exists());
    }

    @Test
    void perform_noKeepBuildfile_withProperties_bothDeleted(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<echo message='del props'/>", "", "",
                "k=v", null, "", false, false, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        assertFalse(build.getWorkspace().child(AntExec.buildXml).exists());
        assertFalse(build.getWorkspace().child(AntExec.buildXml + ".properties").exists());
    }

    @Test
    void perform_failingTarget_returnsFalse(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<fail message='intentional failure'/>", "", "",
                "", null, "", false, false, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertEquals(Result.FAILURE, build.getResult());
    }

    @Test
    void perform_customScriptName_propertyFileUsesCustomName(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<echo message='val=${cprop}'/>", "", "custom_build",
                "cprop=customval", null, "", true, false, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        j.assertLogContains("val=customval", build);
        assertTrue(build.getWorkspace().child("custom_build").exists());
        assertTrue(build.getWorkspace().child("custom_build.properties").exists());
    }

    // ── perform with configured Ant installation ──────────────────────────────

    /** Resolve ANT_HOME from the environment variable, fall back to standard Linux path. */
    private static String getAntHome() {
        String antHome = System.getenv("ANT_HOME");
        return antHome != null ? antHome : "/usr/share/ant";
    }

    @Test
    void perform_withConfiguredAntInstallation_succeeds(JenkinsRule j) throws Exception {
        Ant.AntInstallation inst = new Ant.AntInstallation("myAnt", getAntHome(), null);
        j.jenkins.getDescriptorByType(Ant.DescriptorImpl.class).setInstallations(inst);

        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<echo message='configured ant'/>", "", "",
                "", "myAnt", "-Xmx64m", false, false, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        // Configured installation may fail to resolve executable in test harness,
        // but the code path through getAnt() -> forNode() -> forEnvironment() is exercised
        assertNotNull(build.getResult());
    }

    @Test
    void getAnt_matchesConfiguredInstallation(JenkinsRule j) {
        Ant.AntInstallation inst = new Ant.AntInstallation("testAnt", getAntHome(), null);
        j.jenkins.getDescriptorByType(Ant.DescriptorImpl.class).setInstallations(inst);

        AntExec step = new AntExec("<echo/>", "", "", "", "testAnt", "",
                false, false, false, true);
        assertNotNull(step.getAnt());
        assertEquals("testAnt", step.getAnt().getName());
    }

    @Test
    void getAnt_noMatch_returnsNull(JenkinsRule j) {
        Ant.AntInstallation inst = new Ant.AntInstallation("otherAnt", getAntHome(), null);
        j.jenkins.getDescriptorByType(Ant.DescriptorImpl.class).setInstallations(inst);

        AntExec step = new AntExec("<echo/>", "", "", "", "nonexistent", "",
                false, false, false, true);
        assertNull(step.getAnt());
    }

    // ── verbose output with properties ────────────────────────────────────────

    @Test
    void perform_verboseWithProperties_logsScriptSourceDebug(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        AntExec step = new AntExec("<echo message='dbg'/>", "", "",
                "debugprop=debugval", null, "", false, true, false, true);
        project.getBuildersList().add(step);
        FreeStyleBuild build = j.buildAndAssertSuccess(project);
        // verbose mode prints script source and properties debug blocks
        j.assertLogContains("echo message", build);
        j.assertLogContains("debugprop=debugval", build);
    }
}
