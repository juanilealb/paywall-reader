package com.juani.paywallreader.data.capture

internal const val DEFUDDLE_ASSET_PATH = "defuddle/defuddle-0.18.1.js"

internal object ArticleCaptureScripts {
    fun defuddleBootstrap(defuddleBundle: String): String = """
        (function() {
            if (!window.Defuddle) {
                $defuddleBundle
            }
        })();
    """.trimIndent()

    val CAPTURE_SCRIPT: String = """
        (function() {
            function meta(name) {
                var node = document.querySelector('meta[name="' + name + '"], meta[property="' + name + '"]');
                return node ? (node.getAttribute('content') || '') : '';
            }

            function textFromHtml(html) {
                var container = document.createElement('div');
                container.innerHTML = html || '';
                return (container.innerText || container.textContent || '').trim();
            }

            function htmlToBasicMarkdown(html, title, url) {
                var container = document.createElement('div');
                container.innerHTML = html || '';
                var blocks = [];
                Array.prototype.slice.call(container.querySelectorAll('script, style, noscript, svg')).forEach(function(node) {
                    node.remove();
                });
                Array.prototype.slice.call(container.querySelectorAll('h1,h2,h3,h4,h5,h6,p,blockquote,li,pre,figcaption')).forEach(function(node) {
                    var text = (node.innerText || node.textContent || '').replace(/\s+/g, ' ').trim();
                    if (!text) return;
                    var name = node.nodeName.toLowerCase();
                    if (name === 'h1') blocks.push('# ' + text);
                    else if (name === 'h2') blocks.push('## ' + text);
                    else if (name === 'h3') blocks.push('### ' + text);
                    else if (name === 'blockquote') blocks.push('> ' + text);
                    else if (name === 'li') blocks.push('- ' + text);
                    else if (name === 'pre') blocks.push('```\n' + (node.innerText || node.textContent || '').trim() + '\n```');
                    else blocks.push(text);
                });
                if (!blocks.length) {
                    var fallbackText = textFromHtml(html);
                    blocks = fallbackText ? fallbackText.split(/\n{2,}/).map(function(text) { return text.trim(); }).filter(Boolean) : [];
                }
                return ['# ' + ((title || '').trim() || url || location.href), '', 'Source: ' + (url || location.href), ''].concat(blocks).join('\n\n').trim();
            }

            function fallbackPayload(engine, errorMessage) {
                var article = document.querySelector('article') || document.querySelector('main') || document.body;
                var html = article ? article.outerHTML : (document.documentElement ? document.documentElement.outerHTML : '');
                var title = meta('og:title') || document.title || '';
                return {
                    extractionEngine: engine,
                    extractionError: errorMessage || '',
                    title: title,
                    author: meta('author') || meta('article:author') || '',
                    excerpt: meta('description') || meta('og:description') || '',
                    imageUrl: meta('og:image') || '',
                    html: html,
                    text: article ? (article.innerText || article.textContent || '') : (document.body ? (document.body.innerText || document.body.textContent || '') : ''),
                    markdown: htmlToBasicMarkdown(html, title, location.href)
                };
            }

            try {
                if (window.Defuddle) {
                    var captureDocument = document.cloneNode(true);
                    var result = new window.Defuddle(captureDocument, {
                        url: location.href,
                        separateMarkdown: true,
                        markdown: false,
                        useAsync: false
                    }).parse();
                    var contentHtml = result.content || '';
                    var markdown = result.contentMarkdown || htmlToBasicMarkdown(contentHtml, result.title, location.href);
                    return JSON.stringify({
                        extractionEngine: 'defuddle',
                        extractionError: '',
                        title: result.title || meta('og:title') || document.title || '',
                        author: result.author || meta('author') || meta('article:author') || '',
                        excerpt: result.description || meta('description') || meta('og:description') || '',
                        imageUrl: result.image || meta('og:image') || '',
                        html: contentHtml,
                        text: textFromHtml(contentHtml) || (document.body ? (document.body.innerText || '') : ''),
                        markdown: markdown
                    });
                }
            } catch (error) {
                return JSON.stringify(fallbackPayload('basic-after-defuddle-error', error && error.message ? error.message : String(error)));
            }

            return JSON.stringify(fallbackPayload('basic-no-defuddle', 'Defuddle global was not installed'));
        })();
    """.trimIndent()
}
