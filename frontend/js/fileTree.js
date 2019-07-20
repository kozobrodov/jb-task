(function ( $ ) {

    /**
     * Implementation of file data tree which
     * uses index map (from file path to tree node)
     * for faster and easier access to tree nodes
     * by file path.
     */
    var getIndexedFileDataTree = function(root) {
        function IndexedFileDataTree() {
            this.root = root;

            // Index tree for easier access to nodes by path
            var pathToNodeIndex = {}
            function index(node) {
                pathToNodeIndex[node.fileData.path] = node;
                if (node.fileData.expandable)
                    node.children.forEach(function(e) {
                        index(e);
                    });
            }
            function removeFromIndex(node) {
                delete pathToNodeIndex[node.fileData.path];
                if (node.fileData.expandable)
                    node.children.forEach(function(e) {
                        removeFromIndex(e);
                    });
            }
            index(this.root);

            // Define member functions

            /**
             * Get node by file path
             */
            this.get = function(path) {
                var node = pathToNodeIndex[path];
                if (node == null) {
                    console.error("No node was found by path: " + path);
                    return null;
                }
                return node;
            }

            /**
             * Add node to node with specific file path
             */
            this.add = function(parentPath, node) {
                index(node);
                pathToNodeIndex[parentPath].children.push(node);
            }

            /**
             * Replace all subnodes of node with specific
             * file path
             */
            this.set = function(parentPath, nodes) {
                var node = get(parentPath);
                if (node.fileData.expandable)
                    node.children.forEach(removeFromIndex);
                node.children = nodes;
                nodes.forEach(index);
            }

            /**
             * Remove all subnodes of node with specific
             * file path
             */
            this.clear = function(parentPath) {
                var node = get(parentPath);
                if (node.fileData.expandable)
                    node.children.forEach(removeFromIndex);
                node.children = [];
            }
        }
        return new IndexedFileDataTree();
    }

    /**
     * State holder which uses `window.localStorage` to
     * store current UI state (see `defaultConfig` for
     * details)
     */
    var getLocalStorageStateHolder = function() {
        function LocalStorageStateHolder() {
            // Init root node
            function initState() {
                var rootNode = {
                    fileData: {path: '', type: 'directory', isExpandable: true},
                    children: []
                };
                localStorage.setItem('ru.kozobrodov.fileTree', JSON.stringify(rootNode));
                return rootNode;
            }
            this.tree = getIndexedFileDataTree(
                JSON.parse(localStorage.getItem('ru.kozobrodov.fileTree')) || initState()
            );

            this.addNodes = function(path, children) {
                this.tree.set(path, children);
            }

            this.clearNode = function(path) {
                this.tree.clear(path);
            }

            this.getCurrentState = function() {
                return this.tree.root;
            }
        }
        return new LocalStorageStateHolder();
    }

    /**
     * Data provider which loads the full directory
     * structures as JSON file
     */
    var getJsonDataProvider = function(jsonPath) {
        function JsonDataProvider() {
            this.load = function(callback) {
                $.ajax({
                    url: jsonPath,
                    dataType: 'json',
                    context : this,
                    success: function (data) {
                        this.tree = getIndexedFileDataTree(data);
                        callback();
                    }
                });
            }

            this.list = function(path) {
                return this.tree.get(path).children;
            }

        }
        return new JsonDataProvider();
    }

    /**
     * Default configuration of the plugin
     */
    var defaultConfig = {
        /**
         * Object which handles changes on file tree state.
         *
         * Must provide the following methods:
         * - `addNodes(parentPath, fileData)` (where `fileData`
         *   is an array of `FileData`(see below)) which is
         *   called when expandable file is opened (expanded)
         * - `clearNode(path)` which is called when expandable
         *   file is closed (collapsed)
         * - `getCurrentState()` which returns tree representing
         *   current state
         *
         * State tree must be built from nodes represented by
         * the following structure:
         *
         * ```
         * {
         *     fileData: {path: "<file_path>", type: "<file_type>", isExpandable: <boolean>},
         *     children: <array_of_subnodes>
         * }
         * ```
         */
        stateHolder: getLocalStorageStateHolder(),

        /**
         * Object which provides loadable data, must provide
         * only one method: `list(path)` which returns array
         * of `FileData` objects:
         *
         * ```
         * {
         *     path: "<file_path>",
         *     type: "<file_type>",
         *     isExpandable: <boolean>
         * }
         * ```
         */
        dataProvider: {},

        /**
         * Map of file MIME type to appropriate icons classes
         */
        typeToIconClassMap: {
            "": ""
        }
    };

    /**
     * Plugin entry point. Defines JQuery function `fileTree`
     * which can be used to initialize tree view on specific
     * DOM element. Optionally, gets configuration which can
     * override default configuration (see `defaultConfig` for
     * details).
     */
    $.fn.fileTree = function(config) {
        var settings = $.extend(defaultConfig, config);
        if (typeof settings.jsonLocation !== 'undefined') {
            settings.dataProvider = getJsonDataProvider(settings.jsonLocation);
        }
        return this.each(function() {
            settings.dataProvider.load(function() {
                // //todo: do something here
            });
        });
    }

    // Init:
    //      Load state
    //      If state exist:
    //          render existing state (node by node)
    //      else
    //          Get data for root
    //          Render it
    //          Update state
    //
    // Keeping state
    //
    // Getting data (and resetting it)
    //
    // Rendering - node or subtree
}( jQuery ));