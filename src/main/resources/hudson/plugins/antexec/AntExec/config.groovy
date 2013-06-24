/*
 * The MIT License
 *
 * Copyright (c) 2011, Milos Svasek
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

package hudson.plugins.antexec.AntExec
f = namespace(lib.FormTagLib)

f.entry(title: _("Script source"), field: "scriptSource", class: "fixed-width", description: _("Source description", rootURL)) {
    f.textarea()
}
if (descriptor.installations.length != 0) {
    f.entry(title: _("Ant Version")) {
        select(class: "setting-input", name: "instance.antName") {
            option(value: "(Default)", _("Default"))
            descriptor.installations.each {
                f.option(selected: it.name == instance?.antName, value: it.name, it.name)
            }
        }
    }
}
f.advanced {
    f.entry(title: _("Script name"), field: "scriptName", description: _("ScriptName description")) {
        f.expandableTextbox()
    }
    f.entry(title: _("Keep buildfile"), field: "keepBuildfile") {
        f.checkbox()
        f.description {_("Do not delete build file so you can use it in one of the next build step again.")}
    }
    f.entry(title: _("Extended script source"), field: "extendedScriptSource", class: "fixed-width", description: _("Extended source description")) {
        f.textarea()
    }
    f.entry(title: _("Properties"), field: "properties", class: "fixed-width") {
        f.expandableTextbox()
    }
    f.advanced {
        f.entry(title: _("Java Options"), field: "antOpts") {
            f.expandableTextbox()
        }
        f.entry(title: _("Verbose mode"), field: "verbose") {
            f.checkbox()
            f.description {_("Enabling verbose output")}
        }
        f.entry(title: _("Emacs mode"), field: "emacs") {
            f.checkbox()
            f.description {_("Enabling logging information without adornments")}
        }
        f.entry(title: _("Do not use Ant-Contrib"), field: "noAntcontrib") {
            f.checkbox()
            f.description {_("Disabling usage of Ant-Contrib Tasks in this build step.")}
        }
    }
}