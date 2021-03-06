##The module name must match the value of the rename-to attribute from Rt.gwt.xml
#set($moduleName = "xre")

/**
 * XWiki's custom RT editor controller.
 * Usage: \$xwiki.jsfx.use("path/to/XWikiRt.js", {'forceSkinAction': true, 'lazy': false})
 *
 * @type object
 * @param lazy {@code true} if you want to load the RT code on demand, {@code false} if you want to load the
 *            RT code when the page is loaded
 */
var Rt =
{
    /**
     * Indicates the state of the RT GWT module. Possible values are: 0 (uninitialized), 1 (loading), 2 (loaded).
     */
    readyState: 0,

    /**
     * The queue of functions to execute after the RT module is loaded.
     */
    onModuleLoadQueue: [],

    /**
     * All the RT editor instances, mapped to their hookId.
     */
    instances: {},

    /**
     * Loads the RT JS code on demand(which is Rt.onModuleLoad() ).
     */
    load : function() {
        // Test if the code has been already loaded.
        // GWT loads the RT code in an in-line frame with the 'com.xpn.xwiki.wysiwyg.Wysiwyg' id.
        if (document.getElementById('${moduleName}') || this.readyState != 0) {
            return;
        }

        // Start loading the RT GWT module.
        this.readyState = 1;

        // Create the script tag to be used for importing the GWT script loader.
        var script = document.createElement('script');
        script.type = 'text/javascript';
        script.src = '$xwiki.getSkinFile("js/xwiki/rte/${moduleName}/${moduleName}.nocache.js")';

        // The default GWT script loader calls document.write() twice which prevents us from loading the WYSIWYG code
        // on demand, after the document has been loaded. To overcome this we have to overwrite the document.write()
        // method before the GWT script loader is executed and restore it after.
        // NOTE: The GWT script loader uses document.write() to compute the URL from where it is loaded.
        var counter = 0;
        var limit = 2;
        var oldWrite = document.write;
        var newWrite = function(html) {
            if (counter < limit) {
                counter++;
                // Try to wrap onScriptLoad in order to be notified when the WYSIWYG script is loaded.
                Rt.maybeHookOnScriptLoad();
                // Fail silently if the script element hasn't been attached to the document.
                if (!script.parentNode) {
                    return;
                }
                // Create a DIV and put the HTML inside.
                var div = document.createElement('div');
                // We have to replace all the script tags because otherwise IE drops them.
                div.innerHTML = html.replace(/<script\b([\s\S]*?)<\/script>/gi, "<pre script=\"script\"$1</pre>");
                // Move DIV contents after the GWT script loader.
                var nextSibling = script.nextSibling;
                while (div.firstChild) {
                    var child = div.firstChild;
                    // Recover the script tags.
                    if (child.nodeName.toLowerCase() == 'pre' && child.getAttribute('script') == 'script') {
                        var pre = child;
                        pre.removeAttribute('script');
                        // Create the script tag.
                        child = document.createElement('script');
                        // Copy all the attributes.
                        for (var i = 0; i < pre.attributes.length; i++) {
                            var attrNode = pre.attributes[i];
                            // In case of IE we have to copy only the specified attributes.
                            if (typeof attrNode.specified == 'undefined'
                                    || (typeof attrNode.specified == 'boolean' && attrNode.specified)) {
                                child.setAttribute(attrNode.nodeName, attrNode.nodeValue);
                            }
                        }
                        // Copy the script text.
                        child.text = typeof pre.innerText == 'undefined' ? pre.textContent : pre.innerText;
                        // Don't forget to remove the placeholder.
                        div.removeChild(pre);
                    }
                    if (nextSibling) {
                        script.parentNode.insertBefore(child, nextSibling);
                    } else {
                        script.parentNode.appendChild(child);
                    }
                }
            }
            if (counter >= limit) {
                document.write = oldWrite;
                oldWrite = undefined;
                script = undefined;
                counter = undefined;
            }
        }

        // Append the script tag to the head.
        var heads = document.getElementsByTagName('head');
        if (heads.length > 0) {
            document.write = newWrite;
            heads[0].appendChild(script);
        }
    }
    ,

    /**
     * Schedules a function to be executed after the RT module is loaded.
     * A call to this method forces the RT
     * module to be loaded, unless the second parameter, {@code lazy}, is set to {@code true}.
     *
     * @param fCode a function
     * @param lazy {@code true} to prevent loading the WYSIWYG module at this point, {@code false} otherwise
     */
    onModuleLoad: function(fCode, lazy) {
        if (typeof fCode != 'function') {
            return;
        }
        switch (this.readyState) {
            // uninitialized
            case 0:
                if (!lazy) {
                    this.load();
                }
            // fall-through

            // loading
            case 1:
                this.onModuleLoadQueue.push(fCode);
                break;

            // loaded
            case 2:
                fCode();
                break;
        }
    },

    /**
     * Executes all the functions scheduled from on module load.
     */
    fireOnModuleLoad: function() {
        // The WYSIWYG module has been loaded successfully.
        this.readyState = 2;

        // Execute all the scheduled functions.
        for (var i = 0; i < this.onModuleLoadQueue.length; i++) {
            this.onModuleLoadQueue[i]();
        }

        // There's no need to schedule functions anymore. They will be execute immediately.
        this.onModuleLoadQueue = undefined;
    },

    /**
     * Try to wrap onScriptLoad in order to be notified when the WYSIWYG script is loaded.
     */
    maybeHookOnScriptLoad: function() {
        if (${moduleName} && ${moduleName}.onScriptLoad)
        {
            var onScriptLoad = ${moduleName}.onScriptLoad;
            ${moduleName}.onScriptLoad = function() {
                Rt.hookGwtOnLoad();
                onScriptLoad();

                // Restore the default onScriptLoad function.
                if (${moduleName} && ${moduleName}.onScriptLoad)
                {
                    ${moduleName}.onScriptLoad = onScriptLoad;
                }
                onScriptLoad = undefined;
            }

            // Prevent further calls to this method.
            this.maybeHookOnScriptLoad = function() {};
        }
    },

    /**
     * Wrap gwtOnLoad in order to be notified when the WYSIWYG module is loaded.
     */
    hookGwtOnLoad: function() {
        var iframe = document.getElementById('${moduleName}');
        var gwtOnLoad = iframe.contentWindow.gwtOnLoad;
        iframe.contentWindow.gwtOnLoad = function(errFn, modName, modBase) {
            gwtOnLoad(function() {
                Rt.fireOnModuleLoad = function() {
                };
                if (typeof errFn == 'function') {
                    errFn();
                }
            }, modName, modBase);
            Rt.fireOnModuleLoad();

            // Restore the default gwtOnLoad function.
            iframe.contentWindow.gwtOnLoad = gwtOnLoad;
            iframe = undefined;
            gwtOnLoad = undefined;
        }

        // Prevent further calls to this method.
        this.hookGwtOnLoad = function() {};
    },

    /**
     * @return the RT editor instance associated with the given hookId
     */
    getInstance: function(hookId) {
        return this.instances[hookId];
    }
};

#if ("$!request.lazy" != "true")
    Rt.load();
#end