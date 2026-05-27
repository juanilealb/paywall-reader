package com.juani.paywallreader.data.capture

import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleCaptureScriptsTest {
    @Test
    fun `capture script uses Defuddle as primary extraction engine`() {
        val script = ArticleCaptureScripts.CAPTURE_SCRIPT

        assertTrue(script.contains("var captureDocument = document.cloneNode(true)"))
        assertTrue(script.contains("new window.Defuddle(captureDocument"))
        assertTrue(script.contains("extractionEngine: 'defuddle'"))
        assertTrue(script.contains("contentMarkdown"))
    }

    @Test
    fun `defuddle bootstrap installs bundled Defuddle only when page has no Defuddle global`() {
        val script = ArticleCaptureScripts.defuddleBootstrap("window.Defuddle = function Defuddle() {};")

        assertTrue(script.contains("if (!window.Defuddle)"))
        assertTrue(script.contains("window.Defuddle = function Defuddle() {};"))
    }
}
