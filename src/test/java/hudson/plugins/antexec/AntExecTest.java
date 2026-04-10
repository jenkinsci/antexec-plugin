package hudson.plugins.antexec;

import hudson.util.FormValidation;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.*;

@WithJenkins
class AntExecTest {

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
}
