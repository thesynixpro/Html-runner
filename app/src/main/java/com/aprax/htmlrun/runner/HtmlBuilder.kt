package com.aprax.htmlrun.runner

/**
 * Builds the document that is shown in the preview WebView.
 * The HTML tab can hold either a fragment or a complete document; in both cases
 * the CSS and JS tabs are injected at the right place and the console bridge is
 * installed before any user script runs.
 */
object HtmlBuilder {

    private val bridgeScript = """
        (function () {
            var post = function (level, args) {
                try {
                    var out = [];
                    for (var i = 0; i < args.length; i++) out.push(format(args[i]));
                    var text = out.join(' ');
                    if (typeof AndroidRunner !== 'undefined') AndroidRunner.post(level, text);
                } catch (error) {}
            };
            function format(value) {
                try {
                    if (typeof value === 'string') return value;
                    if (value instanceof Error) return value.stack || (value.name + ': ' + value.message);
                    if (value === undefined) return 'undefined';
                    if (value === null) return 'null';
                    if (typeof value === 'function') return value.toString().split('\n')[0] + ' ...';
                    if (typeof value === 'object') {
                        var seen = [];
                        var json = JSON.stringify(value, function (key, item) {
                            if (typeof item === 'object' && item !== null) {
                                if (seen.indexOf(item) > -1) return '[Circular]';
                                seen.push(item);
                            }
                            if (typeof item === 'function') return '[Function]';
                            return item;
                        }, 2);
                        return json === undefined ? String(value) : json;
                    }
                    return String(value);
                } catch (error) {
                    return String(value);
                }
            }
            var methods = ['log', 'info', 'warn', 'error', 'debug'];
            for (var i = 0; i < methods.length; i++) {
                (function (name) {
                    var original = console[name];
                    console[name] = function () {
                        post(name === 'debug' ? 'log' : name, Array.prototype.slice.call(arguments));
                        try { if (original) original.apply(console, arguments); } catch (error) {}
                    };
                })(methods[i]);
            }
            window.addEventListener('error', function (event) {
                var message = event.message || 'Script error';
                if (event.filename) message += ' (' + event.filename + ':' + event.lineno + ')';
                post('error', [message]);
            });
            window.addEventListener('unhandledrejection', function (event) {
                var reason = event.reason;
                post('error', ['Unhandled promise rejection: ' + (reason && reason.stack ? reason.stack : reason)]);
            });
        })();
    """.trimIndent()

    fun build(html: String, css: String, js: String): String {
        val styleTag = if (css.isBlank()) "" else "<style>\n$css\n</style>"
        val scriptTag = if (js.isBlank()) "" else "<script>\n$js\n</script>"
        val bridgeTag = "<script>\n$bridgeScript\n</script>"
        val isFullDocument = FULL_DOCUMENT.containsMatchIn(html)

        if (!isFullDocument) {
            return buildString {
                append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n")
                append(bridgeTag).append('\n')
                if (styleTag.isNotEmpty()) append(styleTag).append('\n')
                append("</head>\n<body>\n")
                append(html).append('\n')
                if (scriptTag.isNotEmpty()) append(scriptTag).append('\n')
                append("</body>\n</html>")
            }
        }

        var document = html
        document = if (HEAD.containsMatchIn(document)) {
            document.replace(HEAD) { it.value + "\n" + bridgeTag }
        } else if (HTML_TAG.containsMatchIn(document)) {
            document.replace(HTML_TAG) { it.value + "\n" + bridgeTag }
        } else {
            bridgeTag + "\n" + document
        }

        if (styleTag.isNotEmpty()) {
            document = when {
                HEAD_CLOSE.containsMatchIn(document) -> document.replace(HEAD_CLOSE) { styleTag + "\n" + it.value }
                HEAD.containsMatchIn(document) -> document.replace(HEAD) { it.value + "\n" + styleTag }
                HTML_TAG.containsMatchIn(document) -> document.replace(HTML_TAG) { it.value + "\n" + styleTag }
                else -> styleTag + "\n" + document
            }
        }

        if (scriptTag.isNotEmpty()) {
            document = when {
                BODY_CLOSE.containsMatchIn(document) -> document.replace(BODY_CLOSE) { scriptTag + "\n" + it.value }
                BODY_TAG.containsMatchIn(document) ->
                    document.replace(BODY_TAG) { it.value + "\n" + scriptTag }
                else -> document + "\n" + scriptTag
            }
        }

        return document
    }

    private val FULL_DOCUMENT = Regex("""<!doctype\s+html|<html[\s>]""", RegexOption.IGNORE_CASE)
    private val HEAD = Regex("""<head[^>]*>""", RegexOption.IGNORE_CASE)
    private val HEAD_CLOSE = Regex("""</head\s*>""", RegexOption.IGNORE_CASE)
    private val HTML_TAG = Regex("""<html[^>]*>""", RegexOption.IGNORE_CASE)
    private val BODY_TAG = Regex("""<body[^>]*>""", RegexOption.IGNORE_CASE)
    private val BODY_CLOSE = Regex("""</body\s*>""", RegexOption.IGNORE_CASE)
}